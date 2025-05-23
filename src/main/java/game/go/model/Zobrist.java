// --- File: src/main/java/game/go/model/Zobrist.java ---
package game.go.model;

import java.util.Random;

public final class Zobrist {

    private static final int MAX = 25;
    private static final long[][][] TABLE = new long[MAX][MAX][3];

    static {
        Random rnd = new Random(123456L);
        for (int y = 0; y < MAX; y++)
            for (int x = 0; x < MAX; x++)
                for (int k = 0; k < 3; k++)
                    TABLE[y][x][k] = rnd.nextLong();
    }

    public static long fullHash(Board b) {
        long h = 0;
        int N = b.getSize();
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                int idx = switch (b.get(new Point(x, y))) {
                    case EMPTY -> 0;
                    case BLACK -> 1;
                    case WHITE -> 2;
                };
                h ^= TABLE[y][x][idx];
            }
        return h;
    }

    private Zobrist() {}   // util class
}
