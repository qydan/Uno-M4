import java.io.Serial;
import java.io.Serializable;

/**
 * Defines a single Uno card that has two sides: Light and Dark.
 * Each side has its own color and rank.
 * @author Danilo Bukvic Ajan Balaganesh Aydan Eng Aws Ali
 * @version 4.0
 */
public class UnoCard implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UnoColor lightColor;
    private final UnoRank lightRank;
    private final UnoColor darkColor;
    private final UnoRank darkRank;

    /**
     * Creates a new card with specific colors and ranks for both the Light and Dark sides.
     * @param lightColor Color for the light side.
     * @param lightRank  Rank (number or action) for the light side.
     * @param darkColor  Color for the dark side.
     * @param darkRank   Rank (number or action) for the dark side.
     */
    public UnoCard(UnoColor lightColor, UnoRank lightRank, UnoColor darkColor, UnoRank darkRank) {
        this.lightColor = lightColor;
        this.lightRank = lightRank;
        this.darkColor = darkColor;
        this.darkRank = darkRank;
    }

    /**
     * figuring out which color to return based on whether we are currently on the Dark side.
     * @param isDark true if the game is currently in Dark mode.
     * @return the color of the card for the active side.
     */
    public UnoColor getColor(boolean isDark) {
        return isDark ? darkColor : lightColor;
    }

    /**
     * figuring out which rank to return based on whether we are currently on the Dark side.
     * @param isDark true if the game is currently in Dark mode.
     * @return the rank of the card for the active side.
     */
    public UnoRank getRank(boolean isDark) {
        return isDark ? darkRank : lightRank;
    }

    /**
     * Checks if this card is considered a Wild card on the current side.
     * We need this to know if the player should be prompted to pick a color.
     * @param isDark true if the game is currently in Dark mode.
     * @return true if the card is a Wild, Wild Draw Two, or Wild Draw Color.
     */
    public boolean isWild(boolean isDark) {
        UnoRank r = getRank(isDark);
        return r == UnoRank.WILD || r == UnoRank.WILD_DRAW_TWO || r == UnoRank.WILD_DRAW_COLOR;
    }

    /**
     * Decides if this card can legally be played on top of another card.
     * It matches if the colors match, the ranks match, or if this card is Wild.
     * @param top         The card currently sitting on top of the discard pile.
     * @param activeColor The color currently required (important if the previous card was Wild).
     * @param isDark      Whether we are checking against the Dark or Light side.
     * @return true if this is a valid move.
     */
    public boolean matches(UnoCard top, UnoColor activeColor, boolean isDark) {
        if (isWild(isDark)) return true;
        return getColor(isDark) == activeColor || getRank(isDark) == top.getRank(isDark);
    }

    /**
     * Returns a simple string like "RED-FIVE" for the current side.
     * @param isDark Which side we want the text for.
     * @return A text description of the card.
     */
    public String toText(boolean isDark) {
        return getColor(isDark) + "-" + getRank(isDark);
    }

    /**
     * Shows both sides of the card, useful for debugging logs.
     * Example: "RED-FIVE / TEAL-FIVE"
     */
    @Override
    public String toString() {
        return toText(false) + " / " + toText(true);
    }
}