package test.aiTests;

import org.junit.jupiter.api.Test;
import org.junit.Before;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.ui.ai.Game;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;

public class GameTest {
    @Test public void testGetMoveFromIndex(){
        Game testGame;
        try {
            testGame = new Game();
        }catch(IOException e) {
            assert false;
        }
        //assert testGame.getMoveFromIndex(4) == Move;
    }

    @Test public void testGetMoveIndex(){
        Game testGame;
        try {
            testGame = new Game();
        }catch (IOException e){
            assert false;
        }
        //testGame.getMoveIndex(Move)
    }

    @Test public void testGetMoveTable(){
        Game testGame;
        try {
            testGame = new Game();
        }catch (IOException e){
            assert false;
        }
        //testGame.getMoveTable(Set<Move>)
    }
    @Test public void testGetGameEnded(){
        Game testGame;
        try {
            testGame = new Game();
        }catch (IOException e ){
            assert false;
        }
        //assert testGame.getGameEnded() == 0;
    }

    @Test public void testGetEncodedBoard(){
        Game testGame;
        try {
            testGame = new Game();
        }catch(IOException e){
            assert false;
        }
        //assert !testGame.getEncodedBoard().isEmpty();
    }

    @Test public void testGetEncodedPlayerTickets(){
        Game testGame;
        try {
            testGame = new Game();
        }catch(IOException e){
            assert false;
        }
        //assert testGame.getEncodedPlayerTickets().size() > 5;
    }

    @Test public void testGetNumberOfRoundsSinceReveal(){
        Game testGame;
        try {
            testGame = new Game();
        }catch(IOException e){
            assert false;
        }
        //assert testGame.getNumOfRoundsSinceReveal() == 3;
    }
}
