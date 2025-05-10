// --- File: game/go/model/BoardTest.java ---
package game.go.model;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {

    @Test
    void testGetSize() {
        Board b = new Board(5);
        assertEquals(5, b.getSize());
    }

    @Test
    void testPlaceStoneAndGet() {
        Board b = new Board(3);
        Board.MoveResult r = b.placeStone(new Point(1, 1), Stone.BLACK);
        assertTrue(r.valid, "Stone placement should be valid");
        assertEquals(Stone.BLACK, b.get(new Point(1, 1)));
    }

    @Test
    void testGetEmpty() {
        Board b = new Board(3);
        assertEquals(Stone.EMPTY, b.get(new Point(0, 0)));
    }

    @Test
    void testAreaControlledBy() {
        Board b = new Board(4);
        b.placeStone(new Point(0, 0), Stone.WHITE);
        b.placeStone(new Point(1, 1), Stone.WHITE);
        assertEquals(2, b.areaControlledBy(Stone.WHITE));
    }

    @Test
    void testGetCapturedByInitiallyZero() {
        Board b = new Board(3);
        assertEquals(0, b.getCapturedBy(Stone.BLACK));
        assertEquals(0, b.getCapturedBy(Stone.WHITE));
    }
}