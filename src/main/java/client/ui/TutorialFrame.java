package client.ui;

import game.go.model.Point;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Tutorial frame for teaching Go basics
 */
public class TutorialFrame extends JFrame {
    
    private final GoBoardPanel boardPanel;
    private final JTextArea instructionArea;
    private final JButton nextButton;
    private final JButton prevButton;
    private final List<TutorialStep> steps;
    private int currentStep = 0;
    
    /**
     * Step in the tutorial
     */
    private static class TutorialStep {
        private final String instructions;
        private final char[][] boardState;
        private final Point highlightPoint;
        
        public TutorialStep(String instructions, char[][] boardState, Point highlightPoint) {
            this.instructions = instructions;
            this.boardState = boardState;
            this.highlightPoint = highlightPoint;
        }
    }
    
    /**
     * Creates a new tutorial frame
     */
    public TutorialFrame() {
        super("Go Tutorial");
        
        // Create components
        boardPanel = new GoBoardPanel();
        instructionArea = new JTextArea(5, 40);
        nextButton = new JButton("Next >");
        prevButton = new JButton("< Previous");
        
        // Setup instruction area
        instructionArea.setEditable(false);
        instructionArea.setLineWrap(true);
        instructionArea.setWrapStyleWord(true);
        instructionArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        // Setup layout
        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(instructionArea);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Create tutorial steps
        steps = createTutorialSteps();
        
        // Add listeners
        nextButton.addActionListener((ActionEvent e) -> {
            if (currentStep < steps.size() - 1) {
                currentStep++;
                showCurrentStep();
            }
        });
        
        prevButton.addActionListener((ActionEvent e) -> {
            if (currentStep > 0) {
                currentStep--;
                showCurrentStep();
            }
        });
        
        // Window settings
        setSize(800, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Show first step
        showCurrentStep();
    }
    
    /**
     * Create the tutorial steps
     */
    private List<TutorialStep> createTutorialSteps() {
        List<TutorialStep> tutorialSteps = new ArrayList<>();
        
        // Step 1: Introduction to Go
        char[][] emptyBoard = new char[19][19];
        for (int y = 0; y < 19; y++) {
            for (int x = 0; x < 19; x++) {
                emptyBoard[y][x] = '.';
            }
        }
        
        tutorialSteps.add(new TutorialStep(
            "Welcome to Go! Go is an ancient board game originating from China over 2,500 years ago.\n\n" +
            "The game is played on a grid of 19×19 lines (smaller 9×9 or 13×13 boards are also common for beginners).\n\n" +
            "The basic rules are simple, but the game offers profound strategic complexity.",
            emptyBoard, null
        ));
        
        // Step 2: Stone placement
        char[][] step2Board = deepCopyBoard(emptyBoard);
        step2Board[3][3] = 'B';
        
        tutorialSteps.add(new TutorialStep(
            "Players take turns placing stones on the intersections of the grid.\n\n" +
            "Black plays first, followed by White. Once placed, stones do not move, but they can be captured.\n\n" +
            "In this example, Black has placed a stone at the 4-4 point (or 'hoshi'), a common opening.",
            step2Board, new Point(3, 3)
        ));
        
        // Step 3: Liberties
        char[][] step3Board = deepCopyBoard(step2Board);
        step3Board[3][4] = 'W';
        
        tutorialSteps.add(new TutorialStep(
            "Each stone has 'liberties' - empty adjacent points connected horizontally or vertically (not diagonally).\n\n" +
            "The black stone has 4 liberties initially. After White plays, Black's stone now has 3 liberties.\n\n" +
            "Understanding liberties is crucial because stones with no liberties are captured.",
            step3Board, new Point(3, 4)
        ));
        
        // Step 4: Capture
        char[][] step4Board = deepCopyBoard(emptyBoard);
        step4Board[3][3] = 'B';
        step4Board[2][3] = 'W';
        step4Board[4][3] = 'W';
        step4Board[3][2] = 'W';
        
        tutorialSteps.add(new TutorialStep(
            "When a stone loses all its liberties, it is captured and removed from the board.\n\n" +
            "Here, the black stone has only one liberty left (at the bottom). If White plays there, the black stone will be captured.",
            step4Board, new Point(3, 4)
        ));
        
        // Step 5: Capture demonstration
        char[][] step5Board = deepCopyBoard(step4Board);
        step5Board[3][4] = 'W';
        step5Board[3][3] = '.'; // Black stone is captured
        
        tutorialSteps.add(new TutorialStep(
            "White has played at the black stone's last liberty.\n\n" +
            "The black stone has been captured and removed from the board.\n\n" +
            "Captured stones count as territory points at the end of the game.",
            step5Board, new Point(3, 4)
        ));
        
        // Step 6: Connected groups
        char[][] step6Board = deepCopyBoard(emptyBoard);
        step6Board[3][3] = 'B';
        step6Board[3][4] = 'B';
        step6Board[4][3] = 'B';
        step6Board[2][3] = 'W';
        step6Board[3][2] = 'W';
        
        tutorialSteps.add(new TutorialStep(
            "Connected stones of the same color form a group.\n\n" +
            "A group has liberties at all empty points adjacent to any stone in the group.\n\n" +
            "In this example, the black group has 5 liberties. The entire group is captured only when all its liberties are filled.",
            step6Board, new Point(3, 3)
        ));
        
        // Step 7: Ko rule
        char[][] step7Board = deepCopyBoard(emptyBoard);
        step7Board[3][3] = 'B';
        step7Board[2][3] = 'B';
        step7Board[4][3] = 'B';
        step7Board[3][2] = 'B';
        step7Board[3][4] = 'W';
        step7Board[2][4] = 'W';
        step7Board[4][4] = 'W';
        step7Board[5][3] = 'W';
        step7Board[4][2] = 'W';
        step7Board[3][1] = 'W';
        step7Board[2][2] = 'W';
        step7Board[1][3] = 'W';
        
        tutorialSteps.add(new TutorialStep(
            "The 'Ko' rule prevents infinite loops of capturing and recapturing.\n\n" +
            "If a player captures exactly one stone, the opponent cannot immediately recapture it if it would recreate the previous board position.\n\n" +
            "Instead, they must play elsewhere first, then they can recapture.",
            step7Board, new Point(3, 3)
        ));
        
        // Step 8: Suicide rule
        char[][] step8Board = deepCopyBoard(emptyBoard);
        step8Board[0][1] = 'W';
        step8Board[1][0] = 'W';
        
        tutorialSteps.add(new TutorialStep(
            "The 'Suicide' rule prohibits playing a stone that would have no liberties after placement, unless it captures opponent stones.\n\n" +
            "In this example, Black cannot play at the corner (0,0) because it would have no liberties and doesn't capture any white stones.\n\n" +
            "This prevents players from capturing their own stones.",
            step8Board, new Point(0, 0)
        ));
        
        // Step 9: Passing
        char[][] step9Board = deepCopyBoard(emptyBoard);
        
        tutorialSteps.add(new TutorialStep(
            "Players can 'pass' their turn if they don't want to place a stone.\n\n" +
            "When both players pass consecutively, the game ends.\n\n" +
            "This typically happens when both players agree that no more profitable moves are available.",
            step9Board, null
        ));
        
        // Step 10: Scoring
        char[][] step10Board = new char[9][9]; // Smaller board for demonstration
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                step10Board[y][x] = '.';
            }
        }
        // Set up some black and white territories
        step10Board[0][0] = 'B'; step10Board[0][1] = 'B'; step10Board[1][0] = 'B';
        step10Board[7][7] = 'W'; step10Board[7][8] = 'W'; step10Board[8][7] = 'W';
        
