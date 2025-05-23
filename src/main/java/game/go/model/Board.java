package game.go.model;

import java.util.*;

/**
 * Go tahtası: grup-esir alma, özgürlük kontrolü, kopyalama, rollback
 */
public class Board {

    private final int size;
    private final Stone[][] grid;
    private int blackCaptured = 0, whiteCaptured = 0;
    private final Set<Point> lastCaptured = new HashSet<>();

    public Board(int size) {
        this.size = size;
        this.grid = new Stone[size][size];
        for (var row : grid) {
            Arrays.fill(row, Stone.EMPTY);
        }
    }

    /**
     * Belirli bir rengin taşlarını sayar
     *
     * @param color Sayılacak taş rengi
     * @return Bulunan taş sayısı
     */
    public int countStones(Stone color) {
        int count = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (grid[x][y] == color) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Tahtayı boş bir duruma sıfırlar
     */
    public void clear() {
        for (Stone[] row : grid) {
            Arrays.fill(row, Stone.EMPTY);
        }
        blackCaptured = 0;
        whiteCaptured = 0;
        lastCaptured.clear();
    }

    /**
     * Tahtanın string temsilini döndürür (debug için)
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Board [").append(size).append("x").append(size).append("]\n");

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Stone s = grid[x][y];
                sb.append(s == Stone.BLACK ? "B" : (s == Stone.WHITE ? "W" : "."));
            }
            sb.append("\n");
        }

        sb.append("Black captured: ").append(blackCaptured).append("\n");
        sb.append("White captured: ").append(whiteCaptured);

        return sb.toString();
    }

    /**
     * Handicap taşlarını yerleştirir
     *
     * @param handicap Yerleştirilecek taş sayısı (2-9)
     */
    public void placeHandicapStones(int handicap) {
        if (handicap < 2 || handicap > 9) {
            throw new IllegalArgumentException("Handicap must be between 2 and 9");
        }

        // Handicap taşlarının konumları (9x9, 13x13 ve 19x19 tahta boyutları için)
        int[] hoshiPositions;

        if (size == 9) {
            hoshiPositions = new int[]{2, 6};
        } else if (size == 13) {
            hoshiPositions = new int[]{3, 9};
        } else if (size == 19) {
            hoshiPositions = new int[]{3, 9, 15};
        } else {
            throw new IllegalArgumentException("Unsupported board size for handicap: " + size);
        }

        // Handicap pozisyonlarını yerleştir
        List<Point> handicapPoints = new ArrayList<>();

        // Köşe pozisyonları (4 köşe)
        handicapPoints.add(new Point(hoshiPositions[0], hoshiPositions[0]));
        handicapPoints.add(new Point(hoshiPositions[hoshiPositions.length - 1], hoshiPositions[0]));
        handicapPoints.add(new Point(hoshiPositions[0], hoshiPositions[hoshiPositions.length - 1]));
        handicapPoints.add(new Point(hoshiPositions[hoshiPositions.length - 1], hoshiPositions[hoshiPositions.length - 1]));

        // 5+ handicap için merkez noktaları ekle
        if (handicap >= 5 && size >= 13) {
            handicapPoints.add(new Point(hoshiPositions[1], hoshiPositions[1])); // Orta nokta
        }

        // 6+ handicap için kenar ortalarını ekle
        if (handicap >= 6 && size >= 13) {
            handicapPoints.add(new Point(hoshiPositions[0], hoshiPositions[1])); // Sol orta
        }

        if (handicap >= 7 && size >= 13) {
            handicapPoints.add(new Point(hoshiPositions[hoshiPositions.length - 1], hoshiPositions[1])); // Sağ orta
        }

        if (handicap >= 8 && size >= 13) {
            handicapPoints.add(new Point(hoshiPositions[1], hoshiPositions[0])); // Üst orta
        }

        if (handicap >= 9 && size >= 19) {
            handicapPoints.add(new Point(hoshiPositions[1], hoshiPositions[hoshiPositions.length - 1])); // Alt orta
        }

        // İstenen kadar handicap taşı yerleştir
        for (int i = 0; i < handicap && i < handicapPoints.size(); i++) {
            Point p = handicapPoints.get(i);
            grid[p.x()][p.y()] = Stone.BLACK;
        }
    }

    /**
     * Verilen rengin esir sayısını ayarlar
     *
     * @param color Esir sayısı ayarlanacak renk
     * @param count Yeni esir sayısı
     */
    public void setCapturedCount(Stone color, int count) {
        if (color == Stone.BLACK) {
            blackCaptured = count;
        } else if (color == Stone.WHITE) {
            whiteCaptured = count;
        }
    }

    /**
     * Son hamleyi geri alır (basit implementasyon - Ko kuralı kontrolü yok)
     *
     * @param lastMovePoint Son hamle noktası
     * @param lastMoveColor Son hamleyi yapan renk
     * @param capturedPoints Esir alınan taşların noktaları
     */
    public void undoMove(Point lastMovePoint, Stone lastMoveColor, List<Point> capturedPoints) {
        // Son hamleyi geri al
        if (lastMovePoint.inBounds(size)) {
            grid[lastMovePoint.x()][lastMovePoint.y()] = Stone.EMPTY;
        }

        // Esir alınan taşları geri koy
        Stone capturedColor = lastMoveColor.opponent();
        for (Point p : capturedPoints) {
            if (p.inBounds(size)) {
                grid[p.x()][p.y()] = capturedColor;
            }
        }

        // Esir sayısını güncelle
        if (lastMoveColor == Stone.BLACK) {
            blackCaptured -= capturedPoints.size();
            if (blackCaptured < 0) {
                blackCaptured = 0;
            }
        } else {
            whiteCaptured -= capturedPoints.size();
            if (whiteCaptured < 0) {
                whiteCaptured = 0;
            }
        }
    }

    /**
     * Derin kopya üretir.
     */
    public Board copy() {
        Board b2 = new Board(size);
        for (int x = 0; x < size; x++) {
            System.arraycopy(this.grid[x], 0, b2.grid[x], 0, size);
        }
        b2.blackCaptured = this.blackCaptured;
        b2.whiteCaptured = this.whiteCaptured;
        return b2;
    }

    /**
     * Bu nesnenin durumunu b'ye eşitler.
     */
    public void setState(Board b) {
        if (b.size != this.size) {
            throw new IllegalArgumentException("Boyut uyuşmuyor");
        }
        for (int x = 0; x < size; x++) {
            System.arraycopy(b.grid[x], 0, this.grid[x], 0, size);
        }
        this.blackCaptured = b.blackCaptured;
        this.whiteCaptured = b.whiteCaptured;
        this.lastCaptured.clear();
    }

    /**
     * İstenilen konumdaki taşı kaldırır. GameState'de ölü taşları işaretlemek
     * için kullanılır.
     */
    public void removeStone(Point p) {
        if (p.inBounds(size)) {
            grid[p.x()][p.y()] = Stone.EMPTY;
        }
    }

    /**
     * Son esir alınan taşların konumlarını döndürür
     * 
     * @return Son esir alınan taşların konumları
     */
    public Set<Point> getLastCaptured() {
        return Collections.unmodifiableSet(lastCaptured);
    }

    /**
     * Taşı koyar: doluluk, grup-esir alma, suicide kontrolü.
     */
public MoveResult placeStone(Point p, Stone color) {
    if (!p.inBounds(size)) {
        // Tahta dışı - sıra değişmez
        return new MoveResult(false, "Hamle tahta dışında");
    }
    if (grid[p.x()][p.y()] != Stone.EMPTY) {
        // Pozisyon dolu - sıra değişmez
        return new MoveResult(false, "Bu pozisyonda zaten bir taş var");
    }

    // Taşı yerleştir
    grid[p.x()][p.y()] = color;
    Stone opp = color.opponent();

    // Son esir listesini temizle
    lastCaptured.clear();
    
    // 1) Komşu rakip grupları topla, esir al
    List<Point> toRemove = new ArrayList<>();
    for (Point n : neighbors(p)) {
        if (n.inBounds(size) && grid[n.x()][n.y()] == opp) {
            Set<Point> grp = groupOf(n);
            if (!hasLiberty(grp)) {
                toRemove.addAll(grp);
            }
        }
    }

    // 2) Eğer rakip taş yakalanmadıysa, hamle intihar mı kontrol et
    if (toRemove.isEmpty()) {
        // Rakip taş yakalanmadı, intihar hamlesi kontrolü
        Set<Point> myGroup = groupOf(p);
        if (!hasLiberty(myGroup)) {
            // İntihar hamlesi - taşı geri al ve sıra değişmez
            grid[p.x()][p.y()] = Stone.EMPTY;
            return new MoveResult(false, "İntihar hamlesi yapılamaz");
        }
    }

    // 3) Esir al
    for (Point dead : toRemove) {
        if (grid[dead.x()][dead.y()] != Stone.EMPTY) {
            // Son esir konumlarını kaydet
            lastCaptured.add(new Point(dead.x(), dead.y()));
            
            // Taşı kaldır
            grid[dead.x()][dead.y()] = Stone.EMPTY;
            
            // Esir sayısını arttır
            if (color == Stone.BLACK) {
                blackCaptured++; // Siyah oyuncu beyaz taş esir aldı
            } else {
                whiteCaptured++; // Beyaz oyuncu siyah taş esir aldı
            }
        }
    }

    return new MoveResult(true, "");
}
    /**
     * Bir grup taşı BFS ile toplar.
     */
    public Set<Point> groupOf(Point start) {
        Stone col = grid[start.x()][start.y()];
        if (col == Stone.EMPTY) {
            return Collections.emptySet();
        }

        Set<Point> seen = new LinkedHashSet<>();
        Deque<Point> dq = new ArrayDeque<>();
        dq.add(start);
        while (!dq.isEmpty()) {
            Point cur = dq.remove();
            if (!seen.add(cur)) {
                continue;
            }
            for (Point n : neighbors(cur)) {
                if (n.inBounds(size) && grid[n.x()][n.y()] == col) {
                    dq.add(n);
                }
            }
        }
        return seen;
    }

    /**
     * Bir grup için herhangi bir boşluk (liberty) var mı?
     */
    public boolean hasLiberty(Set<Point> grp) {
        for (Point p : grp) {
            for (Point n : neighbors(p)) {
                if (n.inBounds(size) && grid[n.x()][n.y()] == Stone.EMPTY) {
                    return true; // En az bir özgürlük bulundu
                }
            }
        }
        return false; // Hiç özgürlük bulunamadı
    }

    /**
     * Verilen noktanın komşularını döndürür (kuzey, güney, doğu, batı)
     */
    public List<Point> neighbors(Point p) {
        return List.of(
                new Point(p.x() - 1, p.y()),
                new Point(p.x() + 1, p.y()),
                new Point(p.x(), p.y() - 1),
                new Point(p.x(), p.y() + 1)
        );
    }

    public int getSize() {
        return size;
    }

    public Stone get(Point p) {
        if (p == null || !p.inBounds(size)) {
            return null; // Tahta dışı
        }
        return grid[p.x()][p.y()];
    }

    public Stone getAtCoord(int x, int y) {
        if (x < 0 || x >= size || y < 0 || y >= size) {
            return null; // Tahta dışı
        }
        return grid[x][y];
    }

    public int getCapturedBy(Stone color) {
        return color == Stone.BLACK ? blackCaptured : whiteCaptured;
    }

    public int areaControlledBy(Stone color) {
        int cnt = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (grid[x][y] == color) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    /**
     * Tahta durumunu 2D grid olarak döndürür
     */
    public char[][] getGridAsCharArray() {
        char[][] result = new char[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Stone stone = grid[x][y];
                if (stone == Stone.BLACK) {
                    result[y][x] = 'B';
                } else if (stone == Stone.WHITE) {
                    result[y][x] = 'W';
                } else {
                    result[y][x] = '.';
                }
            }
        }
        return result;
    }

 
    
    public static class MoveResult {

        public final boolean valid;
        public final String message;

        public MoveResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }
}