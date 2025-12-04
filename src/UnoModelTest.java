import org.junit.Test;
import org.junit.Before;
import java.util.*;
import static org.junit.Assert.*;

/**
 * Test class for UnoModel logic.
 * Updated for M4 to handle Round scoring checks.
 * @author Danilo Bukvic Ajan Balaganesh Aydan Eng Aws Ali
 * @version 4.0
 */
public class UnoModelTest {
    private UnoModel model;
    private UnoViewStub view;

    @Before
    public void setUp() {
        model = new UnoModel(2, Arrays.asList("Alice", "Bob"), Arrays.asList(false, false));
        view = new UnoViewStub();
        model.addView(view);
    }

    @Test
    public void testInitialState() {
        assertNotNull("View should receive initial update", view.lastEvent);
        assertEquals("Alice", view.lastEvent.getCurrentPlayerName());
        assertEquals(7, view.lastEvent.getHand().size());
        assertFalse("Should start on Light side", view.lastEvent.isDark());
    }

    @Test
    public void testDrawCard() {
        int initialSize = view.lastEvent.getHand().size();
        model.draw();
        assertEquals("Hand should increase by 1", initialSize + 1, view.lastEvent.getHand().size());
        assertTrue("Must press next after drawing", view.lastEvent.isMustPressNext());
    }

    @Test
    public void testTurnProgression() {
        model.setTopCard(new UnoCard(UnoColor.RED, UnoRank.FIVE, UnoColor.TEAL, UnoRank.FIVE));
        List<UnoCard> hand = new ArrayList<>();
        hand.add(new UnoCard(UnoColor.RED, UnoRank.SIX, UnoColor.TEAL, UnoRank.SIX));
        hand.add(new UnoCard(UnoColor.BLUE, UnoRank.NINE, UnoColor.PINK, UnoRank.NINE));
        model.forceHand(0, hand);

        model.play(0);
        model.nextPlayer();
        assertEquals("Bob", view.lastEvent.getCurrentPlayerName());
    }

    @Test
    public void testRoundWinning() {
        // Setup Alice to win the ROUND (1 card left)
        UnoCard winner = new UnoCard(UnoColor.RED, UnoRank.ONE, UnoColor.TEAL, UnoRank.ONE);
        model.forceHand(0, new ArrayList<>(List.of(winner)));

        // Give Bob some cards so Alice gets points
        UnoCard c1 = new UnoCard(UnoColor.BLUE, UnoRank.FIVE, UnoColor.PINK, UnoRank.FIVE); // 5 pts
        model.forceHand(1, new ArrayList<>(List.of(c1)));

        model.setTopCard(new UnoCard(UnoColor.RED, UnoRank.TWO, UnoColor.TEAL, UnoRank.TWO));

        model.play(0);

        // In M4, this triggers handleRoundEnd, not necessarily handleEnd(Game Over)
        assertTrue("Should trigger round end", view.handleRoundEndCalled);
        assertTrue("Info should mention Round Over or Win", view.lastInfo.contains("wins round"));
    }

    @Test(expected = IllegalStateException.class)
    public void testPlayInvalidCardThrowsException() {
        model.setTopCard(new UnoCard(UnoColor.RED, UnoRank.FIVE, UnoColor.TEAL, UnoRank.FIVE));
        List<UnoCard> hand = new ArrayList<>();
        hand.add(new UnoCard(UnoColor.BLUE, UnoRank.NINE, UnoColor.PINK, UnoRank.NINE));
        model.forceHand(0, hand);
        model.play(0); // Should fail
    }
}