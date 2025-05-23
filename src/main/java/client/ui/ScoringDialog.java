package client.ui;

import game.go.model.GameState;
import game.go.model.Point;
import game.go.model.Stone;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Dialog for marking dead stones and calculating scores at game end
 */
public class ScoringDialog extends JFrame {
    
    private final GoBoardPanel boardPanel;
    private final GameState gameState;
    private final JLabel scoreLabel;
    private final JButton doneButton;
    private final JButton resetButton;
    private final double komi;
    
    /**
     * Creates a new scoring dialog
     * 
     * @param gameState The current game state
     */
    public ScoringDialog(GameState gameState) {
        super("Game Scoring");
        this.gameState = gameState;
        // DÜZELTME: getKomi() metodunu çağırıyoruz (GameState'e eklendi)
        this.komi = gameState.getKomi();
        
        // Create components
        boardPanel = new GoBoardPanel();
        scoreLabel = new JLabel("Click stones to mark as dead");
        doneButton = new JButton("Done");
        resetButton = new JButton("Reset");
        
        // Initialize board with current game state
        boardPanel.setBoard(gameState.board().getGridAsCharArray());
        
        // Setup layout
        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(scoreLabel, BorderLayout.NORTH);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(resetButton);
        buttonPanel.add(doneButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Add listeners
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = boardPanel.getBoardCoordinates(e.getX(), e.getY());
                if (p != null) {
                    toggleDeadStone(p);
                }
            }
        });
        
        doneButton.addActionListener((ActionEvent e) -> {
            calculateFinalScore();
            dispose();
        });
        
        resetButton.addActionListener((ActionEvent e) -> {
            gameState.resetDeadStones();
            updateBoardDisplay();
            updateScoreDisplay();
        });
        
        // Window settings
        setSize(800, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Initial update
        updateScoreDisplay();
    }
    
    /**
     * Toggle a stone as dead/alive
     */
    private void toggleDeadStone(Point p) {
        if (gameState.toggleDeadStone(p)) {
            updateBoardDisplay();
            updateScoreDisplay();
        }
    }
    
    /**
     * Update the board display to show dead stones
     */
    private void updateBoardDisplay() {
        // Get current board state
        char[][] board = gameState.board().getGridAsCharArray();
        
        // Mark dead stones
        for (Point p : gameState.getMarkedDeadStones()) {
            // Mark with lowercase for dead stones
            if (board[p.y()][p.x()] == 'B') {
                board[p.y()][p.x()] = 'b';
            } else if (board[p.y()][p.x()] == 'W') {
                board[p.y()][p.x()] = 'w';
            }
        }
        
        boardPanel.setBoard(board);
    }
    
    /**
     * Update the score display
     */
    private void updateScoreDisplay() {
        Map<Stone, Integer> scores = gameState.calculateTerritorialScores();
        int blackScore = scores.get(Stone.BLACK);
        int whiteScore = scores.get(Stone.WHITE);
        
        scoreLabel.setText(String.format("Black: %d | White: %d (includes %.1f komi) | Click stones to mark as dead", 
                blackScore, whiteScore, komi));
    }
    
    /**
     * Calculate and display the final score
     */
    private void calculateFinalScore() {
        Map<Stone, Integer> scores = gameState.calculateTerritorialScores();
        int blackScore = scores.get(Stone.BLACK);
        int whiteScore = scores.get(Stone.WHITE);
        
        String result;
        if (blackScore > whiteScore) {
            result = String.format("Black wins by %.1f points (%d - %d)", 
                    blackScore - whiteScore, blackScore, whiteScore);
        } else if (whiteScore > blackScore) {
            result = String.format("White wins by %.1f points (%d - %d)", 
                    whiteScore - blackScore, whiteScore, blackScore);
        } else {
            result = String.format("Game ends in a draw (%d - %d)", blackScore, whiteScore);
        }
        
        // Show final result in a message dialog
        javax.swing.JOptionPane.showMessageDialog(this, result, "Final Score", 
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Display the scoring dialog
     */
    public static void showScoringDialog(GameState gameState) {
        SwingUtilities.invokeLater(() -> {
            new ScoringDialog(gameState).setVisible(true);
        });
    }
}