        tutorialSteps.add(new TutorialStep(
            "At the end of the game, territory is counted.\n\n" +
            "A player's score is the sum of:\n" +
            "1. The number of empty intersections surrounded by their stones\n" +
            "2. The number of opponent's stones they captured\n\n" +
            "White usually receives a 'komi' (compensation points, typically 6.5) for going second.\n\n" +
            "The player with the highest score wins.",
            step10Board, null
        ));
        
        // Step 11: Eyes and Life
        char[][] step11Board = deepCopyBoard(emptyBoard);
        step11Board[3][3] = 'B'; step11Board[3][4] = 'B'; step11Board[3][5] = 'B';
        step11Board[4][3] = 'B'; step11Board[4][5] = 'B';
        step11Board[5][3] = 'B'; step11Board[5][4] = 'B'; step11Board[5][5] = 'B';
        
        tutorialSteps.add(new TutorialStep(
            "An 'eye' is an empty point surrounded by stones of the same color.\n\n" +
            "A group with two or more eyes cannot be captured because an opponent cannot fill all liberties.\n\n" +
            "In this example, Black has formed a group with two eyes at (4,4). This group is 'alive' and cannot be captured.",
            step11Board, new Point(4, 4)
        ));
        
        // Step 12: Dead stones
        char[][] step12Board = deepCopyBoard(emptyBoard);
        step12Board[1][1] = 'B'; step12Board[1][2] = 'B'; step12Board[1][3] = 'B';
        step12Board[2][1] = 'B'; step12Board[2][3] = 'B';
        step12Board[3][1] = 'B'; step12Board[3][2] = 'B'; step12Board[3][3] = 'B';
        step12Board[2][2] = 'W';
        
