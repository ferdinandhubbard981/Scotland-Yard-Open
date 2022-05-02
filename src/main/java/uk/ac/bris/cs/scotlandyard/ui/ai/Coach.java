package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Coach {
    private static final Float UPDATETHRESHOLD = 0.51f;
    private static final int NUMOFSIMS = 10000;
    private static final int NUMOFITER = Integer.MAX_VALUE;
    private static final int NUMPITGAMES = 10;
    private static final String MRXTRAININGEXAMPLESFILE = "mrXTraining.examples";
    private static final String DETTRAININGEXAMPLESFILE = "detTraining.examples";
    private static final boolean PITNNETSTOGETHER = false; //whether to pit or not
    private static final int PITITERATIONPERIOD = 100; //pit every x iterations
    private static final float DROPOUT = 0.8f;
    private static final int BATCHSIZE = 64;
    private static final float LR = 0.001f;
    private static final int TRAININGSAMPLESIZE = BATCHSIZE * 1; //min number of examples in training set
    private static final boolean OVERWRITEPREVNNET = false;
    public static final String MRXCHECKPOINTDIR = "checkpoints/valid/mrX";
    public static final String DETCHECKPOINTDIR = "detCheckpoints/valid/det";
    private static final String MRXMINLOSSCHECKPOINTDIR = "checkpoints/minloss/mrX";
    private static final String DETMINLOSSCHECKPOINTDIR = "checkpoints/minloss/det";

    Game game;
    NeuralNet mrXNnet;
    NeuralNet detNnet;
//    List<List<TrainingEntry>> mrXTrainingExamplesHistory;
//    List<List<TrainingEntry>> detTrainingExamplesHistory;

    boolean skipFirstSelfPlay;

    public Coach(Game game) throws IOException{
        this.game = game;
        this.mrXNnet = new NeuralNet(true, OVERWRITEPREVNNET);
        this.detNnet = new NeuralNet(false, OVERWRITEPREVNNET);
//        this.mrXTrainingExamplesHistory = new ArrayList<>();
//        this.detTrainingExamplesHistory = new ArrayList<>();
        this.skipFirstSelfPlay = false;

    }
    //trainingExample: <gameState, ismrX, policy, gameOutcome>
    public Pair<List<TrainingEntry>, List<TrainingEntry>> generateOneGame() throws IOException {
//        plays out a game and makes each move a new training example
        System.out.printf("generating game\n");
        List<TrainingEntry> mrXTrainingExamples = new ArrayList<>();
        List<TrainingEntry> detTrainingExamples = new ArrayList<>();
        this.game.getInitBoard();
        int episodeStep = 0;
        int gameOutcome = this.game.getGameEnded();
        while (gameOutcome == 0) {
            episodeStep++;
//            System.out.printf("move step: %d\n", episodeStep);
            Game tempGame = new Game(this.game);
            MCTS curMCTS = new MCTS(this.mrXNnet, this.detNnet);
            List<Float> pi = curMCTS.getActionProb(tempGame, NUMOFSIMS, 0);
            //NOTE this will generate more training examples for detectives than mrX
            //add training example
            if (this.game.currentIsMrX) mrXTrainingExamples.add(new TrainingEntry(new NnetInput(this.game), pi, 0)); //we do not know the gameOutcome yet so 0 is a placeholder
            else detTrainingExamples.add(new TrainingEntry(new NnetInput(this.game), pi, 0)); //we do not know the gameOutcome yet so 0 is a placeholder

//            getting randomMove from all possible Moves
            //get valid move list
            List<Float> validMoveVals = pi.stream().filter(val -> val != 0).toList();
            //select random valid move
            float floatVal = validMoveVals.get(ThreadLocalRandom.current().nextInt(0, validMoveVals.size()));
            //get move index

            this.game.setValidMoves();
            this.game.updateCurrentPlayer();
            List<Integer> validMoveTable = this.game.getValidMoveTable();
            int moveIndex = validMoveTable.indexOf(1);
            //get next state
            this.game.getNextState(moveIndex);

            gameOutcome = this.game.getGameEnded(); //1 is current player won -1 is current player lost

        }
        //set gameOutcome to all training examples
        for (TrainingEntry trainingExample : mrXTrainingExamples)
            trainingExample.setGameOutcome(gameOutcome);
        for (TrainingEntry trainingExample : detTrainingExamples)
            trainingExample.setGameOutcome(gameOutcome);

        //mrx on the left
        //det on the right
        return new Pair<>(mrXTrainingExamples, detTrainingExamples);
    }

    public void learn() throws IOException {
        //performs 1 iteration of training
        float minMrXLoss = 10000f;
        float minDetLoss = 10000f;
        for (int currIteration = 0; currIteration < NUMOFITER; currIteration++) {
            System.out.printf("Iteration %d\n\n", currIteration);
            System.out.printf("time: %d\n", System.nanoTime());
            List<TrainingEntry> mrXTrainingExamples = new ArrayList<>();
            List<TrainingEntry> detTrainingExamples = new ArrayList<>();
            //generating training data
            while (mrXTrainingExamples.size() < TRAININGSAMPLESIZE) {
                Pair<List<TrainingEntry>, List<TrainingEntry>> newTrainingData= this.generateOneGame();
                mrXTrainingExamples.addAll(newTrainingData.left());
                detTrainingExamples.addAll(newTrainingData.right());
                //removing excess training examples
                if (mrXTrainingExamples.size() > TRAININGSAMPLESIZE)
                    mrXTrainingExamples = mrXTrainingExamples.subList(0, TRAININGSAMPLESIZE);
                if (detTrainingExamples.size() > TRAININGSAMPLESIZE)
                    detTrainingExamples = detTrainingExamples.subList(0, TRAININGSAMPLESIZE);
                //add training examples to file
//                boolean overwrite = j == 0; //if first game in iteration we want to overwrite previous iteration training examples
//                this.saveTrainExamples(mrXTrainingExamples, true, overwrite);
//                this.saveTrainExamples(detTrainingExamples, false, overwrite);
            }

            //shuffling training examples to avoid overfitting
            Collections.shuffle(mrXTrainingExamples);
            Collections.shuffle(detTrainingExamples);
            //load previous iteration
            List<NeuralNet> prevNnets= List.of(new NeuralNet(true, false), new NeuralNet(false, false));
            //train
            boolean overwrite = OVERWRITEPREVNNET && currIteration == 0;
            float mrXLoss = this.mrXNnet.train(mrXTrainingExamples, DROPOUT, BATCHSIZE, LR, overwrite);
            float detLoss = this.detNnet.train(detTrainingExamples, DROPOUT, BATCHSIZE, LR, overwrite);
            //todo if average total loss is > that previous: savecheckpoint to minlosscheckpoint/mrx .. /det
            if (mrXLoss < minMrXLoss) {
                this.mrXNnet.save_checkpoint(MRXMINLOSSCHECKPOINTDIR);
                minMrXLoss = mrXLoss;
            }
            if (detLoss < minDetLoss) {
                this.mrXNnet.save_checkpoint(DETMINLOSSCHECKPOINTDIR);
                minDetLoss = detLoss;
            }
            List<NeuralNet> newNnets = List.of(this.mrXNnet, this.detNnet);

            //check if we are pitting this iteration
            if (PITNNETSTOGETHER && (currIteration+1) % PITITERATIONPERIOD == 0) {
                //pit new vs old
                System.out.printf("pitting against previous version\n\n");
                Arena arena = new Arena(this.game, newNnets , prevNnets);
                Pair<Integer, Integer> playthroughOutcomes = arena.playGames(NUMPITGAMES, NUMOFSIMS);

                //get winrates
                System.out.printf("\n\nnewAi:\nwins: %d\nlosses: %d\n\n", playthroughOutcomes.left(), playthroughOutcomes.right());
                int total = playthroughOutcomes.left() + playthroughOutcomes.right();
                float winrate = (float)playthroughOutcomes.left() / total;
                if (total == 0 || winrate < UPDATETHRESHOLD){
                    //reject new model
                    System.out.printf("\nRejecting model wr: %f\n", winrate);
                    this.mrXNnet.load_checkpoint(MRXCHECKPOINTDIR, false);
                    this.detNnet.load_checkpoint(DETCHECKPOINTDIR, false);
                }
                else {
                    //accept new model
                    System.out.printf("\nAccepting model wr: %f\n", winrate);
                    this.mrXNnet.save_checkpoint(MRXCHECKPOINTDIR);
                    this.detNnet.save_checkpoint(DETCHECKPOINTDIR);
                }
            }



            prevNnets.get(0).closeSess();
            prevNnets.get(1).closeSess();
            }

        }


    public String getCheckpointFile(boolean isMrX) {
        if (isMrX) return MRXTRAININGEXAMPLESFILE;
        return DETTRAININGEXAMPLESFILE;
    }

    public void saveTrainExamples(List<List<TrainingEntry>> trainingExamplesHistory, boolean isMrX, boolean overwrite) throws IOException {
        //todo save data in a way that can be loaded directly into network (as tensors)
        // Creating binary file
        FileOutputStream fout = new FileOutputStream(getCheckpointFile(isMrX) + ".examples", !overwrite);
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

    public List<List<TrainingEntry>> getTrainExamples(boolean isMrX) throws IOException {
        FileInputStream fin = new FileInputStream(getCheckpointFile(isMrX));
        DataInputStream din = new DataInputStream(fin);
        List<List<TrainingEntry>> newTrainingExamplesHistory = new ArrayList<>();
        int trainingExamplesHistorySize = din.readInt();
        for (int i = 0; i < trainingExamplesHistorySize; i++) {
            int listSize = din.readInt();
            List<TrainingEntry> trainingExamples = new ArrayList<>();
            for (int j = 0; j < listSize; j++) {
                trainingExamples.add(new TrainingEntry(din));
            }
            newTrainingExamplesHistory.add(trainingExamples);
        }
        din.close();
        fin.close();
        return newTrainingExamplesHistory;
    }

}
