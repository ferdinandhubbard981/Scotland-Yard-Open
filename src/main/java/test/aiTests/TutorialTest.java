package test.aiTests;

import org.junit.jupiter.api.Test;
import org.junit.Before;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @see "https://examples.javacodegeeks.com/core-java/junit/junit-assertthat-example/"
 */
public class TutorialTest {
    String persistentData = "hello";

    @Before public void doSomeInitialisation(){
        System.out.println(this.persistentData.length());
    }

    @Test public void testSomething(){
        assertThat(1, is(1));
        assertThat(this.persistentData, isA(String.class));
    }
}
