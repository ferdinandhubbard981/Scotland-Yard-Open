package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.*;

import javax.annotation.Nonnull;

import com.esotericsoftware.minlog.Log;
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
		return monteCarloTreeSearch(board, endTime);
	}

	public void onStart() {

	}

	private Move monteCarloTreeSearch(Board board, long endTime) {
		//initialize root node
		MCTSNode rootNode = new MCTSNode((Board.GameState) board, null, ImmutableList.of(), true, null);
		MCTSNode current = rootNode;
//		current.setChildren(current.findChildren());
		while (endTime-System.nanoTime() > TimeUnit.SECONDS.toNanos(1)) {
//			System.err.format("%d", TimeUnit.NANOSECONDS.toSeconds(endTime-System.nanoTime()));
			if (!current.isLeafNode()) {
				current = rootNode.getNodeWithHighestUCBVal(rootNode);
			}
			else if (current.getNumberOfVisits() == 0) {
				current.rollout();
				current = rootNode.getNodeWithHighestUCBVal(rootNode);
			}
			else {
				if (!current.isTerminalState()) current.setChildren(current.findChildren());
				current = rootNode.getNodeWithHighestUCBVal(rootNode);
			}

		}
//		MCTSNode cutTree = removeAllNodesWithMinVisits(rootNode, 10);
		MCTSNode bestNode = getBestChildNode(rootNode);
		Move bestMove = bestNode.getIncidentMove();
		return bestMove;
	}

	MCTSNode removeAllNodesWithMinVisits(MCTSNode rootNode, int minVisits) {



		class ModMCTSNode extends MCTSNode {

			ModMCTSNode(MCTSNode copyNode) {
				super(copyNode.gameState, copyNode.parent, copyNode.children, !copyNode.mrXToMove, copyNode.incidentMove);
				this.numberOfVisits = copyNode.numberOfVisits;
				this.monteCarloVal = copyNode.monteCarloVal;
			}

			void removeChild(MCTSNode childToBeRemoved) {
				List<MCTSNode> newChildren = this.children.stream().filter(child -> child.hashCode() != childToBeRemoved.hashCode()).toList();
				this.children = ImmutableList.copyOf(newChildren);
			}

			void addChild(MCTSNode childToBeAdded) {
				List<MCTSNode> newChildren = new ArrayList<>(this.children);
				newChildren.add(childToBeAdded);
				this.children = ImmutableList.copyOf(newChildren);
			}

		}
		ModMCTSNode modRootNode = new ModMCTSNode(rootNode);
		if (modRootNode.getNumberOfVisits() < minVisits || modRootNode.isLeafNode()) return null;
		if (!modRootNode.isLeafNode()) {
			for (MCTSNode child : modRootNode.getChildren()) {
				MCTSNode newChild = removeAllNodesWithMinVisits(child, minVisits);
				modRootNode.removeChild(child);
				if (newChild != null) modRootNode.addChild(newChild);
			}
		}
		return modRootNode;
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

		ImmutableSet<Move> possibleMoves = parentState.getAvailableMoves();
//		Move output = possibleMoves.stream().filter(move -> parentState.advance(move) == state.)
		for (Move move : possibleMoves) {
			if (parentState.advance(move).hashCode() == state.hashCode()) return move;
		}
		throw new IllegalArgumentException("no move found matching desired state");
	}

	private float positionEvaluationFunction(List<Float> detectiveDistances) {
		return detectiveDistances.stream()
				.reduce(0f, Float::sum);
	}







}

