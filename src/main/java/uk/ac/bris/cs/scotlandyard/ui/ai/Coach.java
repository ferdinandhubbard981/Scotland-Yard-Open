package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Coach {
    private static final Float UPDATETHRESHOLD = 0.55f;
    private static final int NUMOFSIMS = 1000;
    private static final int NUMOFITER = 10;
    private static final int NUMTRAININGGAMES = 2;
    private static final int NUMPITGAMES = 6;
    private static final String MRXTRAININGEXAMPLESFILE = "mrXTraining.examples";
    private static final String DETTRAININGEXAMPLESFILE = "detTraining.examples";
    private static final boolean PITNNETSTOGETHER = false;
    private static final int PITITERATIONPERIOD = 5; //pit every x iterations
    private static final float DROPOUT = 0.8f;
    private static final int BATCHSIZE = 16;
    private static final float LR = 0.001f;

    Game game;
    NeuralNet mrXNnet;
    NeuralNet detNnet;
//    List<List<TrainingEntry>> mrXTrainingExamplesHistory;
//    List<List<TrainingEntry>> detTrainingExamplesHistory;

    boolean skipFirstSelfPlay;

    public Coach(Game game) throws IOException{
        this.game = game;
        this.mrXNnet = new NeuralNet(this.game, true);
        this.detNnet = new NeuralNet(this.game, false);
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
        for (int i = 0; i < NUMOFITER; i++) {
            System.out.printf("Iteration %d\n\n", i);
            List<TrainingEntry> mrXTrainingExamples = new ArrayList<>();
            List<TrainingEntry> detTrainingExamples = new ArrayList<>();
            //generating training data
            for (int j = 0; j < NUMTRAININGGAMES; j++) {
                //todo make sure there is a minimum number of examples
                Pair<List<TrainingEntry>, List<TrainingEntry>> newTrainingData= this.generateOneGame();
                mrXTrainingExamples.addAll(newTrainingData.left());
                detTrainingExamples.addAll(newTrainingData.right());
                //add training examples to file
//                boolean overwrite = j == 0; //if first game in iteration we want to overwrite previous iteration training examples
//                this.saveTrainExamples(mrXTrainingExamples, true, overwrite);
//                this.saveTrainExamples(detTrainingExamples, false, overwrite);
            }




            //load mrX training data

            //shuffling training examples to avoid overfitting
            Collections.shuffle(mrXTrainingExamples);
            Collections.shuffle(detTrainingExamples);

            //save old nnets
            this.mrXNnet.save_checkpoint(NeuralNet.MRXCHECKPOINTDIR);
            this.detNnet.save_checkpoint(NeuralNet.DETCHECKPOINTDIR);
            //save previous iteration\
            List<NeuralNet> prevNnets= List.of(new NeuralNet(this.mrXNnet), new NeuralNet(this.detNnet));
            //train
            this.mrXNnet.train(mrXTrainingExamples, DROPOUT, BATCHSIZE, LR);
            this.detNnet.train(detTrainingExamples, DROPOUT, BATCHSIZE, LR);
            List<NeuralNet> newNnets = List.of(this.mrXNnet, this.detNnet);
            int total;
            float winrate;
            if (PITNNETSTOGETHER && i % PITITERATIONPERIOD == 0) {
                //pit new vs old
                System.out.printf("pitting against previous version\n\n");
                Arena arena = new Arena(this.game, newNnets , prevNnets);
                Pair<Integer, Integer> playthroughOutcomes = arena.playGames(NUMPITGAMES, NUMOFSIMS);

                //get winrates
                System.out.printf("\n\nnewAi:\nwins: %d\nlosses: %d\n\n", playthroughOutcomes.left(), playthroughOutcomes.right());
                total = playthroughOutcomes.left() + playthroughOutcomes.right();
                winrate = (float)playthroughOutcomes.left() / total;
            }
            else {
                total = 1000;
                winrate = 1000;
            }

            if (total == 0 || winrate < UPDATETHRESHOLD){
                //reject new model
                System.out.printf("\nRejecting model wr: %f\n", winrate);
                this.mrXNnet.load_checkpoint(NeuralNet.MRXCHECKPOINTDIR);
                this.detNnet.load_checkpoint(NeuralNet.DETCHECKPOINTDIR);
            }
            else {
                //accept new model
                System.out.printf("\nAccepting model wr: %f\n", winrate);
                this.mrXNnet.save_checkpoint(NeuralNet.MRXCHECKPOINTDIR);
                this.detNnet.save_checkpoint(NeuralNet.DETCHECKPOINTDIR);
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
