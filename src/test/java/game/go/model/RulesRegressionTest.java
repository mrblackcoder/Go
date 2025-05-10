
// --- File: game/go/model/RulesRegressionTest.java ---
package game.go.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RulesRegressionTest {
    private GameState gs;

    @BeforeEach
    void init() {
        gs = new GameState(5);
    }

    @Test
    @DisplayName("Occupied point must be rejected")
    void occupiedPointForbidden() {
        Board.MoveResult first = gs.play(new Point(2, 2));
        assertTrue(first.valid);
        Board.MoveResult second = gs.play(new Point(2, 2));
        assertFalse(second.valid);
    }

    @Test
    @DisplayName("Out-of-bounds coordinates are rejected")
    void oobForbidden() {
        Board.MoveResult r = gs.play(new Point(-1, 0));
        assertFalse(r.valid);
        assertEquals("Hamle tahta dışında", r.message);
    }
//
//    @Test
//    @DisplayName("Single-stone capture updates counters")
//    void singleStoneCapture() {
//        Board b = new Board(3);
//        b.placeStone(new Point(0, 1), Stone.BLACK);
//        b.placeStone(new Point(1, 0), Stone.BLACK);
//        b.placeStone(new Point(2, 1), Stone.BLACK);
//        Board.MoveResult atari = b.placeStone(new Point(1, 1), Stone.WHITE);
//        assertTrue(atari.valid);
//        Board.MoveResult cap = b.placeStone(new Point(1, 2), Stone.BLACK);
//        assertTrue(cap.valid);
//        assertEquals(1, b.getCapturedBy(Stone.BLACK));
//        //assertTrue(b.getLastCaptured().contains(new Point(1, 1)));
//    }

//    @Nested
//    class KoRule {
//        @Test
//        @DisplayName("Immediate recapture is rejected")
//        void koViolationRejected() {
//            GameState s = new GameState(3);
//            s.play(new Point(0, 1)); s.play(new Point(1, 1));
//            s.play(new Point(1, 0)); s.play(new Point(2, 1));
//            s.play(new Point(1, 2));
//            Board.MoveResult ko = s.play(new Point(1, 1));
//            assertFalse(ko.valid);
//            assertEquals("Ko ihlali", ko.message);
//        }
//    }

    @Test
    @DisplayName("Two consecutive passes end the game")
    void twoPassesEndGame() {
        gs.play(new Point(2, 2));
        gs.pass(); assertFalse(gs.isOver());
        gs.pass(); assertTrue(gs.isOver());
    }

    @Test
    @DisplayName("Resign ends the game immediately")
    void resignEndsGame() {
        Board.MoveResult r = gs.resign();
        assertTrue(r.valid);
        assertTrue(gs.isOver());
    }

    @Test
    @DisplayName("Area + capture scoring")
    void scoringWorks() {
        GameState s = new GameState(3);
        s.play(new Point(1, 1)); s.play(new Point(0, 0));
        s.play(new Point(0, 1)); s.play(new Point(2, 0));
        s.play(new Point(1, 0)); s.pass(); s.pass();
        assertTrue(s.scoreFor(Stone.BLACK) > s.scoreFor(Stone.WHITE));
    }
}