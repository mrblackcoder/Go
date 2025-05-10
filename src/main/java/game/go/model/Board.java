package game.go.model;

import java.util.*;

/**
 * Go tahtası: grup-esir alma, özgürlük kontrolü, kopyalama, rollback
 */
public class Board {

    private final int size;
    private final Stone[][] grid;
    private int blackCaptured = 0, whiteCaptured = 0;

    public Board(int size) {
        this.size = size;
        this.grid = new Stone[size][size];
        for (var row : grid) {
            Arrays.fill(row, Stone.EMPTY);
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
    }

    /**
     * İstenilen konumdaki taşı kaldırır.
     * GameState'de ölü taşları işaretlemek için kullanılır.
     */
    public void removeStone(Point p) {
        if (p.inBounds(size)) {
            grid[p.x()][p.y()] = Stone.EMPTY;
        }
    }

    /**
     * Taşı koyar: doluluk, grup-esir alma, suicide kontrolü.
     */
    public MoveResult placeStone(Point p, Stone color) {
        if (!p.inBounds(size)) {
            return new MoveResult(false, "Hamle tahta dışında");
        }
        if (grid[p.x()][p.y()] != Stone.EMPTY) {
            return new MoveResult(false, "Hücre dolu");
        }

        grid[p.x()][p.y()] = color;
        Stone opp = color.opponent();

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

        // 2) Suicide kontrolü (eğer rakip taş esirlenmediyse ve kendi grubunun özgürlüğü yoksa)
        if (toRemove.isEmpty()) {
            Set<Point> myGrp = groupOf(p);
            if (!hasLiberty(myGrp)) {
                grid[p.x()][p.y()] = Stone.EMPTY;
                return new MoveResult(false, "Suicide hamle");
            }
        }

        // 3) Esir al
        for (Point dead : toRemove) {
            grid[dead.x()][dead.y()] = Stone.EMPTY;
            if (color == Stone.BLACK) {
                blackCaptured++;
            } else {
                whiteCaptured++;
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
                    return true;
                }
            }
        }
        return false;
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
        return grid[p.x()][p.y()];
    }

    public Stone getAtCoord(int x, int y) {
        if (x >= 0 && x < size && y >= 0 && y < size) {
            return grid[x][y];
        }
        return null;
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