package pcd.ass02.common;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FSStatView extends JFrame {

    public interface ViewListener {
        void onStartRequested(String dirPath, long maxFS, int nBands);
        void onStopRequested();
    }

    private final JTextField dirField;
    private final JTextField maxFSField;
    private final JTextField nBandsField;
    private final JButton startButton;
    private final JButton stopButton;
    private final JLabel statusLabel;
    private final JTextArea reportArea;
    private ViewListener listener;

    public FSStatView(final String titleSuffix,
                      final String defaultDir,
                      final long defaultMaxFS,
                      final int defaultNBands) {
        super("FSStat Interactive Monitor - " + titleSuffix);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 520);
        setLocationRelativeTo(null);
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration Parameters"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        configPanel.add(new JLabel("Directory (D):"), gbc);
        dirField = new JTextField(defaultDir, 30);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        configPanel.add(dirField, gbc);
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(dirField.getText());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                dirField.setText(selected.getAbsolutePath());
            }
        });
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        configPanel.add(browseButton, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        configPanel.add(new JLabel("Max File Size (MaxFS in bytes):"), gbc);
        maxFSField = new JTextField(String.valueOf(defaultMaxFS), 15);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        configPanel.add(maxFSField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        configPanel.add(new JLabel("Number of Bands (NB):"), gbc);
        nBandsField = new JTextField(String.valueOf(defaultNBands), 10);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        configPanel.add(nBandsField, gbc);
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        statusLabel = new JLabel("Status: Idle");
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(Box.createHorizontalStrut(15));
        controlPanel.add(statusLabel);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
        configPanel.add(controlPanel, gbc);
        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(reportArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Dynamic Statistics Report"));
        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(configPanel, BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        startButton.addActionListener(e -> {
            if (listener != null) {
                try {
                    String dir = dirField.getText().trim();
                    long maxFS = Long.parseLong(maxFSField.getText().trim());
                    int nBands = Integer.parseInt(nBandsField.getText().trim());
                    if (dir.isEmpty() || maxFS <= 0 || nBands <= 0) {
                        JOptionPane.showMessageDialog(this,
                                "Please provide a valid directory and positive values for MaxFS and NB.",
                                "Invalid Parameters",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    setRunningState(true, "Scanning");
                    reportArea.setText("Initializing scan on: " + dir + "\n");
                    listener.onStartRequested(dir, maxFS, nBands);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this,
                            "MaxFS and NB must be valid integer numbers!",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        stopButton.addActionListener(e -> {
            if (listener != null) {
                stopButton.setEnabled(false);
                statusLabel.setText("Status: Stopping");
                listener.onStopRequested();
            }
        });
    }

    public void setViewListener(final ViewListener listener) {
        this.listener = listener;
    }

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    public void updateReport(final FSReport report) {
        SwingUtilities.invokeLater(() -> reportArea.setText(report.toFormattedString()));
    }

    public void notifyCompleted(final FSReport finalReport, final long elapsedMs) {
        SwingUtilities.invokeLater(() -> {
            reportArea.setText(finalReport.toFormattedString()
                    + String.format("\nScan COMPLETED in %d ms", elapsedMs));
            setRunningState(false, "Completed (" + elapsedMs + " ms)");
        });
    }

    public void notifyStopped(final FSReport partialReport) {
        SwingUtilities.invokeLater(() -> {
            if (partialReport != null) {
                reportArea.setText(partialReport.toFormattedString()
                        + "\nScan STOPPED by user - Partial results shown");
            } else {
                reportArea.append("\nScan STOPPED by user");
            }
            setRunningState(false, "Stopped by user");
        });
    }

    public void notifyError(final Throwable err) {
        SwingUtilities.invokeLater(() -> {
            reportArea.append("\nERROR: " + err.getMessage());
            setRunningState(false, "Error");
        });
    }

    private void setRunningState(final boolean running, final String statusText) {
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
        statusLabel.setText("Status: " + statusText);
    }
}