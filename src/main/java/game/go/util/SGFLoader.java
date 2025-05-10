package game.go.util;

import game.go.model.Point;
import game.go.model.Stone;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for loading SGF (Smart Game Format) files
 */
public class SGFLoader {
    
    // Regular expression patterns for parsing SGF properties
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("([A-Z]+)\\[([^\\]]*)]");
    private static final Pattern MOVE_PATTERN = Pattern.compile("([BW])\\[([a-z]{0,2})]");
    private static final Pattern DATE_PATTERN = Pattern.compile("DT\\[([^\\]]*)]");
    private static final Pattern BOARD_SIZE_PATTERN = Pattern.compile("SZ\\[([0-9]+)]");
    private static final Pattern KOMI_PATTERN = Pattern.compile("KM\\[([0-9\\.]+)]");
    private static final Pattern BLACK_PLAYER_PATTERN = Pattern.compile("PB\\[([^\\]]*)]");
    private static final Pattern WHITE_PLAYER_PATTERN = Pattern.compile("PW\\[([^\\]]*)]");
    
    /**
     * Loads an SGF file and returns a GameRecorder with the game information
     * 
     * @param filePath Path to the SGF file
     * @return GameRecorder with the loaded game
     * @throws IOException If file reading fails
     * @throws ParseException If SGF parsing fails
     */
    public static GameRecorder loadFromFile(String filePath) throws IOException, ParseException {
        // Read the entire file content
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }
        
        // Parse SGF content
        return parseContent(content.toString());
    }
    
    /**
     * Parses SGF content and returns a GameRecorder
     * 
     * @param content SGF content as string
     * @return GameRecorder with the parsed game
     * @throws ParseException If SGF parsing fails
     */
    private static GameRecorder parseContent(String content) throws ParseException {
        // Get board size
        int boardSize = 19; // Default
        Matcher boardSizeMatcher = BOARD_SIZE_PATTERN.matcher(content);
        if (boardSizeMatcher.find()) {
            boardSize = Integer.parseInt(boardSizeMatcher.group(1));
        }
        
        // Get player names
        String blackPlayer = "Black";
        Matcher blackPlayerMatcher = BLACK_PLAYER_PATTERN.matcher(content);
        if (blackPlayerMatcher.find()) {
            blackPlayer = blackPlayerMatcher.group(1);
        }
        
        String whitePlayer = "White";
        Matcher whitePlayerMatcher = WHITE_PLAYER_PATTERN.matcher(content);
        if (whitePlayerMatcher.find()) {
            whitePlayer = whitePlayerMatcher.group(1);
        }
        
        // Get date
        Date gameDate = new Date();
        Matcher dateMatcher = DATE_PATTERN.matcher(content);
        if (dateMatcher.find()) {
            String dateStr = dateMatcher.group(1);
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                gameDate = sdf.parse(dateStr);
            } catch (Exception e) {
                // Use current date if parsing fails
            }
        }
        
        // Create game recorder
        GameRecorder recorder = new GameRecorder(boardSize, blackPlayer, whitePlayer, gameDate);
        
        // Get komi
        Matcher komiMatcher = KOMI_PATTERN.matcher(content);
        if (komiMatcher.find()) {
            try {
                double komi = Double.parseDouble(komiMatcher.group(1));
                recorder.setKomi(komi);
            } catch (NumberFormatException e) {
                // Use default komi if parsing fails
            }
        }
        
        // Parse moves
        Matcher moveMatcher = MOVE_PATTERN.matcher(content);
        while (moveMatcher.find()) {
            String colorStr = moveMatcher.group(1);
            String coordStr = moveMatcher.group(2);
            
            Stone color = colorStr.equals("B") ? Stone.BLACK : Stone.WHITE;
            
            if (coordStr.isEmpty()) {
                // This is a pass
                recorder.recordPass(color);
            } else if (coordStr.equals("resign")) {
                // This is a resignation
                recorder.recordResign(color);
            } else {
                // This is a move
                try {
                    int x = coordStr.charAt(0) - 'a';
                    int y = coordStr.charAt(1) - 'a';
                    recorder.recordMove(new Point(x, y), color);
                } catch (Exception e) {
                    throw new ParseException("Invalid move coordinates: " + coordStr, 0);
                }
            }
        }
        
        return recorder;
    }
    
    /**
     * Extract property values from SGF content
     * 
     * @param content SGF content
     * @param pattern Regular expression pattern for the property
     * @return List of property values
     */
    private static List<String> extractProperties(String content, Pattern pattern) {
        List<String> values = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            values.add(matcher.group(2));
        }
        return values;
    }
}