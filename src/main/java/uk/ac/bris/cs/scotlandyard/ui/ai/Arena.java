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
    public Arena(String ai1Name, String ai2Name, Game game) {
        ImmutableList<Ai> ais = scanAis();
        this.player1 = ais.stream().filter(ai -> ai.name().equals(ai1Name)).findAny().get(); //player1 is always mrX because he plays first
        this.player2 = ais.stream().filter(ai -> ai.name().equals(ai2Name)).findAny().get();
        this.game = game;
    }

    Integer playGame() {
        //performs one game
        //returns 1 if player1 wins, -1 if player2 wins

        Board.GameState gameState = this.game.getInitBoard();
        int numOfDetectives = gameState.getPlayers().stream().filter(Piece::isDetective).toList().size();

        List<Ai> players = new ArrayList<>();
        players.add(this.player1);
        for (int i = 0; i < numOfDetectives; i++) {
            players.add(this.player2);
        }

        int curPlayer = 0;
        //start new game
        //TODO make it random
        int it = 0;
        //while game not ended
        while (this.game.getGameEnded(gameState) == 0) {
            it++;
            if (VERBOSE) System.out.printf("Turn %d Player %d%n", it, curPlayer);

            //pick move
            int moveIndex = this.game.getMoveIndex(players.get(curPlayer + 1).pickMove(gameState, MOVETIME));
            //if 5 detectives 0, 1, 2, 3, 4, 0
            curPlayer = (curPlayer + 1) % (numOfDetectives+1);//change player for next move
            List<Integer> validMoves = this.game.getValidMoveIndexes();
            if (validMoves.get(moveIndex) == 0) {
                //invalid move was selected
                System.out.printf("\n\nmove %d is invalid\n\n", moveIndex);
                assert(!validMoves.isEmpty());
            }
            gameState = this.game.getNextState(gameState, moveIndex);
            this.game.setValidMoves(gameState);
        }
        if (VERBOSE) System.out.printf("Game Over at turn %d. MrXWon = %b", it, this.game.getGameEnded(gameState) == 1);
        return this.game.getGameEnded(gameState);//1 means mrX won
    }

    Pair<Integer, Integer> playGames(int numOfGames) {
        numOfGames /= 2;
        int p1Won = 0;
        int p2Won = 0;
        for (int i = 0; i < numOfGames; i++) {
            int gameResult = this.playGame();
            if (gameResult == 1) p1Won++;
            else p2Won++;
        }

        //switch players so each ai play both mrx and detectives
        Ai temp = this.player1;
        this.player1 = this.player2;
        this.player2 = temp;

        for (int i = 0; i < numOfGames; i++) {
            int gameResult = this.playGame();
            if (gameResult == 1) p2Won++; //p2 is mrX now
            else p1Won++;
        }

        return new Pair<>(p1Won, p2Won);
    }
}
