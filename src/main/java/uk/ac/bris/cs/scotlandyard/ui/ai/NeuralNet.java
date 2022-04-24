package uk.ac.bris.cs.scotlandyard.ui.ai;
import org.javatuples.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import java.util.List;
import org.javatuples.Triplet;
import org.tensorflow.*;
public class NeuralNet {

    public NeuralNet(Game game){

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
