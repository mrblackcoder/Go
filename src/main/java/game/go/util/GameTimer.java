package game.go.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * Timer to keep track of player time in Go game
 */
public class GameTimer {
    private int secondsRemaining;
    private final Timer timer;
    private final JLabel displayLabel;
    private boolean isRunning = false;
    private Runnable timeoutAction;
    
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
            displayLabel.setText(timeText);
        }
    }
    
    /**
     * Add time to the timer
     * 
     * @param seconds Seconds to add
     */
    public void addTime(int seconds) {
        this.secondsRemaining += seconds;
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
}