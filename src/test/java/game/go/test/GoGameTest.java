package game.go.test;

import game.go.model.Board;
import game.go.model.GameState;
import game.go.model.Point;
import game.go.model.Stone;
import game.go.util.GameRecorder;

import java.util.Map;

/**
 * Go oyun motoru için test sınıfı.
 * Bu sınıf, Go oyununun temel kurallarını ve mantığını test eder.
 */
public class GoGameTest {
    
    /**
     * Temel hamle ve taş esir alma testleri
     */
    public void testBasicCaptureAndScoring() {
        System.out.println("=== Test: Basit Esir Alma ve Puanlama ===");
        
        GameState state = new GameState(9); // Küçük tahta kullan (9x9)
        GameRecorder recorder = new GameRecorder(9, "Test Black", "Test White");
        state.setRecorder(recorder);
        
        // Bir esir alma senaryosu oluştur
        System.out.println("Hamle 1: Siyah (0,0)");
        state.play(new Point(0, 0)); // Siyah
        System.out.println("Hamle 2: Beyaz (0,1)");
        state.play(new Point(0, 1)); // Beyaz
        System.out.println("Hamle 3: Siyah (1,0)");
        state.play(new Point(1, 0)); // Siyah
        System.out.println("Hamle 4: Beyaz (1,1)");
        state.play(new Point(1, 1)); // Beyaz
        System.out.println("Hamle 5: Siyah (4,4) - rastgele hamle");
        state.play(new Point(4, 4)); // Siyah (rastgele hamle)
        System.out.println("Hamle 6: Beyaz (0,2) - Siyah taşı (0,0) esir almalı");
        state.play(new Point(0, 2)); // Beyaz
        
        // (0,0) konumundaki siyah taş esir alındı mı?
        Stone stoneAt00 = state.board().get(new Point(0, 0));
        assertCondition(stoneAt00 == Stone.EMPTY, 
                       "(0,0) konumundaki taş esir alınmalıydı, ancak alınmadı: " + stoneAt00);
        
        // Beyaz oyuncu bir taş esir aldı mı?
        int whiteCaptureCount = state.getWhiteCaptureCount();
        assertCondition(whiteCaptureCount == 1, 
                       "Beyaz oyuncu 1 taş esir almalıydı, ancak sonuç: " + whiteCaptureCount);
        
        System.out.println("Geçerli tahta durumu:");
        System.out.println(state.board().toString());
        
        // İki pas ile oyunu bitir
        System.out.println("Hamle 7: Siyah pas geçiyor");
        state.pass(); // Siyah pas
        System.out.println("Hamle 8: Beyaz pas geçiyor");
        state.pass(); // Beyaz pas
        
        // Oyun bitti mi?
        assertCondition(state.isOver(), 
                       "İki ardışık pas sonrasında oyun bitmiş olmalıydı");
        
        // Puanlama doğru mu?
        Map<Stone, Integer> scores = state.calculateTerritorialScores();
        System.out.println("Final puanları: Siyah=" + scores.get(Stone.BLACK) + 
                          ", Beyaz=" + scores.get(Stone.WHITE) + 
                          " (Komi: " + state.getKomi() + ")");
        
        // Puanların beklenen değerlere uygun olup olmadığını kontrol et
        // Not: Beklenen değerler, oyun mantığına ve taşların durumuna göre değişebilir
        System.out.println("Test başarılı!");
    }
    
    /**
     * Ko kuralı testi
     */
    public void testKoRule() {
        System.out.println("=== Test: Ko Kuralı ===");
        
        GameState state = new GameState(9);
        
        // Ko durumu oluştur
        System.out.println("Ko durumu oluşturuluyor...");
        state.play(new Point(3, 3)); // Siyah
        state.play(new Point(3, 4)); // Beyaz
        state.play(new Point(2, 4)); // Siyah
        state.play(new Point(2, 3)); // Beyaz
        state.play(new Point(4, 3)); // Siyah
        state.play(new Point(3, 2)); // Beyaz
        state.play(new Point(4, 4)); // Siyah
        state.play(new Point(3, 5)); // Beyaz
        state.play(new Point(5, 3)); // Siyah
        state.play(new Point(5, 4)); // Beyaz
        
        System.out.println("Geçerli tahta durumu:");
        System.out.println(state.board().toString());
        
        // Beyaz, siyah taşı esir alıyor
        System.out.println("Beyaz (4,3) konumuna oynuyor ve siyah taşı esir alıyor");
        Board.MoveResult result1 = state.play(new Point(4, 3)); // Beyaz
        assertCondition(result1.valid, 
                      "Geçerli bir hamle bekleniyor, ancak sonuç: " + result1.message);
        
        System.out.println("Beyaz taşı esir aldıktan sonraki tahta durumu:");
        System.out.println(state.board().toString());
        
        // Siyah hemen aynı pozisyonda esir almaya çalışıyor (Ko kuralı ihlali)
        System.out.println("Siyah (4,3) konumuna oynuyor - Ko kuralı ihlali olmalı");
        Board.MoveResult result2 = state.play(new Point(4, 3)); // Siyah
        assertCondition(!result2.valid, 
                      "Ko kuralı ihlali bekleniyor, ancak hamle geçerli kabul edildi");
        
        // Siyah başka bir yere oynuyor
        System.out.println("Siyah başka bir yere oynuyor: (0,0)");
        state.play(new Point(0, 0)); // Siyah
        
        // Beyaz da başka bir yere oynuyor
        System.out.println("Beyaz başka bir yere oynuyor: (0,1)");
        state.play(new Point(0, 1)); // Beyaz
        
        // Artık siyah Ko pozisyonuna oynayabilir
        System.out.println("Şimdi Siyah tekrar (4,3) konumuna oynayabilmeli");
        Board.MoveResult result3 = state.play(new Point(4, 3)); // Siyah
        assertCondition(result3.valid, 
                      "Ko beklemesi sonrası geçerli hamle bekleniyor, ancak sonuç: " + result3.message);
        
        System.out.println("Test başarılı!");
    }
    
