package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadLocalRandom;

public class Coach {
    private static final Float UPDATETHRESHOLD = 0.55f;
    //!!!IF YOU CHANGE NUMITERSFORTRAININGEXAMPLESHISTORY then you must delete all PREVIOUS training examples
    private static final int NUMITERSFORTRAININGEXAMPLESHISTORY = 1;
    private static final int NUMOFSIMS = 20;
    private static final int NUMOFITER = 10;
    private static final int NUMEPS = 1;
    private static final boolean SKIPFIRSTSELFPLAY = false;
    private static final String SAVEFOLDER = "";
    private static final int NUMOFGAMES = 100;
    private static final String LOADFILE = "checkpoint_x.pth.tar";

    Game game;
    NeuralNet nnet;
    NeuralNet pnet;
    MCTS mcts;
    Queue<List<TrainingEntry>> trainingExamplesHistory;
    boolean skipFirstSelfPlay;

    public Coach(Game game, NeuralNet nnet) {
        this.game = game;
        this.nnet = nnet;
        this.pnet = new NeuralNet(this.game); //TODO check
        this.mcts = new MCTS(this.game, this.nnet);
        this.trainingExamplesHistory = new SynchronousQueue<>();
        this.skipFirstSelfPlay = false;
    }
    //trainingExample: <gameState, ismrX, policy, gameOutcome>
    public List<TrainingEntry> executeEpisode() {
//        plays out a game and makes each move a new training example
        List<TrainingEntry> trainingExamples = new ArrayList<>();
        MyGameState gameState = this.game.getInitBoard();
        int episodeStep = 0;
        int gameOutcome = 0;
        while (gameOutcome == 0) {
            episodeStep++;
            List<Float> pi = this.mcts.getActionProb(gameState, NUMOFSIMS);
            trainingExamples.add(new TrainingEntry(new NnetInput(gameState), pi, 0)); //gameOutcome here is temporary and is overridden
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
                Queue<TrainingEntry> iterationTrainingExamples = new SynchronousQueue<>();
                for (int j = 0; j < NUMEPS; j++) {
                    this.mcts = new MCTS(this.game, this.nnet);
                    iterationTrainingExamples.addAll(this.executeEpisode());
                }
                this.trainingExamplesHistory.add(iterationTrainingExamples.stream().toList());
                if (this.trainingExamplesHistory.size() > NUMITERSFORTRAININGEXAMPLESHISTORY) {
                    System.out.printf("removing oldest entry in trainExamplesHistory of len: %d", this.trainingExamplesHistory.size());
                    this.trainingExamplesHistory.remove();
                }
                try {
                    this.saveTrainExamples(i-1);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                List<TrainingEntry> trainingExamples = new ArrayList<>();
                for (List<TrainingEntry> trainingExampleList : this.trainingExamplesHistory) {
                    trainingExamples.addAll(trainingExampleList);
                }
                Collections.shuffle(trainingExamples);
                this.nnet.save_checkpoint(SAVEFOLDER, "temp.pth.tar");
                this.pnet.load_checkpoint(SAVEFOLDER, "temp.pth.tar");
                MCTS pmcts = new MCTS(this.game, this.pnet);
                this.nnet.train(trainingExamples);
                MCTS nmcts = new MCTS(this.game, this.nnet);

                System.out.printf("pitting against previous version");
                Arena arena = new Arena(nmcts, pmcts, this.game);
                Pair<Integer, Integer> playthroughOutcomes = arena.playGames(NUMOFGAMES, NUMOFSIMS);
                System.out.printf("\n\nnewAi:\nwins: %d\nlosses: %d\n\n", playthroughOutcomes.left(), playthroughOutcomes.right());
                int total = playthroughOutcomes.left() + playthroughOutcomes.right();
                float winrate = (float)playthroughOutcomes.left() / total;
                if (total == 0 || winrate < UPDATETHRESHOLD){
                    System.out.printf("\n\nRejecting model wr: %f", winrate);
                    this.nnet.load_checkpoint(SAVEFOLDER, "temp.pth.tar");
                }
                else {
                    System.out.printf("\n\nAccepting model wr: %f", winrate);
                    this.nnet.save_checkpoint(SAVEFOLDER, this.getCheckpointFile(i));
                    this.nnet.save_checkpoint(SAVEFOLDER, "temp.pth.tar");
                    }
                }

            }

        }


    public String getCheckpointFile(int iteration) {
        return String.format("checkpoint_%d.pth.tar", iteration);
    }

    public void saveTrainExamples(int iteration) throws IOException {
        //TODO implement
        // Creating binary file
        FileOutputStream fout = new FileOutputStream(getCheckpointFile(iteration) + ".examples");
        DataOutputStream dout=new DataOutputStream(fout);
        //write num of elements for list reconstruction
        dout.write(trainingExamplesHistory.size());
        for (List<TrainingEntry>  trainingExamples: trainingExamplesHistory) {
            //write num of elements for list reconstruction
            dout.write(trainingExamples.size());
            for (TrainingEntry trainingExample : trainingExamples) {
                dout.write(trainingExample.toBytes());
            }
        }
        dout.close();
        fout.close();
    }

    public void loadTrainExamples() throws IOException {
        FileInputStream fin = new FileInputStream(SAVEFOLDER + LOADFILE + ".examples");
        DataInputStream din = new DataInputStream(fin);
        Queue<List<TrainingEntry>> newTrainingExamplesHistory = new SynchronousQueue<>();
        int trainingExamplesHistorySize = din.readInt();
        for (int i = 0; i < trainingExamplesHistorySize; i++) {
            int listSize = din.readInt();
            List<TrainingEntry> trainingExamples = new ArrayList<>();
            for (int j = 0; j < listSize; j++) {
                trainingExamples.add(new TrainingEntry(din));
            }
            newTrainingExamplesHistory.add(trainingExamples);
        }
        this.trainingExamplesHistory = newTrainingExamplesHistory;
        din.close();
        fin.close();
    }

}
