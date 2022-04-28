package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.javatuples.Pair;
import org.tensorflow.*;
import org.tensorflow.types.TBool;

import java.io.IOException;
import java.util.List;

public class NeuralNet {
    private static final int SEED = 123;
    private static final float LR = 0.001f;
    private static final float DROPOUT = 0.3f;
    private static final int EPOCHS = 10;
    private static final int BATCH_SIZE = 64;
    private static final int NUM_CHANNELS = 512;
    private static final String MRXGRAPH = "pathtofile";
    private static final String DETGRAPH = "pathtofile";
    public final boolean isMrX;
    private Session nnet;
    public NeuralNet(Game game, boolean isMrX) throws IOException{
        this.isMrX = isMrX;
        String path = (game.currentIsMrX) ? MRXGRAPH : DETGRAPH;
        load_model(path);
        load_checkpoint(path);
    }

    public void train(List<TrainingEntry> trainingExamples) {
        for (TrainingEntry example : trainingExamples) {
//            Shape shape = Shape.of(NNETINPUTBOARDSIZE);
            List<Tensor> result = this.nnet.runner()
                    .feed("input" ,example.getNnetInput())
                    .feed("expectedPolicyOutput", example.getExpectedPolicyOutput())
                    .feed("expectedGameOutput", example.getExepectedGameOutput())
                    .feed("is_training", TBool.scalarOf(true))
                    .fetch("total_loss:0")
                    .fetch("accuracy:0")
                    .addTarget("optimize").run();
        }
    }

    public Pair<List<Float>, Float> predict(NnetInput gameState) {
        Tensor input = gameState.getTensor();
        List<Tensor> output = this.nnet.runner().feed("input", input).fetch("policy").fetch("stateVal").run();
        return tensorsToPair(output);
    }

    private Pair<List<Float>, Float> tensorsToPair(List<Tensor> output) {
        //TODO
        return null;
    }

    public void save_checkpoint(String path) {
        this.nnet.save(path);
    }
    public void load_checkpoint(String path) {
    this.nnet.restore(path);
    }

    public void load_model(String path) {
        SavedModelBundle load = SavedModelBundle.load(path);
        this.nnet = load.session();
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
