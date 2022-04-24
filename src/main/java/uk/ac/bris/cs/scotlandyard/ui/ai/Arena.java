package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static uk.ac.bris.cs.scotlandyard.ResourceManager.scanAis;

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

    public Arena(MCTS mcts1, MCTS mcts2, Game game) {
        this.mcts1 = mcts1;
        this.mcts2 = mcts2;
        this.game = game;
    }

    Integer playGame(int numOfSims) {
        //performs one game
        //returns 1 if player1 wins, -1 if player2 wins

        MyGameState gameState = this.game.getInitBoard();
//        List<Ai> players = new ArrayList<>();
//        players.add(this.player1);
//        players.add(this.player2);
        List<MCTS> players = new ArrayList<>();
        players.add(this.mcts1);
        players.add(this.mcts2);

        //start new game
        //TODO make it random
        int it = 0;
        //while game not ended
        while (this.game.getGameEnded(gameState) == 0) {
            it++;
            if (VERBOSE) System.out.printf("Turn %d Player %d%n", it, this.game.currentIsMrX);

            //pick move
            int aiIndex = (this.game.currentIsMrX) ? 0: 1;
//            int moveIndex = this.game.getMoveIndex(players.get(aiIndex).pickMove(gameState, MOVETIME));
            List<Float> policy = players.get(aiIndex).getActionProb(gameState, numOfSims);
            Float highestPolicyVal = policy.stream()
                    .reduce(0f, (maxVal, element) -> maxVal = (element > maxVal) ? element: maxVal);
            int moveIndex = policy.indexOf(highestPolicyVal);
            List<Integer> validMoves = this.game.getValidMoveIndexes();
            if (validMoves.get(moveIndex) == 0) {
                //invalid move was selected
                System.out.printf("\n\nmove %d is invalid\n\n", moveIndex);
                assert(!validMoves.isEmpty());
            }
            gameState = this.game.getNextState(gameState, moveIndex);
        }
        if (VERBOSE) System.out.printf("Game Over at turn %d. MrXWon = %b", it, this.game.getGameEnded(gameState) == 1);
        return this.game.getGameEnded(gameState);//1 means mrX won
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
