import org.junit.*;
import static org.junit.Assert.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.io.File;

/**
 * Test class for UnoController.
 * Updated for M4 to test Undo, Redo, Save, and Load commands.
 *
 * @author Danilo Bukvic Ajan Balaganesh Aydan Eng Aws Ali
 * @version 4.0
 */
public class UnoControllerTest {
    private MockModel model;
    private UnoViewStub view;
    private UnoController controller;

    @Before
    public void setUp() {
        model = new MockModel();
        view = new UnoViewStub();
        controller = new UnoController(model, view);
    }

    @Test
    public void testPlayCard() {
        controller.actionPerformed(new ActionEvent(this, 0, "PLAY:0"));
        assertTrue("play() should be called", model.playCalled);
    }

    @Test
    public void testUndo() {
        controller.actionPerformed(new ActionEvent(this, 0, "UNDO"));
        assertTrue("undo() should be called", model.undoCalled);
    }

    @Test
    public void testRedo() {
        controller.actionPerformed(new ActionEvent(this, 0, "REDO"));
        assertTrue("redo() should be called", model.redoCalled);
    }

    @Test
    public void testPlayWild() {
        model.isWild = true;
        controller.actionPerformed(new ActionEvent(this, 0, "PLAY:0"));
        assertTrue("playWild() should be called", model.playWildCalled);
    }

    @Test
    public void testDraw() {
        controller.actionPerformed(new ActionEvent(this, 0, "DRAW"));
        assertTrue("draw() should be called", model.drawCalled);
    }

    @Test
    public void testNext() {
        controller.actionPerformed(new ActionEvent(this, 0, "NEXT"));
        assertTrue("nextPlayer() should be called", model.nextPlayerCalled);
    }

    // MOCK MODEL
    static class MockModel extends UnoModel {
        boolean playCalled = false;
        boolean playWildCalled = false;
        boolean drawCalled = false;
        boolean nextPlayerCalled = false;
        boolean undoCalled = false;
        boolean redoCalled = false;
        boolean saveCalled = false;

        boolean isWild = false;
        boolean returnNull = false;

        public MockModel() {
            super(2, List.of("P1", "P2"), List.of(false, false));
        }

        @Override public void play(int index) { playCalled = true; }
        @Override public void playWild(int index, UnoColor color) { playWildCalled = true; }
        @Override public void draw() { drawCalled = true; }
        @Override public void nextPlayer() { nextPlayerCalled = true; }
        @Override public void undo() { undoCalled = true; }
        @Override public void redo() { redoCalled = true; }
        @Override public void saveGame(File f) { saveCalled = true; }

        @Override
        public boolean isCardWild(int index) { return isWild; }

        @Override
        public UnoCard peekCardInHand(int index) {
            if (returnNull) return null;
            return isWild
                    ? new UnoCard(UnoColor.WILD, UnoRank.WILD, UnoColor.WILD, UnoRank.WILD_DRAW_COLOR)
                    : new UnoCard(UnoColor.RED, UnoRank.FIVE, UnoColor.TEAL, UnoRank.FIVE);
        }
    }
}