package game.go.model;

import java.util.*;

/**
 * Enhanced Go game state manager
 * Features improved Ko rule detection, territory scoring, and game mechanics
 */
public class GameState {

    private final Board board;
    private Stone currentPlayer = Stone.BLACK;
    private int consecutivePasses = 0;
    private final Deque<Long> positionHistory = new ArrayDeque<>();
    private boolean gameOver = false;
    private Point lastMove = null;
    private String gameOverReason = "";
    private final Set<Point> markedDeadStones = new HashSet<>();

    /**
     * Zobrist sınıfı (hash değeri hesaplama için)
     * Gerçek uygulamada bu ayrı bir dosyada olacaktır
     */
    private static class Zobrist {
        private static final Random RANDOM = new Random(42); // Sabit seed
        private static final long[][][] TABLE = new long[19][19][3]; // En büyük tahta boyutu için
        
        static {
            // Hash tablosunu başlat
            for (int x = 0; x < 19; x++) {
                for (int y = 0; y < 19; y++) {
                    for (int s = 0; s < 3; s++) { // BOŞ, SİYAH, BEYAZ
                        TABLE[x][y][s] = RANDOM.nextLong();
                    }
                }
            }
        }
        
        public static long fullHash(Board board) {
            long hash = 0;
            int size = board.getSize();
            
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    Point p = new Point(x, y);
                    Stone stone = board.get(p);
                    int stoneIndex = (stone == Stone.EMPTY) ? 0 : 
                                     (stone == Stone.BLACK) ? 1 : 2;
                    hash ^= TABLE[x][y][stoneIndex];
                }
            }
            
            return hash;
        }
    }

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