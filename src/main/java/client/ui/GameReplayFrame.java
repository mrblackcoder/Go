package client.ui;

import game.go.model.GameState;
import game.go.model.Point;
import game.go.model.Stone;
import game.go.util.GameRecorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;

/**
 * Frame for replaying recorded games
 */
public class GameReplayFrame extends JFrame {
    
    private final GoBoardPanel boardPanel;
    private final List<GameRecorder.Move> moves;
    private final GameState gameState;
    private int currentMoveIndex = -1;
    private final JSlider moveSlider;
    private final JButton playButton;
    private final JButton nextButton;
    private final JButton prevButton;
    private final JLabel moveCountLabel;
    private Timer replayTimer;
    private int replaySpeed = 1000; // 1 saniye varsayılan oynatma hızı
    
    /**
     * Creates a new game replay frame
     */
    public GameReplayFrame(String title, List<GameRecorder.Move> moves, int boardSize) {
        super(title);
        
        // Debug - Hamle sayısını kontrol et
        System.out.println("GameReplayFrame received " + moves.size() + " moves");
        for (int i = 0; i < Math.min(10, moves.size()); i++) {
            System.out.println("Move " + i + ": " + moves.get(i));
        }
        
        this.moves = moves;
        this.gameState = new GameState(boardSize);
        
        // Create components
        boardPanel = new GoBoardPanel();
        moveSlider = new JSlider(0, Math.max(1, moves.size()), 0);
        playButton = new JButton("▶ Play");
        nextButton = new JButton("→ Next");
        prevButton = new JButton("← Prev");
        moveCountLabel = new JLabel("0/" + moves.size());
        
        // Hız kontrol butonları ekleyelim
        JButton slowerButton = new JButton("Slower");
        JButton fasterButton = new JButton("Faster");
        
        // Set up layout
        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(prevButton);
        buttonPanel.add(playButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(new JLabel("Speed:"));
        buttonPanel.add(slowerButton);
        buttonPanel.add(fasterButton);
        buttonPanel.add(moveCountLabel);
        
        controlPanel.add(moveSlider, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(controlPanel, BorderLayout.SOUTH);
        
        // Add listeners
        moveSlider.addChangeListener((ChangeEvent e) -> {
            if (!moveSlider.getValueIsAdjusting()) {
                goToMove(moveSlider.getValue());
            }
        });
        
        playButton.addActionListener((ActionEvent e) -> {
            togglePlayPause();
        });
        
        nextButton.addActionListener((ActionEvent e) -> {
            if (currentMoveIndex < moves.size() - 1) {
                goToMove(currentMoveIndex + 1);
            }
        });
        
        prevButton.addActionListener((ActionEvent e) -> {
            if (currentMoveIndex > 0) {
                goToMove(currentMoveIndex - 1);
            } else if (currentMoveIndex == 0) {
                resetGame();
                currentMoveIndex = -1;
                moveSlider.setValue(0);
                updateBoardDisplay();
                updateMoveCounter();
            }
        });
        
        // Hız kontrol butonları için event listener'ları
        slowerButton.addActionListener((ActionEvent e) -> {
            changeReplaySpeed(false);
        });
        
        fasterButton.addActionListener((ActionEvent e) -> {
            changeReplaySpeed(true);
        });
        
        // Window settings
        setSize(800, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    
    /**
     * Toggles play/pause state
     */
    private void togglePlayPause() {
        if (replayTimer != null && replayTimer.isRunning()) {
            pauseReplay();
        } else {
            startReplay();
        }
    }
    
    /**
     * Starts or resumes replay
     */
    private void startReplay() {
        if (currentMoveIndex >= moves.size() - 1) {
            // Start from beginning if at the end
            currentMoveIndex = -1;
            resetGame();
            updateBoardDisplay();
            updateMoveCounter();
        }
        
        // Stop any existing timer
        if (replayTimer != null && replayTimer.isRunning()) {
            replayTimer.stop();
        }
        
        replayTimer = new Timer(replaySpeed, (ActionEvent evt) -> {
            if (currentMoveIndex < moves.size() - 1) {
                goToMove(currentMoveIndex + 1);
            } else {
                pauseReplay();
            }
        });
        replayTimer.start();
        playButton.setText("⏸ Pause");
    }
    
    /**
     * Pauses replay
     */
    private void pauseReplay() {
        if (replayTimer != null) {
            replayTimer.stop();
        }
        playButton.setText("▶ Play");
    }
    
    /**
     * Changes replay speed
     * @param faster True to increase speed, false to decrease
     */
    private void changeReplaySpeed(boolean faster) {
        if (faster) {
            replaySpeed = Math.max(200, replaySpeed - 200); // Minimum 200ms (5fps)
        } else {
            replaySpeed = Math.min(3000, replaySpeed + 200); // Maximum 3000ms
        }
        
        System.out.println("Replay speed: " + replaySpeed + "ms");
        
        // Update running timer if exists
        if (replayTimer != null && replayTimer.isRunning()) {
            replayTimer.setDelay(replaySpeed);
        }
    }
    
    /**
     * Update the move counter display
     */
    private void updateMoveCounter() {
        int currentMove = currentMoveIndex + 1; // Display 1-based index
        moveCountLabel.setText(currentMove + "/" + moves.size());
    }
    
    /**
     * Go to a specific move in the game
     */
    private void goToMove(int moveIndex) {
        if (moveIndex < 0 || moveIndex >= moves.size() || moveIndex == currentMoveIndex) {
            return;
        }
        
        System.out.println("Going to move: " + moveIndex);
        
        // If we're moving backward, reset and replay
        if (moveIndex < currentMoveIndex) {
            resetGame();
            currentMoveIndex = -1;
        }
        
        // Play all moves up to the target
        while (currentMoveIndex < moveIndex) {
            currentMoveIndex++;
            if (currentMoveIndex < moves.size()) {
                GameRecorder.Move move = moves.get(currentMoveIndex);
                
                if (move.getPoint() != null) {
                    gameState.play(move.getPoint());
                } else if (move.isPass()) {
                    gameState.pass();
                } else if (move.isResign()) {
                    gameState.resign();
                }
            }
        }
        
        moveSlider.setValue(moveIndex);
        updateBoardDisplay();
        updateMoveCounter();
    }
    
    /**
     * Reset the game state
     */
    private void resetGame() {
        gameState.reset();
    }
    
    /**
     * Update the board display based on current game state
     */
    private void updateBoardDisplay() {
        try {
            // Convert the game state to char[][] for display
            char[][] boardDisplay = gameState.board().getGridAsCharArray();
            boardPanel.setBoard(boardDisplay);
            
            // Show last move if available
            if (currentMoveIndex >= 0 && currentMoveIndex < moves.size() && moves.get(currentMoveIndex).getPoint() != null) {
                boardPanel.setLastMove(moves.get(currentMoveIndex).getPoint());
            } else {
                boardPanel.setLastMove(null);
            }
        } catch (Exception e) {
            System.err.println("Error updating board display: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Display the replay frame
     */
    public void showReplay() {
        try {
            resetGame();
            updateBoardDisplay();
            updateMoveCounter();
            
            // Additional debug information
            System.out.println("Showing replay with " + moves.size() + " moves");
            setVisible(true);
        } catch (Exception e) {
            System.err.println("Error showing replay: " + e.getMessage());
            e.printStackTrace();
        }
    }
}