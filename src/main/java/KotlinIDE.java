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

    public KotlinIDE() {
        setTitle("Kotlin IDE");
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
        editorPane.setBackground(Color.DARK_GRAY);
        syntaxHighlighter = new SyntaxHighlighter(editorPane);

        // Initialize analysis components
//        kotlinAnalyzer = new KotlinAnalyzer();
//        completionEngine = new CompletionEngine();
//        completionPopup = new CompletionPopup(this, editorPane);
//        inspectionPanel = new InspectionPanel(editorPane);

        lineNumberArea = new JTextArea("1");
        lineNumberArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lineNumberArea.setEditable(false);
        lineNumberArea.setBackground(Color.DARK_GRAY);
        lineNumberArea.setForeground(Color.WHITE);
        lineNumberArea.setBorder(new EmptyBorder(0, 5, 0, 5));

        outputPane = new JTextPane();
        outputPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputPane.setEditable(false);
        outputPane.setBackground(Color.BLACK);

        // Initialize text styles for colored output
        stdoutStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(stdoutStyle, Color.WHITE);
        stderrStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(stderrStyle, Color.RED); // Red color for errors

        // Make error locations clickable
        outputPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
//                    handleOutputClick(e.getPoint());
                }
            }
        });
        outputPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        runButton = new JButton("Run");
        runButton.setFont(new Font("Arial", Font.BOLD, 14));
        runButton.setBackground(Color.GREEN);
        runButton.setForeground(Color.WHITE);
        runButton.setFocusPainted(false);
        runButton.setBorderPainted(false);
        runButton.setOpaque(true);

        stopButton = new JButton("Stop");
        stopButton.setFont(new Font("Arial", Font.BOLD, 14));
        stopButton.setBackground(Color.RED);
        stopButton.setForeground(Color.WHITE);
        stopButton.setFocusPainted(false);
        stopButton.setBorderPainted(false);
        stopButton.setOpaque(true);
        stopButton.setEnabled(false); // will be enabled when script starts running

        clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearButton.setBackground(Color.RED);
        clearButton.setForeground(Color.WHITE);  // White text
        clearButton.setFocusPainted(false);
        clearButton.setBorderPainted(false);
        clearButton.setOpaque(true);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.WHITE);

        exitCodeLabel = new JLabel(" ");
        exitCodeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        exitCodeLabel.setForeground(Color.WHITE);

        // Add sample script
        editorPane.setText(
                "// Kotlin Script Example\n" +
                        "println(\"Hello from Kotlin!\")\n" +
                        "\n" +
                        "for (i in 1..5) {\n" +
                        "    println(\"Count: $i\")\n" +
                        "    Thread.sleep(500)\n" +
                        "}\n" +
                        "\n" +
                        "println(\"Done!\")"
        );

        updateLineNumbers();
    }

    private void placeComponents() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.DARK_GRAY);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setBackground(Color.DARK_GRAY);
        controlPanel.add(runButton);
        controlPanel.add(stopButton);
        controlPanel.add(clearButton);
        controlPanel.add(new JLabel("   "));
        controlPanel.add(statusLabel);
        controlPanel.add(new JLabel("   "));
        controlPanel.add(exitCodeLabel);

        add(controlPanel, BorderLayout.NORTH);

        // Split pane for script editor and script output
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);
        splitPane.setBackground(Color.DARK_GRAY);

        // Left side: Script Editor with line numbers
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBackground(Color.DARK_GRAY);
        TitledBorder editorBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80)),
                "Kotlin Script Editor",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12),
                new Color(200, 200, 200)  // Light text for title
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
        outputPanel.setBackground(Color.DARK_GRAY);
        TitledBorder outputBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80)),
                "Script Output",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12),
                new Color(200, 200, 200)  // Light text for title
        );
        outputPanel.setBorder(outputBorder);

        JScrollPane outputScroll = new JScrollPane(outputPane);
        outputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        outputScroll.getViewport().setBackground(new Color(30, 30, 30));
        outputPanel.add(outputScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(editorPanel);
        splitPane.setRightComponent(outputPanel);

        add(splitPane, BorderLayout.CENTER);
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
        // We need to update line numbers as user types
        editorPane.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateLineNumbers();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateLineNumbers();
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

    private void runScript() {
        String scriptContent = editorPane.getText();

        if (scriptContent.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "There is nothing to run.",
                    "Empty Script",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Disable run button, enable stop button
        runButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("Running...");
        statusLabel.setForeground(new Color(76, 175, 80));
        exitCodeLabel.setText(" ");

        if (!outputPane.getText().isEmpty()) {
            appendToOutput("\n" + "=".repeat(100) + "\n", stdoutStyle);
        }
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
                    statusLabel.setForeground(Color.GRAY);

                    if (exitCode == 0) {
                        exitCodeLabel.setText("Exit Code: 0 (Success)");
                        exitCodeLabel.setForeground(new Color(76, 175, 80));
                    } else {
                        exitCodeLabel.setText("Exit Code: " + exitCode + " (Error)");
                        exitCodeLabel.setForeground(new Color(244, 67, 54));
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
            appendToOutput("\n>>> Script stopped\n", stdoutStyle);
            statusLabel.setText("Stopped");
            statusLabel.setForeground(Color.RED);
            runButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }
}
