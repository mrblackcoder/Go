package game.go.manager;

import game.go.model.Board.MoveResult;
import game.go.model.GameState;
import game.go.model.Point;
import game.go.model.Stone;
import game.go.util.GameRecorder;

/**
 * Example game manager implementation that ensures proper move recording
 */
public class GameManager {
    private final GameState gameState;
    private final GameRecorder recorder;
    private String blackPlayerName;
    private String whitePlayerName;
    
    /**
     * Creates a new game manager
     * 
     * @param boardSize Size of the game board
     * @param blackPlayerName Name of the black player
     * @param whitePlayerName Name of the white player
     */
    public GameManager(int boardSize, String blackPlayerName, String whitePlayerName) {
        this.gameState = new GameState(boardSize);
        this.blackPlayerName = blackPlayerName;
        this.whitePlayerName = whitePlayerName;
        
        // Create and attach the recorder - CRITICAL step
        this.recorder = new GameRecorder(boardSize, blackPlayerName, whitePlayerName);
        gameState.setRecorder(recorder);
        

System.out.println("GameManager: Initialized with recorder. Status: " + 
                 (recorder.isRecording() ? "Recording" : "Not recording"));
    }
    
    /**
     * Place a stone at the specified position
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if the move was successful
     */
    public boolean placeStone(int x, int y) {
        Point p = new Point(x, y);
        // GameState will handle recording the move
        return gameState.play(p).valid;
    }
    
    /**
     * Pass the current turn
     * 
     * @return True if pass was successful
     */
    public boolean pass() {
        // GameState will handle recording the pass
        return gameState.pass().valid;
    }
    
    /**
     * Current player resigns
     * 
     * @return True if resignation was successful
     */
   public boolean resign() {
    // GameState will handle recording the resignation
    MoveResult result = gameState.resign();
    return result.valid;
}
    
    /**
     * Check if recording is enabled
     * 
     * @return True if recording is enabled
     */
    public boolean isRecording() {
        return recorder != null && recorder.isRecording();
    }
    
    /**
     * Enable or disable recording
     * 
     * @param enable True to enable recording, false to disable
     */
    public void enableRecording(boolean enable) {
        if (recorder != null) {
            recorder.enableRecording(enable);
            System.out.println("GameManager: Recording " + (enable ? "enabled" : "disabled"));
        }
    }
    
    /**
     * Save the game record to a file
     * 
     * @param filePath Path to save the SGF file
     * @return True if saving was successful
     */
    public boolean saveGame(String filePath) {
        try {
            if (recorder != null) {
                // Print debug info before saving
                recorder.printMoves();
                
                recorder.saveToSgf(filePath);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error saving game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get the current player
     * 
     * @return Current player's stone color
     */
    public Stone getCurrentPlayer() {
        // DÜZELTME 2: Doğru metod çağrılıyor - toPlay() kullanılıyor
        return gameState.toPlay();
    }
    
    /**
     * Get the game state
     * 
     * @return Game state
     */
    public GameState getGameState() {
        return gameState;
    }
    
    /**
     * Get the number of recorded moves
     * 
     * @return Number of recorded moves
     */
    public int getMoveCount() {
        return recorder != null ? recorder.getMoveCount() : 0;
    }
}