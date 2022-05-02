package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.tensorflow.ndarray.NdArray;
import org.tensorflow.ndarray.NdArrays;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static uk.ac.bris.cs.scotlandyard.ui.ai.Game.POSSIBLEMOVES;

public class TrainingEntry {
    private NnetInput gameState;
    private List<Float> policyValues;
    private Integer gameOutcome; //1 for mrX won -1 for det won

    public TrainingEntry(NnetInput gameState, List<Float> policyValues, int gameOutcome) {
        this.gameState = gameState;
        this.policyValues = policyValues;
        this.gameOutcome = gameOutcome;
    }

    public TrainingEntry(DataInputStream din) throws IOException {
//        gamestate
        this.gameState = new NnetInput(din);
//        policy values
        int policySize = din.readInt();
        List<Float> newPolicy = new ArrayList<>();
        for (int i = 0; i < policySize; i++) {
            newPolicy.add(din.readFloat());
        }
        this.policyValues = newPolicy;
//        gameoutcome
        this.gameOutcome = din.readInt();
    }
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        //gamestate
        output.write(gameState.toBytes());

//        policy
        output.write(policyValues.size());
        for (int i = 0; i < policyValues.size(); i++) {
            output.write(policyValues.get(i).byteValue());
        }
//gameoutcome
        output.write(gameOutcome);
        return output.toByteArray();
    }

    public NnetInput getGameState() {
        return this.gameState;
    }

    public List<Float> getPolicyValues() {
        return this.policyValues;
    }

    public Integer getGameOutcome() {
        return this.gameOutcome;
    }

    public void setGameOutcome(Integer gameOutcome) {
        this.gameOutcome = gameOutcome;
    }

    public NdArray<Float> getNnetInput() {
        return this.gameState.getNdArr();
    }

    public NdArray<Float> getExpectedPolicyOutput() {
        //OPTIMISE TFloat16 or 32?
        if (policyValues.size() != POSSIBLEMOVES) throw new IllegalArgumentException();
        float[] output = new float[this.policyValues.size()];
        for (int i = 0; i < POSSIBLEMOVES; i++) output[i] = this.policyValues.get(i);
        return NdArrays.vectorOf(output);
    }

    public NdArray<Float> getExepectedGameOutput() {
        return NdArrays.scalarOf(this.gameOutcome.floatValue());
    }
}
