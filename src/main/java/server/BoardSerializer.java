package server;

import game.go.model.Board;
import game.go.model.Point;
import game.go.model.Stone;
import java.util.StringJoiner;

public class BoardSerializer {

    /** Tahtayı JSON dizisine çevirir (".", "B", "W") */
    public static String toJson(Board b) {
        int N = b.getSize();
        StringBuilder sb = new StringBuilder("[");
        for (int y = 0; y < N; y++) {
            StringJoiner row = new StringJoiner(",", "[", "]");
            for (int x = 0; x < N; x++) {
                Stone s = b.get(new Point(x, y));
                row.add("\"" + (s == Stone.BLACK ? "B"
                               : s == Stone.WHITE ? "W" : ".") + "\"");
            }
            sb.append(row);
            if (y < N - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }
}
