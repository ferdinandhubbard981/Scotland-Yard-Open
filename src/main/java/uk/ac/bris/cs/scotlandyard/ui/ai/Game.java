package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.javatuples.Quintet;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class Game {
    //found using python script to read graph.txt
    MyGameState currentState;
    boolean currentIsMrX;
    Set<Move> validMoves;
    public static final int POSSIBLEMOVES = 467; //TODO 467 is a mistake it doesn't take in account doubleMoves and SecretMoves

    //VALUES FOR NNET ENCODING OF BOARD
    private static final int NUMOFNODESINGRAPH = 199;
    public static final int NNETINPUTBOARDSIZE = NUMOFNODESINGRAPH+1; //NODE 0 is representative of unknown location
    public static final int PLAYERSINPUTSIZE = 6;
    public static final int TICKETSINPUTSIZE = 5;

    //TODO instantiate moveMap in onStart() function using graph.txt
    //moveMap: Quintet<source, dest1, dest2, transport1, transport2> -> Integer
    //if move is singleMove then dest2 = -1 and transport2 = null
    public final Map<Quintet<Integer, Integer, Integer, ScotlandYard.Transport, ScotlandYard.Transport>, Integer> moveMap; //maps every one of the $POSSIBLEMOVES to an integer
    public Game(Map<Quintet<Integer, Integer, Integer, ScotlandYard.Transport, ScotlandYard.Transport>, Integer> moveMap) {
        this.moveMap = moveMap;
        this.currentIsMrX = true;
    }

    public void setValidMoves() {
        this.validMoves = this.currentState.getAvailableMoves();
    }
    //TODO randomize game setup
    public void getInitBoard() {
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
        this.currentState = new MyGameState(setup, ImmutableSet.of(mrX.piece()), ImmutableList.of(), mrX, ImmutableList.copyOf(detectives));
        this.setValidMoves();
        this.currentIsMrX = updateCurrentPlayer();
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

    public void getNextState(int moveIndex) {
        Stream<Move> filteredMoves = this.validMoves.stream().filter(move -> getMoveIndex(move) == moveIndex);
        if (filteredMoves.toList().size() > 1) throw new IllegalArgumentException("\n\nmore than one matching move\n\n");
        else if (filteredMoves.toList().size() == 0) throw new IllegalArgumentException("\n\nno matching move\n\n");
        this.currentState = this.currentState.advance(filteredMoves.findAny().get()); //TODO don't create new gameState. Apparently it's inefficient (make your own more efficient version?)
        this.setValidMoves();
        this.currentIsMrX = updateCurrentPlayer();
    }

    public int getMoveIndex(Move move) {
        return moveMap.get(getStrippedMove(move));
    }

    public List<Integer> getMoveTable(Set<Move> moves) {
        //populate moveMask with 0
        List<Integer> moveMask = Collections.nCopies(POSSIBLEMOVES, 0);
        List<Integer> moveIndexes = moves.stream().map(this::getMoveIndex).toList();
        for (int move : moveIndexes) moveMask.set(move, 1);
        return moveMask;
    }
//    @param return List<Integer> len $POSSIBLEMOVES
    public List<Integer> getValidMoveTable() {
        return getMoveTable(this.validMoves);
    }

    ScotlandYard.Transport ticketToTransport(ScotlandYard.Ticket ticket) {

        return switch (ticket) {
            case DOUBLE -> throw new IllegalArgumentException();
            case SECRET -> ScotlandYard.Transport.FERRY; //because we are marking Ferry == Secret therefore also marking all secret
            //and non-ferry moves as the same thing ie: ferry move = secret move
            case TAXI -> ScotlandYard.Transport.TAXI;
            case BUS -> ScotlandYard.Transport.BUS;
            default -> ScotlandYard.Transport.UNDERGROUND;
        };
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

    public int getGameEnded() {
        //no winner
        if (this.currentState.getWinner().isEmpty()) return 0;
        //Winner == current player
        else if (this.currentState.getWinner().stream().anyMatch(Piece::isMrX) == this.currentIsMrX) return 1;
        //Winner != current player
        else return -1;
    }

    public String stringRepresentation() {
        return this.currentState.toString(); //TODO check if this is enough for hashmap (needs to generate a unique key)
    }

    private Boolean updateCurrentPlayer() {
        return this.validMoves.stream().findAny().get().commencedBy().isMrX();
    }

    public List<List<Integer>> getEncodedBoard() {
//        TODO
        List<List<Integer>> board = Collections.nCopies(NNETINPUTBOARDSIZE, Collections.nCopies(PLAYERSINPUTSIZE, 0));
//        get player locations
        List<Integer> playerLocations = new ArrayList<>(); //playerLocationsList ORDER is as Follows: MRX, RED, GREEN, BLUE, WHITE, YELLOW
//        getting mrX location. NOTE: location 0 means mrX location is unknown
        ImmutableList<LogEntry> mrXTravelLog = this.currentState.getMrXTravelLog();
        if (!mrXTravelLog.isEmpty()) {
            //mrX location is not unknown
//            playerLocations.set(0, this.currentState.getMrXTravelLog().)
        }
//        encode players
        return board;
    }


    public List<List<Integer>> getEncodedPlayerTickets() {
//        TODO
        return null;
    }

    public Integer getNumOfRoundsSinceReveal() {
//        TODO
        return null;
    }
}


