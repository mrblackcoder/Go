
// --- File: game/go/model/Stone.java ---
package game.go.model;

/**
 * Taş tipi: BOŞ, SİYAH veya BEYAZ.
 */
public enum Stone {
    EMPTY, BLACK, WHITE;

    public Stone opponent() {
        return switch (this) {
            case BLACK -> WHITE;
            case WHITE -> BLACK;
            default -> EMPTY;
        };
    }
}