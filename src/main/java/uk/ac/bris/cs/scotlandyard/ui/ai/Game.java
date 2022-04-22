package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import org.javatuples.Quintet;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class Game {
    //found using python script to read graph.txt
    Set<Move> validMoves;
    private static final int POSSIBLEMOVES = 467; //TODO 467 is a mistake it doesn't take in account doubleMoves and SecretMoves
    //TODO instantiate moveMap in onStart() function using graph.txt
    //moveMap: Quintet<source, dest1, dest2, transport1, transport2> -> Integer
    //if move is singleMove then dest2 = -1 and transport2 = null
    public final Map<Quintet<Integer, Integer, Integer, ScotlandYard.Transport, ScotlandYard.Transport>, Integer> moveMap; //maps every one of the $POSSIBLEMOVES to an integer
    public Game(Map<Quintet<Integer, Integer, Integer, ScotlandYard.Transport, ScotlandYard.Transport>, Integer> moveMap) {
        this.moveMap = moveMap;
    }

    public void setValidMoves(Board.GameState gameState) {
        this.validMoves = gameState.getAvailableMoves();
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
        return POSSIBLEMOVES;
    }

    public Board.GameState getNextState(Board.GameState gameState, int moveIndex) {
        Stream<Move> filteredMoves = this.validMoves.stream().filter(move -> this.moveMap.get(getStrippedMove(move)) == moveIndex);
        if (filteredMoves.toList().size() > 1) throw new IllegalArgumentException("\n\nmore than one matching move\n\n");
        else if (filteredMoves.toList().size() == 0) throw new IllegalArgumentException("\n\nno matching move\n\n");
        return gameState.advance(filteredMoves.findAny().get()); //TODO don't create new gameState that's inefficient apparently (make your own more efficient version?)
    }

//    @param return List<Integer> len $POSSIBLEMOVES
    public List<Integer> getValidMoveIndexes(Board.GameState gameState) {
        //populate moveMask with 0
        List<Integer> moveMask = Collections.nCopies(this.getActionSize(), 0);
        List<Integer> validMoveIndexes = this.validMoves.stream().map(move -> moveMap.get(getStrippedMove(move))).toList();
        for (int move : validMoveIndexes) {
            moveMask.set(move, 1);
        }
        return moveMask;
    }

    ScotlandYard.Transport ticketToTransport(ScotlandYard.Ticket ticket) {

        switch(ticket) {
            case DOUBLE:
                throw new IllegalArgumentException();
            case SECRET:
                return ScotlandYard.Transport.FERRY; //because we are marking Ferry == Secret therefore also marking all secret
                //and non-ferry moves as the same thing ie: ferry move = secret move
            case TAXI: return ScotlandYard.Transport.TAXI;
            case BUS: return ScotlandYard.Transport.BUS;
            default: return ScotlandYard.Transport.UNDERGROUND;
        }
    }
    private Quintet<Integer, Integer, Integer, ScotlandYard.Transport, ScotlandYard.Transport> getStrippedMove(Move move) {
        return move.accept(new Move.Visitor<>() {
            public Quintet<Integer, Integer, Integer, ScotlandYard.Transport, ScotlandYard.Transport> visit(Move.SingleMove singleMove) {
                return new Quintet<>(singleMove.source(), singleMove.destination, -1, ticketToTransport(singleMove.ticket), null);
            }

            public Quintet<Integer, Integer, Integer, ScotlandYard.Transport, ScotlandYard.Transport> visit(Move.DoubleMove doubleMove) {
                return new Quintet<>(doubleMove.source(), doubleMove.destination1, doubleMove.destination2, ticketToTransport(doubleMove.ticket1), ticketToTransport(doubleMove.ticket2));
            }
        });

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
        return gameState.toString(); //TODO do proper implementation (I do not think .toString() works)
    }

    public Boolean isMaximisingForMrX() {
        return this.validMoves.stream().findAny().get().commencedBy().isMrX();
    }
}
