package uk.ac.bris.cs.scotlandyard.ui.ai;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import com.fasterxml.jackson.databind.type.PlaceholderForType;
import org.javatuples.Pair;
import org.tensorflow.Session;
import org.tensorflow.proto.framework.GraphDef;
import uk.ac.bris.cs.scotlandyard.model.Board;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.javatuples.Triplet;
import org.tensorflow.Graph;

public class NeuralNet {
    private static final float LR = 0.001f;
    private static final float DROPOUT = 0.3f;
    private static final int EPOCHS = 10;
    private static final int BATCH_SIZE = 64;
    private static final int NUM_CHANNELS = 512;
    public NeuralNet(Game game) throws IOException {
        Graph graph = new Graph();
        Session sess = new Session(graph);
        byte[] graphDef = Files.readAllBytes(Paths.get("path"));
        graph.importGraphDef(GraphDef.parseFrom(graphDef));

    }

    public void train(List<TrainingEntry> trainingExamples) {

    }

    public Pair<List<Float>, Float> predict(NnetInput gameState) {
        return null;
    }

    public void save_checkpoint(String folder, String fileName) {

    }

    public void load_checkpoint(String folder, String fileName) {

    }
}
