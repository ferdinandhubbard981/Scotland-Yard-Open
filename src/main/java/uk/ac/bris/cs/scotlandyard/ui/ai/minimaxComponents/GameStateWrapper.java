package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.ui.ai.MyGameState;
import uk.ac.bris.cs.scotlandyard.ui.ai.NnetAI;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class GameStateWrapper implements MinimaxGameState<Move, Board.GameState> {
    private final boolean isGameFinished;
    private final int score;
    private final Move moveLeadingToState;
    private final Board.GameState gameState;
    private final boolean isMrXPlaying;

    public GameStateWrapper(Board board, Move moveLeadingToState){
        this(NnetAI.build(board), moveLeadingToState);
    }
    public GameStateWrapper(GameStateWrapper gameState, Move moveLeadingToState){
        this(gameState.getUnderlyingGameState(), moveLeadingToState);
    }
    public GameStateWrapper(Board.GameState gameState, Move moveLeadingToState){
        this.gameState = gameState;
        this.moveLeadingToState = moveLeadingToState;
        this.isMrXPlaying = isMrXPlayingCalculator();
        this.isGameFinished = isGameFinishedCalculator();
        this.score = scoreCalculator();
    }

    private boolean isGameFinishedCalculator(){
        return !this.gameState.getWinner().isEmpty();
    }
    private boolean isMrXPlayingCalculator(){
        return this.gameState.getAvailableMoves().asList().get(0).commencedBy().isMrX();
    }

    private int getMrXLocation(){
        if(this.isMrXPlaying) return this.gameState.getAvailableMoves().asList().get(0).source();
        for (LogEntry entry : this.gameState.getMrXTravelLog().reverse()){
            if (entry.location().isPresent()) return entry.location().get();
        }
        return new Random().nextInt(1,200);
    }

    private int scoreCalculator(){
        if (this.isMrXPlaying && this.isGameFinished) return -999999;
        if (this.isGameFinished) return 999999;
        List<Integer> distances = this.gameState.getPlayers().stream()
                .filter(Piece::isMrX)
                .map(piece -> {
                    int detectiveLocation = this.gameState.getDetectiveLocation((Piece.Detective) piece).get();
                    PathFinder<ImmutableSet<ScotlandYard.Transport>> pathFinder =
                            new PathFinder<>(
                                    this.gameState.getSetup().graph,
                                    detectiveLocation,
                                    getMrXLocation()
                            );
                    pathFinder.search();
                    return pathFinder.getDistance();
                }).toList();
        //gamestatewrapper will take perspective of mrX.
        //mrX wins by maximising distance, so positive and largest distance
        //detectives win by minimising distance, so negative and minimising
        return this.isMrXPlaying ?
                distances.stream().max(Integer::compare).get() :
                distances.stream().min(Integer::compare).get() * -1;
    }

    @Override
    public boolean isGameFinished() {
        return this.isGameFinished;
    }

    @Override
    public int getScore() {
        return this.score;
    }

    @Override
    public Move getMoveLeadingToState() {
        return this.moveLeadingToState;
    }

    @Override
    public Board.GameState getUnderlyingGameState() {
        return this.gameState;
    }

    @Override
    public ImmutableSet<Move> getAvailableMoves() {
        return this.gameState.getAvailableMoves();
    }

    @Override
    public MinimaxGameState<Move, Board.GameState> useMove(Move move) {
        return new GameStateWrapper(this.gameState.advance(move), move);
    }

    @Override
    public boolean isMaximisingPlayer() {
        return this.isMrXPlaying;
    }
}
