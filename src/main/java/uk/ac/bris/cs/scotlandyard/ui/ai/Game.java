package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.javatuples.Quintet;
import org.javatuples.Triplet;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

public class Game {
    private static final String MOVEMAPFILEPATH = "moveArcs.txt";
    //found using python script to read graph.txt
    MyGameState currentState;
    boolean currentIsMrX;
    Set<Move> validMoves;
    public static final int POSSIBLEMOVES = 467; //TODO 467 is a mistake it doesn't take in account doubleMoves and SecretMoves

    //VALUES FOR NNET ENCODING OF BOARD
    public static final int NNETINPUTBOARDSIZE = 199;
    public static final int PLAYERSINPUTSIZE = 6;
    public static final int TICKETSINPUTSIZE = 5;

    //TODO instantiate moveMap in onStart() function using graph.txt
    //moveMap: Quintet<source, dest1, dest2, Ticket1, Ticket2> -> Integer
    //if move is singleMove then dest2 = null and Ticket2 = null
    public final Map<Quintet<Integer, Integer, Integer, Ticket, Ticket>, Integer> moveMap; //maps every one of the $POSSIBLEMOVES to an integer
    public Game() throws IOException {
        this.moveMap = this.makeMoveMap();
        this.currentIsMrX = true;
    }

    private Map<Quintet<Integer, Integer, Integer, Ticket, Ticket>, Integer> makeMoveMap() throws IOException {
        Map<Quintet<Integer, Integer, Integer, Ticket, Ticket>, Integer> output = new HashMap<>();
        int numOfPossibleMoves = 0;
        //open file
        BufferedReader br = new BufferedReader(new FileReader(MOVEMAPFILEPATH));
        //make moveArcs map
        //Map<sourceMove, List<Triplet<source, destination, Ticket>>>
        Map<Integer, List<Triplet<Integer, Integer, Ticket>>> singleMoveArcs = new HashMap<>();
        String line;
        while ((line = br.readLine()) != null) {
            List<String> strings = List.of(line.split(" "));
            int source = Integer.getInteger(strings.get(0));
            List<Triplet<Integer, Integer, Ticket>> moveArcs = singleMoveArcs.getOrDefault(source, new ArrayList<>());
            moveArcs.addAll(stringToMoveArc(strings));
            singleMoveArcs.put(source, moveArcs);

        }
        //add single moves
        //iterate through every node on the graph
        for (int i = 1; i < NNETINPUTBOARDSIZE+1; i++) {
            //iterate through every move arc from source i
            for (Triplet<Integer, Integer, Ticket> moveArc : singleMoveArcs.get(i)) {
                int source = moveArc.getValue0(), dest1 = moveArc.getValue1();
                Ticket Ticket1 = moveArc.getValue2();
                //add move
                output.put(new Quintet<>(source, dest1, null, Ticket1, null), numOfPossibleMoves);
                numOfPossibleMoves++;
            }
        }
        //add double moves
        //iterate through every node on the graph
        for (int i = 0; i < NNETINPUTBOARDSIZE; i++) {
            //iterate through every move arc from source i
            for (Triplet<Integer, Integer, Ticket> moveArc : singleMoveArcs.get(i)) {
                int source = moveArc.getValue0(), dest1 = moveArc.getValue1();
                Ticket Ticket1 = moveArc.getValue2();
                //iterate thorugh every move arc from every destination
                for (Triplet<Integer, Integer, Ticket> moveArc2 : singleMoveArcs.get(dest1)) {
                    int dest2 = moveArc2.getValue1();
                    Ticket Ticket2 = moveArc2.getValue2();
                    //add move
                    output.put(new Quintet<>(source, dest1, dest2, Ticket1, Ticket2), numOfPossibleMoves);
                    numOfPossibleMoves++;
                }
            }
        }
        return output;
    }

    private List<Triplet<Integer, Integer, Ticket>> stringToMoveArc(List<String> inputStrings) {
        assert(inputStrings.size() == 3);
        List<Triplet<Integer, Integer, Ticket>> output = new ArrayList<>();
        //val0: source, val1: dest1, val2: Ticket
        List<Ticket> validTickets = getTickets(inputStrings.get(2));
        for (Ticket ticket : validTickets) {
            output.add(new Triplet<>(Integer.getInteger(inputStrings.get(0)), Integer.getInteger(inputStrings.get(1)), ticket));
        }
        return output;
    }

    private List<Ticket> getTickets(String s) {
        if (s.equals("Taxi")) return List.of(Ticket.TAXI, Ticket.SECRET);
        if (s.equals("Bus")) return List.of(Ticket.BUS, Ticket.SECRET);
        if (s.equals("Underground")) return List.of(Ticket.UNDERGROUND, Ticket.SECRET);
        if (s.equals("Ferry")) return List.of(Ticket.SECRET);
        throw new IllegalArgumentException("Tickets not found");

    }

    public void setValidMoves() {
        this.validMoves = this.currentState.getAvailableMoves();
    }

