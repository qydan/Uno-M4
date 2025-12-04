import java.io.*;
import java.util.*;

/**
 * The brain of the operation. This class manages the deck, the players, the turns,
 * and all the rules of Uno Flip. It also handles saving, loading, and undoing moves.
 *
 * @author Danilo Bukvic Ajan Balaganesh Aydan Eng Aws Ali
 * @version 4.0
 */
public class UnoModel implements Serializable {
    private static final long serialVersionUID = 1L;

    // Transient because we don't save the UI components when saving the game file
    private transient List<UnoView> views = new ArrayList<>();

    private final List<UnoPlayer> players = new ArrayList<>();
    private final Deque<UnoCard> drawPile = new ArrayDeque<>();
    private final Deque<UnoCard> discard = new ArrayDeque<>();

    // We store the history of the game as byte arrays (snapshots) so we can jump back in time
    private transient Deque<byte[]> undoStack = new ArrayDeque<>();
    private transient Deque<byte[]> redoStack = new ArrayDeque<>();

    private int current = 0;
    private int gameDirection = 1;
    private boolean mustPressNext = false;
    private UnoColor activeColor = UnoColor.NONE;
    private String info = "Welcome to Uno!";
    private int nextSteps = 1;
    private boolean isDark = false;

    private static final int WINNING_SCORE = 500;

    /**
     * Sets up the game with the given players and immediately starts the first round.
     *
     * @param numPlayers How many people are playing (2-4).
     * @param names      The names of the players.
     * @param isAI       True/False list corresponding to if that player is a bot.
     */
    public UnoModel(int numPlayers, List<String> names, List<Boolean> isAI) {
        if (numPlayers < 2 || numPlayers > 4) {
            throw new IllegalArgumentException("Number of players must be 2–4.");
        }
        for (int i = 0; i < numPlayers; i++) {
            players.add(new UnoPlayer(names.get(i), isAI.get(i)));
        }
        initializeRound();
    }

    /**
     * Resets the deck, shuffles, deals cards, and starts a fresh round.
     * Called at the start of the game and after someone empties their hand.
     */
    private void initializeRound() {
        drawPile.clear();
        discard.clear();
        List<UnoCard> deck = buildFlipDeck();
        Collections.shuffle(deck, new Random());
        deck.forEach(drawPile::push);

        // Clear everyone's hand and deal 7 new cards
        for(UnoPlayer p : players) p.resetHand();
        for (int k = 0; k < 7; k++) {
            for (UnoPlayer p : players) p.hand.add(drawPile.pop());
        }

        // Flip the first card to start the pile
        UnoCard first = drawPile.pop();
        discard.push(first);
        activeColor = first.getColor(isDark);
        // If the first card happens to be a Wild, pick a safe default color
        if (first.isWild(isDark)) activeColor = isDark ? UnoColor.TEAL : UnoColor.RED;

        // Clear history because you can't undo into a previous round
        if(undoStack == null) undoStack = new ArrayDeque<>();
        if(redoStack == null) redoStack = new ArrayDeque<>();
        undoStack.clear();
        redoStack.clear();

        info = "Round Start! Target: " + WINNING_SCORE + " pts.";
        notifyViews();
    }

    /**
     * Connects a UI View to this model so it receives updates.
     * @param v The view object.
     */
    public void addView(UnoView v) {
        if (views == null) views = new ArrayList<>();
        views.add(v);
        notifyViews();
    }

    /**
     * Packages up the current state of the game and sends it to all connected Views.
     */
    private void notifyViews() {
        if (views == null) return;
        UnoPlayer p = players.get(current);
        // Send a copy of the hand so the View can't mess with the real one
        List<UnoCard> handCopy = new ArrayList<>(p.hand);
        UnoCard top = discard.peek();
        String topText = top != null ? top.toText(isDark) : "None";
        if (activeColor != UnoColor.NONE) topText += " [" + activeColor + "]";

        UnoEvent event = new UnoEvent(this, handCopy, topText, p.name, info, mustPressNext, activeColor, isDark, p.isAI);
        for (UnoView v : views) v.handleUpdate(event);
    }

    // --- UNDO / REDO LOGIC ---

    /**
     * Snapshots the current model and pushes it onto the undo stack.
     * Call this right before making any changes (playing, drawing, etc.).
     */
    public void saveState() {
        try {
            undoStack.push(serializeState());
            redoStack.clear(); // Once you make a new move, you can't redo old paths
            // Cap the stack at 50 to prevent memory issues
            if (undoStack.size() > 50) undoStack.removeLast();
        } catch (IOException e) {
            System.err.println("Failed to save state for undo: " + e.getMessage());
        }
    }

