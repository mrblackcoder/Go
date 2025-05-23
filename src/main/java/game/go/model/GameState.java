package game.go.model;

import game.go.model.Board.MoveResult;
import game.go.util.GameRecorder;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Go oyunu için durum yönetim sınıfı.
 * <p>
 * Bu sınıf, oyun tahtasının durumunu, oyuncu sırasını, puanları ve oyunun genel
 * durumunu yönetir. Ayrıca hamle geçerliliği kontrolleri, Ko kuralı uygulaması,
 * ve skor hesaplaması gibi temel oyun işlevlerini içerir.
 * </p>
 */
public class GameState {

    private static final Logger LOGGER = Logger.getLogger(GameState.class.getName());

    // Tahta ve oyuncu durumu
    private final Board board;
    private Stone currentPlayer = Stone.BLACK; // Siyah başlar
    private int consecutivePasses = 0;
    private boolean gameOver = false;
    private GameRecorder recorder;
    private double komi = 6.5; // Beyaz için varsayılan avantaj puanı
    private String gameOverReason = ""; // Oyun sonu sebebi

    // Skor hesaplaması için
    private final Set<Point> blackTerritory = new HashSet<>();
    private final Set<Point> whiteTerritory = new HashSet<>();

    // Ko kuralı için
    private long previousBoardHash = 0;

    // Skor fazı için
    private final Set<Point> markedDeadStones = new HashSet<>();

    // Esir alınan taşları takip etmek için
    private int blackCaptureCount = 0;
    private int whiteCaptureCount = 0;

    /**
     * Belirtilen boyutta yeni bir oyun durumu oluşturur.
     *
     * @param size Tahta boyutu (genellikle 9, 13 veya 19)
     */
    public GameState(int size) {
        this.board = new Board(size);
        initialize();
    }

    /**
     * Oyun durumunu başlangıç haline getirir.
     */
    private void initialize() {
        currentPlayer = Stone.BLACK;
        consecutivePasses = 0;
        gameOver = false;
        gameOverReason = "";
        blackTerritory.clear();
        whiteTerritory.clear();
        previousBoardHash = 0;
        markedDeadStones.clear();
        blackCaptureCount = 0;
        whiteCaptureCount = 0;

        LOGGER.info("GameState initialized");
    }

    /**
     * Oyun kaydediciyi ayarlar.
     *
     * @param recorder Kullanılacak oyun kaydedici
     */
    public void setRecorder(GameRecorder recorder) {
        this.recorder = recorder;
        LOGGER.info("Game recorder set: " + (recorder != null ? "Active" : "None"));
    }

    /**
     * Belirli bir noktaya taş koymayı dener.
     * <p>
     * Bu metod, belirtilen noktaya mevcut oyuncunun taşını yerleştirmeye
     * çalışır. Hamle geçerliyse, sıra diğer oyuncuya geçer. Ko kuralı ihlalleri
     * ve intihar hamleleri kontrol edilir.
     * </p>
     *
     * @param p Taşın konulacağı nokta
     * @return Hamle sonucu (geçerli olup olmadığı ve hata mesajı)
     */
public Board.MoveResult play(Point p) {
    // Oyun bitti kontrolü
    if (gameOver) {
        return new Board.MoveResult(false, "Oyun zaten bitmiş durumda");
    }

    // Geçerlilik kontrolü
    if (p == null) {
        return new Board.MoveResult(false, "Geçersiz hamle noktası: null");
    }

    if (!p.inBounds(board.getSize())) {
        return new Board.MoveResult(false, "Hamle tahta dışında");
    }

    // Ko kuralı için mevcut durumu yedekle
    Board boardBackup = board.copy();
    long currentBoardHash = Zobrist.fullHash(board);

    MoveResult result = board.placeStone(p, toPlay());
    if (!result.valid) {
        // Hamle geçersiz - sıra değişmez
        return result;
    }
    
    // KO KURALI KONTROLÜ: Tahtanın yeni durumu önceki duruma eşitse
    long newBoardHash = Zobrist.fullHash(board);
    if (newBoardHash == previousBoardHash) {
        // Hamleyi geri al ve sıra değişmez
        board.setState(boardBackup);
        return new Board.MoveResult(false, "Ko ihlali");
    }
    
    // Önceki durumu güncelle (bir sonraki hamle için)
    previousBoardHash = currentBoardHash;
    
    // Hamle geçerli, esir sayılarını güncelle
    updateCaptureCount();
    
    // Hamleyi kaydet
    if (recorder != null) {
        recorder.recordMove(p, currentPlayer);
    }
    
    // Log bilgisi
    LOGGER.log(Level.INFO, "{0} oyuncusu ({1},{2}) konumuna taş koydu. Sıradaki oyuncu: {3}",
            new Object[]{currentPlayer, p.x(), p.y(), currentPlayer.opponent()});
    
    // Hamle başarılı ise:
    // Ardışık pas sayacını sıfırla ve sırayı diğer oyuncuya ver
    consecutivePasses = 0;
    currentPlayer = currentPlayer.opponent();
    
    return new Board.MoveResult(true, "");
}
    /**
     * Esir alınan taş sayılarını günceller.
     */
    private void updateCaptureCount() {
        int newBlackCaptures = board.getCapturedBy(Stone.BLACK);
        int newWhiteCaptures = board.getCapturedBy(Stone.WHITE);

        if (newBlackCaptures > blackCaptureCount) {
            LOGGER.log(Level.INFO, "Siyah {0} beyaz taş esir aldı", (newBlackCaptures - blackCaptureCount));
        }

        if (newWhiteCaptures > whiteCaptureCount) {
            LOGGER.log(Level.INFO, "Beyaz {0} siyah taş esir aldı", (newWhiteCaptures - whiteCaptureCount));
        }

        blackCaptureCount = newBlackCaptures;
        whiteCaptureCount = newWhiteCaptures;
    }

