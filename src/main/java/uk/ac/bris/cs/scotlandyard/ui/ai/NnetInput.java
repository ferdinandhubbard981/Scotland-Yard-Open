package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.tensorflow.Tensor;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.NdArray;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.buffer.FloatDataBuffer;
import org.tensorflow.types.TFloat16;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TInt32;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static uk.ac.bris.cs.scotlandyard.ui.ai.Game.*;

//@param board 2d vector of one hot encoded players (6 bits, 1 for each player)
public class NnetInput {
    List<List<Integer>> board; //6 Ints
    List<List<Integer>> playerTickets;
    Integer numOfRoundsSinceReveal;


    public NnetInput(Game game) {
//        game should already have gameState in it;
//        board
        this.board = game.getEncodedBoard();
//        playerTickets
        this.playerTickets = game.getEncodedPlayerTickets();
//        numOfRoundsSinceReveal
        this.numOfRoundsSinceReveal = game.getNumOfRoundsSinceReveal();
    }

    public NnetInput(DataInputStream din) throws IOException {
//        board
        int boardSize = din.readInt();
        List<List<Integer>> newBoard = new ArrayList<>();
        for (int i = 0; i < boardSize; i++) {
            List<Integer> node = new ArrayList<>();
            for (int j = 0; j < 6; j++) {
                node.add(din.readInt());
            }
            newBoard.add(node);
        }
        this.board = newBoard;
//        playerTickets
        List<List<Integer>> newPlayers = new ArrayList<>();
        for (int i = 0; i < PLAYERSINPUTSIZE; i++) {
            List<Integer> playerTickets = new ArrayList<>();
            for (int j = 0; j < TICKETSINPUTSIZE; j++) {
                playerTickets.get(din.readInt());
            }
            newPlayers.add(playerTickets);
        }
        this.playerTickets = newPlayers;
//        numOfRoundsSinceReveal
        this.numOfRoundsSinceReveal = din.readInt();
    }

    public byte[] toBytes() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        board
        output.write(this.board.size());
        for (List<Integer> pos : this.board) {
            assert(pos.size() == PLAYERSINPUTSIZE);
            for (int i = 0; i < pos.size(); i++) {
                output.write(pos.get(i));
            }
        }
//        playerTickets
        assert(this.playerTickets.size() == PLAYERSINPUTSIZE);
        for (List<Integer> tickets : this.playerTickets) {
            assert(tickets.size() == TICKETSINPUTSIZE);
            for (Integer ticket : tickets) {
                output.write(ticket);
            }
        }
//        numOfRoundsSinceReveal
        output.write(this.numOfRoundsSinceReveal);
        return output.toByteArray();
    }

    public TFloat32 getTensor() {
//        float[] listOut = new float[NNETINPUTBOARDSIZE * PLAYERSINPUTSIZE];
        FloatNdArray ndArr = NdArrays.ofFloats(Shape.of(1, NNETINPUTBOARDSIZE * PLAYERSINPUTSIZE));
        for (int i = 0; i < NNETINPUTBOARDSIZE; i++) {
            for (int j = 0; j < PLAYERSINPUTSIZE; j++) {
//                listOut[i] = this.board.get(i).get(j);
                ndArr.set(NdArrays.scalarOf(this.board.get(i).get(j).floatValue()), 0, i);
            }
        }
//        NdArrays.wrap(Shape.of(1, NNETINPUTBOARDSIZE * PLAYERSINPUTSIZE), FloatDataBuffer)
        TFloat32 output = TFloat32.tensorOf(ndArr);
        return output;
    }
}
