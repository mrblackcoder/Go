package game.go.model;

import game.go.util.GameRecorder;
import java.util.*;

/**
 * Enhanced Go game state manager
 * Features improved Ko rule detection, territory scoring, and game mechanics
 * Now with consistent move recording
 */
public class GameState {

    private Board board;
    private Stone currentPlayer = Stone.BLACK;
    private int consecutivePasses = 0;
    private final Deque<Long> positionHistory = new ArrayDeque<>();
    private boolean gameOver = false;
    private Point lastMove = null;
    private String gameOverReason = "";
    private final Set<Point> markedDeadStones = new HashSet<>();
    private double komi = 6.5; // Default komi value (can be changed)
    
    // Add a game recorder reference
    private GameRecorder recorder = null;

    /**
     * Creates a new game state with specified board size
     * 
     * @param size Board size (typically 9, 13, or 19)
     */
    public GameState(int size) {
        this.board = new Board(size);
        // Record hash of initial position
        positionHistory.push(Zobrist.fullHash(board));
    }
    
    /**
     * Sets a game recorder to record all moves
     * 
     * @param recorder The game recorder to use
     */
    public void setRecorder(GameRecorder recorder) {
        this.recorder = recorder;
        System.out.println("GameState: Recorder attached. Recording is " + 
                          (recorder != null && recorder.isRecording() ? "enabled" : "disabled"));
    }

    /**
     * Places a stone on the board: first tries on a clone, then checks for Ko,
     * finally applies to the real board.
     * 
     * @param p Point to play
     * @return Result of the move
     */
    public Board.MoveResult play(Point p) {
        if (gameOver) {
            return new Board.MoveResult(false, "Game is over. Reason: " + gameOverReason);
        }

        // 1) Try move on clone
        Board tmpBoard = board.copy();
        Board.MoveResult trial = tmpBoard.placeStone(p, currentPlayer);
        if (!trial.valid) {
            return trial;  // "Cell occupied" or "Suicide move"
        }
        long newPositionHash = Zobrist.fullHash(tmpBoard);

        // 2) Ko check: new position cannot equal any previous position
        // Stricter super-ko rule (no position repetition)
        if (positionHistory.contains(newPositionHash)) {
            return new Board.MoveResult(false, "Ko violation: this position has occurred before");
        }

        // 3) Valid move: update real board, push to history, change turn
        board.setState(tmpBoard);
        positionHistory.push(newPositionHash);
        lastMove = p;
        consecutivePasses = 0;
        
        // IMPORTANT: Record this move before changing the current player
        if (recorder != null) {
            recorder.recordMove(p, currentPlayer);
            System.out.println("GameState: Recorded move at " + p + " by " + currentPlayer);
        } else {
            System.out.println("GameState: Move at " + p + " NOT recorded (no recorder)");
        }
        
        currentPlayer = currentPlayer.opponent();
        return new Board.MoveResult(true, "");
    }

    /**
     * Passes the current turn. Two consecutive passes end the game.
     * 
     * @return Result of the move
     */
    public Board.MoveResult pass() {
        if (gameOver) {
            return new Board.MoveResult(false, "Game is over. Reason: " + gameOverReason);
        }

        if (++consecutivePasses >= 2) {
            gameOver = true;
            gameOverReason = "Both players passed";
        }
        positionHistory.push(Zobrist.fullHash(board));
        lastMove = null;
        
        // IMPORTANT: Record this pass before changing the current player
        if (recorder != null) {
            recorder.recordPass(currentPlayer);
            System.out.println("GameState: Recorded pass by " + currentPlayer);
        } else {
            System.out.println("GameState: Pass by " + currentPlayer + " NOT recorded (no recorder)");
        }
        
        currentPlayer = currentPlayer.opponent();
        return new Board.MoveResult(true, "");
    }

    /**
     * Current player resigns. Game ends immediately.
     * 
     * @return Result of the move
     */
    public Board.MoveResult resign() {
        if (gameOver) {
            return new Board.MoveResult(false, "Game is already over. Reason: " + gameOverReason);
        }

        gameOver = true;
        gameOverReason = currentPlayer + " resigned";
        
        // IMPORTANT: Record this resignation before changing anything
        if (recorder != null) {
            recorder.recordResign(currentPlayer);
            System.out.println("GameState: Recorded resignation by " + currentPlayer);
        } else {
            System.out.println("GameState: Resignation by " + currentPlayer + " NOT recorded (no recorder)");
        }
        
        return new Board.MoveResult(true, gameOverReason);
    }

    /**
     * Mark a stone as dead during scoring phase
     * 
     * @param p Point to mark
     * @return True if marking was successful
     */
    public boolean toggleDeadStone(Point p) {
        if (!gameOver) {
            return false;
        }
        
        Stone stone = board.get(p);
        if (stone == Stone.EMPTY) {
            return false;
        }
        
        if (markedDeadStones.contains(p)) {
            markedDeadStones.remove(p);
        } else {
            markedDeadStones.add(p);
            
            // Also mark all connected stones of the same color as dead
            Set<Point> group = board.groupOf(p);
            markedDeadStones.addAll(group);
        }
        
        return true;
    }

