package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Game {
    private static final int POSSIBLEMOVES = 467;
    public Game(Board.GameState gameState) {
    }
    
    public Board.GameState getInitBoard() {
        GameSetup setup = null;
        try {
            setup = new GameSetup(ScotlandYard.standardGraph(), ScotlandYard.STANDARD24MOVES);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Player mrX = new Player(Piece.MrX.MRX, ScotlandYard.defaultMrXTickets() , ScotlandYard.generateMrXLocation(5));
        List<Player> detectives = new ArrayList<>();
        List<Integer> detectiveLocations = ScotlandYard.generateDetectiveLocations(5, 5);
        for (int i = 0; i < ScotlandYard.DETECTIVES.size(); i++) {
            Player newDet = new Player(ScotlandYard.DETECTIVES.asList().get(i), ScotlandYard.defaultDetectiveTickets(), detectiveLocations.get(i));
            detectives.add(newDet);
        }
        return new MyGameStateFactory().build(setup, mrX, ImmutableList.copyOf(detectives));
    }

//    private Object getRandomTickets(boolean isMrX) {
//        Map<ScotlandYard.Ticket, Integer> tickets = new HashMap<>();
//        for (ScotlandYard.Ticket ticket: Arrays.stream(ScotlandYard.Ticket.values())
//                .filter(ticket -> ticket != ScotlandYard.Ticket.DOUBLE && ticket != ScotlandYard.Ticket.SECRET)
//                .collect(Collectors.toSet())) {
//            tickets.put(ticket, ThreadLocalRandom.current().nextInt(0, 101))
//        } ScotlandYard.defaultMrXTickets()
//        if (isMrX) {
//            tickets.put(ScotlandYard.Ticket.SECRET, )
//        }
//        else {
//
//        }
//
//        return tickets;
//    }

//    private GameSetup getRandomSetup() {
//
//        int numOfMoves = ThreadLocalRandom.current().nextInt(1, 100);
//
//        Set<Integer> revealMoves = new HashSet<>();
//        for (int i = 1; i < numOfMoves+1; i++) {
//            //1 in 5 chance of move being revealmove
//            if (ThreadLocalRandom.current().nextInt(0, 5) == 0) revealMoves.add(i);
//        }
//        ImmutableList<Boolean> moveSetup = IntStream.rangeClosed(1, numOfMoves)
//                .mapToObj(revealMoves::contains)
//                .collect(ImmutableList.toImmutableList());;
//        try {
//            return new GameSetup(ScotlandYard.standardGraph(), moveSetup);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public int getActionSize() {
        return this.POSSIBLEMOVES;
    }

    public Board.GameState getNextState(Board.GameState gameState, Move move) {
        return gameState.advance(move);
    }

    public Set<Move> getValidMoves(Board.GameState gameState) {
        return gameState.getAvailableMoves();
    }

    public int getGameEnded(Board.GameState gameState) {
        //no winner
        if (gameState.getWinner().isEmpty()) return 0;
        //mrX winner
        else if (gameState.getWinner().stream().anyMatch(piece -> piece.isMrX())) return 1;
        //detective Winners
        else return -1;
    }

    public String stringRepresentation(Board.GameState gameState) {
        return gameState.toString();
    }

}
