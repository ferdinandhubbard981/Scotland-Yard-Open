package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    List<List<TrainingEntry>> mrXTrainingExamplesHistory;
    List<List<TrainingEntry>> detTrainingExamplesHistory;

    boolean skipFirstSelfPlay;

    public Coach(Game game) throws IOException{
        this.game = game;
        this.mrXNnet = new NeuralNet(this.game, true);
        this.detNnet = new NeuralNet(this.game, false);
        this.newmcts = new MCTS(this.mrXNnet, this.detNnet);
        this.mrXTrainingExamplesHistory = new ArrayList<>();
        this.detTrainingExamplesHistory = new ArrayList<>();
        this.skipFirstSelfPlay = false;

    }
    //trainingExample: <gameState, ismrX, policy, gameOutcome>
    public Pair<List<TrainingEntry>, List<TrainingEntry>> executeEpisode() throws IOException {
//        plays out a game and makes each move a new training example
        List<TrainingEntry> mrXTrainingExamples = new ArrayList<>();
        List<TrainingEntry> detTrainingExamples = new ArrayList<>();
        this.game.getInitBoard();
        int episodeStep = 0;
        int gameOutcome = this.game.getGameEnded();
        while (gameOutcome == 0) {
            episodeStep++;
            System.err.printf("episode step: %d\n", episodeStep);
            Game tempGame = new Game(this.game);
            List<Float> pi = this.newmcts.getActionProb(tempGame, NUMOFSIMS);
            //NOTE this will generate more training examples for detectives than mrX
            //add training example
            if (this.game.currentIsMrX)
                mrXTrainingExamples.add(new TrainingEntry(new NnetInput(this.game), pi, 0)); //we do not know the gameOutcome yet so 0 is a placeholder
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
        //performs 1 epoch
        for (int i = 0; i < NUMOFITER; i++) {
            System.out.printf("Iteration %d\n\n", i);
            if (!SKIPFIRSTSELFPLAY || i > 1) {
                List<TrainingEntry> mrXIterationTrainingExamples = new ArrayList<>();
                List<TrainingEntry> detIterationTrainingExamples = new ArrayList<>();
                //generating training data
                for (int j = 0; j < NUMEPS; j++) {
                    this.newmcts = new MCTS(this.mrXNnet, this.detNnet);
                    Pair<List<TrainingEntry>, List<TrainingEntry>> newTrainingData= this.executeEpisode();
                    mrXIterationTrainingExamples.addAll(newTrainingData.left());
                    detIterationTrainingExamples.addAll(newTrainingData.right());
                }
                //formatting training data
                this.mrXTrainingExamplesHistory.add(mrXIterationTrainingExamples.stream().toList());
                this.detTrainingExamplesHistory.add(detIterationTrainingExamples.stream().toList());

//                if (this.mrXTrainingExamplesHistory.size() > NUMITERSFORTRAININGEXAMPLESHISTORY) {
//                    System.out.printf("removing oldest entry in trainExamplesHistory of len: %d", this.mrXTrainingExamplesHistory.size());
//                    this.mrXTrainingExamplesHistory.remove();
//                }
//                if (this.detTrainingExamplesHistory.size() > NUMITERSFORTRAININGEXAMPLESHISTORY) {
//                    System.out.printf("removing oldest entry in trainExamplesHistory of len: %d", this.detTrainingExamplesHistory.size());
//                    this.detTrainingExamplesHistory.remove();
//                }

                //saving training data
                try {
                    this.saveTrainExamples(mrXTrainingExamplesHistory, i-1, true);
                    this.saveTrainExamples(detTrainingExamplesHistory, i-1, false);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }

                //load mrX training data
                List<TrainingEntry> mrXTrainingExamples = new ArrayList<>();
                for (List<TrainingEntry> trainingExampleList : this.mrXTrainingExamplesHistory) {
                    mrXTrainingExamples.addAll(trainingExampleList);
                }
                //shuffling training examples to avoid overfitting
                Collections.shuffle(mrXTrainingExamples);

                //load det training data
                List<TrainingEntry> detTrainingExamples = new ArrayList<>();
                for (List<TrainingEntry> trainingExampleList : this.detTrainingExamplesHistory) {
                    detTrainingExamples.addAll(trainingExampleList);
                }
                //shuffling training examples to avoid overfitting
                Collections.shuffle(detTrainingExamples);

                //save old nnets
                this.mrXNnet.save_checkpoint(NeuralNet.MRXCHECKPOINTDIR);
                this.detNnet.save_checkpoint(NeuralNet.DETCHECKPOINTDIR);
                //save previous iteration
                MCTS previousMCTS = new MCTS(this.mrXNnet, this.detNnet);
                //train
                this.mrXNnet.train(mrXTrainingExamples);
                this.detNnet.train(detTrainingExamples);
                MCTS newMCTS = new MCTS(this.mrXNnet, this.detNnet);

                //pit new vs old
                System.out.printf("pitting against previous version");
                Arena arena = new Arena(this.game, newMCTS , previousMCTS);
                Pair<Integer, Integer> playthroughOutcomes = arena.playGames(NUMOFGAMES, NUMOFSIMS);

                //get winrates
                System.out.printf("\n\nnewAi:\nwins: %d\nlosses: %d\n\n", playthroughOutcomes.left(), playthroughOutcomes.right());
                int total = playthroughOutcomes.left() + playthroughOutcomes.right();
                float winrate = (float)playthroughOutcomes.left() / total;
                if (total == 0 || winrate < UPDATETHRESHOLD){
                    //reject new model
                    System.out.printf("\n\nRejecting model wr: %f", winrate);
                    this.mrXNnet.load_checkpoint(NeuralNet.MRXCHECKPOINTDIR);
                    this.detNnet.load_checkpoint(NeuralNet.DETCHECKPOINTDIR);
                }
                else {
                    //accept new model
                    System.out.printf("\n\nAccepting model wr: %f", winrate);
                    String mrXPath = SAVEFOLDER + this.getCheckpointFile(true, i);
                    String detPath = SAVEFOLDER + this.getCheckpointFile(false, i);
                    this.mrXNnet.save_checkpoint(mrXPath);
                    this.detNnet.save_checkpoint(detPath);
                    }
                }

            }

        }


    public String getCheckpointFile(boolean isMrX, int iteration) {
        String player = "Det";
        if (isMrX) player = "MrX";
        return String.format("checkpoint_%s_%d.pth.tar", player, iteration);
    }

    public void saveTrainExamples(List<List<TrainingEntry>> trainingExamplesHistory, int iteration, boolean isMrX) throws IOException {
        // Creating binary file
        FileOutputStream fout = new FileOutputStream(getCheckpointFile(isMrX, iteration) + ".examples");
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

    public List<List<TrainingEntry>> getTrainExamples() throws IOException {
        FileInputStream fin = new FileInputStream(SAVEFOLDER + LOADFILE + ".examples");
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
