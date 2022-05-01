package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.javatuples.Pair;
import org.tensorflow.*;
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

import static uk.ac.bris.cs.scotlandyard.ui.ai.Game.POSSIBLEMOVES;

public class NeuralNet {
    private static final int SEED = 123;
    private static final float LR = 0.001f;
    private static final float DROPOUT = 0.3f;
    private static final int EPOCHS = 10;
    private static final int BATCH_SIZE = 64;
    private static final int NUM_CHANNELS = 512;
    private static final String MRXGRAPH = "mrXGraph.pb";
    private static final String DETGRAPH = "detGraph.pb";
    public static final String MRXCHECKPOINTDIR = "mrXCheckpoints";
    public static final String DETCHECKPOINTDIR = "detCheckpoints";
    private static final String LOSSESFILE = "losses.txt";


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

    public void train(List<TrainingEntry> trainingExamples) throws IOException {
        System.out.printf("training\n");
        List<Float> losses = new ArrayList<>();
        for (TrainingEntry example : trainingExamples) {
//            Shape shape = Shape.of(NNETINPUTBOARDSIZE);
            List<Tensor> result = this.sess.runner()
                    .feed("input" ,example.getNnetInput())
                    .feed("dropout", TFloat32.scalarOf(0.3f))
                    .feed("targetPolicy", example.getExpectedPolicyOutput())
                    .feed("targetV", example.getExepectedGameOutput())
                    .feed("is_training", TBool.scalarOf(true))
                    .fetch("add:0")
//                    .fetch("softmax_cross_entropy_loss/value:0")
                    .addTarget("Adam").run();
            TFloat32 loss = (TFloat32) result.get(0);
            losses.add(loss.getFloat());
        }
        //todo output loss values to file
        writeFloatListToFile(losses, LOSSESFILE);

    }

    public Pair<List<Float>, Float> predict(NnetInput gameState) {
        Tensor input = gameState.getTensor();
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
        assert(tensors.size() == 2);
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
        final boolean checkpointExists = Files.exists(Paths.get(checkpointDir));
        TString checkpointPrefix = TString.scalarOf(Paths.get(checkpointDir, "ckpt").toString());
        if (checkpointExists) {
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
        FileWriter writer = new FileWriter(filePath, true);
        for(Float element: floatList) {
            writer.write(String.format("%f\n", element));
        }
        writer.close();
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
