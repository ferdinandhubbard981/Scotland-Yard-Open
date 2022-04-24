package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.List;

//@param board 2d vector of one hot encoded players (6 bits, 1 for each player)
public class NnetInput {
    List<List<Boolean>> board; //6 Booleans
    List<List<Integer>> players;
    Integer numOfRoundsSinceReveal;

    public NnetInput(Byte[] byteArray) {

    }
    public NnetInput(List<List<Boolean>> board, List<List<Integer>> players, int numOfRoundsSinceReveal) {
        this.board = board;
        this.players = players;
        this.numOfRoundsSinceReveal = numOfRoundsSinceReveal;
    }

    public byte[] toBytes() {
        return null;
    }
}
