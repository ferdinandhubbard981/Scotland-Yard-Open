package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.*;

import javax.annotation.Nonnull;

import com.google.common.collect.*;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {
	Board.GameState currentGameState;
	@Nonnull @Override public String name() { return "alphaTwo"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		long startTime = System.nanoTime();
		// returns a random move, replace with your own implementation
//		var moves = board.getAvailableMoves().asList();
//		return moves.get(new Random().nextInt(moves.size()));
		long endTime = startTime + timeoutPair.right().toNanos(timeoutPair.left());
		return monteCarloTreeSearch(board, startTime, endTime);
	}

	public void onStart() {

	}

	private Move monteCarloTreeSearch(Board board, long startTime, long endTime) {
		//initialize root node
		MCTSNode rootNode = new MCTSNode((Board.GameState) board, null, ImmutableList.of(), null);
		MCTSNode current = rootNode;
		current.setChildren(current.findChildren());
		while (endTime-startTime > TimeUnit.SECONDS.toNanos(1) && !current.isTerminalState()) {
			if (!current.isLeafNode()) {
				current = rootNode.getNodeWithHighestUCBVal(rootNode.getChildren().stream().findAny().get());
			}
			else if (current.getMonteCarloVal() == -2) current.rollout();
			else {
				current = rootNode.getNodeWithHighestUCBVal(rootNode.getChildren().stream().findAny().get());
				if (current.getNumberOfVisits() != 0) {
					current.setChildren(current.findChildren());
					current = rootNode.getNodeWithHighestUCBVal(rootNode.getChildren().stream().findAny().get());
				}
			}

		}
		MCTSNode bestNode = getBestChildNode(rootNode);
		Move bestMove = getMove(bestNode);
		return bestMove;
	}

	MCTSNode getBestChildNode(MCTSNode root) {
		ImmutableList<MCTSNode> children = root.getChildren();
		MCTSNode bestChild = children.stream().findAny().get();
		for (MCTSNode child : children) {
			if (child.getMonteCarloVal() > bestChild.getMonteCarloVal()) bestChild = child;
		}
		return bestChild;
	}

	Move getMove(MCTSNode node) {
		Board.GameState parentState = node.getParent().getState();
		Board.GameState state = node.getState();

		ImmutableSet<Move> possibleMoves = node.getParent().getState().getAvailableMoves();
		for (Move move : possibleMoves) {
			if (parentState.advance(move) == state) return move;
		}
		throw new IllegalArgumentException("no move found matching desired state");
	}

	private float positionEvaluationFunction(List<Float> detectiveDistances) {
		return detectiveDistances.stream()
				.reduce(0f, Float::sum);
	}







}

