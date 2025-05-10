
// --- File: game/go/model/GameStateTest.java ---
package game.go.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GameStateTest {
    private GameState g;

    @BeforeEach
    void init() {
        g = new GameState(3);
    }

//    @Test
//    void koIsForbidden() {
//        g.play(new Point(0, 1));
//        g.play(new Point(1, 1));
//        g.play(new Point(1, 0));
//        g.play(new Point(2, 1));
//        g.play(new Point(1, 2));
//        Board.MoveResult res = g.play(new Point(1, 1));
//        assertFalse(res.valid, "Ko rule violation");
//        assertEquals("Ko ihlali", res.message);
//    }

    @Test
    void twoPassEndsGame() {
        assertFalse(g.isOver());
        g.pass();
        assertFalse(g.isOver());
        g.pass();
        assertTrue(g.isOver(), "Two consecutive passes should end the game");
    }
}
