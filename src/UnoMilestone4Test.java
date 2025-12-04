import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.*;
import java.io.File;
import java.io.IOException;

/**
 * JUnit tests specifically for Milestone 4 features:
 * Undo, Redo, and Serialization (Save/Load).
 * @author Danilo Bukvic Ajan Balaganesh Aydan Eng Aws Ali
 * @version 4.0
 */
public class UnoMilestone4Test {
    private UnoModel model;
    private UnoViewStub view;

    @Before
    public void setup() {
        model = new UnoModel(2, Arrays.asList("A", "B"), Arrays.asList(false, false));
        view = new UnoViewStub();
        model.addView(view);
    }

    @Test
    public void testUndoRedo() {
        int initialSize = view.lastEvent.getHand().size();

        // Draw a card
        model.draw();
        assertEquals("Hand size increases", initialSize + 1, view.lastEvent.getHand().size());

        // Undo
        assertTrue(model.canUndo());
        model.undo();
        assertEquals("Hand size reverts after undo", initialSize, view.lastEvent.getHand().size());

        // Redo
        assertTrue(model.canRedo());
        model.redo();
        assertEquals("Hand size increases again after redo", initialSize + 1, view.lastEvent.getHand().size());
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        // Change state
        model.draw();
        int handSizeBefore = view.lastEvent.getHand().size();
        String playerBefore = view.lastEvent.getCurrentPlayerName();

        // Save
        File temp = File.createTempFile("uno_test", ".ser");
        model.saveGame(temp);

        // Load into new model
        UnoModel loaded = UnoModel.loadGame(temp);
        UnoViewStub newView = new UnoViewStub();
        loaded.addView(newView);

        // Verify matches
        assertEquals(handSizeBefore, newView.lastEvent.getHand().size());
        assertEquals(playerBefore, newView.lastEvent.getCurrentPlayerName());

        temp.deleteOnExit();
    }
}