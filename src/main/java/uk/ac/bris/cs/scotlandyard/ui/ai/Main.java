package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.io.IOException;

/**
 * Delegates to the actual UI main
 */
public class Main {
	private static final Boolean ISTRAINING = true;
	public static void main(String[] args) {
		if (!ISTRAINING) {
			uk.ac.bris.cs.scotlandyard.Main.main(args);
		}
		else {
			try {
				Game game = new Game();
				Coach coach = new Coach(game);
				coach.learn();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
