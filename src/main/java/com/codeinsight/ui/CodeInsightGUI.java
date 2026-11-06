package com.codeinsight.ui;

import com.codeinsight.analyzer.ProjectScanner;
import com.codeinsight.config.UserPreferences;
import com.codeinsight.model.AnalysisResult;
import com.codeinsight.model.MethodMetrics;
import com.codeinsight.model.SkippedFileInfo;
import com.codeinsight.model.ThresholdConfig;
import com.codeinsight.report.CsvReportExporter;
import com.codeinsight.report.HtmlReportExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Swing Desktop GUI for Code Insight.
 * Employs SwingWorker for background processing to prevent UI freezing.
 */
public class CodeInsightGUI extends JFrame {

    private final JTextField pathField = new JTextField(35);
    private final JSpinner cycloSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
    private final JSpinner nestSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 50, 1));
    private final JSpinner linesSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 1000, 5));

    private final JButton analyzeButton = new JButton("Run Analysis");
    private final JButton browseFileButton = new JButton("Browse File...");
    private final JButton browseFolderButton = new JButton("Browse Folder...");
    private final JButton resetButton = new JButton("Reset Settings");

    private final JLabel statusLabel = new JLabel("Ready. Select a Java file or folder to analyze.");
    private final JProgressBar progressBar = new JProgressBar();

    private final JLabel scannedCountLabel = new JLabel("0");
    private final JLabel analyzedCountLabel = new JLabel("0");
    private final JLabel skippedCountLabel = new JLabel("0");
    private final JLabel methodsCountLabel = new JLabel("0");
    private final JLabel flaggedCountLabel = new JLabel("0");

    private final JTextField searchField = new JTextField(20);
    private final JCheckBox flaggedOnlyCheckBox = new JCheckBox("Show Flagged for Review Only");

    private final DefaultTableModel tableModel;
    private final JTable resultsTable;
    private final TableRowSorter<DefaultTableModel> tableSorter;

    private final JButton exportCsvButton = new JButton("Export CSV...");
    private final JButton exportHtmlButton = new JButton("Export HTML...");
    private final JButton viewSkippedButton = new JButton("View Skipped Files");

    private AnalysisResult currentResult = null;

    public CodeInsightGUI() {
        super("Code Insight: Source Code Complexity Analyzer (PBL Group Project)");

        // Set modern system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);

        // Load saved preferences
        ThresholdConfig savedThresholds = UserPreferences.loadThresholds();
        cycloSpinner.setValue(savedThresholds.maxComplexity());
        nestSpinner.setValue(savedThresholds.maxNesting());
        linesSpinner.setValue(savedThresholds.maxLines());
        pathField.setText(UserPreferences.loadLastPath());

        // Setup table model & columns
        String[] columns = {"File", "Class", "Method", "Range", "Lines", "Cyclomatic", "Nesting", "Status", "Remarks"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4 || columnIndex == 5 || columnIndex == 6) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setFillsViewportHeight(true);
        resultsTable.setRowHeight(24);
        tableSorter = new TableRowSorter<>(tableModel);
        resultsTable.setRowSorter(tableSorter);

        // Custom status badge renderer
        resultsTable.getColumnModel().getColumn(7).setCellRenderer(new StatusBadgeRenderer());

        // Layout components
        initLayout();
        initEventHandlers();

        // Update button states
        updateActionButtons();
    }

    private void initLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // 1. Top Panel: Header + Controls
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));

        // Header Title Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Code Insight: Source Code Complexity Analyzer");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitleLabel = new JLabel("Java Programming Group PBL Project - Standard JDK AST Analysis");
        subtitleLabel.setForeground(Color.GRAY);
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        headerPanel.setBorder(new EmptyBorder(0, 0, 8, 0));
        topPanel.add(headerPanel, BorderLayout.NORTH);

        // Settings & Input Box
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        controlsPanel.setBorder(new TitledBorder("Analysis Target & Review Thresholds"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1: Target Path
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        controlsPanel.add(new JLabel("Target Path:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        controlsPanel.add(pathField, gbc);

        JPanel browsePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        browsePanel.add(browseFileButton);
        browsePanel.add(browseFolderButton);
        gbc.gridx = 2; gbc.weightx = 0;
        controlsPanel.add(browsePanel, gbc);

        // Row 2: Thresholds and Run Button
        JPanel thresholdsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        thresholdsPanel.add(new JLabel("Max Cyclo:"));
        thresholdsPanel.add(cycloSpinner);
        thresholdsPanel.add(new JLabel("Max Nesting:"));
        thresholdsPanel.add(nestSpinner);
        thresholdsPanel.add(new JLabel("Max Lines:"));
        thresholdsPanel.add(linesSpinner);
        thresholdsPanel.add(resetButton);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        controlsPanel.add(thresholdsPanel, gbc);

        gbc.gridx = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        analyzeButton.setFont(analyzeButton.getFont().deriveFont(Font.BOLD, 13f));
        analyzeButton.setBackground(new Color(37, 99, 235));
        analyzeButton.setForeground(Color.WHITE);
        analyzeButton.setFocusPainted(false);
        controlsPanel.add(analyzeButton, gbc);

        topPanel.add(controlsPanel, BorderLayout.CENTER);

        // Progress bar and status label
        JPanel progressPanel = new JPanel(new BorderLayout(4, 4));
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressPanel.add(statusLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.SOUTH);
        topPanel.add(progressPanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 2. Center Panel: Stats Cards + Filter Bar + Table
        JPanel centerPanel = new JPanel(new BorderLayout(6, 6));

        // Stats Cards Bar
        JPanel statsPanel = new JPanel(new GridLayout(1, 5, 8, 8));
        statsPanel.setBorder(new EmptyBorder(4, 0, 4, 0));
        statsPanel.add(createStatCard("Files Scanned", scannedCountLabel, Color.DARK_GRAY));
        statsPanel.add(createStatCard("Files Analyzed", analyzedCountLabel, new Color(16, 120, 60)));
        statsPanel.add(createStatCard("Files Skipped", skippedCountLabel, new Color(180, 40, 40)));
        statsPanel.add(createStatCard("Total Methods", methodsCountLabel, Color.DARK_GRAY));
        statsPanel.add(createStatCard("Flagged for Review", flaggedCountLabel, new Color(220, 38, 38)));
        centerPanel.add(statsPanel, BorderLayout.NORTH);

        // Table with Search Bar
        JPanel tableContainer = new JPanel(new BorderLayout(4, 4));
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterBar.add(new JLabel("Filter Table:"));
        searchField.setToolTipText("Filter by file, class, or method name");
        filterBar.add(searchField);
        filterBar.add(flaggedOnlyCheckBox);
        tableContainer.add(filterBar, BorderLayout.NORTH);
        tableContainer.add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        centerPanel.add(tableContainer, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Panel: Actions (Export CSV, Export HTML, View Skipped)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel noteLabel = new JLabel("* A 'REVIEW' flag is a prompt for refactoring scrutiny, not proof of defective code.");
        noteLabel.setFont(noteLabel.getFont().deriveFont(Font.ITALIC, 11f));
        noteLabel.setForeground(Color.GRAY);
        bottomPanel.add(noteLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.add(viewSkippedButton);
        buttonPanel.add(exportCsvButton);
        buttonPanel.add(exportHtmlButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color valueColor) {
        JPanel card = new JPanel(new BorderLayout(2, 2));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(6, 8, 6, 8)));
        card.setBackground(Color.WHITE);

        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 10f));
        titleLbl.setForeground(Color.GRAY);

        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 18f));
        valueLabel.setForeground(valueColor);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void initEventHandlers() {
        browseFileButton.addActionListener(e -> chooseTarget(false));
        browseFolderButton.addActionListener(e -> chooseTarget(true));

        analyzeButton.addActionListener(e -> runAnalysisWorker());

        resetButton.addActionListener(e -> {
            cycloSpinner.setValue(ThresholdConfig.DEFAULT_MAX_COMPLEXITY);
            nestSpinner.setValue(ThresholdConfig.DEFAULT_MAX_NESTING);
            linesSpinner.setValue(ThresholdConfig.DEFAULT_MAX_LINES);
            UserPreferences.resetToDefaults();
            if (currentResult != null) {
                // Re-evaluate current results with default thresholds
                applyThresholdsToCurrentResult();
            }
        });

        // Instant table filter
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateTableFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { updateTableFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { updateTableFilter(); }
        });

        flaggedOnlyCheckBox.addActionListener(e -> updateTableFilter());

        // Real-time threshold adjustment on spinners
        cycloSpinner.addChangeListener(e -> applyThresholdsToCurrentResult());
        nestSpinner.addChangeListener(e -> applyThresholdsToCurrentResult());
        linesSpinner.addChangeListener(e -> applyThresholdsToCurrentResult());

        exportCsvButton.addActionListener(e -> exportReport(true));
        exportHtmlButton.addActionListener(e -> exportReport(false));
        viewSkippedButton.addActionListener(e -> showSkippedFilesDialog());
    }

    private void chooseTarget(boolean selectDirectory) {
        JFileChooser chooser = new JFileChooser(pathField.getText().trim());
        if (selectDirectory) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Java Project Folder");
        } else {
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setDialogTitle("Select Java Source File");
            chooser.setFileFilter(new FileNameExtensionFilter("Java Source Files (*.java)", "java"));
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void runAnalysisWorker() {
        String pathStr = pathField.getText().trim();
        if (pathStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter or browse to a Java file or directory to analyze.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path target = Path.of(pathStr);
        if (!Files.exists(target)) {
            JOptionPane.showMessageDialog(this,
                    "The specified path does not exist:\n" + target.toAbsolutePath(),
                    "Path Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int cyclo = (Integer) cycloSpinner.getValue();
        int nest = (Integer) nestSpinner.getValue();
        int lines = (Integer) linesSpinner.getValue();
        ThresholdConfig config = new ThresholdConfig(cyclo, nest, lines);

        // Persist preferences
        UserPreferences.saveThresholds(config);
        UserPreferences.saveLastPath(pathStr);

        // UI state for background execution
        setControlsEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(0);
        statusLabel.setText("Scanning directory and discovering Java files...");

        SwingWorker<AnalysisResult, String> worker = new SwingWorker<>() {
            @Override
            protected AnalysisResult doInBackground() throws Exception {
                return ProjectScanner.scan(target, config, (current, total, file) -> {
                    int percent = (int) (((double) current / Math.max(1, total)) * 100);
                    progressBar.setValue(percent);
                    statusLabel.setText(String.format("Analyzing [%d/%d]: %s", current, total, file));
                });
            }

            @Override
            protected void done() {
                try {
                    currentResult = get();
                    displayResults(currentResult);
                    statusLabel.setText(String.format(
                            "Analysis completed in %d ms (%d methods found across %d analyzed file(s)).",
                            currentResult.durationMillis(), currentResult.metrics().size(), currentResult.analyzedFilesCount()));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CodeInsightGUI.this,
                            "Analysis failed: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Analysis failed: " + ex.getMessage());
                } finally {
                    setControlsEnabled(true);
                    progressBar.setVisible(false);
                    updateActionButtons();
                }
            }
        };

        worker.execute();
    }

    private void displayResults(AnalysisResult result) {
        tableModel.setRowCount(0);

        for (MethodMetrics m : result.metrics()) {
            boolean review = result.config().isReviewRequired(m);
            String remarks = result.config().getReviewRemarks(m);

            tableModel.addRow(new Object[]{
                    m.fileName(),
                    m.className(),
                    m.methodName(),
                    m.lineRange(),
                    (int) m.lineCount(),
                    m.cyclomaticComplexity(),
                    m.maximumNestingDepth(),
                    review ? "REVIEW" : "OK",
                    remarks
            });
        }

        // Update statistics cards
        scannedCountLabel.setText(String.valueOf(result.totalFilesScanned()));
        analyzedCountLabel.setText(String.valueOf(result.analyzedFilesCount()));
        skippedCountLabel.setText(String.valueOf(result.skippedFiles().size()));
        methodsCountLabel.setText(String.valueOf(result.metrics().size()));
        flaggedCountLabel.setText(String.valueOf(result.flaggedMethodsCount()));

        updateActionButtons();
    }

    private void applyThresholdsToCurrentResult() {
        if (currentResult == null) return;

        int cyclo = (Integer) cycloSpinner.getValue();
        int nest = (Integer) nestSpinner.getValue();
        int lines = (Integer) linesSpinner.getValue();
        ThresholdConfig newConfig = new ThresholdConfig(cyclo, nest, lines);

        currentResult = new AnalysisResult(
                currentResult.targetPath(),
                newConfig,
                currentResult.metrics(),
                currentResult.skippedFiles(),
                currentResult.totalFilesScanned(),
                currentResult.durationMillis());

        displayResults(currentResult);
    }

    private void updateTableFilter() {
        String query = searchField.getText().trim().toLowerCase();
        boolean flaggedOnly = flaggedOnlyCheckBox.isSelected();

        RowFilter<DefaultTableModel, Object> filter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ?> entry) {
                if (flaggedOnly) {
                    String status = entry.getStringValue(7);
                    if (!"REVIEW".equalsIgnoreCase(status)) return false;
                }
                if (query.isEmpty()) return true;

                // Match against File (0), Class (1), or Method (2)
                String file = entry.getStringValue(0).toLowerCase();
                String clazz = entry.getStringValue(1).toLowerCase();
                String method = entry.getStringValue(2).toLowerCase();
                return file.contains(query) || clazz.contains(query) || method.contains(query);
            }
        };

        tableSorter.setRowFilter(filter);
    }

    private void setControlsEnabled(boolean enabled) {
        analyzeButton.setEnabled(enabled);
        browseFileButton.setEnabled(enabled);
        browseFolderButton.setEnabled(enabled);
        pathField.setEnabled(enabled);
        cycloSpinner.setEnabled(enabled);
        nestSpinner.setEnabled(enabled);
        linesSpinner.setEnabled(enabled);
        resetButton.setEnabled(enabled);
    }

    private void updateActionButtons() {
        boolean hasResults = currentResult != null && !currentResult.metrics().isEmpty();
        exportCsvButton.setEnabled(hasResults);
        exportHtmlButton.setEnabled(hasResults);
        viewSkippedButton.setEnabled(currentResult != null && !currentResult.skippedFiles().isEmpty());
    }

    private void exportReport(boolean isCsv) {
        if (currentResult == null) return;

        JFileChooser chooser = new JFileChooser();
        String defaultName = isCsv ? "complexity-report.csv" : "complexity-report.html";
        chooser.setSelectedFile(new File(defaultName));
        chooser.setDialogTitle(isCsv ? "Save CSV Report" : "Save HTML Report");
        chooser.setFileFilter(isCsv
                ? new FileNameExtensionFilter("CSV Spreadsheet (*.csv)", "csv")
                : new FileNameExtensionFilter("HTML Web Page (*.html)", "html"));

        int choice = chooser.showSaveDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            Path file = chooser.getSelectedFile().toPath();
            try {
                if (isCsv) {
                    CsvReportExporter.export(currentResult, file);
                } else {
                    HtmlReportExporter.export(currentResult, file);
                }

                int openChoice = JOptionPane.showConfirmDialog(this,
                        "Report successfully saved to:\n" + file.toAbsolutePath() + "\n\nWould you like to open it now?",
                        "Export Successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                if (openChoice == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file.toFile());
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to export report: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showSkippedFilesDialog() {
        if (currentResult == null || currentResult.skippedFiles().isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("The following files encountered syntax errors or could not be read.\n");
        sb.append("They were skipped without interrupting the analysis of other files:\n\n");

        for (SkippedFileInfo skipped : currentResult.skippedFiles()) {
            sb.append("• ").append(skipped.fileName()).append(" (").append(skipped.filePath()).append("):\n");
            sb.append("  ").append(skipped.reason().replace("\n", "\n  ")).append("\n\n");
        }

        JTextArea textArea = new JTextArea(sb.toString(), 16, 60);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);

        JOptionPane.showMessageDialog(this, scrollPane, "Skipped Files Details", JOptionPane.WARNING_MESSAGE);
    }
}