    /**
     * Mevcut oyuncunun pas geçmesini sağlar.
     * <p>
     * İki ardışık pas, oyunun bitmesine neden olur.
     * </p>
     *
     * @return Pas hamlesinin sonucu
     */
    public Board.MoveResult pass() {
        if (gameOver) {
            return new Board.MoveResult(false, "Oyun zaten bitmiş durumda");
        }

        // Hamleyi kaydet
        if (recorder != null) {
            recorder.recordPass(currentPlayer);
        }

        LOGGER.log(Level.INFO, "{0} pas geçti. Sıradaki oyuncu: {1}",
                new Object[]{currentPlayer, currentPlayer.opponent()});

        // Pas sayacını artır ve oyun bitişini kontrol et
        consecutivePasses++;
        if (consecutivePasses >= 2) {
            gameOver = true;
            gameOverReason = "İki ardışık pas ile oyun bitti";
            LOGGER.info("Oyun sona erdi: " + gameOverReason);
        }

        // Sırayı değiştir
        currentPlayer = currentPlayer.opponent();

        return new Board.MoveResult(true, "");
    }

    /**
     * Mevcut oyuncunun istifa etmesini sağlar.
     *
     * @return İstifa sonucu
     */
    public MoveResult resign() {
        if (gameOver) {
            return new Board.MoveResult(false, "Oyun zaten bitmiş durumda");
        }
        
        // Hamleyi kaydet
        if (recorder != null) {
            recorder.recordResign(currentPlayer);
        }
        
        LOGGER.log(Level.INFO, "{0} istifa etti", currentPlayer);
        
        // Oyunu bitir
        gameOver = true;
        gameOverReason = currentPlayer.toString() + " resigned";
        
        // Hamle sonucunu döndür
        return new Board.MoveResult(true, "Player " + currentPlayer + " resigned");
    }

    /**
     * Oyun durumunu sıfırlar.
     */
    public void reset() {
        board.clear();
        initialize();
        LOGGER.info("Oyun durumu sıfırlandı");
    }

    /**
     * Belirtilen oyuncunun kontrol ettiği bölge puanını hesaplar.
     * <p>
     * Bölge puanı, oyuncunun taşları, esir aldığı taşlar ve çevrelediği boş
     * alanların toplamıdır.
     * </p>
     *
     * @param player Puan hesaplanacak oyuncu
     * @return Oyuncunun puanı
     */
    public int scoreFor(Stone player) {
        Map<Stone, Integer> scores = calculateTerritorialScores();
        return scores.getOrDefault(player, 0);
    }

