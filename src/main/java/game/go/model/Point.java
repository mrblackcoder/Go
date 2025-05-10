package game.go.model;

public record Point(int x, int y) {
    public boolean inBounds(int size) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }
    public Point[] neighbors() {
        return new Point[]{ up(), down(), left(), right() };
    }
    public Point up()    { return new Point(x, y - 1); }
    public Point down()  { return new Point(x, y + 1); }
    public Point left()  { return new Point(x - 1, y); }
    public Point right() { return new Point(x + 1, y); }

    /** Utility: "12,5" formatından Point üret */
    public static Point fromCsv(String csv) {
        String[] parts = csv.split(",");
        return new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
