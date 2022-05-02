package uk.ac.bris.cs.scotlandyard.ui.ai;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Arena {
    private static final boolean VERBOSE = true;
    private static final Pair<Long, TimeUnit> MOVETIME = new Pair<>(2L, TimeUnit.SECONDS);
    Game game;
    List<NeuralNet> newNnets;
    List<NeuralNet> prevNnets;
//    public Arena(String ai1Name, String ai2Name, Game game) {
//        ImmutableList<Ai> ais = scanAis();
//        this.player1 = ais.stream().filter(ai -> ai.name().equals(ai1Name)).findAny().get(); //player1 is always mrX because he plays first
//        this.player2 = ais.stream().filter(ai -> ai.name().equals(ai2Name)).findAny().get();
//        this.game = game;
//    }

    public Arena(Game game, List<NeuralNet> newNnets, List<NeuralNet> prevNnets) {
        if (!(newNnets.size() == 2)) throw new IllegalArgumentException();
        if (!(prevNnets.size() == 2)) throw new IllegalArgumentException();
        this.newNnets = newNnets;
        this.prevNnets = prevNnets;
        this.game = game;
    }

    Integer playGame(int numOfSims) throws IOException {
        //performs one game
        //returns 1 if player1 wins, -1 if player2 wins

        this.game.getInitBoard();
//        List<Ai> players = new ArrayList<>();
//        players.add(this.player1);
//        players.add(this.player2);

        //start new game
        int it = 0;
        //while game not ended
        while (this.game.getGameEnded() == 0) {
            MCTS previousMCTS = new MCTS(prevNnets.get(0), prevNnets.get(1));
            MCTS newMCTS = new MCTS(newNnets.get(0), newNnets.get(1));
            List<MCTS> players = new ArrayList<>();
            players.add(previousMCTS);
            players.add(newMCTS);
            it++;
            if (VERBOSE) {
                System.out.printf("\n\nTurn %d \n\n", it);
                String temp = (this.game.currentIsMrX) ? "mrX" : "detective";
                System.out.printf("\nPlayer is : %s\n", temp);
            }

            //pick move
            int aiIndex = (this.game.currentIsMrX) ? 0: 1;
            Game tempGame = new Game(this.game);
//            int moveIndex = this.game.getMoveIndex(players.get(aiIndex).pickMove(gameState, MOVETIME));
            List<Float> policy = players.get(aiIndex).getActionProb(tempGame, numOfSims, 0);
            Float highestPolicyVal = policy.stream()
                    .reduce(0f, (maxVal, element) -> maxVal = (element > maxVal) ? element: maxVal);
            int moveIndex = policy.indexOf(highestPolicyVal);
            List<Integer> validMoveTable = this.game.getValidMoveTable();
            if (validMoveTable.get(moveIndex) == 0) {
                //invalid move was selected
                System.out.printf("\n\nmove %d is invalid\n\n", moveIndex);
                //check that there is at least 1 valid move. if not the game should have ended
                if (!validMoveTable.stream().anyMatch(num -> num == 1)) throw new IllegalArgumentException();
                moveIndex = validMoveTable.indexOf(1);
            }
            this.game.getNextState(moveIndex);
        }
        if (VERBOSE) System.out.printf("Game Over at turn %d. MrXWon = %b", it, this.game.getGameEnded() == 1);
        return this.game.getGameEnded();//1 means mrX won
    }

    Pair<Integer, Integer> playGames(int numOfGames, int numOfSims) throws IOException {
        numOfGames /= 2;
        int p1Won = 0;
        int p2Won = 0;
        for (int i = 0; i < numOfGames; i++) {
            int gameResult = this.playGame(numOfSims);
            if (gameResult == 1) p1Won++;
            else p2Won++;
        }

        //switch players so each ai play both mrx and detectives
        List<NeuralNet> temp = this.newNnets;
        this.newNnets = this.prevNnets;
        this.prevNnets = temp;

        for (int i = 0; i < numOfGames; i++) {
            int gameResult = this.playGame(numOfSims);
            if (gameResult == 1) p2Won++; //p2 is mrX now
            else p1Won++;
        }

        return new Pair<>(p1Won, p2Won);
    }
}