    /**
     * Reverts the game to the state it was in before the last move.
     */
    public void undo() {
        if (!canUndo()) return;
        try {
            // Save where we are now to the redo stack before going back
            redoStack.push(serializeState());
            restoreState(undoStack.pop());
            info = "Undid last move.";
            notifyViews();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Re-applies a move that was just undone.
     */
    public void redo() {
        if (!canRedo()) return;
        try {
            undoStack.push(serializeState());
            restoreState(redoStack.pop());
            info = "Redid move.";
            notifyViews();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    /**
     * Turns this entire object into a byte array so we can store it.
     */
    private byte[] serializeState() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        return bos.toByteArray();
    }

    /**
     * Reads a byte array and overwrites the current object's fields with that old data.
     */
    private void restoreState(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        UnoModel restored = (UnoModel) ois.readObject();

        // Copy everything over
        this.players.clear(); this.players.addAll(restored.players);
        this.drawPile.clear(); this.drawPile.addAll(restored.drawPile);
        this.discard.clear(); this.discard.addAll(restored.discard);
        this.current = restored.current;
        this.gameDirection = restored.gameDirection;
        this.mustPressNext = restored.mustPressNext;
        this.activeColor = restored.activeColor;
        this.nextSteps = restored.nextSteps;
        this.isDark = restored.isDark;
    }

    // --- GAMEPLAY ACTIONS ---

    /**
     * Player attempts to play a card from their hand.
     *
     * @param handIndex The position of the card in their hand.
     */
    public void play(int handIndex) {
        saveState(); // Save before changing anything!
        ensureAwaitingAction();
        UnoPlayer p = players.get(current);
        UnoCard chosen = p.hand.get(handIndex);
        UnoCard top = discard.peek();

        if (!chosen.matches(top, activeColor, isDark)) {
            // If the move was bad, we didn't actually change state, so pop the save we just made
            undoStack.pop();
            throw new IllegalStateException("Illegal move: " + chosen.toText(isDark));
        }

        p.hand.remove(handIndex);
        discard.push(chosen);
        activeColor = chosen.getColor(isDark);
        handleCardEffect(chosen, p.hand);
    }

    /**
     * Player attempts to play a Wild card and specifies the color they want.
     *
     * @param handIndex   The position of the card.
     * @param chosenColor The color they picked.
     */
    public void playWild(int handIndex, UnoColor chosenColor) {
        saveState();
        ensureAwaitingAction();
        UnoPlayer p = players.get(current);
        UnoCard chosen = p.hand.get(handIndex);

        p.hand.remove(handIndex);
        discard.push(chosen);
        activeColor = chosenColor;
        handleCardEffect(chosen, p.hand);
    }

    /**
     * Player draws a card from the deck.
     */
    public void draw() {
        saveState();
        ensureAwaitingAction();
        UnoPlayer p = players.get(current);
        p.hand.add(popOrRecycle());
        mustPressNext = true;
        info = p.name + " drew 1 card.";
        notifyViews();
    }

    /**
     * Passes the turn to the next person.
     * This is usually called after someone draws or plays a card.
     */
    public void nextPlayer() {
        if (!mustPressNext) throw new IllegalStateException("Perform action first.");

        current = properIndex(current + gameDirection * nextSteps);
        mustPressNext = false;
        nextSteps = 1;
        info = players.get(current).name + "'s turn.";
        notifyViews();
    }

    /**
     * If the current player is a Bot, this figures out their best move and does it.
     */
    public void playAITurn() {
        if (mustPressNext) {
            nextPlayer();
            return;
        }
        // AI logic handles its own saves via play() and draw() calls
        UnoPlayer ai = players.get(current);
        if (!ai.isAI) return;

        UnoCard top = discard.peek();
        int bestIdx = -1;
        int wildIdx = -1;

        // AI Strategy: Try to match normally, save Wilds for last resort
        for (int i = 0; i < ai.hand.size(); i++) {
            UnoCard c = ai.hand.get(i);
            if (c.matches(top, activeColor, isDark)) {
                if (c.isWild(isDark)) wildIdx = i;
                else if (isActionCard(c)) { bestIdx = i; break; } // Aggressive play
                else if (bestIdx == -1) bestIdx = i;
            }
        }

        if (bestIdx == -1 && wildIdx != -1) bestIdx = wildIdx;

        if (bestIdx != -1) {
            if (isCardWild(bestIdx)) {
                // Bots just pick a random valid color for now
                UnoColor[] opts = isDark ?
                        new UnoColor[]{UnoColor.TEAL, UnoColor.PINK, UnoColor.PURPLE, UnoColor.ORANGE} :
                        new UnoColor[]{UnoColor.RED, UnoColor.BLUE, UnoColor.GREEN, UnoColor.YELLOW};
                playWild(bestIdx, opts[new Random().nextInt(opts.length)]);
            } else {
                play(bestIdx);
            }
        } else {
            draw();
        }
    }

    /**
     * Applies the special rules for the card that was just played (Skip, Flip, etc.).
     */
    private void handleCardEffect(UnoCard chosen, List<UnoCard> currentHand) {
        UnoRank r = chosen.getRank(isDark);
        String msg = " played " + chosen.toText(isDark);

        switch (r) {
            case FLIP -> {
                isDark = !isDark;
                UnoCard top = discard.peek();
                activeColor = top.getColor(isDark);
                // If we flip onto a wild, default to Teal/Red so we don't get stuck
                if (top.isWild(isDark)) activeColor = isDark ? UnoColor.TEAL : UnoColor.RED;
                msg += " FLIP!";
            }
            case DRAW_FIVE -> {
                int victim = properIndex(current + gameDirection);
                for(int i=0; i<5; i++) players.get(victim).hand.add(popOrRecycle());
                nextSteps = 2; // Skip them
                msg += " (Next draws 5)";
            }
            case SKIP_EVERYONE -> { nextSteps = 0; msg += " (Play again!)"; }
            case REVERSE -> { gameDirection = -gameDirection; msg += " Reverse"; }
            case SKIP -> { nextSteps = 2; msg += " Skip"; }
            case DRAW_ONE -> {
                int victim = properIndex(current + gameDirection);
                players.get(victim).hand.add(popOrRecycle());
                nextSteps = 2;
                msg += " Draw 1";
            }
            case WILD_DRAW_TWO -> {
                int victim = properIndex(current + gameDirection);
                for(int i=0; i<2; i++) players.get(victim).hand.add(popOrRecycle());
                nextSteps = 2;
            }
            case WILD_DRAW_COLOR -> {
                int victim = properIndex(current + gameDirection);
                UnoPlayer vp = players.get(victim);
                boolean found = false;
                // They draw until they hit the color the current player just picked
                while(!found) {
                    UnoCard c = popOrRecycle();
                    vp.hand.add(c);
                    if (c.getColor(isDark) == activeColor) found = true;
                }
                nextSteps = 2;
            }
            default -> nextSteps = 1;
        }

        // Check for round winner
        if (currentHand.isEmpty()) {
            handleRoundWin();
            return;
        }

        mustPressNext = true;
        info = players.get(current).name + msg;
        notifyViews();
    }

    /**
     * Calculates points when someone goes out, updates scores, and checks if the whole game is over.
     */
    private void handleRoundWin() {
        UnoPlayer winner = players.get(current);
        int points = 0;
        // Sum up points from everyone else's hands
        for (UnoPlayer p : players) {
            for (UnoCard c : p.hand) {
                UnoRank r = c.getRank(isDark);
                switch(r) {
                    case WILD_DRAW_COLOR -> points += 60;
                    case WILD_DRAW_TWO -> points += 50;
                    case WILD -> points += 40;
                    case DRAW_FIVE, FLIP, SKIP_EVERYONE -> points += 30;
                    case SKIP, REVERSE, DRAW_ONE -> points += 20;
                    default -> points += (r.ordinal() < 10 ? r.ordinal() : 0);
                }
            }
        }
        winner.addScore(points);

        String scoreMsg = winner.name + " wins round! Points: +" + points + "\nTotal Scores:\n";
        for(UnoPlayer p : players) scoreMsg += p.name + ": " + p.getScore() + "\n";

        if (winner.getScore() >= WINNING_SCORE) {
            info = "GAME OVER. " + winner.name + " WINS!";
            notifyViews();
            for (UnoView v : views) v.handleEnd(scoreMsg + "\nGAME OVER!");
        } else {
            info = "Round Over. Next Round starting...";
            notifyViews();
            for (UnoView v : views) v.handleRoundEnd(scoreMsg);
            initializeRound(); // Automatically start the next round
        }
    }

    /**
     * Saves the entire game object to a file.
     * @param file The file location to save to.
     */
    public void saveGame(File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
        }
    }

    /**
     * Loads a game object from a file.
     * @param file The file to load from.
     * @return The loaded UnoModel.
     */
    public static UnoModel loadGame(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            UnoModel m = (UnoModel) ois.readObject();
            // Re-init the transient fields that weren't saved
            if (m.views == null) m.views = new ArrayList<>();
            if (m.undoStack == null) m.undoStack = new ArrayDeque<>();
            if (m.redoStack == null) m.redoStack = new ArrayDeque<>();
            return m;
        }
    }

    // --- HELPER METHODS ---

    private int properIndex(int idx) { int n = players.size(); return ((idx % n) + n) % n; }
    private void ensureAwaitingAction() { if (mustPressNext) throw new IllegalStateException("Press next."); }

    private UnoCard popOrRecycle() {
        if (drawPile.isEmpty()) recycle();
        return drawPile.isEmpty() ? null : drawPile.pop();
    }

    private void recycle() {
        if (discard.isEmpty()) return;
        UnoCard top = discard.pop();
        List<UnoCard> back = new ArrayList<>(discard);
        discard.clear();
        discard.push(top);
        Collections.shuffle(back);
        back.forEach(drawPile::push);
    }

    public UnoCard peekCardInHand(int i) { if (i>=0 && i<players.get(current).hand.size()) return players.get(current).hand.get(i); return null; }
    public boolean isCardWild(int i) { return peekCardInHand(i).isWild(isDark); }
    private boolean isActionCard(UnoCard c) { UnoRank r = c.getRank(isDark); return r == UnoRank.SKIP || r == UnoRank.REVERSE || r == UnoRank.DRAW_ONE || r == UnoRank.DRAW_FIVE || r == UnoRank.FLIP; }

    private List<UnoCard> buildFlipDeck() {
        List<UnoCard> deck = new ArrayList<>();
        UnoColor[] lights = {UnoColor.RED, UnoColor.BLUE, UnoColor.GREEN, UnoColor.YELLOW};
        UnoColor[] darks = {UnoColor.ORANGE, UnoColor.PINK, UnoColor.TEAL, UnoColor.PURPLE};
        for (int i=0; i<4; i++) {
            for (int n=1; n<=9; n++) deck.add(new UnoCard(lights[i], UnoRank.values()[n], darks[i], UnoRank.values()[n]));
            deck.add(new UnoCard(lights[i], UnoRank.FLIP, darks[i], UnoRank.FLIP));
            deck.add(new UnoCard(lights[i], UnoRank.DRAW_ONE, darks[i], UnoRank.DRAW_FIVE));
            deck.add(new UnoCard(lights[i], UnoRank.SKIP, darks[i], UnoRank.SKIP_EVERYONE));
            deck.add(new UnoCard(lights[i], UnoRank.REVERSE, darks[i], UnoRank.REVERSE));
        }
        deck.add(new UnoCard(UnoColor.WILD, UnoRank.WILD, UnoColor.WILD, UnoRank.WILD_DRAW_COLOR));
        deck.add(new UnoCard(UnoColor.WILD, UnoRank.WILD_DRAW_TWO, UnoColor.WILD, UnoRank.WILD_DRAW_COLOR));
        return deck;
    }

    // --- TESTING HELPERS (These are what was missing!) ---

    /**
     * Forcibly sets the top card of the discard pile.
     * USED FOR TESTING ONLY.
     */
    public void setTopCard(UnoCard c) {
        discard.push(c);
        activeColor = c.getColor(isDark);
    }

    /**
     * Gets the top card of the discard pile.
     * USED FOR TESTING ONLY.
     */
    public UnoCard getDiscardTop() {
        return discard.peek();
    }

    /**
     * Checks if the game is in dark mode.
     * USED FOR TESTING ONLY.
     */
    public boolean isDark() {
        return isDark;
    }

    /**
     * Gets the number of cards in the draw pile.
     * USED FOR TESTING ONLY.
     */
    public int getDrawPileSize() {
        return drawPile.size();
    }

    /**
     * Forcibly replaces a player's hand.
     * USED FOR TESTING ONLY.
     */
    public void forceHand(int playerIdx, List<UnoCard> cards) {
        players.get(playerIdx).hand.clear();
        players.get(playerIdx).hand.addAll(cards);
    }
}