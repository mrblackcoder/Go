package game.go.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.Timer;
import java.awt.Color;
import javax.swing.SwingUtilities;

/**
 * Enhanced timer to keep track of player time in Go game
 * Now with warning functionality and better display
 */
public class GameTimer {
    private int secondsRemaining;
    private final Timer timer;
    private final JLabel displayLabel;
    private boolean isRunning = false;
    private Runnable timeoutAction;
    
    // Uyarı bayrakları
    private boolean warningPlayed = false;
    private boolean criticalPlayed = false;
    
    // Uyarı süresi eşikleri (saniye cinsinden)
    private static final int WARNING_THRESHOLD = 600; // 10 dakika
    private static final int CRITICAL_THRESHOLD = 300; // 5 dakika
    
    // Uyarı işleyicisi
    private Runnable warningAction;
    private Runnable criticalAction;

    /**
     * Creates a new game timer with the specified initial time
     * 
     * @param initialMinutes Initial time in minutes
     * @param displayLabel Label to display the timer (can be null)
     */
    public GameTimer(int initialMinutes, JLabel displayLabel) {
        this.secondsRemaining = initialMinutes * 60;
        this.displayLabel = displayLabel;
        updateDisplay();
        
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (secondsRemaining > 0) {
                    secondsRemaining--;
                    updateDisplay();
                    checkTimeWarnings(); // Süre uyarılarını kontrol et
                } else {
                    stop();
                    if (timeoutAction != null) {
                        timeoutAction.run();
                    }
                }
            }
        });
    }
    
    /**
     * Check if time warnings need to be triggered
     */
    private void checkTimeWarnings() {
        // Kritik zaman uyarısı (5 dakika kaldığında)
        if (secondsRemaining <= CRITICAL_THRESHOLD && !criticalPlayed) {
            criticalPlayed = true;
            if (criticalAction != null) {
                criticalAction.run();
            }
        } 
        // Normal uyarı (10 dakika kaldığında)
        else if (secondsRemaining <= WARNING_THRESHOLD && !warningPlayed) {
            warningPlayed = true;
            if (warningAction != null) {
                warningAction.run();
            }
        }
    }
    
    /**
     * Set warning time action
     * 
     * @param action Action to perform when warning time is reached
     */
    public void setWarningAction(Runnable action) {
        this.warningAction = action;
    }
    
    /**
     * Set critical time action
     * 
     * @param action Action to perform when critical time is reached
     */
    public void setCriticalAction(Runnable action) {
        this.criticalAction = action;
    }
    
    /**
     * Start the timer
     */
    public void start() {
        if (!isRunning) {
            timer.start();
            isRunning = true;
        }
    }
    
    /**
     * Stop the timer
     */
    public void stop() {
        if (isRunning) {
            timer.stop();
            isRunning = false;
        }
    }
    
    /**
     * Reset the timer to the specified number of minutes
     * 
     * @param minutes Minutes to reset to
     */
    public void reset(int minutes) {
        stop();
        this.secondsRemaining = minutes * 60;
        this.warningPlayed = false;
        this.criticalPlayed = false;
        updateDisplay();
    }
    
    /**
     * Set action to perform when timer reaches zero
     * 
     * @param action Action to perform
     */
    public void setTimeoutAction(Runnable action) {
        this.timeoutAction = action;
    }
    
    /**
     * Get the remaining time in seconds
     * 
     * @return Seconds remaining
     */
    public int getSecondsRemaining() {
        return secondsRemaining;
    }
    
    /**
     * Check if the timer is running
     * 
     * @return True if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Update the display label with current time
     */
    private void updateDisplay() {
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);
        
        if (displayLabel != null) {
            final String finalTimeText = timeText;
            SwingUtilities.invokeLater(() -> {
                displayLabel.setText(finalTimeText);
                
                // Kalan süreye göre renk değiştir
                if (minutes < 5) {
                    displayLabel.setForeground(Color.RED);
                } else if (minutes < 10) {
                    displayLabel.setForeground(new Color(255, 165, 0)); // Turuncu
                } else {
                    displayLabel.setForeground(Color.BLACK);
                }
            });
        }
    }
    
    /**
     * Add time to the timer
     * 
     * @param seconds Seconds to add
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
    }
    
    /**
     * Get the formatted time string
     * 
     * @return Time in MM:SS format
     */
    public String getTimeText() {
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * Check if time is at warning level
     * 
     * @return true if time is at warning level
     */
    public boolean isAtWarningLevel() {
        return secondsRemaining <= WARNING_THRESHOLD;
    }
    
    /**
     * Check if time is at critical level
     * 
     * @return true if time is at critical level
     */
    public boolean isAtCriticalLevel() {
        return secondsRemaining <= CRITICAL_THRESHOLD;
    }
}