        tutorialSteps.add(new TutorialStep(
            "A 'dead' stone is one that is surrounded and has no way to form two eyes.\n\n" +
            "In this example, the white stone at (2,2) is completely surrounded. It is considered 'dead' because it cannot escape capture.\n\n" +
            "At the end of the game, dead stones are removed without actually playing the capturing moves.",
            step12Board, new Point(2, 2)
        ));
        
        // Step 13: Seki
        char[][] step13Board = new char[9][9];
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                step13Board[y][x] = '.';
            }
        }
        step13Board[3][3] = 'B'; step13Board[3][4] = 'B'; step13Board[4][5] = 'B'; step13Board[5][4] = 'B'; step13Board[5][3] = 'B';
        step13Board[4][2] = 'W'; step13Board[5][2] = 'W'; step13Board[6][3] = 'W'; step13Board[6][4] = 'W'; step13Board[5][5] = 'W';
        
        tutorialSteps.add(new TutorialStep(
            "'Seki' is a situation where neither player can capture the other's stones without putting their own in danger.\n\n" +
            "In this example, both black and white groups share liberties. If either player tries to fill these shared liberties, they put their own stones at risk of capture.\n\n" +
            "Seki positions are considered 'alive' and remain on the board at the end of the game.",
            step13Board, new Point(4, 3)
        ));
        
        // Step 14: Strategy - Corners, Sides, Center
        char[][] step14Board = new char[9][9];
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                step14Board[y][x] = '.';
            }
        }
        step14Board[0][0] = 'B'; step14Board[8][0] = 'B'; step14Board[0][8] = 'B'; step14Board[8][8] = 'B';
        step14Board[4][0] = 'B'; step14Board[0][4] = 'B'; step14Board[8][4] = 'B'; step14Board[4][8] = 'B';
        step14Board[4][4] = 'B';
        
        tutorialSteps.add(new TutorialStep(
            "Go strategy typically follows this priority: corners first, then sides, then center.\n\n" +
            "Corners are valuable because they require fewer stones to create secure territory.\n\n" +
            "Sides are next in value, and the center is typically developed last.\n\n" +
            "This principle is often summarized as: 'Corner, Side, Center'.",
            step14Board, null
        ));
        
        // Step 15: Common Opening Moves
        char[][] step15Board = deepCopyBoard(emptyBoard);
        step15Board[3][3] = 'B'; step15Board[15][15] = 'W'; step15Board[15][3] = 'B'; step15Board[3][15] = 'W';
        
        tutorialSteps.add(new TutorialStep(
            "Common opening moves in Go include playing near the 4-4 points (hoshi) on the board.\n\n" +
            "Professional games often start with players claiming corners by playing on or around these star points.\n\n" +
            "Other common opening points include 3-3 (closer to corner), 3-4, and 4-5.\n\n" +
            "This completes the basic Go tutorial. Happy playing!",
            step15Board, null
        ));
        
        return tutorialSteps;
    }
    
    /**
     * Create a deep copy of a board
     */
    private char[][] deepCopyBoard(char[][] original) {
        char[][] copy = new char[original.length][original[0].length];
        for (int y = 0; y < original.length; y++) {
            System.arraycopy(original[y], 0, copy[y], 0, original[y].length);
        }
        return copy;
    }
    
    /**
     * Show the current tutorial step
     */
    private void showCurrentStep() {
        TutorialStep step = steps.get(currentStep);
        
        // Update board display
        boardPanel.setBoard(step.boardState);
        boardPanel.setLastMove(step.highlightPoint);
        
        // Update instruction text
        instructionArea.setText((currentStep + 1) + "/" + steps.size() + ": " + step.instructions);
        instructionArea.setCaretPosition(0);
        
        // Update button states
        prevButton.setEnabled(currentStep > 0);
        nextButton.setEnabled(currentStep < steps.size() - 1);
        
        // Update window title
        setTitle("Go Tutorial - Step " + (currentStep + 1) + " of " + steps.size());
    }
    
    /**
     * Display the tutorial
     */
    public static void showTutorial() {
        SwingUtilities.invokeLater(() -> {
            new TutorialFrame().setVisible(true);
        });
    }
}