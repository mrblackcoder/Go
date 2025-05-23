package game.go.model;

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