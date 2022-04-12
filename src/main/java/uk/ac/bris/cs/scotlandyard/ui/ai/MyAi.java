package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	Floyd mrXRouteTable;
	Floyd detectiveRouteTable;
	boolean mrXPlaying = false;
	int lastKnownMrXPosition; //used by detective player

	@Nonnull @Override public String name() { return "alphaTwo"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		//		var moves = board.getAvailableMoves().asList();
		//		return moves.get(new Random().nextInt(moves.size()));
		if (this.mrXRouteTable == null) this.mrXRouteTable = new Floyd(board.getSetup().graph, true );
		if (this.detectiveRouteTable == null) this.detectiveRouteTable = new Floyd(board.getSetup().graph, false);
		this.mrXPlaying = !this.mrXPlaying;
		return (this.mrXPlaying ? nextMrXMove(board) : nextDetectiveMove(board));
	}
	private int getClosestDetectivePosition(Board board){
		ImmutableList<Integer> positions =
				(ImmutableList<Integer>) board.getPlayers()
				.stream().map(detective -> board.getDetectiveLocation((Piece.Detective) detective).get())
						.toList();
		int currentClosestDetectiveDistance = Integer.MAX_VALUE;
		int currentClosestDetectivePosition = -1;
		for (int position : positions){
			final int routeTableDist = detectiveRouteTable.minimumDistances.get(position).get(lastKnownMrXPosition);
			if(routeTableDist < currentClosestDetectiveDistance){
				currentClosestDetectivePosition = position;
				currentClosestDetectiveDistance = routeTableDist;
			}
		}
		return currentClosestDetectivePosition;
	}
	private Move defaultMove(Board board){
		ImmutableList<Move> moves = board.getAvailableMoves().asList();
		return moves.get(new Random().nextInt(moves.size()));
	}
	private Move nextDetectiveMove(Board board){
		return null;
	}

	private int getMrXLocation(Board board, boolean isMrX){
		if (isMrX) return board.getAvailableMoves().asList().get(0).source(); //avoids insider information

		for (LogEntry entry : board.getMrXTravelLog().reverse()){
			if (entry.location().orElse(null) != null) return entry.location().get();
		}
		//we don't know where mrX was last seen, so a random direction?
		return new Random().nextInt(0, 200);
	}

	private Move nextMrXMove(Board board) {
		int mrXLocation = getMrXLocation(board, true);
		int nextNode = mrXRouteTable.getNextNode(
				mrXLocation, getClosestDetectivePosition(board)
		);
		//get transport method connecting the nodes, find the move that has same
		ImmutableList<Integer> destinations = (ImmutableList<Integer>) board.getAvailableMoves()
				.stream().map(m -> m.accept(new Move.Visitor<Integer>() {
					@Override
					public Integer visit(Move.SingleMove move) {
						return move.destination;
					}

					@Override
					public Integer visit(Move.DoubleMove move) {
						return move.destination2;
					}
				})).toList();
		for (int i = 0; i < destinations.size(); i++) {
			if (nextNode == destinations.get(i)) return board.getAvailableMoves().asList().get(i);
		}
		return defaultMove(board);
	}
}
