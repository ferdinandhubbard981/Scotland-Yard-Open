package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public class Coach {
    private static final int NUMOFSIMS = 800;
    private static final int NUMOFITER = 767;
    private static final boolean SKIPFIRSTSELFPLAY = false;
    Game game;
    NeuralNet nnet;
    NeuralNet pnet;
    MCTS mcts;
    List<TrainingEntry> trainExamplesHistory;
    boolean skipFirstSelfPlay;

    public Coach(Game game, NeuralNet nnet) {
        this.game = game;
        this.nnet = nnet;
        this.pnet = new NeuralNet(this.game); //TODO check
        this.mcts = new MCTS(this.game, this.nnet);
        this.trainExamplesHistory = new ArrayList<>();
        this.skipFirstSelfPlay = false;
    }
    //trainingExample: <gameState, ismrX, policy, gameOutcome>
    public List<TrainingEntry> executeEpisode() {
//        plays out a game and makes each move a new training example
        List<TrainingEntry> trainingExamples = new ArrayList<>();
        Board.GameState gameState = this.game.getInitBoard();
        int episodeStep = 0;
        int gameOutcome = 0;
        while (gameOutcome == 0) {
            episodeStep++;
            List<Float> pi = this.mcts.getActionProb(gameState, NUMOFSIMS);
            trainingExamples.add(new TrainingEntry(gameState, pi, 0)); //gameOutcome here is temporary and is overridden
//            getting randomMove from all possible Moves
            List<Float> validMoveVals = pi.stream().filter(val -> val != 0).toList();
            float floatVal = validMoveVals.get(ThreadLocalRandom.current().nextInt(0, validMoveVals.size()));
            int moveIndex = pi.indexOf(floatVal);//TODO check but it should be a valid move
            gameState = this.game.getNextState(gameState, moveIndex);
            gameOutcome = this.game.getGameEnded(gameState); //1 is always mrX -1 is always det

        }
        for (TrainingEntry trainingExample : trainingExamples)
            trainingExample.setGameOutcome(gameOutcome);
        return trainingExamples;
    }

    public void learn() {
        for (int i = 0; i < NUMOFITER; i++) {
            System.out.printf("Iteration %d\n\n", i);
            if (!SKIPFIRSTSELFPLAY || i > 1) {
                Queue<>
            }

        }
    }

    public String getCheckpointFile(int iteration) {
        return String.format("checkpoint_%d.pth.tar", iteration);
    }

    public void saveTrainExamples(int iteration) {

    }

    public void loadTrainExamples() {

    }

}
