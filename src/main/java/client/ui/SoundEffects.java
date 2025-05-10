package client.ui;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Sound effects for the Go game
 */
public class SoundEffects {
    
    private static final Map<String, Clip> soundClips = new HashMap<>();
    private static boolean soundEnabled = true;
    
    public static final String STONE_PLACE = "stone_place";
    public static final String CAPTURE = "capture";
    public static final String GAME_START = "game_start";
    public static final String GAME_END = "game_end";
    public static final String ERROR = "error";
    
    static {
        // Load sound clips
        loadClip(STONE_PLACE, "sounds/stone_place.wav");
        loadClip(CAPTURE, "sounds/capture.wav");
        loadClip(GAME_START, "sounds/game_start.wav");
        loadClip(GAME_END, "sounds/game_end.wav");
        loadClip(ERROR, "sounds/error.wav");
    }
    
    /**
     * Load a sound clip from the resources
     * 
     * @param name Name to identify the sound
     * @param path Path to the sound file
     */
    private static void loadClip(String name, String path) {
        try {
            URL url = SoundEffects.class.getClassLoader().getResource(path);
            if (url == null) {
                System.err.println("Could not find sound file: " + path);
                return;
            }
            
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            soundClips.put(name, clip);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error loading sound clip: " + path);
            e.printStackTrace();
        }
    }
    
    /**
     * Play a sound
     * 
     * @param soundName Name of the sound to play
     */
    public static void play(String soundName) {
        if (!soundEnabled || !soundClips.containsKey(soundName)) {
            return;
        }
        
        Clip clip = soundClips.get(soundName);
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }
    
    /**
     * Enable or disable sounds
     * 
     * @param enable True to enable sounds, false to disable
     */
    public static void enableSound(boolean enable) {
        soundEnabled = enable;
    }
    
    /**
     * Check if sounds are enabled
     * 
     * @return True if sounds are enabled, false otherwise
     */
    public static boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    /**
     * Add a new sound
     * 
     * @param name Name to identify the sound
     * @param path Path to the sound file
     */
    public static void addSound(String name, String path) {
        loadClip(name, path);
    }
    
    /**
     * Stop all sounds
     */
    public static void stopAll() {
        for (Clip clip : soundClips.values()) {
            if (clip.isRunning()) {
                clip.stop();
            }
        }
    }
}