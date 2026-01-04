import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KotlinIDE extends JFrame {
    private JTextPane editorPane;
    private JTextPane outputPane;
    private JTextArea lineNumberArea;
    private SimpleAttributeSet stdoutStyle;
    private SimpleAttributeSet stderrStyle;

    private JButton runButton;
    private JButton stopButton;
    private JButton clearButton;

    private JLabel statusLabel;
    private JLabel exitCodeLabel;

    private ScriptExecutor scriptExecutor;
    private SyntaxHighlighter syntaxHighlighter;
    private KotlinAnalyzer kotlinAnalyzer;
    private InspectionPanel inspectionPanel;
    private CodeHighlighter codeHighlighter;
    
    // Regex to match error locations: filename:line:column
    private static final Pattern ERROR_LOCATION_PATTERN = Pattern.compile("(\\w+\\.kts):(\\d+):(\\d+)");

    public KotlinIDE() {
        setTitle("Mini IntelliJ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1500, 800);
        setLocationRelativeTo(null);

        initComponents();
        placeComponents();
        attachListeners();

        setVisible(true);
    }

    private void initComponents() {
        editorPane = new JTextPane();
        editorPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        editorPane.setBackground(ColorPalette.BACKGROUND_COLOR);
        editorPane.setCaretColor(ColorPalette.TEXT_COLOR);
        syntaxHighlighter = new SyntaxHighlighter(editorPane);

        // Initialize analysis components
        kotlinAnalyzer = new KotlinAnalyzer();
        inspectionPanel = new InspectionPanel(editorPane);
        codeHighlighter = new CodeHighlighter(editorPane);

        lineNumberArea = new JTextArea("1");
        lineNumberArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lineNumberArea.setEditable(false);
        lineNumberArea.setBackground(ColorPalette.BACKGROUND_COLOR);
        lineNumberArea.setForeground(ColorPalette.TEXT_COLOR);
        lineNumberArea.setBorder(new EmptyBorder(0, 5, 0, 5));

        outputPane = new JTextPane();
        outputPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputPane.setEditable(false);
        outputPane.setBackground(ColorPalette.OUTPUT_BACKGROUND);

        // Initialize text styles for colored output
        stdoutStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(stdoutStyle, ColorPalette.TEXT_COLOR);
        stderrStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(stderrStyle, ColorPalette.ERROR_COLOR);

        // Make error locations clickable
        outputPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    handleOutputClick(e.getPoint());
                }
            }
        });
        outputPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        runButton = new JButton("Run");
        runButton.setFont(new Font("Arial", Font.BOLD, 14));
        runButton.setBackground(ColorPalette.SUCCESS_COLOR);
        runButton.setForeground(ColorPalette.TEXT_COLOR);
        runButton.setFocusPainted(false);
        runButton.setBorderPainted(false);
        runButton.setOpaque(true);

        stopButton = new JButton("Stop");
        stopButton.setFont(new Font("Arial", Font.BOLD, 14));
        stopButton.setBackground(ColorPalette.ERROR_COLOR);
        stopButton.setForeground(ColorPalette.TEXT_COLOR);
        stopButton.setFocusPainted(false);
        stopButton.setBorderPainted(false);
        stopButton.setOpaque(true);
        stopButton.setEnabled(false); // will be enabled when script starts running

        clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearButton.setBackground(ColorPalette.ERROR_COLOR);
        clearButton.setForeground(ColorPalette.TEXT_COLOR);
        clearButton.setFocusPainted(false);
        clearButton.setBorderPainted(false);
        clearButton.setOpaque(true);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(ColorPalette.TEXT_COLOR);

        exitCodeLabel = new JLabel(" ");
        exitCodeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        exitCodeLabel.setForeground(ColorPalette.TEXT_COLOR);

        editorPane.setText("// Add Kotlin Script Below...\n");

        updateLineNumbers();
    }

    private void placeComponents() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(ColorPalette.BACKGROUND_COLOR);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setBackground(ColorPalette.BACKGROUND_COLOR);
        controlPanel.add(runButton);
        controlPanel.add(stopButton);
        controlPanel.add(clearButton);
        controlPanel.add(new JLabel("   "));
        controlPanel.add(statusLabel);
        controlPanel.add(new JLabel("   "));
        controlPanel.add(exitCodeLabel);

        add(controlPanel, BorderLayout.NORTH);

        // First pane split is for editor/output and inspection panel
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.8);
        mainSplitPane.setBackground(ColorPalette.BACKGROUND_COLOR);

        // Second pane split is for script editor and script output
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);
        splitPane.setBackground(ColorPalette.BACKGROUND_COLOR);

        // Left side: Script Editor with line numbers
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBackground(ColorPalette.BACKGROUND_COLOR);
        TitledBorder editorBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(),
                "Kotlin Script Editor",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12),
                ColorPalette.TEXT_COLOR
        );
        editorPanel.setBorder(editorBorder);

        JPanel editorWithLines = new JPanel(new BorderLayout());
        editorWithLines.add(lineNumberArea, BorderLayout.WEST);

        JScrollPane editorScroll = new JScrollPane(editorPane);
        editorScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorWithLines.add(editorScroll, BorderLayout.CENTER);

        // Sync line numbers with editor scrolling
        editorScroll.getVerticalScrollBar().addAdjustmentListener(e -> {
            JScrollPane lineScroll = (JScrollPane) lineNumberArea.getParent().getParent();
            lineScroll.getVerticalScrollBar().setValue(e.getValue());
        });

        JScrollPane lineScroll = new JScrollPane(lineNumberArea);
        lineScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        lineScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        editorWithLines.remove(lineNumberArea);
        editorWithLines.add(lineScroll, BorderLayout.WEST);

        editorPanel.add(editorWithLines, BorderLayout.CENTER);

        // Right side: Script Output
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBackground(ColorPalette.BACKGROUND_COLOR);
        TitledBorder outputBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(),
                "Script Output",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12),
                ColorPalette.TEXT_COLOR
        );
        outputPanel.setBorder(outputBorder);

        JScrollPane outputScroll = new JScrollPane(outputPane);
        outputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        outputScroll.getViewport().setBackground(ColorPalette.OUTPUT_BACKGROUND);
        outputPanel.add(outputScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(editorPanel);
        splitPane.setRightComponent(outputPanel);

        // Inspection panel goes at the bottom
        mainSplitPane.setTopComponent(splitPane);
        mainSplitPane.setBottomComponent(inspectionPanel);
        mainSplitPane.setDividerLocation(500);

        add(mainSplitPane, BorderLayout.CENTER);
    }

    private void updateLineNumbers() {
        String text = editorPane.getText();
        int lines = text.split("\n", -1).length;

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            sb.append(i).append("\n");
        }

        lineNumberArea.setText(sb.toString());
    }

    private void appendToOutput(String text, SimpleAttributeSet style) {
        try {
            StyledDocument doc = outputPane.getStyledDocument();
            doc.insertString(doc.getLength(), text, style);
            outputPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void attachListeners() {
        // As user types the script we need to update the lines and do backend analysis
        editorPane.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateLineNumbers();
                triggerAnalysis();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateLineNumbers();
                triggerAnalysis();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateLineNumbers();
            }
        });

        runButton.addActionListener(e -> runScript());
        stopButton.addActionListener(e -> stopScript());
        clearButton.addActionListener(e -> outputPane.setText(""));
    }
    
    private void triggerAnalysis() {
        String code = editorPane.getText();
        
        // Analyze asynchronously
        kotlinAnalyzer.analyzeAsync(code, result -> {
            SwingUtilities.invokeLater(() -> {
                inspectionPanel.updateInspections(result);
                codeHighlighter.applyUnderlines(result);
            });
        });
    }

    private void runScript() {
        String scriptContent = editorPane.getText();

        if (scriptContent.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "There is nothing to run.",
                    "Empty Script",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        runButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("Running...");
        statusLabel.setForeground(ColorPalette.SUCCESS_COLOR);
        exitCodeLabel.setText(" ");
        outputPane.setText("");
        appendToOutput(">>> Starting script execution...\n\n", stdoutStyle);

        // Create and start script executor
        scriptExecutor = new ScriptExecutor(scriptContent, new ScriptExecutor.OutputListener() {
            @Override
            public void onOutput(String line) {
                SwingUtilities.invokeLater(() -> {
                    appendToOutput(line + "\n", stdoutStyle);
                });
            }

            @Override
            public void onError(String line) {
                SwingUtilities.invokeLater(() -> {
                    appendToOutput(line + "\n", stderrStyle);
                });
            }

            @Override
            public void onComplete(int exitCode) {
                SwingUtilities.invokeLater(() -> {
                    runButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    statusLabel.setText("Finished");
                    statusLabel.setForeground(ColorPalette.TEXT_COLOR);

                    if (exitCode == 0) {
                        exitCodeLabel.setText("Exit Code: 0");
                        exitCodeLabel.setForeground(ColorPalette.SUCCESS_COLOR);
                    } else {
                        exitCodeLabel.setText("Exit Code: " + exitCode);
                        exitCodeLabel.setForeground(ColorPalette.ERROR_COLOR);
                    }

                    appendToOutput("\n>>> Script finished with exit code: " + exitCode + "\n", stdoutStyle);
                });
            }
        });

        scriptExecutor.start();
    }

    private void stopScript() {
        if (scriptExecutor != null) {
            scriptExecutor.stop();
            appendToOutput("\n>>> Script execution stopped\n", stderrStyle);
            statusLabel.setText("Stopped");
            statusLabel.setForeground(ColorPalette.ERROR_COLOR);
            runButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }
    
    private void handleOutputClick(Point point) {
        try {
            int offset = outputPane.viewToModel2D(point);
            StyledDocument doc = outputPane.getStyledDocument();
            
            // Find the line containing the click
            String text = doc.getText(0, doc.getLength());
            int lineStart = text.lastIndexOf('\n', offset - 1) + 1;
            int lineEnd = text.indexOf('\n', offset);
            String line = text.substring(lineStart, lineEnd);
            
            Matcher matcher = ERROR_LOCATION_PATTERN.matcher(line);
            if (matcher.find()) {
                int errorLine = Integer.parseInt(matcher.group(2));
                int errorColumn = Integer.parseInt(matcher.group(3));
                navigateToPosition(errorLine, errorColumn);
            }
        } catch (BadLocationException ex) {
            // Ignore clicks that don't match error pattern
        }
    }
    
    private void navigateToPosition(int line, int column) {
        try {
            String text = editorPane.getText();
            String[] lines = text.split("\n", -1);
            
            if (line < 1 || line > lines.length)
                return;

            // Calculate offset
            int offset = 0;
            for (int i = 0; i < line - 1; i++) {
                offset += lines[i].length() + 1; // +1 for "\n" character
            }
            offset += Math.max(0, Math.min(column - 1, lines[line - 1].length()));
            
            editorPane.setCaretPosition(offset);
            editorPane.requestFocusInWindow();

            int lineLength = lines[line - 1].length();
            editorPane.select(offset, offset + lineLength); //Highlight the line
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
