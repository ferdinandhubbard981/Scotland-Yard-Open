package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

public class NnetAI implements Ai {
    private static final int MRXUNKOWNPOSITION = 100;
    Game gameAPI;
    @Nonnull @Override public String name() { return "NnetAI";}

    @Nonnull @Override public void onStart() {

        try {
            this.gameAPI = new Game();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }



    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {
        //set currentState from board
        this.gameAPI.setGameState(build(board));
        this.gameAPI.setValidMoves();
        this.gameAPI.updateCurrentPlayer();
        NeuralNet mrXNnet = null;
        NeuralNet detNnet = null;
        try {
            mrXNnet = new NeuralNet(this.gameAPI, true);
            detNnet = new NeuralNet(this.gameAPI, false);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        MCTS mcts = new MCTS(mrXNnet, detNnet);
        Game tempGame = new Game(this.gameAPI);
        List<Float> policy = mcts.getActionProb(tempGame, 5);
        Float highestPolicyVal = policy.stream()
                .reduce(0f, (maxVal, element) -> maxVal = (element > maxVal) ? element: maxVal);
        int moveIndex = policy.indexOf(highestPolicyVal);
        this.gameAPI.setValidMoves();
        List<Integer> validMoveTable = this.gameAPI.getValidMoveTable();
        if (moveIndex == -1 || validMoveTable.get(moveIndex) == 0) {
            //invalid move was selected
            System.out.printf("\n\nmove %d is an invalid move\n\n", moveIndex);
            //check that there is at least 1 valid move. if not the game should have ended
            assert(validMoveTable.stream().anyMatch(num -> num == 1));
            //setting moveIndex to a random valid move;
            moveIndex = validMoveTable.indexOf(1);
        }
        Move output = this.gameAPI.getMoveFromIndex(moveIndex);
        if (output == null) output = board.getAvailableMoves().stream().findFirst().get();
        return output;
    }

    static public MyGameState build(Board board) {
        ImmutableSet<Piece> remaining = getRemainingPieces(board.getAvailableMoves());
        Player mrX = getPlayer(board, Piece.MrX.MRX);
        List<Player> detectives = new ArrayList<>();
        for (Piece piece : board.getPlayers().stream().filter(Piece::isDetective).toList()) {
            detectives.add(getPlayer(board, piece));
        }
        return new MyGameState(board.getSetup(), remaining, board.getMrXTravelLog(), mrX, ImmutableList.copyOf(detectives));
    }

    private Player getPlayer(Board board, Piece piece) {
        ImmutableMap<Ticket, Integer> tickets = ticketBoardToMap(board.getPlayerTickets(piece).orElseThrow());
        int location;
        if (piece.isMrX()) {
            //if not mrx turn mrx playing = 0. otherwise mrx playing = mrx position
            int mrXPlaying = board.getAvailableMoves().stream().filter(move -> move.commencedBy().isMrX()).map(move -> move.source()).findFirst().orElse(0);
            if (mrXPlaying != 0) {
                location = mrXPlaying;
            }
            else {
                List<LogEntry> mrXRevealedTravelLog = board.getMrXTravelLog().stream()
                        .filter(logEntry -> logEntry.location().isPresent()).toList();
                if (!mrXRevealedTravelLog.isEmpty()) {
                    //mrX location has been revealed
                    //set mrX location to last known location
                    location = mrXRevealedTravelLog.get(mrXRevealedTravelLog.size() - 1).location().orElseThrow();
                }
                else location = MRXUNKOWNPOSITION; //if mrx position is uknown we just place him in the center of the board //todo
            }
        }
        else location = board.getDetectiveLocation((Piece.Detective) piece).orElseThrow();
        return new Player(piece, tickets, location);
    }

    ImmutableMap<Ticket, Integer> ticketBoardToMap(Board.TicketBoard ticketBoard) {
        Map<Ticket, Integer> tickets = new HashMap<>();
        for (Ticket ticket : Ticket.values()) {
            tickets.put(ticket, ticketBoard.getCount(ticket));
        }
        return ImmutableMap.copyOf(tickets);
    }
    private ImmutableSet<Piece> getRemainingPieces(ImmutableSet<Move> availableMoves) {
        //DEBUG if a detective(1) is blocking the only possible move of another detective(2) who has yet to move and that detective(1) moves out of the way, then this function is innacurate
        //however this doesn't cause a big innacuracy in MTCS search
        //to fix this I would need to check for detectives blocking moves myself

        Set<Piece> remainingPlayers = availableMoves.stream().map(Move::commencedBy).collect(Collectors.toSet());
        return ImmutableSet.copyOf(remainingPlayers);
    }
}