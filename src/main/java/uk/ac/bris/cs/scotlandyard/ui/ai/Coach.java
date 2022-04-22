package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.javatuples.Triplet;
import uk.ac.bris.cs.scotlandyard.model.Board;

import java.util.List;

public class Coach {
    public Coach(Game game, NeuralNet nnet) {

    }

    public Triplet<Board.GameState, List<Float>, Integer> executeEpisode() {
        return null;
    }

    public void learn() {

    }

    public String getCheckpointFile(int iteration) {
        return null;
    }

    public void saveTrainExamples(int iteration) {

    }

    public void loadTrainExamples() {

    }

}
