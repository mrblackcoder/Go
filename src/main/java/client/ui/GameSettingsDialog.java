package client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Dialog for game settings configuration
 */
public class GameSettingsDialog extends JDialog {
    
    private final JComboBox<Integer> boardSizeCombo;
    private final JComboBox<Integer> handicapCombo;
    private final JSpinner timeControlSpinner;
    private final JCheckBox komiCheckbox;
    private final JSpinner komiSpinner;
    private final JCheckBox soundCheckbox;
    
    private boolean confirmed = false;
    
    /**
     * Creates a new game settings dialog
     * 
     * @param owner The parent frame
     */
    public GameSettingsDialog(Frame owner) {
        super(owner, "Game Settings", true);
        
        // Create components
        boardSizeCombo = new JComboBox<>(new Integer[]{9, 13, 19});
        boardSizeCombo.setSelectedItem(19); // Default
        
        handicapCombo = new JComboBox<>(new Integer[]{0, 2, 3, 4, 5, 6, 7, 8, 9});
        handicapCombo.setSelectedItem(0); // Default
        
        timeControlSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 120, 1));
        
        komiCheckbox = new JCheckBox("Use Komi");
        komiCheckbox.setSelected(true);
        
        komiSpinner = new JSpinner(new SpinnerNumberModel(6.5, 0.0, 10.0, 0.5));
        komiSpinner.setEnabled(komiCheckbox.isSelected());
        
        soundCheckbox = new JCheckBox("Enable Sound Effects");
        soundCheckbox.setSelected(true);
        
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        // Layout
        setLayout(new BorderLayout());
        
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Board Size
        gbc.gridx = 0;
        gbc.gridy = 0;
        settingsPanel.add(new JLabel("Board Size:"), gbc);
        
        gbc.gridx = 1;
        settingsPanel.add(boardSizeCombo, gbc);
        
        // Handicap
        gbc.gridx = 0;
        gbc.gridy = 1;
        settingsPanel.add(new JLabel("Handicap:"), gbc);
        
        gbc.gridx = 1;
        settingsPanel.add(handicapCombo, gbc);
        
        // Time Control
        gbc.gridx = 0;
        gbc.gridy = 2;
        settingsPanel.add(new JLabel("Time Control (minutes):"), gbc);
        
        gbc.gridx = 1;
        settingsPanel.add(timeControlSpinner, gbc);
        
        // Komi
        gbc.gridx = 0;
        gbc.gridy = 3;
        settingsPanel.add(komiCheckbox, gbc);
        
        gbc.gridx = 1;
        settingsPanel.add(komiSpinner, gbc);
        
        // Sound
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        settingsPanel.add(soundCheckbox, gbc);
        
        // Add a separator
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        settingsPanel.add(new JSeparator(), gbc);
        
        // Add explanation labels for settings
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        JTextArea explanationText = new JTextArea(
            "Board Size: 19x19 is standard, 13x13 and 9x9 are for quicker games.\n" +
            "Handicap: Stones given to weaker player at start (Black).\n" +
            "Komi: Points given to White to compensate for Black's first move advantage.\n" +
            "Time Control: Maximum time per player."
        );
        explanationText.setEditable(false);
        explanationText.setBackground(settingsPanel.getBackground());
        explanationText.setWrapStyleWord(true);
        explanationText.setLineWrap(true);
        explanationText.setFont(new Font("SansSerif", Font.ITALIC, 12));
        JScrollPane scrollPane = new JScrollPane(explanationText);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(300, 80));
        settingsPanel.add(scrollPane, gbc);
        
        // Add to dialog
        add(settingsPanel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Event handlers
        komiCheckbox.addActionListener((ActionEvent e) -> {
            komiSpinner.setEnabled(komiCheckbox.isSelected());
        });
        
        okButton.addActionListener((ActionEvent e) -> {
            confirmed = true;
            dispose();
        });
        
        cancelButton.addActionListener((ActionEvent e) -> {
            dispose();
        });
        
        // Dialog properties
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }
    
    /**
     * Check if the settings were confirmed
     * 
     * @return True if confirmed, false if canceled
     */
    public boolean isConfirmed() {
        return confirmed;
    }
    
    /**
     * Get the selected board size
     * 
     * @return Board size (9, 13, or 19)
     */
    public int getBoardSize() {
        return (Integer) boardSizeCombo.getSelectedItem();
    }
    
    /**
     * Get the selected handicap
     * 
     * @return Handicap stones (0-9)
     */
    public int getHandicap() {
        return (Integer) handicapCombo.getSelectedItem();
    }
    
    /**
     * Get the selected time control
     * 
     * @return Time control in minutes
     */
    public int getTimeControl() {
        return (Integer) timeControlSpinner.getValue();
    }
    
    /**
     * Get the selected komi value
     * 
     * @return Komi value (or 0 if disabled)
     */
    public double getKomi() {
        return komiCheckbox.isSelected() ? (Double) komiSpinner.getValue() : 0.0;
    }
    
    /**
     * Check if sound effects are enabled
     * 
     * @return True if sound effects are enabled
     */
    public boolean isSoundEnabled() {
        return soundCheckbox.isSelected();
    }
    
    /**
     * Show the settings dialog and return the dialog instance
     * 
     * @param owner Parent frame
     * @return The dialog instance
     */
    public static GameSettingsDialog showDialog(Frame owner) {
        GameSettingsDialog dialog = new GameSettingsDialog(owner);
        dialog.setVisible(true);
        return dialog;
    }
}