
// --- File: game/go/model/GameStateRulesTest.java ---
package game.go.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GameStateRulesTest {
    private GameState gs;

    @BeforeEach
    void setUp() {
        gs = new GameState(3);
    }
//
//    @Test
//    void koViolationShouldReturnInvalid() {
//        GameState s = new GameState(3);
//        s.play(new Point(0, 1));
//        s.play(new Point(1, 1));
//        s.play(new Point(1, 0));
//        s.play(new Point(2, 1));
//        s.play(new Point(1, 2)); // captures W(1,1)
//
//        Board.MoveResult res = s.play(new Point(1, 1));
//        assertFalse(res.valid, "Immediate recapture must be rejected (Ko rule)");
//        assertEquals("Ko ihlali", res.message);
//    }

    @Test
    void twoConsecutivePassesEndGame() {
        gs.play(new Point(2, 2));
        Board.MoveResult first = gs.pass();
        assertTrue(first.valid);
        assertFalse(gs.isOver(), "One pass should not end the game");

        Board.MoveResult second = gs.pass();
        assertTrue(second.valid);
        assertTrue(gs.isOver(), "Two passes should end the game");
    }

    @Test
    void suicideMoveIsForbidden() {
        Board b = new Board(3);
        b.placeStone(new Point(0, 1), Stone.BLACK);
        b.placeStone(new Point(1, 0), Stone.BLACK);
        b.placeStone(new Point(2, 1), Stone.BLACK);
        b.placeStone(new Point(1, 2), Stone.BLACK);
        Board.MoveResult res = b.placeStone(new Point(1, 1), Stone.WHITE);
        assertFalse(res.valid, "Suicide move must be rejected");
        assertEquals("Suicide hamle", res.message);
    }

    @Test
    void simpleCaptureReflectsInScore() {
        GameState s = new GameState(3);
        s.play(new Point(1, 1));
        s.play(new Point(0, 0));
        s.play(new Point(0, 1));
        s.play(new Point(2, 0));
        s.play(new Point(1, 0)); // captures (0,0)
        s.pass();
        s.pass();
        assertTrue(s.isOver());
        assertTrue(s.scoreFor(Stone.BLACK) > s.scoreFor(Stone.WHITE), "Black should lead after capture");
    }
}