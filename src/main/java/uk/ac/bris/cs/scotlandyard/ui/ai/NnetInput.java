package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;

import java.io.ByteArrayOutputStream;
import java.util.List;

//@param board 2d vector of one hot encoded players (6 bits, 1 for each player)
public class NnetInput {
    List<List<Integer>> board; //6 Booleans
    List<List<Integer>> players;
    Integer numOfRoundsSinceReveal;

    public NnetInput(Byte[] byteArray) {

    }
    public NnetInput(MyGameState gameState) {
        //TODO convert gameState to Nnet input

    }

    public byte[] toBytes() {
        //TODO
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(board.size());
        for (List<Integer> pos : board) {
            for (int i = 0; i < pos.size(); i++) {
                output.write(pos.get(i));
            }
        }

    }
}
