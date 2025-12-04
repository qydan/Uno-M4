import java.awt.Color;

/**
 * The interface that any UI (like our GUI frame) must implement to talk to the Game Model.
 * This ensures the Model doesn't care exactly how the game is displayed, just that it can send updates.
 * @author Danilo Bukvic Ajan Balaganesh Aydan Eng Aws Ali
 * @version 4.0
 */
public interface UnoView {

    /**
     * Called whenever the game state changes (card played, drawn, turn change).
     * The view should update the screen to reflect the new data in the event.
     * @param e The snapshot of the current game state.
     */
    void handleUpdate(UnoEvent e);

    /**
     * Called when the entire game is over (someone reached 500 points).
     * @param message The final winning message to show.
     */
    void handleEnd(String message);

    /**
     * Called when a single round ends, but the game isn't over yet.
     * @param message The summary of the round and current scores.
     */
    void handleRoundEnd(String message);

    /**
     * Asks the user to pick a color. This pops up when a Wild card is played.
     * @return The UnoColor the user picked.
     */
    UnoColor promptForWildColor();

    /**
     * Shows a generic popup message to the user, like an error or status update.
     * @param message The text to display.
     */
    void showInfo(String message);

    /**
     * Helper to convert our internal UnoColors to actual Java AWT Colors for painting.
     * @param c The internal UnoColor.
     * @return The matching Java Color object.
     */
    Color mapCardColor(UnoColor c);
}