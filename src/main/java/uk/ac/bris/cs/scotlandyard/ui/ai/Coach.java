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
    NeuralNet mrXNnet;
    NeuralNet detNnet;
    MCTS newmcts;
    Queue<List<TrainingEntry>> mrXrainingExamplesHistory;
    Queue<List<TrainingEntry>> dettrainingExamplesHistory;

    boolean skipFirstSelfPlay;

    public Coach(Game game) throws IOException {
        this.game = game;
        this.mrXNnet = new NeuralNet(this.game, true);
        this.detNnet = new NeuralNet(this.game, false); //TODO check
        this.newmcts = new MCTS(this.mrXNnet, this.detNnet);
        this.mrXrainingExamplesHistory = new SynchronousQueue<>();
        this.dettrainingExamplesHistory = new SynchronousQueue<>();
        this.skipFirstSelfPlay = false;

    }
    //trainingExample: <gameState, ismrX, policy, gameOutcome>
    public List<TrainingEntry> executeEpisode() {
        //TODO sort examples into two lists. One for mrXNnet one for detNnet
//        plays out a game and makes each move a new training example
        List<TrainingEntry> trainingExamples = new ArrayList<>();
        this.game.getInitBoard();
        int episodeStep = 0;
        int gameOutcome = 0;
        while (gameOutcome == 0) {
            episodeStep++;
            List<Float> pi = this.newmcts.getActionProb(this.game, NUMOFSIMS);
            trainingExamples.add(new TrainingEntry(new NnetInput(this.game), pi, 0)); //gameOutcome here is temporary and is overridden
//            getting randomMove from all possible Moves
            List<Float> validMoveVals = pi.stream().filter(val -> val != 0).toList();
            float floatVal = validMoveVals.get(ThreadLocalRandom.current().nextInt(0, validMoveVals.size()));
            int moveIndex = pi.indexOf(floatVal);//TODO check but it should be a valid move
            this.game.getNextState(moveIndex);
            gameOutcome = this.game.getGameEnded(); //1 is always mrX -1 is always det

        }
        for (TrainingEntry trainingExample : trainingExamples)
            trainingExample.setGameOutcome(gameOutcome);
        return trainingExamples;
    }

    public void learn() {
        //iterations
        for (int i = 0; i < NUMOFITER; i++) {
            System.out.printf("Iteration %d\n\n", i);
            if (!SKIPFIRSTSELFPLAY || i > 1) {
                Queue<TrainingEntry> iterationTrainingExamples = new SynchronousQueue<>();
                //generating training data
                for (int j = 0; j < NUMEPS; j++) {
                    this.newmcts = new MCTS(this.mrXNnet, this.detNnet);
                    iterationTrainingExamples.addAll(this.executeEpisode());
                }
                //formatting training data
                this.trainingExamplesHistory.add(iterationTrainingExamples.stream().toList());
                if (this.trainingExamplesHistory.size() > NUMITERSFORTRAININGEXAMPLESHISTORY) {
                    System.out.printf("removing oldest entry in trainExamplesHistory of len: %d", this.trainingExamplesHistory.size());
                    this.trainingExamplesHistory.remove();
                }
                //saving training data
                try {
                    this.saveTrainExamples(i-1);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                //loading training examples in iterations
                List<TrainingEntry> trainingExamples = new ArrayList<>();
                for (List<TrainingEntry> trainingExampleList : this.trainingExamplesHistory) {
                    trainingExamples.addAll(trainingExampleList);
                }
                //shuffling training examples to avoid overfitting
                Collections.shuffle(trainingExamples);
                this.mrXNnet.save_checkpoint(SAVEFOLDER, "temp.pth.tar");
                this.detNnet.load_checkpoint(SAVEFOLDER, "temp.pth.tar");
                MCTS previousMCTS = new MCTS(this.mrXNnet, this.detNnet);
                this.mrXNnet.train(mrXTrainingExamples);
                this.detNnet.train(detTrainingExamples);
                MCTS newMCTS = new MCTS(this.mrXNnet, this.detNnet)

                System.out.printf("pitting against previous version");
                Arena arena = new Arena(this.game, nmcts, pmcts);
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
