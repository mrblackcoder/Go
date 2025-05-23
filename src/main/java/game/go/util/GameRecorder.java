package game.go.util;

import game.go.model.Point;
import game.go.model.Stone;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Go oyunu hamlelerini kaydeden basitleştirilmiş sınıf.
 * Sadece oyun kaydetme özelliklerini içerir, yükleme özellikleri kaldırılmıştır.
 */
public class GameRecorder {
    
    private static final Logger LOGGER = Logger.getLogger(GameRecorder.class.getName());
    
    /**
     * Hamle bilgisini temsil eden iç sınıf
     */
    public static class Move implements Serializable {
        private final Point point;
        private final boolean isPass;
        private final boolean isResign;
        private final Stone player;
        private final long timestamp;
        
        /**
         * Normal hamle oluşturur
         */
        public Move(Point point, Stone player) {
            if (point == null) {
                throw new IllegalArgumentException("Point cannot be null for a regular move");
            }
            if (player == null) {
                throw new IllegalArgumentException("Player cannot be null");
            }
            this.point = point;
            this.isPass = false;
            this.isResign = false;
            this.player = player;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * Pas veya istifa hamlesi oluşturur
         */
        public Move(boolean isPass, boolean isResign, Stone player) {
            if (player == null) {
                throw new IllegalArgumentException("Player cannot be null");
            }
            if (isPass && isResign) {
                throw new IllegalArgumentException("A move cannot be both pass and resign");
            }
            this.point = null;
            this.isPass = isPass;
            this.isResign = isResign;
            this.player = player;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * Hamleyi SGF formatına dönüştürür
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
            }
            return ""; 
        }
        
        // Getters
        public Point getPoint() { return point; }
        public boolean isPass() { return isPass; }
        public boolean isResign() { return isResign; }
        public Stone getPlayer() { return player; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            if (isResign) {
                return player + " resigned";
            } else if (isPass) {
                return player + " passed";
            } else if (point != null) {
                return player + " played at (" + point.x() + "," + point.y() + ")";
            }
            return "Invalid move"; 
        }
    }
    
    // Hamleleri içeren liste
    private final List<Move> moves = new ArrayList<>();
    
    // Oyun bilgileri
    private final int boardSize;
    private final String blackPlayer;
    private final String whitePlayer;
    private final Date gameDate;
    private double komi = 6.5;
    
    // Durum değişkenleri
    private boolean recording = true;
    private boolean gameFinished = false;
    
    /**
     * Yeni bir oyun kaydedici oluşturur
     */
    public GameRecorder(int boardSize, String blackPlayer, String whitePlayer) {
        if (boardSize < 1) {
            throw new IllegalArgumentException("Board size must be positive");
        }
        this.boardSize = boardSize;
        this.blackPlayer = blackPlayer != null ? blackPlayer : "Black";
        this.whitePlayer = whitePlayer != null ? whitePlayer : "White";
        this.gameDate = new Date();
        
        LOGGER.log(Level.INFO, "GameRecorder: Created new recorder with board size {0}", boardSize);
    }
    
    /**
     * Belirtilen noktada bir hamleyi kaydeder
     */
    public boolean recordMove(Point p, Stone player) {
        if (!recording || gameFinished) {
            LOGGER.warning("GameRecorder: Recording disabled or game finished, move not recorded");
            return false;
        }
        
        if (p == null || player == null) {
            LOGGER.warning("GameRecorder: Cannot record move with null point or player");
            return false;
        }
        
        try {
            Move newMove = new Move(p, player);
            moves.add(newMove);
            LOGGER.log(Level.INFO, "GameRecorder: Recorded move: {0} at ({1},{2}). Total moves: {3}", 
                      new Object[]{player, p.x(), p.y(), moves.size()});
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "GameRecorder: Error recording move", e);
            return false;
        }
    }
    
    /**
     * Pas hamlesini kaydeder
     */
    public boolean recordPass(Stone player) {
        if (!recording || gameFinished) {
            LOGGER.warning("GameRecorder: Recording disabled or game finished, pass not recorded");
            return false;
        }
        
        if (player == null) {
            LOGGER.warning("GameRecorder: Cannot record pass with null player");
            return false;
        }
        
        try {
            Move newMove = new Move(true, false, player);
            moves.add(newMove);
            LOGGER.log(Level.INFO, "GameRecorder: Recorded pass by {0}. Total moves: {1}", 
                      new Object[]{player, moves.size()});
            
            // İki oyuncu üst üste pas geçtiyse oyunu bitmiş olarak işaretle
            checkConsecutivePasses();
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "GameRecorder: Error recording pass", e);
            return false;
        }
    }
    
    /**
     * İstifa hamlesini kaydeder
     */
    public boolean recordResign(Stone player) {
        if (!recording || gameFinished) {
            LOGGER.warning("GameRecorder: Recording disabled or game finished, resignation not recorded");
            return false;
        }
        
        if (player == null) {
            LOGGER.warning("GameRecorder: Cannot record resignation with null player");
            return false;
        }
        
        try {
            Move newMove = new Move(false, true, player);
            moves.add(newMove);
            gameFinished = true;
            LOGGER.log(Level.INFO, "GameRecorder: Recorded resignation by {0}. Total moves: {1}. Game marked as finished.", 
                      new Object[]{player, moves.size()});
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "GameRecorder: Error recording resignation", e);
            return false;
        }
    }
    
    /**
     * Son iki hamlenin pas olup olmadığını kontrol eder
     */
    private void checkConsecutivePasses() {
        if (moves.size() >= 2) {
            Move lastMove = moves.get(moves.size() - 1);
            Move secondLastMove = moves.get(moves.size() - 2);
            
            if (lastMove.isPass() && secondLastMove.isPass()) {
                LOGGER.info("GameRecorder: Two consecutive passes detected, marking game as finished");
                gameFinished = true;
            }
        }
    }
    
    /**
     * Hamlelerin doğru kaydedildiğini doğrular
     */
    public void verifyRecordedMoves() {
        LOGGER.info("======= MOVE VERIFICATION =======");
        LOGGER.log(Level.INFO, "Total recorded moves: {0}", moves.size());
        
        // Oyun bilgilerini göster
        LOGGER.log(Level.INFO, "Game details: {0} (Black) vs {1} (White)", 
                  new Object[]{blackPlayer, whitePlayer});
        LOGGER.log(Level.INFO, "Board size: {0}x{0}, Komi: {1}", 
                  new Object[]{boardSize, komi});
        
        // Her hamleyi yazdır
        for (int i = 0; i < moves.size(); i++) {
            LOGGER.log(Level.INFO, "Move {0}: {1}", new Object[]{i + 1, moves.get(i)});
        }
        
        LOGGER.info("================================");
    }
    
    /**
     * Oyun kaydını SGF formatında kaydeder
     */
    /**
 * Oyun kaydını SGF formatında kaydeder
 * 
 * @param filePath Kaydedilecek dosya yolu
 * @return İşlem başarılı ise true, başarısız ise false
 */
