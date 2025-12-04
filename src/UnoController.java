import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFileChooser;
import java.io.File;

/**
 * Controller class for the Uno game that handles user interactions and updates the model.
 * Updated for M4 to support Save/Load/Undo/Redo and testability.
 *
 * @author Danilo Bukvic Ajan Balaganesh Aydan Eng Aws Ali
 * @version 4.0
 */
public class UnoController implements ActionListener {

    private UnoModel model;
    private final UnoView view;

    /**
     * Constructor for UnoController.
     * Changed param from UnoFrame to UnoView to allow testing with Stubs.
     *
     * @param model The UnoModel instance representing the game state.
     * @param view The UnoView instance (can be a Frame or a Stub).
     */
    public UnoController(UnoModel model, UnoView view) {
        this.model = model;
        this.view = view;
    }

    /**
     * Helper to switch model reference after loading a game.
     * @param m The new model.
     */
    public void setModel(UnoModel m) {
        this.model = m;
    }

    /**
     * Handles user actions from the UI.
     * @param e The ActionEvent triggered.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        try {
            if (cmd.startsWith("PLAY:")) {
                int idx = Integer.parseInt(cmd.substring("PLAY:".length()));
                if (model.isCardWild(idx)) {
                    UnoColor c = view.promptForWildColor();
                    if (c != null && c != UnoColor.NONE) model.playWild(idx, c);
                } else {
                    model.play(idx);
                }
            } else if (cmd.equals("DRAW")) {
                model.draw();
            } else if (cmd.equals("NEXT")) {
                try {
                    model.nextPlayer();
                } catch (IllegalStateException ex) {
                    model.playAITurn();
                }
            } else if (cmd.equals("UNDO")) {
                model.undo();
            } else if (cmd.equals("REDO")) {
                model.redo();
            } else if (cmd.equals("SAVE")) {
                // Safely get a parent component for the dialog
                Component parent = (view instanceof Component) ? (Component) view : null;
                JFileChooser fc = new JFileChooser();
                if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                    model.saveGame(fc.getSelectedFile());
                    view.showInfo("Game Saved!");
                }
            } else if (cmd.equals("LOAD")) {
                Component parent = (view instanceof Component) ? (Component) view : null;
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                    UnoModel newModel = UnoModel.loadGame(fc.getSelectedFile());
                    this.model = newModel;
                    newModel.addView(view);
                    view.showInfo("Game Loaded!");
                }
            }
        } catch (Exception ex) {
            view.showInfo("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}