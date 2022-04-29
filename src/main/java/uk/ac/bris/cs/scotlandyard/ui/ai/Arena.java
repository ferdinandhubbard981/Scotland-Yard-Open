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
    Ai player1;
    Ai player2;
    Game game;
    MCTS mcts1;
    MCTS mcts2;
//    public Arena(String ai1Name, String ai2Name, Game game) {
//        ImmutableList<Ai> ais = scanAis();
//        this.player1 = ais.stream().filter(ai -> ai.name().equals(ai1Name)).findAny().get(); //player1 is always mrX because he plays first
//        this.player2 = ais.stream().filter(ai -> ai.name().equals(ai2Name)).findAny().get();
//        this.game = game;
//    }

    public Arena(Game game, MCTS mcts1, MCTS mcts2) {
        this.mcts1 = mcts1;
        this.mcts2 = mcts2;
        this.game = game;
    }

    Integer playGame(int numOfSims) {
        //performs one game
        //returns 1 if player1 wins, -1 if player2 wins

        try {
            this.game.getInitBoard();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
//        List<Ai> players = new ArrayList<>();
//        players.add(this.player1);
//        players.add(this.player2);
        List<MCTS> players = new ArrayList<>();
        players.add(this.mcts1);
        players.add(this.mcts2);

        //start new game
        //TODO pit mrX vs Det
        int it = 0;
        //while game not ended
        while (this.game.getGameEnded() == 0) {
            it++;
            if (VERBOSE) {
                System.out.printf("\n\nTurn %d \n\n", it, this.game.currentIsMrX);
                System.out.print("Player: " + this.game.currentIsMrX);
            }

            //pick move
            int aiIndex = (this.game.currentIsMrX) ? 0: 1;
            Game tempGame = new Game(this.game);
//            int moveIndex = this.game.getMoveIndex(players.get(aiIndex).pickMove(gameState, MOVETIME));
            List<Float> policy = players.get(aiIndex).getActionProb(tempGame, numOfSims);
            Float highestPolicyVal = policy.stream()
                    .reduce(0f, (maxVal, element) -> maxVal = (element > maxVal) ? element: maxVal);
            int moveIndex = policy.indexOf(highestPolicyVal);
            List<Integer> validMoveTable = this.game.getValidMoveTable();
            if (validMoveTable.get(moveIndex) == 0) {
                //invalid move was selected
                System.out.printf("\n\nmove %d is invalid\n\n", moveIndex);
                //check that there is at least 1 valid move. if not the game should have ended
                assert(validMoveTable.stream().anyMatch(num -> num == 1));
                moveIndex = validMoveTable.indexOf(1);
            }
            this.game.getNextState(moveIndex);
        }
        if (VERBOSE) System.out.printf("Game Over at turn %d. MrXWon = %b", it, this.game.getGameEnded() == 1);
        return this.game.getGameEnded();//1 means mrX won
    }

    Pair<Integer, Integer> playGames(int numOfGames, int numOfSims) {
        numOfGames /= 2;
        int p1Won = 0;
        int p2Won = 0;
        for (int i = 0; i < numOfGames; i++) {
            int gameResult = this.playGame(numOfSims);
            if (gameResult == 1) p1Won++;
            else p2Won++;
        }

        //switch players so each ai play both mrx and detectives
        Ai temp = this.player1;
        this.player1 = this.player2;
        this.player2 = temp;

        for (int i = 0; i < numOfGames; i++) {
            int gameResult = this.playGame(numOfSims);
            if (gameResult == 1) p2Won++; //p2 is mrX now
            else p1Won++;
        }

        return new Pair<>(p1Won, p2Won);
    }
}
