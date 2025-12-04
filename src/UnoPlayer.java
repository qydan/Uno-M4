import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds all the data for a single player, including their name,
 * whether they are a computer bot, the cards in their hand, and their total score.
 * @author Danilo Bukvic Ajan Balaganesh Aydan Eng Aws Ali
 * @version 4.0
 */
public class UnoPlayer implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String name;
    public final boolean isAI;
    public final List<UnoCard> hand = new ArrayList<>();
    private int score = 0;

    /**
     * Sets up a new player.
     * @param name The display name for the player.
     * @param isAI Set to true if this player should be controlled by the computer.
     */
    public UnoPlayer(String name, boolean isAI) {
        this.name = name;
        this.isAI = isAI;
    }

    /**
     * Gets the player's current total score across all rounds.
     * @return the score.
     */
    public int getScore() {
        return score;
    }

    /**
     * Adds points to the player's total score.
     * usually called at the end of a round.
     * @param points The amount of points to add.
     */
    public void addScore(int points) {
        this.score += points;
    }

    /**
     * Wipes the player's hand clean. Used when starting a brand new round.
     */
    public void resetHand() {
        hand.clear();
    }
}