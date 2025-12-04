import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The main game window. It draws the cards, buttons, and menus.
 * It implements UnoView so the Model can tell it when to redraw.
 *
 * @author Danilo Bukvic Ajan Balaganesh Aydan Eng Aws Ali
 * @version 4.0
 */
public class UnoFrame extends JFrame implements UnoView {
    private final JLabel labelTopCard = new JLabel("Top: -", SwingConstants.CENTER);
    private final JLabel labelPlayer = new JLabel("Player: -", SwingConstants.CENTER);
    private final JLabel labelInfo = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel handPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    private final JButton buttonDraw = new JButton("Draw");
    private final JButton buttonNext = new JButton("Next Player");

    // Menu items for the top bar
    private final JMenuItem menuUndo = new JMenuItem("Undo");
    private final JMenuItem menuRedo = new JMenuItem("Redo");

    private UnoController controller;
    private boolean isDark = false;

    /**
     * Builds the GUI, sets up the menu bar, and asks for player names.
     *
     * @param title The text in the window title bar.
     */
    public UnoFrame(String title) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);

        //Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem itemSave = new JMenuItem("Save Game");
        JMenuItem itemLoad = new JMenuItem("Load Game");
        itemSave.setActionCommand("SAVE");
        itemLoad.setActionCommand("LOAD");
        fileMenu.add(itemSave);
        fileMenu.add(itemLoad);

        JMenu gameMenu = new JMenu("Game");
        menuUndo.setActionCommand("UNDO");
        menuRedo.setActionCommand("REDO");
        gameMenu.add(menuUndo);
        gameMenu.add(menuRedo);

        menuBar.add(fileMenu);
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);

        //Players
        // In a real app we'd probably use a dialog, defaulting to 2 for simplicity here
        int num = 2;
        List<String> names = new ArrayList<>();
        List<Boolean> isAI = new ArrayList<>();
        for(int i=1; i<=num; i++) { names.add("Player "+i); isAI.add(false); }

        UnoModel model = new UnoModel(num, names, isAI);
        controller = new UnoController(model, this);

        // Listeners
        itemSave.addActionListener(controller);
        itemLoad.addActionListener(controller);
        menuUndo.addActionListener(controller);
        menuRedo.addActionListener(controller);
        buttonDraw.setActionCommand("DRAW");
        buttonDraw.addActionListener(controller);
        buttonNext.setActionCommand("NEXT");
        buttonNext.addActionListener(controller);

        // Components
        JPanel north = new JPanel(new GridLayout(2, 1));
        JPanel stats = new JPanel(new GridLayout(1, 3));
        stats.add(labelTopCard); stats.add(labelPlayer); stats.add(labelInfo);
        north.add(stats);

        JPanel south = new JPanel();
        south.add(buttonDraw); south.add(buttonNext);

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(handPanel), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        model.addView(this);
        setVisible(true);
    }

    /**
     * Called when the model changes. We repaint the whole hand and update text.
     */
    @Override
    public void handleUpdate(UnoEvent e) {
        this.isDark = e.isDark();
        // Flip background color if we are in dark mode
        Color bgColor = isDark ? new Color(50, 0, 50) : new Color(240, 240, 240);
        handPanel.setBackground(bgColor);
        getContentPane().setBackground(bgColor);

        labelTopCard.setText("Top: " + e.getTopCardText());
        labelPlayer.setText("Turn: " + e.getCurrentPlayerName());
        labelInfo.setText(e.getInfo());

        // Redraw hand buttons
        handPanel.removeAll();
        for (int i = 0; i < e.getHand().size(); i++) {
            UnoCard c = e.getHand().get(i);
            JButton b = new JButton(c.toText(isDark));
            b.setBackground(mapCardColor(c.getColor(isDark)));
            b.setForeground(isDark ? Color.WHITE : Color.BLACK);
            b.setActionCommand("PLAY:" + i);
            b.addActionListener(controller);
            // Disable buttons if it's not our turn to play
            b.setEnabled(!e.isMustPressNext() && !e.isAIPlayer());
            handPanel.add(b);
        }

        // Configure buttons based on game state
        if (e.isAIPlayer()) {
            buttonDraw.setEnabled(false);
            buttonNext.setText(e.isMustPressNext() ? "Next Player" : "Run AI Turn");
            buttonNext.setEnabled(true);
        } else {
            buttonDraw.setEnabled(!e.isMustPressNext());
            buttonNext.setText("Next Player");
            buttonNext.setEnabled(e.isMustPressNext());
        }

        handPanel.revalidate();
        handPanel.repaint();
    }

    /**
     * Pops up a dialog when a round ends showing the scores.
     */
    @Override
    public void handleRoundEnd(String message) {
        JOptionPane.showMessageDialog(this, message, "Round Over", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Pops up a dialog when the whole game is won.
     */
    @Override
    public void handleEnd(String message) {
        JOptionPane.showMessageDialog(this, message, "Game Over", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    /**
     * Opens a small dialog asking the user to pick a Wild color.
     */
    @Override
    public UnoColor promptForWildColor() {
        UnoColor[] opts = isDark ?
                new UnoColor[]{UnoColor.TEAL, UnoColor.PINK, UnoColor.PURPLE, UnoColor.ORANGE} :
                new UnoColor[]{UnoColor.RED, UnoColor.GREEN, UnoColor.BLUE, UnoColor.YELLOW};
        int n = JOptionPane.showOptionDialog(this, "Choose Color:", "Wild", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
        return (n >= 0) ? opts[n] : opts[0];
    }

    @Override
    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Helper to get visual colors for the cards.
     */
    @Override
    public Color mapCardColor(UnoColor c) {
        return switch(c) {
            case RED -> new Color(255, 80, 80);
            case GREEN -> new Color(80, 200, 80);
            case BLUE -> new Color(80, 80, 255);
            case YELLOW -> new Color(255, 220, 0);
            case TEAL -> new Color(0, 128, 128);
            case PINK -> new Color(255, 105, 180);
            case PURPLE -> new Color(128, 0, 128);
            case ORANGE -> new Color(255, 165, 0);
            default -> Color.GRAY;
        };
    }
}