package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.io.IOException;

/**
 * Delegates to the actual UI main
 */
public class Main {
//	public static void main(String[] args) {
//		uk.ac.bris.cs.scotlandyard.Main.main(args);
//	}

	public static void main(String[] args) {
		try {
			Game game = new Game();
			Coach coach = new Coach(game);
			coach.learn();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
