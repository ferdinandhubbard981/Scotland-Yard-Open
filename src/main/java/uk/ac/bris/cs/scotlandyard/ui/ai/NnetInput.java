package uk.ac.bris.cs.scotlandyard.ui.ai;

import ch.qos.logback.core.joran.spi.ConsoleTarget;
import uk.ac.bris.cs.scotlandyard.model.Board;

import javax.xml.crypto.Data;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//@param board 2d vector of one hot encoded players (6 bits, 1 for each player)
public class NnetInput {
    private static final int PLAYERSINPUTSIZE = 6;
    private static final int TICKETSINPUTSIZE = 5;
    List<List<Integer>> board; //6 Ints
    List<List<Integer>> players;
    Integer numOfRoundsSinceReveal;


    public NnetInput(MyGameState gameState) {
        //TODO convert gameState to Nnet input

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
//        players
        List<List<Integer>> newPlayers = new ArrayList<>();
        for (int i = 0; i < PLAYERSINPUTSIZE; i++) {
            List<Integer> playerTickets = new ArrayList<>();
            for (int j = 0; j < TICKETSINPUTSIZE; j++) {
                playerTickets.get(din.readInt());
            }
            newPlayers.add(playerTickets);
        }
        this.players = newPlayers;
//        numOfRoundsSinceReveal
        this.numOfRoundsSinceReveal = din.readInt();
    }

    public byte[] toBytes() {
        //TODO
        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        board
        output.write(this.board.size());
        for (List<Integer> pos : this.board) {
            assert(pos.size() == PLAYERSINPUTSIZE);
            for (int i = 0; i < pos.size(); i++) {
                output.write(pos.get(i));
            }
        }
//        players
        assert(this.players.size() == PLAYERSINPUTSIZE);
        for (List<Integer> tickets : this.players) {
            assert(tickets.size() == TICKETSINPUTSIZE);
            for (Integer ticket : tickets) {
                output.write(ticket);
            }
        }
//        numOfRoundsSinceReveal
        output.write(this.numOfRoundsSinceReveal);
        return output.toByteArray();
    }
}
