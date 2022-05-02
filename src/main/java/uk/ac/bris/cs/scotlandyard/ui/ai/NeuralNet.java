package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.javatuples.Pair;
import org.tensorflow.*;
import org.tensorflow.ndarray.NdArray;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.StdArrays;
import org.tensorflow.proto.framework.GraphDef;
import org.tensorflow.types.TBool;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TString;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static uk.ac.bris.cs.scotlandyard.ui.ai.Game.INPUTSIZE;
import static uk.ac.bris.cs.scotlandyard.ui.ai.Game.POSSIBLEMOVES;

public class NeuralNet {
    private static final String MRXGRAPH = "mrXGraph.pb";
    private static final String DETGRAPH = "detGraph.pb";
    public static final String MRXCHECKPOINTDIR = "mrXCheckpoints";
    public static final String DETCHECKPOINTDIR = "detCheckpoints";
    private static final String DETLOSSESFOLDER = "losses/mrXLosses/";
    private static final String MRXLOSSESFOLDER = "losses/detLosses/";

    private static final String TOTALLOSSFILE = "totalLosses.txt";
    private static final String PILOSSFILE = "piLosses.txt";
    private static final String VLOSSFILE = "vLosses.txt";
    private static final boolean OVERWRITEPREVNET = false;


    public final boolean isMrX;
    private Session sess;

    public NeuralNet(NeuralNet clone) {
        this.sess = clone.sess;
        this.isMrX = clone.isMrX;
    }
    public NeuralNet(Game game, boolean isMrX) throws IOException{
        this.isMrX = isMrX;
        String path = (game.currentIsMrX) ? MRXGRAPH : DETGRAPH;
        String checkPointDir = (game.currentIsMrX) ? MRXCHECKPOINTDIR : DETCHECKPOINTDIR;
        load_model(path);
        load_checkpoint(checkPointDir);
    }

    public void train(List<TrainingEntry> trainingExamples, float dropout, int batchSize, float lr) throws IOException {
        //todo load data from file (preferably by changing network). If not then in this function
        //this would allow for larger number of games that don't fit in memory

        System.out.printf("training\n");
        List<Float> totalLossList = new ArrayList<>();
        List<Float> piLossList = new ArrayList<>();
        List<Float> vLossList = new ArrayList<>();
        for (int i = 0; i < trainingExamples.size(); i += batchSize) {
            //todo check index is updated
            Tensor input = getBatchedInput(trainingExamples, batchSize, i);
            Tensor expectedPolicy = getBatchedExpectedPolicy(trainingExamples, batchSize, i);
            Tensor expectedValue = getBatchedExpectedValue(trainingExamples, batchSize, i);
            List<Tensor> result = this.sess.runner()
                    .feed("input" ,input)
                    .feed("dropout", TFloat32.scalarOf(dropout))
                    .feed("lr", TFloat32.scalarOf(lr))//learning rate
                    .feed("targetPolicy", expectedPolicy)
                    .feed("targetV", expectedValue)
                    .feed("is_training", TBool.scalarOf(true))
                    .fetch("add:0") //total loss
                    .fetch("softmax_cross_entropy_loss/value:0") //pi loss
                    .fetch("mean_squared_error/value:0") //v loss
                    .addTarget("Adam").run();
            TFloat32 totalLoss = (TFloat32) result.get(0);
            TFloat32 piLoss = (TFloat32) result.get(1);
            TFloat32 vLoss = (TFloat32) result.get(2);
            totalLossList.add(totalLoss.getFloat());
            piLossList.add(piLoss.getFloat());
            vLossList.add(vLoss.getFloat());
        }
        //output loss values to file
        String lossesFolder = (isMrX) ? MRXLOSSESFOLDER : DETLOSSESFOLDER;
        writeFloatListToFile(totalLossList, lossesFolder + TOTALLOSSFILE);
        writeFloatListToFile(piLossList, lossesFolder + PILOSSFILE);
        writeFloatListToFile(vLossList, lossesFolder + VLOSSFILE);

    }

    private Tensor getBatchedExpectedPolicy(List<TrainingEntry> trainingExamples, int batchSize, Integer currIndex) {
        //todo
        batchSize = Math.min(batchSize, trainingExamples.size() - currIndex);
        NdArray<Float> ndArr = NdArrays.ofFloats(Shape.of(batchSize, POSSIBLEMOVES));
        for (int i = 0; i < batchSize; i++) {
            ndArr.set(trainingExamples.get(currIndex+i).getExpectedPolicyOutput(), i);
        }
        return TFloat32.tensorOf(ndArr);
    }

