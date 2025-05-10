package game.go.util;

import game.go.model.Point;
import game.go.model.Stone;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Records and saves game moves in SGF format.
 * Fixed to properly record all game moves regardless of timing.
 */
public class GameRecorder {
    
    /**
     * Represents a single move in the game
     */
    public static class Move {
        private final Point point;
        private final boolean isPass;
        private final boolean isResign;
        private final Stone player;
        private final long timestamp;
        
        /**
         * Creates a new move at the specified point
         * @param point The position of the move
         * @param player The player making the move
         */
        public Move(Point point, Stone player) {
            this.point = point;
            this.isPass = false;
            this.isResign = false;
            this.player = player;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * Creates a new pass or resign move
         * @param isPass True if this is a pass move
         * @param isResign True if this is a resignation
         * @param player The player making the move
         */
        public Move(boolean isPass, boolean isResign, Stone player) {
            this.point = null;
            this.isPass = isPass;
            this.isResign = isResign;
            this.player = player;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * Converts the move to SGF format
         * @return SGF formatted move
         */
        public String toSgf() {
            if (isResign) {
                return player == Stone.BLACK ? "B[resign]" : "W[resign]";
            } else if (isPass) {
                return player == Stone.BLACK ? "B[]" : "W[]";
            } else if (point != null) {
                char x = (char)('a' + point.x());
                char y = (char)('a' + point.y());
                return player == Stone.BLACK ? "B[" + x + y + "]" : "W[" + x + y + "]";
            } else {
                return ""; // Invalid move, return empty string
            }
        }
        
        // Getters
        public Point getPoint() {
            return point;
        }
        
        public boolean isPass() {
            return isPass;
        }
        
        public boolean isResign() {
            return isResign;
        }
        
        public Stone getPlayer() {
            return player;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            if (isResign) {
                return player + " resigned";
            } else if (isPass) {
                return player + " passed";
            } else if (point != null) {
                return player + " played at (" + point.x() + "," + point.y() + ")";
            } else {
                return "Invalid move";
            }
        }
    }
    
    private final List<Move> moves;
    private final int boardSize;
    private final String blackPlayer;
    private final String whitePlayer;
    private final Date gameDate;
    private double komi = 6.5;
    private boolean recording = true;
    
    /**
     * Creates a new game recorder
     * @param boardSize Size of the game board
     * @param blackPlayer Name of the black player
     * @param whitePlayer Name of the white player
     */
    public GameRecorder(int boardSize, String blackPlayer, String whitePlayer) {
        this.boardSize = boardSize;
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
        this.gameDate = new Date();
        this.moves = new ArrayList<>();
    }
    
    /**
     * Creates a new game recorder with specified date
     * @param boardSize Size of the game board
     * @param blackPlayer Name of the black player
     * @param whitePlayer Name of the white player
     * @param gameDate Date of the game
     */
    public GameRecorder(int boardSize, String blackPlayer, String whitePlayer, Date gameDate) {
        this.boardSize = boardSize;
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
        this.gameDate = gameDate != null ? gameDate : new Date();
        this.moves = new ArrayList<>();
    }
    
    /**
     * Records a move at the specified point
     * @param p The position of the move
     * @param player The player making the move
     */
    public void recordMove(Point p, Stone player) {
        if (!recording) return;
        
        if (p == null || player == null) {
            System.err.println("Warning: Attempted to record move with null point or player");
            return;
        }
        
        System.out.println("Recording move: " + player + " at (" + p.x() + "," + p.y() + ")");
        
        Move move = new Move(p, player);
        moves.add(move);
        
        // Debug: Print move count after recording
        System.out.println("Total moves recorded: " + moves.size());
    }
    
    /**
     * Records a pass move
     * @param player The player passing
     */
    public void recordPass(Stone player) {
        if (!recording) return;
        
        if (player == null) {
            System.err.println("Warning: Attempted to record pass with null player");
            return;
        }
        
        System.out.println("Recording pass by " + player);
        
        Move move = new Move(true, false, player);
        moves.add(move);
        
        // Debug: Print move count after recording
        System.out.println("Total moves recorded: " + moves.size());
    }
    
    /**
     * Records a resignation
     * @param player The player resigning
     */
    public void recordResign(Stone player) {
        if (!recording) return;
        
        if (player == null) {
            System.err.println("Warning: Attempted to record resignation with null player");
            return;
        }
        
        System.out.println("Recording resignation by " + player);
        
        Move move = new Move(false, true, player);
        moves.add(move);
        
        // Debug: Print move count after recording
        System.out.println("Total moves recorded: " + moves.size());
    }
    
    /**
     * Sets the komi value
     * @param komi The komi value
     */
    public void setKomi(double komi) {
        this.komi = komi;
    }
    
    /**
     * Enables or disables recording
     * @param enable True to enable recording, false to disable
     */
    public void enableRecording(boolean enable) {
        this.recording = enable;
        System.out.println("Recording " + (enable ? "enabled" : "disabled"));
    }
    
    /**
     * Checks if recording is enabled
     * @return True if recording is enabled
     */
    public boolean isRecording() {
        return recording;
    }
    
    /**
     * Saves the game record in SGF format
     * @param filePath Path to save the SGF file
     * @throws IOException If file operations fail
     */
    public void saveToSgf(String filePath) throws IOException {
        if (moves.isEmpty()) {
            throw new IOException("No moves to save");
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // SGF format header
            writer.write("(;FF[4]GM[1]SZ[" + boardSize + "]");
            writer.newLine();
            
            // Player information
            writer.write("PB[" + blackPlayer + "]PW[" + whitePlayer + "]");
            writer.newLine();
            
            // Date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            writer.write("DT[" + sdf.format(gameDate) + "]");
            writer.newLine();
            
            // Komi
            writer.write("KM[" + komi + "]");
            writer.newLine();
            
            // Application info
            writer.write("AP[Go Game:1.0]");
            writer.newLine();
            
            // Moves
            for (Move move : moves) {
                String sgfMove = move.toSgf();
                if (!sgfMove.isEmpty()) {
                    writer.write(sgfMove);
                    writer.newLine();
                }
            }
            
            // End SGF
            writer.write(")");
        }
        
        System.out.println("Successfully saved " + moves.size() + " moves to " + filePath);
    }
    
    /**
     * Returns the list of moves
     * @return List of recorded moves
     */
    public List<Move> getMoves() {
        // Return a deep copy to prevent modification from outside
        List<Move> movesCopy = new ArrayList<>(moves.size());
        movesCopy.addAll(moves);
        System.out.println("Returning " + movesCopy.size() + " moves");
        return movesCopy;
    }
    
    /**
     * Returns the total number of moves
     * @return Number of recorded moves
     */
    public int getMoveCount() {
        return moves.size();
    }
    
    /**
     * Returns the move at the specified index
     * @param index Index of the move
     * @return The move at the specified index, or null if index is out of bounds
     */
    public Move getMove(int index) {
        if (index >= 0 && index < moves.size()) {
            return moves.get(index);
        }
        return null;
    }
    
    /**
     * Prints all recorded moves to the console (for debugging)
     */
    public void printMoves() {
        System.out.println("=== Recorded Moves ===");
        System.out.println("Total moves: " + moves.size());
        for (int i = 0; i < moves.size(); i++) {
            System.out.println(i + ": " + moves.get(i));
        }
        System.out.println("=====================");
    }
    
    /**
     * Returns the board size
     * @return Board size
     */
    public int getBoardSize() {
        return boardSize;
    }
    
    /**
     * Returns the black player's name
     * @return Black player's name
     */
    public String getBlackPlayer() {
        return blackPlayer;
    }
    
    /**
     * Returns the white player's name
     * @return White player's name
     */
    public String getWhitePlayer() {
        return whitePlayer;
    }
    
    /**
     * Returns the game date
     * @return Game date
     */
    public Date getGameDate() {
        return gameDate;
    }
    
    /**
     * Returns the komi value
     * @return Komi value
     */
    public double getKomi() {
        return komi;
    }
    
    /**
     * Loads a game record from an SGF file
     * @param filePath Path to the SGF file
     * @return GameRecorder with the loaded game
     * @throws IOException If file reading fails
     * @throws ParseException If SGF parsing fails
     */
    public static GameRecorder loadFromSgf(String filePath) throws IOException, ParseException {
        return SGFLoader.loadFromFile(filePath);
    }
    
    /**
     * Clears all moves
     */
    public void clearMoves() {
        moves.clear();
        System.out.println("Cleared all recorded moves");
    }
    
    /**
     * Adds a comment to the last move
     * @param comment Comment to add
     */
    public void addComment(String comment) {
        // This is a stub for future implementation
        // SGF supports comments but we don't store them in this implementation
    }
}