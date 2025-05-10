package game.go.model;

/**
 * Go oyunu kurallarını test eden sınıf
 */
public class GameRulesTester {
    
    public static void main(String[] args) {
        // Tüm testleri çalıştır
        testBasicStoneCapture();
        testKoRule();
        testSuicideRule();
        testGameEndConditions();
        testScoring();
        
        System.out.println("Tüm testler başarıyla tamamlandı!");
    }
    
    /**
     * Taş esir alma temel kuralını test eder
     */
    private static void testBasicStoneCapture() {
        System.out.println("=== Taş Esir Alma Testi ===");
        
        // Test 1 - Tek taş esir alma
        GameState game = new GameState(9);
        Board board = game.board();
        
        // Siyah ortaya koysun (4,4)
        Point center = new Point(4, 4);
        board.placeStone(center, Stone.BLACK);
        
        // Beyaz etrafını çevirsin
        board.placeStone(new Point(3, 4), Stone.WHITE);
        board.placeStone(new Point(5, 4), Stone.WHITE);
        board.placeStone(new Point(4, 3), Stone.WHITE);
        
        // Son taşı koymadan önce beyaz esir sayısı
        int capturedBefore = board.getCapturedBy(Stone.WHITE);
        
        // Son taşı koy
        board.placeStone(new Point(4, 5), Stone.WHITE);
        
        // Beyazın esir aldığı sayı 1 artmış olmalı
        int capturedAfter = board.getCapturedBy(Stone.WHITE);
        assert capturedAfter == capturedBefore + 1 : "Taş esir alma çalışmıyor!";
        
        // Merkez nokta boş olmalı
        assert board.get(center) == Stone.EMPTY : "Esir alınan taş tahtadan kaldırılmamış!";
        
        System.out.println("Temel esir alma testi başarılı!");
    }
    
    /**
     * Ko kuralını test eder
     */
    private static void testKoRule() {
        System.out.println("\n=== Ko Kuralı Testi ===");
        
        GameState game = new GameState(9);
        Board board = game.board();
        
        // Ko durumunu oluştur
        /*
            . B W .
            B . B W
            . B W .
        */
        board.placeStone(new Point(1, 0), Stone.BLACK);
        board.placeStone(new Point(2, 0), Stone.WHITE);
        board.placeStone(new Point(0, 1), Stone.BLACK);
        board.placeStone(new Point(2, 1), Stone.BLACK);
        board.placeStone(new Point(3, 1), Stone.WHITE);
        board.placeStone(new Point(1, 2), Stone.BLACK);
        board.placeStone(new Point(2, 2), Stone.WHITE);
        
        // Siyah hamle yapsın
        game.play(new Point(1, 1));
        
        // Şimdi tahta:
        /*
            . B W .
            B B B W
            . B W .
        */
        
        // Sıra beyazda, ortadaki siyahı alabilmeli
        Board.MoveResult moveResult = game.play(new Point(2, 1));
        assert moveResult.valid : "Beyaz geçerli hamle yapamadı: " + moveResult.message;
        
        // Şimdi tahta:
        /*
            . B W .
            B W B W
            . B W .
        */
        
        // Sıra siyahta, önceki hamleyi yapmamalı (ko kuralı)
        moveResult = game.play(new Point(1, 1));
        assert !moveResult.valid : "Ko kuralı çalışmıyor! Siyah önceki hamleyi tekrarlayabildi.";
        assert moveResult.message.contains("Ko") : "Ko hata mesajı doğru değil: " + moveResult.message;
        
        System.out.println("Ko kuralı testi başarılı!");
    }
    
    /**
     * İntihar hamlesini (suicide move) test eder
     */
    private static void testSuicideRule() {
        System.out.println("\n=== İntihar Hamlesi Testi ===");
        
        GameState game = new GameState(9);
        Board board = game.board();
        
        // İntihar durumunu oluştur
        /*
            . W .
            W . W
            . W .
        */
        board.placeStone(new Point(1, 0), Stone.WHITE);
        board.placeStone(new Point(0, 1), Stone.WHITE);
        board.placeStone(new Point(2, 1), Stone.WHITE);
        board.placeStone(new Point(1, 2), Stone.WHITE);
        
        // Siyah ortaya koyarsa intihar etmiş olur
        Board.MoveResult moveResult = game.play(new Point(1, 1));
        
        // Bu hamle geçersiz olmalı
        assert !moveResult.valid : "İntihar hamlesi engellenmedi!";
        assert moveResult.message.contains("Suicide") : "İntihar hamle mesajı doğru değil: " + moveResult.message;
        
        System.out.println("İntihar hamlesi testi başarılı!");
    }
    
    /**
     * Oyun bitirme koşullarını test eder
     */
    private static void testGameEndConditions() {
        System.out.println("\n=== Oyun Bitirme Testi ===");
        
        // Test 1 - İki pas ile oyun biter
        GameState game = new GameState(9);
        
        // İlk pas
        Board.MoveResult result1 = game.pass();
        assert result1.valid : "İlk pas geçerli değil";
        assert !game.isOver() : "Tek pas ile oyun bitmemeli";
        
        // İkinci pas
        Board.MoveResult result2 = game.pass();
        assert result2.valid : "İkinci pas geçerli değil";
        assert game.isOver() : "İki pas sonrası oyun bitmeli";
        
        // Test 2 - Pes etme ile oyun biter
        game = new GameState(9);
        Board.MoveResult resignResult = game.resign();
        assert resignResult.valid : "Pes etme geçerli değil";
        assert game.isOver() : "Pes etme sonrası oyun bitmeli";
        
        System.out.println("Oyun bitirme testi başarılı!");
    }
    
    /**
     * Puanlama sistemini test eder
     */
    private static void testScoring() {
        System.out.println("\n=== Puanlama Testi ===");
        
        GameState game = new GameState(9);
        Board board = game.board();
        
        // Başlangıçta skor sıfır olmalı
        assert game.scoreFor(Stone.BLACK) == 0 : "Başlangıç siyah skoru sıfır değil";
        assert game.scoreFor(Stone.WHITE) == 0 : "Başlangıç beyaz skoru sıfır değil";
        
        // Birkaç taş koy
        board.placeStone(new Point(2, 2), Stone.BLACK);
        board.placeStone(new Point(6, 6), Stone.WHITE);
        board.placeStone(new Point(2, 6), Stone.BLACK);
        board.placeStone(new Point(6, 2), Stone.WHITE);
        
        // Kontrol alan sayısı arttı mı?
        assert game.scoreFor(Stone.BLACK) == 2 : "Siyah skoru 2 olmalı";
        assert game.scoreFor(Stone.WHITE) == 2 : "Beyaz skoru 2 olmalı";
        
        // Esir alma
        board.placeStone(new Point(3, 1), Stone.BLACK);
        board.placeStone(new Point(3, 3), Stone.BLACK);
        board.placeStone(new Point(1, 2), Stone.BLACK);
        board.placeStone(new Point(2, 1), Stone.WHITE);
        board.placeStone(new Point(2, 3), Stone.BLACK);
        
        // Şimdi beyaz taş esir alınmalı
        assert board.getCapturedBy(Stone.BLACK) == 1 : "Siyah 1 taş esir almalıydı";
        
        // Skor kontrol et (alan + esir)
        assert game.scoreFor(Stone.BLACK) == 5 : "Siyah toplam skoru 5 olmalı (4 alan + 1 esir)";
        
        System.out.println("Puanlama testi başarılı!");
    }
}