    private Tensor getBatchedExpectedValue(List<TrainingEntry> trainingExamples, int batchSize, int currIndex) {
        //todo
        batchSize = Math.min(batchSize, trainingExamples.size() - currIndex);
        NdArray<Float> ndArr = NdArrays.ofFloats(Shape.of(batchSize));
        for (int i = 0; i < batchSize; i++) {
            ndArr.set(trainingExamples.get(currIndex+i).getExepectedGameOutput(), i);
        }
        return TFloat32.tensorOf(ndArr);
    }

    private Tensor getBatchedInput(List<TrainingEntry> trainingExamples, int batchSize, int currIndex) {
        //todo
        batchSize = Math.min(batchSize, trainingExamples.size() - currIndex);
        NdArray<Float> ndArr = NdArrays.ofFloats(Shape.of(batchSize, INPUTSIZE));
        for (int i = 0; i < batchSize; i++) {
            ndArr.set(trainingExamples.get(currIndex+i).getNnetInput(), i);
        }
        return TFloat32.tensorOf(ndArr);
    }

    public Pair<List<Float>, Float> predict(NnetInput gameState) {
        Tensor input = TFloat32.tensorOf(gameState.getNdArr());
//        Reshape<TFloat32> rinput = Reshape.create()
        List<Tensor> output = this.sess.runner()
                .feed("input", input)
                .feed("is_training", TBool.scalarOf(false))
                .feed("dropout", TFloat32.scalarOf(0.3f))
                .fetch("state_value_output:0")
                .fetch("policy_output:0")
                .run();
        return tensorsToPair(output);
    }

    private Pair<List<Float>, Float> tensorsToPair(List<Tensor> tensors) {
        if (tensors.size() != 2) throw new IllegalArgumentException();
        float[][] converted = StdArrays.array2dCopyOf((TFloat32) tensors.get(1));
        List<Float> policy = new ArrayList<>();
        for (int i = 0; i < POSSIBLEMOVES; i++) {
            policy.add(converted[0][i]);
        }
        Float gameVal = StdArrays.array2dCopyOf((TFloat32) tensors.get(0))[0][0];
        return new Pair<>(policy, gameVal);
    }

    public void save_checkpoint(String checkpointDir) {
        TString checkpointPrefix = TString.scalarOf(Paths.get(checkpointDir, "ckpt").toString());
        this.sess.runner().feed("save/Const", checkpointPrefix).addTarget("save/control_dependency").run();
    }
    public void load_checkpoint(String checkpointDir) {
        final boolean checkpointExists = Files.exists(Paths.get(checkpointDir, "ckpt"));
        TString checkpointPrefix = TString.scalarOf(Paths.get(checkpointDir, "ckpt").toString());
        if (checkpointExists && !OVERWRITEPREVNET) {
            this.sess.runner().feed("save/Const", checkpointPrefix).addTarget("save/restore_all").run();
        } else {
            this.sess.runner().addTarget("init").run();
        }
    }

    public void load_model(String graphDefFile) throws IOException {
        final byte[] graphDef = Files.readAllBytes(Paths.get(graphDefFile));
        Graph graph = new Graph();
        this.sess = new Session(graph);
        graph.importGraphDef(GraphDef.parseFrom(graphDef));

//        SavedModelBundle load = SavedModelBundle.load(path);
//        this.nnet = load.session();
    }

    void writeFloatListToFile(List<Float> floatList, String filePath) throws IOException {
        FileWriter writer = new FileWriter(filePath, !OVERWRITEPREVNET);
        for(Float element: floatList) {
            writer.write(String.format("%f\n", element));
        }
        writer.close();
    }

    public void closeSess() {
        this.sess.close();
    }

//    public void load_model(String path) throws IOException {
//        var graphBytes = Files.readAllBytes(Paths.get(path));
//        Graph graph = new Graph();
//        graph.importGraphDef(GraphDef.parseFrom(graphBytes));
//        this.nnet = new Session(graph,
//                ConfigProto.newBuilder()
//                        .setAllowSoftPlacement(true)
//                        .build()
//        );
//    }
}
