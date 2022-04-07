package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

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
		@SuppressWarnings("unchecked") ImmutableSet<Integer> positions = (ImmutableSet<Integer>) board.getPlayers()
				.stream().map(detective -> board.getDetectiveLocation((Piece.Detective) detective).get());
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
	private Move nextDetectiveMove(Board board){
		return null;
	}
	private Move nextMrXMove(Board board){
		int closestDet = getClosestDetectivePosition(board);
		//this.mrXRouteTable.getPath(board.)
		return null;
	}

//	private Move useFloyd(Board board){
//		ImmutableList<Integer> detectiveLocations =
//		if (this.mrXPlaying){
//			this.mrXRouteTable.getPath()
//		}
//	}

	private float rewardFunction(List<Integer> detectiveDistances) {
		return detectiveDistances.stream()
				.reduce(0, (acc, distance) -> acc + distance).floatValue();


	}
}
