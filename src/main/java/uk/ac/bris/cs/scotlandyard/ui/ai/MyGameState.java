package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MyGameState implements Board.GameState {
    private final GameSetup setup;
    private final ImmutableSet<Piece> remaining; //the players who have yet to play in the round??
    private final ImmutableList<LogEntry> log;
    private final Player mrX;
    private final ImmutableList<Player> detectives;
    private final ImmutableSet<Move> moves;
    private final ImmutableSet<Piece> winner;


    public MyGameState(
            final GameSetup setup,
            final ImmutableSet<Piece> remaining,
            final ImmutableList<LogEntry> log,
            final Player mrX,
            final ImmutableList<Player> detectives) {

        this.setup = setup;
        this.remaining = remaining;
        this.log = log;
        this.mrX = mrX;
        this.detectives = detectives;

        //checking for null inputs
        if (mrX == null) throw new NullPointerException();
        if (detectives == null) throw new NullPointerException();
        if (detectives.contains(null)) throw new NullPointerException();

        //check detectives have 0 x2 & secret tickets
        detectives.forEach((det) -> {
            if (det.has(ScotlandYard.Ticket.DOUBLE)) throw new IllegalArgumentException();
            if (det.has(ScotlandYard.Ticket.SECRET)) throw new IllegalArgumentException();
        });

        //check no duplicate detectives (colour) & no overlapping
        HashMap<String, Boolean> found = new HashMap<>();
        HashMap<Integer, String> locations = new HashMap<>();
        for (Player detective : detectives) {
            String colour = detective.piece().webColour();
            if (found.containsKey(colour) || locations.get(detective.location()) != null)
                throw new IllegalArgumentException();
            found.put(colour, true);
            locations.put(detective.location(), detective.piece().webColour());
        }

        //check empty graph
        if (setup.graph.nodes().size() == 0) throw new IllegalArgumentException();
        //check empty moves
        if (setup.moves.isEmpty()) throw new IllegalArgumentException();

        //get mrX & detective moves
        Set<Move> mrXMoves = getMoves(ImmutableList.of(mrX), detectives);
        Set<Move> detectiveMoves = getMoves(detectives, detectives);

        //if mrX is surrounded by detectives then detectives win aka mrX has no moves left
        if (mrXMoves.isEmpty() && remaining.contains(mrX.piece())) {
            this.winner = getDetectivesAsImmutableSet();
            this.moves = ImmutableSet.of();
            return;
        }
        //if a detective is on the same square than mrX then the detectives win
        for (Player detective : detectives) {
            if (mrX.location() == detective.location()) {
                this.winner = getDetectivesAsImmutableSet();
                this.moves = ImmutableSet.of();
                return;
            }
        }
        //if detectives have no moves left or logbook full then mrX wins
        if (detectiveMoves.isEmpty() || (log.size() == setup.moves.size() && remaining.isEmpty())) {
            this.winner = ImmutableSet.of(mrX.piece());
            this.moves = ImmutableSet.of();
            return;
        }

        //if none of the conditions above are met then carry on
        this.winner = ImmutableSet.of();
        //updates the ACTUAL moves list with the moves of the remaining players
        Set<Move> allMoves = new HashSet<>(mrXMoves);
        allMoves.addAll(detectiveMoves);
        this.moves = ImmutableSet.copyOf(getRemainingPlayersMoves(allMoves));

        testAllMovesHaveTickets();
    }

    private ImmutableSet<Piece> getDetectivesAsImmutableSet(){
        return ImmutableSet.copyOf(this.detectives
                .stream()
                .map(detective -> detective.piece())
                .collect(Collectors.toUnmodifiableSet())
        );
    }

    /**
     * @implNote iterates over transport methods of "source" and returns available moves given tickets.
     * @apiNote used for both mrX and detectives
     */
    private ImmutableSet<Move.SingleMove> getSingleMoves(
            GameSetup setup,
            ImmutableList<Player> detectives,
            Player player,
            int source) {

        //checking if source node exists
        if (!setup.graph.nodes().contains(source)) throw new IllegalArgumentException();

        ImmutableSet.Builder<Move.SingleMove> playerMoves = ImmutableSet.builder();

        Set<ScotlandYard.Ticket> availableTickets = Stream.of(ScotlandYard.Ticket.values())
                .filter(ticketType -> player.tickets().get(ticketType) > 0)
                .collect(Collectors.toSet());

        for (int destination : setup.graph.adjacentNodes(source)) {
            //check if detective is on destination node
            if (detectiveOnLocation(destination, detectives)) continue;
            //gets all transport methods a player can use given their tickets, and adds to playerMoves
            setup.graph
                    .edgeValueOrDefault(source, destination, ImmutableSet.of())
                    .stream()
                    .filter(transportMethod -> availableTickets.contains(transportMethod.requiredTicket()))
                    .forEach(transportMethod -> playerMoves
                            .add(new Move.SingleMove(
                                    player.piece(),
                                    source,
                                    transportMethod.requiredTicket(),
                                    destination
                            )));
            //if player has a secret ticket then add move using secret ticket
            if (availableTickets.contains(ScotlandYard.Ticket.SECRET))
                playerMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
        }
        return playerMoves.build();
    }

    /**
     * @implNote calls & iterates through getSingleMoves, then calls again to find double moves
     * @apiNote used for mrX only
     */
    private ImmutableSet<Move.DoubleMove> getDoubleMoves(
            GameSetup setup,
            ImmutableList<Player> detectives,
            Player player,
            int source) {
        ImmutableSet.Builder<Move.DoubleMove> doubleMoves = ImmutableSet.builder();
        // check if contains x2 ticket and has enough logbook space
        if (player.tickets().get(ScotlandYard.Ticket.DOUBLE) == 0 || log.size() + 2 > setup.moves.size())
            return doubleMoves.build();

        //get single moves
        ImmutableSet<Move.SingleMove> firstMoveList = getSingleMoves(setup, detectives, player, source);

        //iterate through every possible first move
        for (Move.SingleMove move1 : firstMoveList) {
            //making hypothetical player that has used a ticket in order to find second move
            Player hypotheticalPlayer = player.use(move1.ticket);
            //getting second move & building DoubleMove from 2 SingleMoves
            getSingleMoves(setup, detectives, hypotheticalPlayer, move1.destination)
                    .stream()
                    .forEach(move2 -> doubleMoves.add(buildDoubleMove(move1, move2)));
        }
        return doubleMoves.build();
    }


    @Override
    public GameSetup getSetup() {
        return this.setup;
    }

    @Override
    public ImmutableSet<Piece> getPlayers() {
        return ImmutableSet.<Piece>builder()
                .add(this.mrX.piece())
                .addAll(this.detectives.stream()
                        .map(detective -> detective.piece())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
        String colour = detective.webColour();
        return detectives.stream()
                .filter(x -> x.piece().webColour() == colour)
                .map(x -> x.location()).findFirst();
    }

    @Override
    public Optional<TicketBoard> getPlayerTickets(Piece piece) {
        ImmutableList<Player> allPlayers = ImmutableList.<Player>builder()
                .addAll(this.detectives)
                .add(this.mrX)
                .build();
        String colour = piece.webColour();
        Optional<Player> referencedPlayer = allPlayers.stream()
                .filter(player -> player.piece().webColour().equals(colour))
                .findFirst();

        if (referencedPlayer.isEmpty()) return Optional.empty();
        return Optional.of(ticket -> referencedPlayer.get().tickets().get(ticket));
    }

    @Override
    public ImmutableList<LogEntry> getMrXTravelLog() {
        return this.log;
    }

    @Override
    public ImmutableSet<Piece> getWinner() {
        return this.winner;
    }

    @Override
    public ImmutableSet<Move> getAvailableMoves() {
        return this.moves;
    }

    @Override
    public MyGameState advance(Move move) { //TODO make function void and only update current gamestate
//        TODO instead of returning a new one. This should be more efficient because constructors are greedy?
        if (!this.moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

        if (move.commencedBy() == this.mrX.piece()) {
            //add move to log (checking if setup.move is hidden or not)
            List<LogEntry> newLog = getNewLog(move, this.setup, this.log);
            //take used tickets away from mrX
            testMoveInAllMoves(move);
            testAllMovesHaveTickets();
            Player newmrX = this.mrX.use(move.tickets());
            //move mrX position to destination
            newmrX = newmrX.at(getMoveDestination(move));
            //swap to the detectives turn (update the remaining variable)
            //mrX plays first therefore all the detectives have yet to play their turn
            ImmutableSet<Piece> newRemainingPlayers = getDetectivesAsImmutableSet();
            return new MyGameState(this.setup,
                    newRemainingPlayers,
                    ImmutableList.copyOf(newLog),
                    newmrX,
                    ImmutableList.copyOf(detectives)
            );
        }
        //finding detective who made the move
        Player detective = this.detectives.stream()
                .filter(det -> det.piece() == move.commencedBy())
                .findFirst().get();
        int index = this.detectives.indexOf(detective);
        //move detective to destination & give ticket to mrX
        detective = detective.at(getMoveDestination(move)).use(move.tickets());
        Player newMrX = this.mrX.give(getSingleMoveTicket(move));

        //lambda expression needs value to be final
        final Player finalDetective = detective;
        //Ensure that particular detective won't move again this round (remove from remaining players)
        Set<Piece> newRemainingPlayers = this.remaining.stream()
                .filter(det -> det != finalDetective.piece())
                .collect(Collectors.toSet());

        //getting newRemainingPlayers in the form of ImmutableList<Player> for checking of moves afterwards
        Set<Piece> finalNewRemainingPlayers = newRemainingPlayers;
        ImmutableList<Player> remainingDetectives = ImmutableList.copyOf(detectives.stream()
                .filter(det -> finalNewRemainingPlayers.contains(det.piece()))
                .toList());
        //if remaining detectives have no more moves to play then swap to mrX turn (update remaining variable)
        if (getMoves(remainingDetectives, detectives).isEmpty())
            newRemainingPlayers = Set.of(this.mrX.piece());
        if (this.setup.moves.size() == this.log.size())
            newRemainingPlayers = Set.of();

        List<Player> mutableDetectives = new ArrayList<>(this.detectives);
        mutableDetectives.set(index, detective);
        return new MyGameState(this.setup,
                ImmutableSet.copyOf(newRemainingPlayers),
                this.log, newMrX,
                ImmutableList.copyOf(mutableDetectives)
        );
    }


    //helper functions

    //check if detective is on location
    boolean detectiveOnLocation(int location, ImmutableList<Player> detectives) {
        for (Player detective : detectives) {
            if (detective.location() == location) return true;
        }
        return false;
    }

    //combine two singleMoves into a double move
    Move.DoubleMove buildDoubleMove(Move.SingleMove move1, Move.SingleMove move2) {
        Move.DoubleMove doubleMove = new Move.DoubleMove(move1.commencedBy(), move1.source(), move1.ticket, move1.destination,
                move2.ticket, move2.destination);
        return doubleMove;
    }

    //gets the moves of the players who are in the remaining players set;
    Set<Move> getRemainingPlayersMoves(Set<Move> allMoves) {
        Set<Move> output = new HashSet<>();
        for (Move move : allMoves) {
            for (Piece piece : this.remaining) {
                if (move.commencedBy() == piece) output.add(move);
            }
        }
        return output;
    }

    //Find all moves for players list
    Set<Move> getMoves(List<Player> players, List<Player> detectives) {
        Set<Move> playerMoves = new HashSet<>();
        for (Player player : players) {
            playerMoves.addAll(getSingleMoves(this.setup, ImmutableList.copyOf(detectives), player, player.location()));
            playerMoves.addAll(getDoubleMoves(this.setup, ImmutableList.copyOf(detectives), player, player.location()));
        }
        return playerMoves;
    }

    //visitor functions
    int getMoveDestination(Move move) {
        return move.accept(new Move.Visitor<Integer>() {
            public Integer visit(Move.SingleMove singleMove) {
                return singleMove.destination;
            }

            public Integer visit(Move.DoubleMove doubleMove) {
                return doubleMove.destination2;
            }
        });
    }

    List<LogEntry> getNewLog(Move move, GameSetup setup, List<LogEntry> log) {
        return move.accept(new Move.Visitor<>() {
            public List<LogEntry> visit(Move.SingleMove singleMove) {
                List<LogEntry> newLog = new ArrayList<>(log);
                if (!setup.moves.get(newLog.size())) newLog.add(LogEntry.hidden(singleMove.ticket));
                else newLog.add(LogEntry.reveal(singleMove.ticket, singleMove.destination));
                return newLog;
            }

            public List<LogEntry> visit(Move.DoubleMove doubleMove) {
                List<LogEntry> newLog = new ArrayList<>(log);
                if (!setup.moves.get(newLog.size())) newLog.add(LogEntry.hidden(doubleMove.ticket1));
                else newLog.add(LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1));
                if (!setup.moves.get(newLog.size())) newLog.add(LogEntry.hidden(doubleMove.ticket2));
                else newLog.add(LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2));
                return newLog;
            }
        });
    }

    ScotlandYard.Ticket getSingleMoveTicket(Move move) {
        return move.accept(new Move.Visitor<>() {
            public ScotlandYard.Ticket visit(Move.SingleMove singleMove) {
                return singleMove.ticket;
            }

            public ScotlandYard.Ticket visit(Move.DoubleMove doubleMove) {
                throw new IllegalArgumentException("getSingleMoveTicket was called with double move");
            }
        });
    }

    void testAllMovesHaveTickets() {
        for (Move move : this.moves) {
            Piece currentPiece = move.commencedBy();
            Player currentPlayer = (currentPiece.isMrX()) ? mrX : this.detectives.stream().filter(player -> player.piece() == currentPiece).findFirst().get();
            Player tempPlayer = new Player(currentPiece, currentPlayer.tickets(), currentPlayer.location());
            try {tempPlayer.use(move.tickets());}
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    void testMoveInAllMoves(Move move) {
        if (this.moves.stream().noneMatch(currMove -> currMove.equals(move))) {
            throw new IllegalArgumentException("move not in this.moves");
        }
    }

};
