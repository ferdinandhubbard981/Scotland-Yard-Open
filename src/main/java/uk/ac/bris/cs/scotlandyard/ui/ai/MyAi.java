package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.io.*;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.*;

import javax.annotation.Nonnull;

import com.google.common.collect.*;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.ui.ai.old.MCTSNode;

public class MyAi implements Ai {
	String filePath;
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
		String extension = LocalDateTime.now().toString().replace(':', '-');
		this.filePath = String.format("C:\\Users\\ferdi\\Desktop\\Scotland-Yard-Open\\game-data\\%s", extension);
		File file = new File(this.filePath);
		file.mkdirs();
	}

	private Move monteCarloTreeSearch(Board board, long endTime) {
		//initialize root node
		long startTime = System.nanoTime();
		Board.GameState gameState = (Board.GameState) board;
		boolean mrxToMove = board.getAvailableMoves().stream().findAny().get().commencedBy().isMrX();
		MCTSNode rootNode = new MCTSNode((Board.GameState) board, null, ImmutableList.of(), !mrxToMove, null);
		MCTSNode current = rootNode;
//		current.setChildren(current.findChildren());
		while (endTime-System.nanoTime() > TimeUnit.SECONDS.toNanos(1)) {
//			System.err.format("%d", TimeUnit.NANOSECONDS.toSeconds(endTime-System.nanoTime()));
			if (!current.isLeafNode()) {
				current = rootNode.getNodeWithHighestUCBVal(rootNode, true);
			}
			else if (current.getNumberOfVisits() == 0) {
				current.rollout();
				current = rootNode.getNodeWithHighestUCBVal(rootNode, true);
			}
			else {
				if (!current.isTerminalState()) current.setChildren(current.findChildren());
				current = rootNode.getNodeWithHighestUCBVal(rootNode, true);
			}

		}
		long timeTaken = System.nanoTime() - startTime;
//		MCTSNode cutTree = removeAllNodesWithMinVisits(rootNode, 10);
		MCTSNode bestNode = getBestChildNode(rootNode);
		Move bestMove = bestNode.getIncidentMove();
		String fileName = String.format("round%d.txt", board.getMrXTravelLog().size()+1);
		String filePath = String.format("%s\\%s", this.filePath, fileName);
		try {
			writeRoundResults(filePath, rootNode, bestMove, timeTaken);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return bestMove;
	}

	MCTSNode removeAllNodesWithMinVisits(MCTSNode rootNode, int minVisits) {



		class ModMCTSNode extends MCTSNode {

			ModMCTSNode(MCTSNode copyNode) {
				super(copyNode.gameState, copyNode.parent, copyNode.children, !copyNode.mrXToMove, copyNode.incidentMove);
				this.numberOfVisits = copyNode.numberOfVisits;
				this.sumOfStateVals = copyNode.sumOfStateVals;
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
			if (child.getSumOfStateVals() > bestChild.getSumOfStateVals()) bestChild = child;
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
	//misc
	void writeRoundResults(String filePath, MCTSNode rootNode, Move bestMove, long timeTaken) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(new FileOutputStream(filePath, true));
		//write header (round num, time taken, time available)
		Board.GameState board = rootNode.getState();
		if (board.getAvailableMoves().stream().anyMatch(move -> move.commencedBy().isMrX()))
			writer.println(String.format("round %d:\n", board.getMrXTravelLog().size()+1));
		float secondsTaken = TimeUnit.NANOSECONDS.toSeconds(timeTaken);
		writer.println(String.format("time taken: %f seconds\n", secondsTaken));
		int numOfVisits = rootNode.getChildren().stream()
				.map(child -> child.getNumberOfVisits())
				.reduce(0, (total, element) -> total += element);
		writer.println(String.format("speed: %f visits per second\n", numOfVisits/secondsTaken));
		writer.println(String.format("move commenced by: %s\n", bestMove.commencedBy().toString()));
		//write sd
		writer.println(String.format("SD: %f\n", getExplorationStandardDistribution(rootNode)));
		//write num of sims
		writer.println(String.format("total sims: %d\n", numOfVisits));
		//write num of sims per move
		writer.println("visits for each move: ");
		for (MCTSNode child : rootNode.getChildren()) writer.print(String.format("%d, ", child.getNumberOfVisits()));
		writer.println("\n");
		//write num of wins for each move
		writer.println("num of wins: ");
		for (MCTSNode child : rootNode.getChildren()) writer.print(String.format("%f ", child.getSumOfStateVals()));
		writer.println("\n\n");
		writer.close();
	}

	Float getExplorationStandardDistribution(MCTSNode rootNode) {
		List<Integer> values = new ArrayList<>();
		for (MCTSNode child : rootNode.getChildren()) {
			values.add(child.numberOfVisits);
		}
		float mean = values.stream().reduce(0, (total, element) -> total+element).floatValue() / values.size();
		float variance = values.stream().map(val -> Float.valueOf(val)).reduce(0f, (total, element) -> total + (float)Math.pow(element - mean, 2)) / values.size();
		Float sD = (float)Math.sqrt(variance);
		return sD;
	}









}