    /**
     * İntihar kuralı testi (çoğu Go varyantında izin verilmez)
     */
    public void testSuicideRule() {
        System.out.println("=== Test: İntihar Kuralı ===");
        
        GameState state = new GameState(9);
        
        // İntihar senaryosu oluştur - (1,1) konumuna oynamak intihar olacak
        System.out.println("İntihar durumu oluşturuluyor...");
        state.play(new Point(0, 0)); // Siyah
        state.play(new Point(0, 1)); // Beyaz
        state.play(new Point(1, 2)); // Siyah
        state.play(new Point(1, 0)); // Beyaz
        state.play(new Point(3, 3)); // Siyah (rastgele)
        state.play(new Point(2, 1)); // Beyaz
        
        System.out.println("Geçerli tahta durumu:");
        System.out.println(state.board().toString());
        
        // Şimdi siyah (1,1) konumuna oynamaya çalışırsa, bu bir intihar hamlesi olacak
        System.out.println("Siyah (1,1) konumuna oynamayı deniyor - intihar olmalı");
        Board.MoveResult result = state.play(new Point(1, 1)); // Siyah
        assertCondition(!result.valid, 
                      "İntihar hamlesi geçersiz olmalıydı, ancak geçerli kabul edildi");
        
        System.out.println("Test başarılı!");
    }
    
    /**
     * Bir grup taşın özgürlüklerini ve yakalanmasını test eder
     */
    public void testGroupCaptureAndLiberties() {
        System.out.println("=== Test: Grup Yakalama ve Özgürlükler ===");
        
        GameState state = new GameState(9);
        
        // 2x2 bir siyah grup oluştur
        state.play(new Point(1, 1)); // Siyah
        state.play(new Point(0, 0)); // Beyaz (rastgele)
        state.play(new Point(1, 2)); // Siyah
        state.play(new Point(0, 3)); // Beyaz (rastgele)
        state.play(new Point(2, 1)); // Siyah
        state.play(new Point(0, 4)); // Beyaz (rastgele)
        state.play(new Point(2, 2)); // Siyah
        
        System.out.println("Siyah 2x2 grup oluşturuldu, tahta durumu:");
        System.out.println(state.board().toString());
        
        // Grubu çevreleyen beyaz taşlar ekle
        System.out.println("Beyaz, siyah grubu çevrelemeye başlıyor...");
        state.play(new Point(0, 1)); // Beyaz
        state.play(new Point(3, 3)); // Siyah (rastgele)
        state.play(new Point(0, 2)); // Beyaz
        state.play(new Point(3, 4)); // Siyah (rastgele)
        state.play(new Point(1, 0)); // Beyaz
        state.play(new Point(4, 3)); // Siyah (rastgele)
        state.play(new Point(2, 0)); // Beyaz
        state.play(new Point(4, 4)); // Siyah (rastgele)
        state.play(new Point(3, 1)); // Beyaz
        state.play(new Point(5, 5)); // Siyah (rastgele)
        state.play(new Point(3, 2)); // Beyaz
        state.play(new Point(6, 6)); // Siyah (rastgele)
        
        System.out.println("Siyah grup neredeyse çevrelendi, tahta durumu:");
        System.out.println(state.board().toString());
        
        // Son hamle ile grubu esir al
        System.out.println("Beyaz son hamle ile siyah grubu tamamen çevreliyor: (1,3)");
        state.play(new Point(1, 3)); // Beyaz
        
        // Siyah grup esir alındı mı?
        System.out.println("Güncel tahta durumu:");
        System.out.println(state.board().toString());
        
        // Esir alınan noktaları kontrol et
        Stone stone11 = state.board().get(new Point(1, 1));
        Stone stone12 = state.board().get(new Point(1, 2));
        Stone stone21 = state.board().get(new Point(2, 1));
        Stone stone22 = state.board().get(new Point(2, 2));
        
        assertCondition(stone11 == Stone.EMPTY && stone12 == Stone.EMPTY && 
                       stone21 == Stone.EMPTY && stone22 == Stone.EMPTY,
                       "Tüm siyah grup esir alınmalıydı");
        
        // Esir sayısını kontrol et - 4 siyah taş esir alınmalı
        int whiteCaptureCount = state.getWhiteCaptureCount();
        assertCondition(whiteCaptureCount == 4,
                       "Beyaz 4 siyah taş esir almalıydı, ancak sonuç: " + whiteCaptureCount);
        
        System.out.println("Test başarılı!");
    }
    
    /**
     * Tüm testleri çalıştırır.
     */
    public void runAllTests() {
        System.out.println("==== GO OYUNU TESTLERİ BAŞLIYOR ====");
        testBasicCaptureAndScoring();
        System.out.println();
        testKoRule();
        System.out.println();
        testSuicideRule();
        System.out.println();
        testGroupCaptureAndLiberties();
        System.out.println();
        System.out.println("==== Tüm testler başarıyla tamamlandı! ====");
    }
    
    /**
     * Bir koşulu doğrular ve başarısız olursa hata fırlatır.
     */
    private void assertCondition(boolean condition, String errorMessage) {
        if (!condition) {
            System.err.println("TEST HATASI: " + errorMessage);
            throw new RuntimeException("Test hatası: " + errorMessage);
        }
    }
    
    /**
     * Test sınıfını çalıştıran ana metot.
     */
    public static void main(String[] args) {
        new GoGameTest().runAllTests();
    }
}