    /**
     * Her iki oyuncu için bölge puanlarını hesaplar.
     *
     * @return Oyuncu-puan eşleştirmelerinin bulunduğu harita
     */
public Map<Stone, Integer> calculateTerritorialScores() {
    Map<Stone, Integer> scores = new HashMap<>();

    // Tahta durumunu ve taş sayılarını kontrol et
    int blackCount = 0;
    int whiteCount = 0;
    
    // Tahtadaki taşları say
    for (int y = 0; y < board.getSize(); y++) {
        for (int x = 0; x < board.getSize(); x++) {
            Stone stone = board.getAtCoord(x, y); // getAtCoord kullan
            if (stone == Stone.BLACK) {
                blackCount++;
            } else if (stone == Stone.WHITE) {
                whiteCount++;
            }
        }
    }

    // İlk hamle bonuslarını hesapla
    int blackScore = (blackCount > 0) ? 1 : 0;  // Siyah ilk hamlede +1
    int whiteScore = (whiteCount > 0) ? 1 : 0;  // Beyaz ilk hamlede +1

    // Normal oyun puanlarını ekle (ilk hamleler hariç)
    blackScore += blackCount > 1 ? (blackCount - 1) : 0;
    whiteScore += whiteCount > 1 ? (whiteCount - 1) : 0;

    // Esir alınan taşları ekle
    blackScore += board.getCapturedBy(Stone.BLACK);
    whiteScore += board.getCapturedBy(Stone.WHITE);

    // Bölge puanlarını sadece oyun ilerledikten sonra ekle
    if (blackCount + whiteCount > 2) { // Oyun ilerlediyse
        calculateTerritory(); // Bölgeleri hesapla
        blackScore += blackTerritory.size();
        whiteScore += whiteTerritory.size();
    }

    // Ölü işaretli taşları hesapla
    for (Point p : markedDeadStones) {
        Stone stone = board.getAtCoord(p.x(), p.y());
        if (stone == Stone.BLACK) {
            whiteScore++;
        } else if (stone == Stone.WHITE) {
            blackScore++;
        }
    }

    // Komi puanını sadece oyun bittiğinde ekle
    if (gameOver) {
        whiteScore += (int) Math.floor(komi);
    }

    // Debug log ekle
    LOGGER.log(Level.INFO, "Score calculation - Black stones: {0}, captures: {1}, score: {2}",
        new Object[]{blackCount, board.getCapturedBy(Stone.BLACK), blackScore});
    LOGGER.log(Level.INFO, "Score calculation - White stones: {0}, captures: {1}, score: {2}",
        new Object[]{whiteCount, board.getCapturedBy(Stone.WHITE), whiteScore});

    scores.put(Stone.BLACK, blackScore);
    scores.put(Stone.WHITE, whiteScore);

    return scores;
}

/**
     * Flood-fill algoritması kullanarak bölgeleri hesaplar.
     */
    private void calculateTerritory() {
        blackTerritory.clear();
        whiteTerritory.clear();

        int size = board.getSize();
        boolean[][] visited = new boolean[size][size];

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Point p = new Point(x, y);
                if (board.get(p) == Stone.EMPTY && !visited[y][x]) {
                    // Boş nokta bulundu - bölgeyi belirlemek için flood-fill yap
                    Set<Point> emptyRegion = new HashSet<>();
                    Set<Stone> surroundingColors = new HashSet<>();

                    floodFillTerritory(p, emptyRegion, surroundingColors, visited);

                    // Eğer bölge sadece tek bir renkle çevrilmişse, o oyuncunun bölgesidir
                    if (surroundingColors.size() == 1) {
                        Stone surroundingColor = surroundingColors.iterator().next();
                        if (surroundingColor == Stone.BLACK) {
                            blackTerritory.addAll(emptyRegion);
                        } else if (surroundingColor == Stone.WHITE) {
                            whiteTerritory.addAll(emptyRegion);
                        }
                    }
                }
            }
        }
    }

    /**
     * Flood-fill algoritması ile çevrili boş alanları belirler.
     *
     * @param start Başlangıç noktası
     * @param emptyRegion Boş bölge noktalarının ekleneceği küme
     * @param surroundingColors Çevreleyen renklerin ekleneceği küme
     * @param visited Ziyaret edilmiş noktaları işaretlemek için dizi
     */
    private void floodFillTerritory(Point start, Set<Point> emptyRegion, Set<Stone> surroundingColors, boolean[][] visited) {
        int size = board.getSize();
        Queue<Point> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            if (!p.inBounds(size) || visited[p.y()][p.x()]) {
                continue;
            }

            Stone stone = board.get(p);
            if (stone == Stone.EMPTY) {
                // Boş nokta - flood-fill devam ediyor
                visited[p.y()][p.x()] = true;
                emptyRegion.add(p);

                // Komşuları kontrol et
                for (Point neighbor : board.neighbors(p)) {
                    if (neighbor.inBounds(size)) {
                        Stone neighborStone = board.get(neighbor);
                        if (neighborStone == Stone.EMPTY && !visited[neighbor.y()][neighbor.x()]) {
                            queue.add(neighbor);
                        } else if (neighborStone != Stone.EMPTY) {
                            // Çevreleyen renk bulundu
                            surroundingColors.add(neighborStone);
                        }
                    }
                }
            }
        }
    }

    /**
     * Belirtilen taşı ölü/canlı olarak işaretler veya işareti kaldırır.
     * <p>
     * Oyun sonu puanlama fazında kullanılır.
     * </p>
     *
     * @param p İşaretlenecek taşın konumu
     * @return İşaretin değiştirilip değiştirilmediği
     */
    public boolean toggleDeadStone(Point p) {
        Stone stone = board.get(p);
        if (stone == Stone.EMPTY) {
            return false;
        }

        if (markedDeadStones.contains(p)) {
            markedDeadStones.remove(p);
        } else {
            markedDeadStones.add(p);
        }

        return true;
    }

    /**
     * Ölü işaretli taşların listesini döndürür.
     *
     * @return Ölü taşların bulunduğu konumların değiştirilemez kümesi
     */
    public Set<Point> getMarkedDeadStones() {
        return Collections.unmodifiableSet(markedDeadStones);
    }

    /**
     * Tüm ölü taş işaretlerini temizler.
     */
    public void resetDeadStones() {
        markedDeadStones.clear();
    }

    /**
     * İki grup taşı anında birbiriyle değiştirir.
     * <p>
     * Gelişmiş bir özellik olarak eklendi. Bazı Go varyantlarında
     * kullanılabilir.
     * </p>
     *
     * @param p1 Birinci grup için merkez nokta
     * @param p2 İkinci grup için merkez nokta
     * @return İşlemin başarılı olup olmadığı
     */
    public boolean swapGroups(Point p1, Point p2) {
        if (gameOver) {
            return false;
        }

        Stone s1 = board.get(p1);
        Stone s2 = board.get(p2);

        if (s1 == Stone.EMPTY || s2 == Stone.EMPTY || s1 == s2) {
            return false; // Değiştirilecek geçerli gruplar yok
        }

        Set<Point> group1 = board.groupOf(p1);
        Set<Point> group2 = board.groupOf(p2);

        // Taşları geçici olarak kaldır
        for (Point p : group1) {
            board.removeStone(p);
        }
        for (Point p : group2) {
            board.removeStone(p);
        }

        // Ters renkle yerleştir
        for (Point p : group1) {
            board.placeStone(p, s2);
        }
        for (Point p : group2) {
            board.placeStone(p, s1);
        }

        return true;
    }

    /**
     * Handikap taşlarını yerleştirir.
     *
     * @param handicap Yerleştirilecek taş sayısı
     * @return İşlemin başarılı olup olmadığı
     */
    public boolean placeHandicapStones(int handicap) {
        if (handicap < 2) {
            return false; // Geçersiz handikap
        }

        try {
            board.placeHandicapStones(handicap);

            // Handikap taşlarını kaydedici için kaydet
            if (recorder != null) {
                int[] hoshiPositions;
                int size = board.getSize();

                if (size == 9) {
                    hoshiPositions = new int[]{2, 6};
                } else if (size == 13) {
                    hoshiPositions = new int[]{3, 9};
                } else {
                    hoshiPositions = new int[]{3, 9, 15};
                }

                List<Point> handicapPoints = getHandicapPoints(size, handicap, hoshiPositions);
                for (Point p : handicapPoints) {
                    recorder.recordMove(p, Stone.BLACK);
                }
            }

            return true;
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Handikap taşları yerleştirilemedi: {0}", e.getMessage());
            return false;
        }
    }

    /**
     * Verilen handikap için taş konumlarını hesaplar.
     *
     * @param size Tahta boyutu
     * @param handicap Handikap sayısı
     * @param hoshiPositions Hoshi noktalarının konumları
     * @return Handikap taşlarının konumları
     */
    private List<Point> getHandicapPoints(int size, int handicap, int[] hoshiPositions) {
        List<Point> points = new ArrayList<>();

        // Köşe pozisyonları (dört köşe)
        points.add(new Point(hoshiPositions[0], hoshiPositions[0]));
        points.add(new Point(hoshiPositions[hoshiPositions.length - 1], hoshiPositions[0]));
        points.add(new Point(hoshiPositions[0], hoshiPositions[hoshiPositions.length - 1]));
        points.add(new Point(hoshiPositions[hoshiPositions.length - 1], hoshiPositions[hoshiPositions.length - 1]));

        // 5+ handikap için merkez noktasını ekle
        if (handicap >= 5 && size >= 13) {
            points.add(new Point(hoshiPositions[1], hoshiPositions[1]));
        }

        // 6+ handikap için kenar ortalarını ekle
        if (handicap >= 6 && size >= 13) {
            points.add(new Point(hoshiPositions[0], hoshiPositions[1]));
        }

        if (handicap >= 7 && size >= 13) {
            points.add(new Point(hoshiPositions[hoshiPositions.length - 1], hoshiPositions[1]));
        }

        if (handicap >= 8 && size >= 13) {
            points.add(new Point(hoshiPositions[1], hoshiPositions[0]));
        }

        if (handicap >= 9 && size >= 19) {
            points.add(new Point(hoshiPositions[1], hoshiPositions[hoshiPositions.length - 1]));
        }

        // İstenen handikap sayısı kadar döndür
        return points.subList(0, Math.min(handicap, points.size()));
    }

    // Getter ve setter metodları
    /**
     * Tahta nesnesini döndürür.
     *
     * @return Oyun tahtası
     */
    public Board board() {
        return board;
    }

    /**
     * Şu anki oyuncuyu döndürür.
     *
     * @return Sıradaki oyuncu
     */
    public Stone toPlay() {
        return currentPlayer;
    }

    /**
     * Şu anki oyuncuyu döndürür (alternatif isim).
     *
     * @return Sıradaki oyuncu
     */
    public Stone getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Oyunun bitip bitmediğini kontrol eder.
     *
     * @return Oyun bittiyse true
     */
    public boolean isOver() {
        return gameOver;
    }

    /**
     * Komi değerini ayarlar.
     *
     * @param komi Yeni komi değeri
     */
    public void setKomi(double komi) {
        this.komi = komi;
    }

    /**
     * Komi değerini döndürür.
     *
     * @return Mevcut komi değeri
     */
    public double getKomi() {
        return komi;
    }

    /**
     * Oyun sonu sebebini döndürür.
     *
     * @return Oyun bitişinin nedeni
     */
    public String getGameOverReason() {
        return gameOverReason;
    }

    /**
     * Siyah oyuncunun esir aldığı taş sayısını döndürür.
     *
     * @return Siyahın esir aldığı taş sayısı
     */
    public int getBlackCaptureCount() {
        return blackCaptureCount;
    }

    /**
     * Beyaz oyuncunun esir aldığı taş sayısını döndürür.
     *
     * @return Beyazın esir aldığı taş sayısı
     */
    public int getWhiteCaptureCount() {
        return whiteCaptureCount;
    }

    /**
     * Ardışık pas sayısını döndürür.
     *
     * @return Ardışık pas sayısı
     */
    public int getConsecutivePasses() {
        return consecutivePasses;
    }

    /**
     * Oyun durumunun string gösterimini döndürür.
     *
     * @return Oyun durumunun özeti
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GameState: ").append(board.getSize()).append("x").append(board.getSize()).append("\n");
        sb.append("CurrentPlayer: ").append(currentPlayer).append("\n");
        sb.append("GameOver: ").append(gameOver);
        if (gameOver) {
            sb.append(" (").append(gameOverReason).append(")");
        }
        sb.append("\n");
        sb.append("Komi: ").append(komi).append("\n");
        sb.append("Black Captures: ").append(blackCaptureCount).append("\n");
        sb.append("White Captures: ").append(whiteCaptureCount).append("\n");

        // Puanlar
        Map<Stone, Integer> scores = calculateTerritorialScores();
        sb.append("Scores: Black=").append(scores.get(Stone.BLACK))
                .append(", White=").append(scores.get(Stone.WHITE)).append("\n");

        return sb.toString();
    }
}