    public void getInitBoard() {
        GameSetup setup = null;
        try {
            setup = new GameSetup(standardGraph(), STANDARD24MOVES);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Player mrX = new Player(Piece.MrX.MRX, getRandomTickets(true) , generateMrXLocation(5));
        int numOfDetectives = ThreadLocalRandom.current().nextInt(1, DETECTIVES.size()+1);
        List<Player> detectives = new ArrayList<>();
        List<Integer> detectiveLocations = generateDetectiveLocations(5, numOfDetectives);
        for (int i = 0; i < numOfDetectives; i++) {
            //DEBUG detective color randomization??
            Player newDet = new Player(DETECTIVES.asList().get(i), getRandomTickets(false), detectiveLocations.get(i));
            detectives.add(newDet);
        }
        this.currentState = new MyGameState(setup, ImmutableSet.of(mrX.piece()), ImmutableList.of(), mrX, ImmutableList.copyOf(detectives));
        this.setValidMoves();
        this.currentIsMrX = updateCurrentPlayer();
    }

    private ImmutableMap<Ticket, Integer> getRandomTickets(boolean isMrX) {
        Map<Ticket, Integer> tickets = new HashMap<>();
        for (Ticket ticket: Ticket.values()) {
            //we want average tickets to be < 10 so average of 50 / average ¬7.5 give you average of
                    tickets.put(ticket, getRandomNumOfTickets(ticket, isMrX));
        }
        return ImmutableMap.copyOf(tickets);
    }

    private Integer getRandomNumOfTickets(Ticket ticket, boolean isMrX) {
        if (!isMrX && (ticket == Ticket.DOUBLE || ticket == Ticket.SECRET)) return 0;
        //returns weighted randomness
        final int[] desiredTicketAverages;  //TICKETS ORDER: TAXI, BUS, UNDERGROUND, DOUBLE, SECRET
        if (isMrX) desiredTicketAverages = new int[]{4, 3, 3, 2, 5};
        else desiredTicketAverages = new int[]{11, 8, 4, 0, 0};

        if (ticket == Ticket.TAXI) return ThreadLocalRandom.current().nextInt(0, 101) / ThreadLocalRandom.current().nextInt(1, 100 / desiredTicketAverages[0]);
        if (ticket == Ticket.BUS) return ThreadLocalRandom.current().nextInt(0, 101) / ThreadLocalRandom.current().nextInt(1, 100 / desiredTicketAverages[1]);
        if (ticket == Ticket.UNDERGROUND) return ThreadLocalRandom.current().nextInt(0, 101) / ThreadLocalRandom.current().nextInt(1, 100 / desiredTicketAverages[2]);
        if (ticket == Ticket.DOUBLE) return ThreadLocalRandom.current().nextInt(0, 101) / ThreadLocalRandom.current().nextInt(1, 100 / desiredTicketAverages[3]);
        if (ticket == Ticket.SECRET) return ThreadLocalRandom.current().nextInt(0, 101) / ThreadLocalRandom.current().nextInt(1, 100 / desiredTicketAverages[4]);
        throw new IllegalArgumentException("ticket not found");
    }

    private GameSetup getRandomSetup() throws IOException {

        int numOfRounds = ThreadLocalRandom.current().nextInt(1, 100);

        Set<Integer> revealMoves = new HashSet<>();
        for (int i = 1; i < numOfRounds+1; i++) {
            //1 in 5 chance of move being revealmove
            if (ThreadLocalRandom.current().nextInt(0, 5) == 0) revealMoves.add(i);
        }
        ImmutableList<Boolean> moveSetup = IntStream.rangeClosed(1, numOfRounds)
                .mapToObj(revealMoves::contains)
                .collect(ImmutableList.toImmutableList());;

        return new GameSetup(standardGraph(), moveSetup);
    }

    public void getNextState(int moveIndex) {
        this.currentState = this.currentState.advance(getMoveFromIndex(moveIndex)); //TODO don't create new gameState. Apparently it's inefficient (make your own more efficient version?)
        this.setValidMoves();
        this.currentIsMrX = updateCurrentPlayer();
    }

    public Move getMoveFromIndex(int moveIndex) {
        Stream<Move> filteredMoves = this.validMoves.stream().filter(move -> getMoveIndex(move) == moveIndex);
        if (filteredMoves.toList().size() > 1) throw new IllegalArgumentException("\n\nmore than one matching move\n\n");
        else if (filteredMoves.toList().size() == 0) throw new IllegalArgumentException("\n\nno matching move\n\n");
        return filteredMoves.findAny().orElseThrow();
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
    private Quintet<Integer, Integer, Integer, Ticket, Ticket> getStrippedMove(Move move) {
        return move.accept(new Move.Visitor<>() {
            public Quintet<Integer, Integer, Integer, Ticket, Ticket> visit(Move.SingleMove singleMove) {
                return new Quintet<>(singleMove.source(), singleMove.destination, null, singleMove.ticket, null);
            }

            public Quintet<Integer, Integer, Integer, Ticket, Ticket> visit(Move.DoubleMove doubleMove) {
                return new Quintet<>(doubleMove.source(), doubleMove.destination1, doubleMove.destination2, doubleMove.ticket1, doubleMove.ticket2);
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
        return this.validMoves.stream().findAny().orElseThrow().commencedBy().isMrX();
    }

    public List<List<Integer>> getEncodedBoard() {
        List<List<Integer>> board = Collections.nCopies(NNETINPUTBOARDSIZE, Collections.nCopies(PLAYERSINPUTSIZE, 0));
//       Get MrX location
        List<Integer> playerLocations = Collections.nCopies(PLAYERSINPUTSIZE, -1); //playerLocationsList order will match order of playerTickets
        //ie if RED player is first in player locations, he will also be first in playerTickets
//        getting mrX location.
//        NOTE: location -1 means mrX location has not been revealed yet and will not be set on board
        List<LogEntry> mrXRevealedTravelLog = this.currentState.getMrXTravelLog().stream()
                .filter(logEntry -> logEntry.location().isPresent()).toList();
        if (!mrXRevealedTravelLog.isEmpty()) {
            //mrX location has been revealed
            //set mrX location to last known location
            playerLocations.set(0, mrXRevealedTravelLog.get(mrXRevealedTravelLog.size()).location().orElseThrow());
        }
        else playerLocations.set(0,-1); //set mrX location to -1 if it hasn't been revealed yet

        //Get Detective Locations
        List<Piece> detectives = this.currentState.getPlayers().stream().filter(Piece::isDetective).toList();
        for (Piece det : detectives) {
            int location = this.currentState.getDetectiveLocation((Piece.Detective) det).orElse(0);
            playerLocations.set(this.getPlayerIndex(det), location);
        }

//        encode players
        for (int i = 0; i < playerLocations.size(); i++) {
            int location = playerLocations.get(i);
            if (location != -1) {
                List<Integer> oneHotEncodedPlayers = Collections.nCopies(PLAYERSINPUTSIZE, 0);
                oneHotEncodedPlayers.set(i, 1);
                board.set(location, oneHotEncodedPlayers);
            }
        }
        return board;
    }

    private int getPlayerIndex(Piece player) { //ORDER MRX(BLACK), RED, GREEN, BLUE, WHITE, YELLOW

        if (player.webColour().equals(Piece.MrX.MRX.webColour())) return 0;
        else if (player.webColour().equals(Piece.Detective.RED.webColour())) return 1;
        else if (player.webColour().equals(Piece.Detective.GREEN.webColour())) return 2;
        else if (player.webColour().equals(Piece.Detective.BLUE.webColour())) return 3;
        else if (player.webColour().equals(Piece.Detective.WHITE.webColour())) return 4;
        else if (player.webColour().equals(Piece.Detective.YELLOW.webColour())) return 5;

        else throw new IllegalArgumentException("piece did not match any player");

    }


    public List<List<Integer>> getEncodedPlayerTickets() {
        //instantiate all ints to 0
        List<List<Integer>> playerTicketsOneHotEncoded = Collections.nCopies(PLAYERSINPUTSIZE, Collections.nCopies(TICKETSINPUTSIZE, 0));
        List<Board.TicketBoard> playerTickets = Collections.nCopies(TICKETSINPUTSIZE, null);
        //iterate through players
        for (Piece player : this.currentState.getPlayers()) {
            Optional<Board.TicketBoard> tickets = this.currentState.getPlayerTickets(player);
            //add tickets
            playerTickets.set(getPlayerIndex(player), tickets.orElseThrow());
        }
        //one hot encode
        for (int i = 0; i < PLAYERSINPUTSIZE; i++) {
            Board.TicketBoard tickets = playerTickets.get(i);
            List<Integer> ticketIntList = new ArrayList<>();
            if (tickets != null) {
                for (Ticket ticket : Ticket.values()) {
                    ticketIntList.set(ticket.ordinal(), tickets.getCount(ticket)); //DEBUG is ordinal() correct?
                }
            }
            else {
                ticketIntList = Collections.nCopies(TICKETSINPUTSIZE, 0);
            }
            playerTicketsOneHotEncoded.set(i, ticketIntList);
        }
        return playerTicketsOneHotEncoded;
    }

    public Integer getNumOfRoundsSinceReveal() {
        List<LogEntry> mrXTravelLog = this.currentState.getMrXTravelLog();
        //check if there has not been a reveal move yet
        if (mrXTravelLog.stream().noneMatch(logEntry -> logEntry.location().isPresent())) {
            return mrXTravelLog.size();
        }
        else {
            boolean foundRevealMove = false;
            int numOfRounds = -1;
            //iterate thorugh travel log backwards
            //if not found reveal increment numofrounds
            //if found return num of rounds
            for (int i = mrXTravelLog.size()-1; i > -1 && !foundRevealMove; i--) {
                if (mrXTravelLog.get(i).location().isPresent()) foundRevealMove = true;
                numOfRounds++;
            }
            return numOfRounds;
        }
    }
}