public boolean saveToSgf(String filePath) {
    if (moves.isEmpty()) {
        LOGGER.warning("GameRecorder: No moves to save");
        return false;
    }
    
    File file = new File(filePath);
    File parentDir = file.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
        if (!parentDir.mkdirs()) {
            LOGGER.warning("GameRecorder: Failed to create directories for: " + filePath);
            return false;
        }
    }
    
    try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
        
        // SGF format başlığı
        writer.write("(;FF[4]GM[1]SZ[" + boardSize + "]");
        writer.newLine();
        
        // Oyuncu bilgileri
        writer.write("PB[" + blackPlayer + "]PW[" + whitePlayer + "]");
        writer.newLine();
        
        // Tarih
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        writer.write("DT[" + sdf.format(gameDate) + "]");
        writer.newLine();
        
        // Komi
        writer.write("KM[" + komi + "]");
        writer.newLine();
        
        // Uygulama bilgisi
        writer.write("AP[Go Game:2.0]");
        writer.newLine();
        
        // Hamleleri yaz
        int writtenMoves = 0;
        for (Move move : moves) {
            String sgfMove = move.toSgf();
            if (!sgfMove.isEmpty()) {
                writer.write(sgfMove);
                writer.newLine();
                writtenMoves++;
            }
        }
        
        // SGF sonu
        writer.write(")");
        
        LOGGER.log(Level.INFO, "GameRecorder: Successfully saved {0} moves to {1}", 
                  new Object[]{writtenMoves, filePath});
        return true;
    } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "GameRecorder: Error saving SGF file: {0}", e.getMessage());
        return false;
    }
}
    
    /**
     * Tüm kayıtlı hamleleri yazdırır
     */
    public void printMoves() {
        System.out.println("=== GameRecorder: Recorded Moves ===");
        System.out.println("Total moves: " + moves.size());
        
        for (int i = 0; i < moves.size(); i++) {
            System.out.println((i + 1) + ": " + moves.get(i));
        }
        
        System.out.println("================================");
    }
    
    // Getter ve setter metotları
    
    public List<Move> getMoves() {
        return Collections.unmodifiableList(new ArrayList<>(moves));
    }
    
    public int getMoveCount() {
        return moves.size();
    }
    
    public Move getMove(int index) {
        if (index >= 0 && index < moves.size()) {
            return moves.get(index);
        }
        return null;
    }
    
    public void setKomi(double komi) {
        this.komi = komi;
    }
    
    public void enableRecording(boolean enable) {
        this.recording = enable;
        LOGGER.log(Level.INFO, "GameRecorder: Recording {0}", enable ? "enabled" : "disabled");
    }
    
    public void markGameFinished() {
        this.gameFinished = true;
        LOGGER.info("GameRecorder: Game marked as finished");
    }
    
    public int getBoardSize() {
        return boardSize;
    }
    
    public String getBlackPlayer() {
        return blackPlayer;
    }
    
    public String getWhitePlayer() {
        return whitePlayer;
    }
    
    public Date getGameDate() {
        return new Date(gameDate.getTime());
    }
    
    public double getKomi() {
        return komi;
    }
    
    public boolean isRecording() {
        return recording;
    }
    
    public boolean isGameFinished() {
        return gameFinished;
    }
}