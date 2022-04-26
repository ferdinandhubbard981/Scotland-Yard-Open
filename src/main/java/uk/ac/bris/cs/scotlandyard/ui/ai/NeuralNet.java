package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.javatuples.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static uk.ac.bris.cs.scotlandyard.ui.ai.Game.POSSIBLEMOVES;

public class NeuralNet {
    private static final boolean LOADFROMKERAS = true;
    private static final int SEED = 123;
    private static final float LR = 0.001f;
    private static final float DROPOUT = 0.3f;
    private static final int EPOCHS = 10;
    private static final int BATCH_SIZE = 64;
    private static final int NUM_CHANNELS = 512;
    private static final String MRXGRAPH = "pathtofile";
    private static final String DETGRAPH = "pathtofile";
    public final boolean isMrX;
//    private MultiLayerNetwork nnet;
    public NeuralNet(Game game, boolean isMrX) throws IOException{
        this.isMrX = isMrX;
        String path = (game.currentIsMrX) ? MRXGRAPH : DETGRAPH;
        if (LOADFROMKERAS) loadKerasNet(path);
        else load_checkpoint(path);
    }

    public void loadKerasNet(String path) throws IOException{
//        String simpleMlp = new ClassPathResource(path).getFile().getPath();
//        this.nnet = new MultiLayerNetwork(KerasModelImport.importKerasSequentialConfiguration(simpleMlp));
//        this.nnet.init(); DEBUG
    }
    public void train(List<TrainingEntry> trainingExamples) {
        for (TrainingEntry example : trainingExamples) {
//            INDArray input = example.getNnetInput();
//            INDArray expectedOutput = example.getExepectedOutput();
//            this.nnet.fit(input, expectedOutput);
        }
    }

    public Pair<List<Float>, Float> predict(NnetInput gameState) {
//        INDArray input = gameState.getIndArray();
//        this.nnet.output(input);
        //TODO
        //Convert IndArray to Pair<List<Float>, Float>
        Pair<List<Float>, Float> output = new Pair<>(Collections.nCopies(POSSIBLEMOVES, 0f), 0.5f);
        return output;
    }

    public void save_checkpoint(String path) throws IOException {
//        ModelSerializer.writeModel(this.nnet, path, true);
    }

    public void load_checkpoint(String path) throws IOException {
//        this.nnet = ModelSerializer.restoreMultiLayerNetwork(path);
    }
}
