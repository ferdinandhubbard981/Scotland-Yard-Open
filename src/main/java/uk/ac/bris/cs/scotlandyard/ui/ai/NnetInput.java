package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.tensorflow.Tensor;
import org.tensorflow.ndarray.*;
import org.tensorflow.types.TFloat32;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            if (pos.size() != PLAYERSINPUTSIZE) throw new IllegalArgumentException();
            for (int i = 0; i < pos.size(); i++) {
                output.write(pos.get(i));
            }
        }
//        playerTickets
        if (this.playerTickets.size() != PLAYERSINPUTSIZE) throw new IllegalArgumentException();

        for (List<Integer> tickets : this.playerTickets) {
            if (tickets.size() != TICKETSINPUTSIZE) throw new IllegalArgumentException();

            for (Integer ticket : tickets) {
                output.write(ticket);
            }
        }
//        numOfRoundsSinceReveal
        output.write(this.numOfRoundsSinceReveal);
        return output.toByteArray();
    }

    public NdArray<Float> getNdArr() {
//        float[] listOut = new float[NNETINPUTBOARDSIZE * PLAYERSINPUTSIZE];
        int ndSize = NNETINPUTBOARDSIZE * PLAYERSINPUTSIZE + PLAYERSINPUTSIZE * TICKETSINPUTSIZE + 1;
        int ndIndex = 0;
        FloatNdArray ndArr = NdArrays.ofFloats(Shape.of(ndSize));
        for (int i = 0; i < NNETINPUTBOARDSIZE; i++) {
            for (int j = 0; j < PLAYERSINPUTSIZE; j++) {
//                listOut[i] = this.board.get(i).get(j);
                ndArr.set(NdArrays.scalarOf(this.board.get(i).get(j).floatValue()), ndIndex);
                ndIndex++;
            }
        }
        for (int i = 0; i < PLAYERSINPUTSIZE; i++) {
            for (int j = 0; j < TICKETSINPUTSIZE; j++) {
                ndArr.set(NdArrays.scalarOf(this.playerTickets.get(i).get(j).floatValue()), ndIndex);
                ndIndex++;
            }
        }
        ndArr.set(NdArrays.scalarOf(this.numOfRoundsSinceReveal.floatValue()), ndIndex);
        ndIndex++;
        if (ndSize != ndIndex) throw new IllegalArgumentException();
//        NdArrays.wrap(Shape.of(1, NNETINPUTBOARDSIZE * PLAYERSINPUTSIZE), FloatDataBuffer)
        TFloat32 output = TFloat32.tensorOf(ndArr);
        return output;
    }
}
