package game.go.util;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * Go oyunu için gelişmiş zamanlayıcı.
 * Süreli oyunlar için oyuncu süresini tutar, görsel gösterimler ve uyarılar sağlar.
 */
public class GameTimer {
    // Durum takibi
    private int secondsRemaining;
    private boolean isRunning = false;
    
    // UI bileşenleri
    private final JLabel displayLabel;
    
    // Zamanlayıcı
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timerTask;
    
    // Aksiyonlar
    private Runnable timeoutAction;
    private Runnable warningAction;
    private Runnable criticalAction;
    
    // Uyarı bayrakları
    private boolean warningPlayed = false;
    private boolean criticalPlayed = false;
    
    // Uyarı süresi eşikleri (saniye cinsinden)
    private static final int WARNING_THRESHOLD = 600; // 10 dakika
    private static final int CRITICAL_THRESHOLD = 300; // 5 dakika
    
    // Logger
    private static final Logger LOGGER = Logger.getLogger(GameTimer.class.getName());

    /**
     * Belirtilen başlangıç zamanı ile yeni bir oyun zamanlayıcısı oluşturur
     * 
     * @param initialMinutes Dakika cinsinden başlangıç zamanı
     * @param displayLabel Süreyi gösterecek etiket (null olabilir)
     */
    public GameTimer(int initialMinutes, JLabel displayLabel) {
        this.secondsRemaining = initialMinutes * 60;
        this.displayLabel = displayLabel;
        
        // Daemonic tek thread'li scheduler (JVM'i bekletmeyecek)
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GameTimer-Thread");
            t.setDaemon(true);  // Ana uygulama kapandığında thread'in sonlanmasını sağlar
            return t;
        });
        
        // İlk durum gösterimi
        updateDisplay();
        
        LOGGER.log(Level.INFO, "GameTimer initialized with {0} minutes", initialMinutes);
    }
    
    /**
     * Zamanlayıcıyı başlatır.
     * Zaten çalışıyorsa hiçbir şey yapmaz.
     */
    public void start() {
        if (isRunning) {
            LOGGER.fine("Timer is already running");
            return;
        }
        
        // Önceki görevi iptal et
        if (timerTask != null) {
            timerTask.cancel(false);
        }
        
        LOGGER.log(Level.INFO, "Timer started with {0} seconds remaining", secondsRemaining);
        
        // Yeni timer görevi başlat
        timerTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (secondsRemaining > 0) {
                    secondsRemaining--;
                    updateDisplay();
                    checkTimeWarnings();
                } else {
                    stop();
                    LOGGER.info("Timer expired");
                    
                    if (timeoutAction != null) {
                        timeoutAction.run();
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in timer task", e);
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        isRunning = true;
    }
    
    /**
     * Zaman uyarılarını kontrol eder ve gerekirse aksiyonları çalıştırır.
     */
    private void checkTimeWarnings() {
        // Kritik zaman uyarısı (5 dakika kaldığında)
        if (secondsRemaining <= CRITICAL_THRESHOLD && !criticalPlayed) {
            criticalPlayed = true;
            LOGGER.info("Critical time warning triggered at " + secondsRemaining + " seconds");
            
            if (criticalAction != null) {
                try {
                    criticalAction.run();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error executing critical action", e);
                }
            }
        } 
        // Normal uyarı (10 dakika kaldığında)
        else if (secondsRemaining <= WARNING_THRESHOLD && !warningPlayed) {
            warningPlayed = true;
            LOGGER.info("Time warning triggered at " + secondsRemaining + " seconds");
            
            if (warningAction != null) {
                try {
                    warningAction.run();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error executing warning action", e);
                }
            }
        }
    }
    
    /**
     * Zamanlayıcıyı durdurur.
     * Zaten durmuşsa hiçbir şey yapmaz.
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        LOGGER.log(Level.INFO, "Timer stopped with {0} seconds remaining", secondsRemaining);
        
        if (timerTask != null) {
            timerTask.cancel(false);
        }
        
        isRunning = false;
    }
    
    /**
     * Zamanlayıcıyı belirtilen dakika sayısına sıfırlar
     * 
     * @param minutes Yeni süre (dakika)
     */
    public void reset(int minutes) {
        stop();
        
        this.secondsRemaining = minutes * 60;
        this.warningPlayed = false;
        this.criticalPlayed = false;
        
        updateDisplay();
        
        LOGGER.log(Level.INFO, "Timer reset to {0} minutes", minutes);
    }
    
    /**
     * Kalan saniye sayısını döndürür
     * 
     * @return Kalan saniye
     */
    public int getSecondsRemaining() {
        return secondsRemaining;
    }
    
    /**
     * Zamanlayıcının şu anda çalışıp çalışmadığını kontrol eder
     * 
     * @return Çalışıyorsa true, durmuşsa false
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Gösterge etiketini güncel süre ile günceller.
     * Süreye göre renk değiştirir.
     */
    private void updateDisplay() {
        if (displayLabel == null) {
            return;
        }
        
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        final String timeText = String.format("%02d:%02d", minutes, seconds);
        
        SwingUtilities.invokeLater(() -> {
            displayLabel.setText(timeText);
            
            // Kalan süreye göre renk değişimi
            if (minutes < 5) {
                displayLabel.setForeground(Color.RED);
            } else if (minutes < 10) {
                displayLabel.setForeground(new Color(255, 165, 0)); // Turuncu
            } else {
                displayLabel.setForeground(Color.BLACK);
            }
        });
    }
    
    /**
     * Zaman bittiğinde çalıştırılacak aksiyonu ayarlar
     * 
     * @param action Çalıştırılacak aksiyon
     */
    public void setTimeoutAction(Runnable action) {
        this.timeoutAction = action;
    }
    
    /**
     * Uyarı zamanı geldiğinde çalıştırılacak aksiyonu ayarlar
     * 
     * @param action Çalıştırılacak aksiyon
     */
    public void setWarningAction(Runnable action) {
        this.warningAction = action;
    }
    
    /**
     * Kritik zaman geldiğinde çalıştırılacak aksiyonu ayarlar
     * 
     * @param action Çalıştırılacak aksiyon
     */
    public void setCriticalAction(Runnable action) {
        this.criticalAction = action;
    }
    
    /**
     * Zamanlayıcıya belirtilen süreyi ekler
     * 
     * @param seconds Eklenecek saniye sayısı
     */
    public void addTime(int seconds) {
        this.secondsRemaining += seconds;
        
        // Süre eklendiğinde uyarıları sıfırla
        if (this.secondsRemaining > WARNING_THRESHOLD) {
            warningPlayed = false;
        }
        if (this.secondsRemaining > CRITICAL_THRESHOLD) {
            criticalPlayed = false;
        }
        
        updateDisplay();
        
        LOGGER.log(Level.INFO, "Added {0} seconds, new time: {1}", 
                  new Object[]{seconds, getTimeText()});
    }
    
    /**
     * Biçimlendirilmiş zaman metnini döndürür
     * 
     * @return "MM:SS" formatında zaman
     */
    public String getTimeText() {
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * Zamanın uyarı seviyesinde olup olmadığını kontrol eder
     * 
     * @return Uyarı seviyesindeyse true
     */
    public boolean isAtWarningLevel() {
        return secondsRemaining <= WARNING_THRESHOLD;
    }
    
    /**
     * Zamanın kritik seviyede olup olmadığını kontrol eder
     * 
     * @return Kritik seviyedeyse true
     */
    public boolean isAtCriticalLevel() {
        return secondsRemaining <= CRITICAL_THRESHOLD;
    }
    
    /**
     * Dakika ve saniye bölümlerini ayrı ayrı döndürür
     * 
     * @return [dakika, saniye] formatında bir dizi
     */
    public int[] getMinutesAndSeconds() {
        return new int[]{secondsRemaining / 60, secondsRemaining % 60};
    }
    
    /**
     * JVM kapanırken zamanlayıcının düzgün şekilde kapatılmasını sağlar
     */
    public void shutdown() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Timer shutdown interrupted", e);
        }
    }
    
    /**
     * Kalan süreyi belirli bir değere ayarlar
     * 
     * @param minutes Dakika sayısı
     * @param seconds Saniye sayısı
     */
    public void setRemainingTime(int minutes, int seconds) {
        boolean wasRunning = isRunning;
        if (wasRunning) {
            stop();
        }
        
        this.secondsRemaining = minutes * 60 + seconds;
        
        // Süre değiştiğinde uyarı durumlarını güncelle
        if (this.secondsRemaining > WARNING_THRESHOLD) {
            warningPlayed = false;
        } else if (this.secondsRemaining <= CRITICAL_THRESHOLD) {
            warningPlayed = true;
            if (!criticalPlayed) {
                criticalPlayed = true;
                if (criticalAction != null) {
                    criticalAction.run();
                }
            }
        } else if (this.secondsRemaining <= WARNING_THRESHOLD) {
            if (!warningPlayed) {
                warningPlayed = true;
                if (warningAction != null) {
                    warningAction.run();
                }
            }
            criticalPlayed = false;
        }
        
        updateDisplay();
        
        if (wasRunning) {
            start();
        }
        
        LOGGER.log(Level.INFO, "Time manually set to {0}:{1}", 
                  new Object[]{minutes, seconds});
    }
    
    /**
     * Süre bitmiş mi kontrol eder
     * 
     * @return Süre bittiyse true
     */
    public boolean isExpired() {
        return secondsRemaining <= 0;
    }
    
    /**
     * Zamanlayıcının durumunu metin olarak döndürür
     */
    @Override
    public String toString() {
        return String.format("GameTimer[time=%s, running=%s, warned=%s, critical=%s]",
                getTimeText(), isRunning, warningPlayed, criticalPlayed);
    }
}