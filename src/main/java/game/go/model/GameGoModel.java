package game.go.model;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Go oyunu için veri modeli.
 * Oyun durumunu, oyuncu bilgilerini ve skor takibini yönetir.
 */
public class GameGoModel {
    private static final Logger LOGGER = Logger.getLogger(GameGoModel.class.getName());
    
    // Oyuncu bilgileri
    private String role;
    private boolean myTurn;
    private boolean gameInProgress;
    
    // Hamle bilgileri
    private Point lastMove;
    private int consecutivePasses;
    
    // Skor bilgileri
    private int myScore;
    private int oppScore;
    
    // Tahta boyutu
    private final int boardSize;
    
    /**
     * Yeni bir oyun modeli oluşturur
     * @param boardSize Tahta boyutu (standart: 19)
     */
    public GameGoModel(int boardSize) {
        this.boardSize = boardSize;
        reset();
    }
    
    /**
     * Tüm oyun bilgilerini varsayılan değerlere sıfırlar
     */
    public void reset() {
        role = "Unknown";
        myTurn = false;
        gameInProgress = false;
        lastMove = null;
        myScore = 0;
        oppScore = 0;
        consecutivePasses = 0;
        LOGGER.info("Game model reset to initial state");
    }
    
    /**
     * Oyuncu rolünü ayarlar ve ilk sırayı belirler
     * @param role Oyuncu rolü (BLACK/WHITE)
     */
    public void setRole(String role) {
        this.role = role;
        this.myTurn = role.equalsIgnoreCase("BLACK"); // Siyah başlar
        LOGGER.log(Level.INFO, "Role set to {0}, my turn: {1}", new Object[]{role, myTurn});
    }
    
    /**
     * Skor bilgilerini günceller
     * @param myScore Oyuncunun skoru
     * @param oppScore Rakibin skoru
     */
    public void updateScores(int myScore, int oppScore) {
        this.myScore = myScore;
        this.oppScore = oppScore;
        LOGGER.log(Level.INFO, "Scores updated - My score: {0}, Opponent score: {1}", 
                   new Object[]{myScore, oppScore});
    }
    
    /**
     * Sırayı belirler ve günceller
     * @param myTurn Benim sıram ise true
     */
    public void setTurn(boolean myTurn) {
        this.myTurn = myTurn;
        LOGGER.log(Level.INFO, "Turn updated - My turn: {0}", myTurn);
    }
    
    /**
     * Son hamleyi belirler
     * @param lastMove Son hamle noktası
     */
    public void setLastMove(Point lastMove) {
        this.lastMove = lastMove;
        // Yeni hamle yapıldığında pas sayacını sıfırla
        if (lastMove != null) {
            consecutivePasses = 0;
        }
        
        LOGGER.log(Level.INFO, "Last move set to: {0}", 
                  (lastMove != null) ? lastMove.toString() : "null");
    }
    
    /**
     * Pas hamlesi yapıldığında çağrılır
     */
    public void recordPass() {
        consecutivePasses++;
        LOGGER.log(Level.INFO, "Pass recorded, consecutive passes: {0}", consecutivePasses);
        
        // İki ardışık pas oyunu bitirir
        if (consecutivePasses >= 2) {
            LOGGER.info("Two consecutive passes detected - game should end");
        }
    }
    
    /**
     * Oyunun başladığını işaretler
     */
    public void startGame() {
        gameInProgress = true;
        consecutivePasses = 0;
        LOGGER.info("Game marked as in progress");
    }
    
    /**
     * Oyunun bittiğini işaretler
     */
    public void endGame() {
        gameInProgress = false;
        LOGGER.info("Game marked as ended");
    }
    
    // Getter metotları
    
    public String getRole() {
        return role;
    }
    
    public boolean isMyTurn() {
        return myTurn;
    }
    
    public boolean isGameInProgress() {
        return gameInProgress;
    }
    
    public Point getLastMove() {
        return lastMove;
    }
    
    public int getMyScore() {
        return myScore;
    }
    
    public int getOppScore() {
        return oppScore;
    }
    
    public int getBoardSize() {
        return boardSize;
    }
    
    public int getConsecutivePasses() {
        return consecutivePasses;
    }
    
    public boolean hasGameEndedByPasses() {
        return consecutivePasses >= 2;
    }
    
    /**
     * Şu anki oyuncu taşını döndürür
     * @return Siyah veya beyaz taş
     */
    public Stone getMyStone() {
        return role.equalsIgnoreCase("BLACK") ? Stone.BLACK : Stone.WHITE;
    }
    
    /**
     * Rakip taşını döndürür
     * @return Siyah veya beyaz taş
     */
    public Stone getOpponentStone() {
        return role.equalsIgnoreCase("BLACK") ? Stone.WHITE : Stone.BLACK;
    }
    
    /**
     * Sıradaki oyuncunun taşını döndürür
     * @return Sıradaki taş rengi
     */
    public Stone getCurrentTurnStone() {
        if (myTurn) {
            return getMyStone();
        } else {
            return getOpponentStone();
        }
    }
    
    @Override
    public String toString() {
        return "GameGoModel{" + 
               "role='" + role + '\'' + 
               ", myTurn=" + myTurn + 
               ", gameInProgress=" + gameInProgress + 
               ", myScore=" + myScore + 
               ", oppScore=" + oppScore + 
               ", boardSize=" + boardSize + 
               '}';
    }
}