    /**
     * Resets the game state to the beginning
     */
    public void reset() {
        // Create a new board of the same size
        board = new Board(board.getSize());
        
        // Reset game state variables
        currentPlayer = Stone.BLACK;
        consecutivePasses = 0;
        positionHistory.clear();
        positionHistory.push(Zobrist.fullHash(board));
        gameOver = false;
        lastMove = null;
        gameOverReason = "";
        markedDeadStones.clear();
        
        // Don't reset the recorder - it should be preserved
    }

    /**
     * Clear all marked dead stones
     */
    public void resetDeadStones() {
        markedDeadStones.clear();
    }

    /**
     * Returns whether the game is over
     * 
     * @return True if game is over
     */
    public boolean isOver() {
        return gameOver;
    }

    /**
     * Calculates score for specified player (controlled area + captured stones)
     * 
     * @param s Player's stone color
     * @return Player's score
     */
    public int scoreFor(Stone s) {
        return board.areaControlledBy(s) + board.getCapturedBy(s);
    }

    /**
     * Returns the game board
     * 
     * @return Game board
     */
    public Board board() {
        return board;
    }

    /**
     * Returns the current player (whose turn it is)
     * 
     * @return Current player's stone color
     */
    public Stone getCurrentPlayer() {
        return currentPlayer;
    }
    
    /**
     * toPlay metodu - GameSession sınıfıyla uyumluluk için alias
     */
    public Stone toPlay() {
        return getCurrentPlayer();
    }
    
    /**
     * Returns the last move
     * 
     * @return Last move point or null (if passed)
     */
    public Point getLastMove() {
        return lastMove;
    }
    
    /**
     * Returns the game over reason
     * 
     * @return Game over reason
     */
    public String getGameOverReason() {
        return gameOverReason;
    }
    
    /**
     * Gets the set of marked dead stones
     * 
     * @return Set of points containing marked dead stones
     */
    public Set<Point> getMarkedDeadStones() {
        return Collections.unmodifiableSet(markedDeadStones);
    }
    
    /**
     * Set the komi value
     * 
     * @param komi The komi value to set
     */
    public void setKomi(double komi) {
        this.komi = komi;
    }
    
    /**
     * Get the komi value
     * 
     * @return The current komi value
     */
    public double getKomi() {
        return komi;
    }
    
    /**
     * Calculates areas and determines territories
     * Advanced territorial scoring method
     * 
     * @return Territorial scores for both players
     */
    public Map<Stone, Integer> calculateTerritorialScores() {
        Map<Stone, Integer> scores = new HashMap<>();
        Set<Point> visited = new HashSet<>();
        int blackTerritory = 0;
        int whiteTerritory = 0;
        
        // First, remove all dead stones
        Board scoringBoard = board.copy();
        for (Point deadStone : markedDeadStones) {
            Stone capturedColor = scoringBoard.get(deadStone);
            scoringBoard.removeStone(deadStone);
            
            // Add to prisoner count
            if (capturedColor == Stone.BLACK) {
                whiteTerritory++;
            } else if (capturedColor == Stone.WHITE) {
                blackTerritory++;
            }
        }
        
        // Check all empty areas
        for (int y = 0; y < scoringBoard.getSize(); y++) {
            for (int x = 0; x < scoringBoard.getSize(); x++) {
                Point p = new Point(x, y);
                if (scoringBoard.get(p) == Stone.EMPTY && !visited.contains(p)) {
                    // Find this region and its owner
                    Set<Point> region = new HashSet<>();
                    Stone owner = findRegionOwner(p, region, visited, scoringBoard);
                    
                    // Update score based on region owner
                    if (owner == Stone.BLACK) {
                        blackTerritory += region.size();
                    } else if (owner == Stone.WHITE) {
                        whiteTerritory += region.size();
                    }
                }
            }
        }
        
        // Add captured stones count
        blackTerritory += board.getCapturedBy(Stone.BLACK);
        whiteTerritory += board.getCapturedBy(Stone.WHITE);
        
        // Add komi to white's score
        whiteTerritory += (int)komi;
        
        scores.put(Stone.BLACK, blackTerritory);
        scores.put(Stone.WHITE, whiteTerritory);
        
        return scores;
    }
    
    /**
     * Finds the owner of an empty region
     * 
     * @param start Starting point
     * @param region Region points (output parameter)
     * @param visited Visited points (output parameter)
     * @param board Board to check
     * @return Owner of the region or null (disputed territory)
     */
    private Stone findRegionOwner(Point start, Set<Point> region, Set<Point> visited, Board board) {
        Set<Stone> borderingColors = new HashSet<>();
        Queue<Point> queue = new LinkedList<>();
        queue.add(start);
        
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            if (visited.contains(p)) continue;
            
            visited.add(p);
            if (board.get(p) == Stone.EMPTY) {
                region.add(p);
                
                // Check neighbors
                for (Point neighbor : board.neighbors(p)) {
                    if (neighbor.inBounds(board.getSize())) {
                        Stone stone = board.get(neighbor);
                        if (stone == Stone.EMPTY) {
                            queue.add(neighbor);
                        } else {
                            borderingColors.add(stone);
                        }
                    }
                }
            }
        }
        
        // If region is surrounded by only one color, that color owns it
        if (borderingColors.size() == 1) {
            return borderingColors.iterator().next();
        }
        
        // Disputed territory (surrounded by multiple colors)
        return null;
    }
}