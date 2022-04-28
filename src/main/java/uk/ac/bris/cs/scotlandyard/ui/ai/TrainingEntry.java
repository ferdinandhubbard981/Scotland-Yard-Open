package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.tensorflow.Tensor;
import org.tensorflow.ndarray.NdArray;
import org.tensorflow.types.TFloat16;
import org.tensorflow.types.TInt32;
import uk.ac.bris.cs.scotlandyard.model.Board;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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

    public TInt32 getNnetInput() {
        return this.gameState.getTensor();
    }

    public Tensor getExpectedPolicyOutput() {
        //OPTIMISE TFloat16 or 32?
        //convert policyvalues and gameoutcome into Tensor
        float[] output = new float[this.policyValues.size()];
        for (int i = 0; i < this.policyValues.size(); i++) output[i] = this.policyValues.get(i);
        return TFloat16.vectorOf(output);
    }

    public Tensor getExepectedGameOutput() {
        return TInt32.scalarOf(this.gameOutcome);
    }
}
