package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.javatuples.Triplet;
import uk.ac.bris.cs.scotlandyard.model.Board;

import java.util.ArrayList;
import java.util.List;

public class Coach {
    Game game;
    NeuralNet nnet;
    NeuralNet pnet;
    MCTS mcts;
    List<Triplet<Board.GameState, List<Float>, Integer>> trainExamplesHistory;
    boolean skipFirstSelfPlay;
    public Coach(Game game, NeuralNet nnet) {
        this.game = game;
        this.nnet = nnet;
        this.pnet = new NeuralNet(this.game); //TODO check
        this.mcts = mcts;
        this.trainExamplesHistory = new ArrayList<>();
        this.skipFirstSelfPlay = false;
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
