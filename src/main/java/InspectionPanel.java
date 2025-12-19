import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class InspectionPanel extends JPanel {
    private JTable inspectionTable;
    private InspectionTableModel tableModel;
    private JLabel statusLabel;
    private JTextPane editorPane;
    
    public InspectionPanel(JTextPane editorPane) {
        this.editorPane = editorPane;
        
        setLayout(new BorderLayout());
        setBackground(ColorPalette.BACKGROUND_DARK);
        
        initComponents();
    }
    
    private void initComponents() {
        // Status label showing counts
        statusLabel = new JLabel(" No issues");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Table for inspections
        tableModel = new InspectionTableModel();
        inspectionTable = new JTable(tableModel);
        
        // Dark theme styling
        inspectionTable.setBackground(ColorPalette.TABLE_BACKGROUND);
        inspectionTable.setForeground(Color.WHITE);
        inspectionTable.setGridColor(ColorPalette.GRID_COLOR);
        inspectionTable.setSelectionBackground(ColorPalette.SELECTION_BACKGROUND);
        inspectionTable.setSelectionForeground(Color.WHITE);
        inspectionTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        inspectionTable.setRowHeight(25);
        inspectionTable.setShowGrid(true);
        
        // Column widths
        inspectionTable.getColumnModel().getColumn(0).setPreferredWidth(30);  // Severity
        inspectionTable.getColumnModel().getColumn(1).setPreferredWidth(50);  // Line
        inspectionTable.getColumnModel().getColumn(2).setPreferredWidth(300); // Message
        
        // Custom cell renderer for icons and colors
        inspectionTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                // Dark theme colors
                if (!isSelected) {
                    label.setBackground(ColorPalette.TABLE_BACKGROUND);
                    label.setForeground(Color.WHITE);
                }
                
                // Icon column
                if (column == 0) {
                    Diagnostic diag = tableModel.getDiagnosticAt(row);
                    if (diag != null) {
                        label.setText(getIconForSeverity(diag.getSeverity()));
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                }
                
                // Message column - color by severity
                if (column == 2) {
                    Diagnostic diag = tableModel.getDiagnosticAt(row);
                    if (diag != null && !isSelected) {
                        Color color = getColorForSeverity(diag.getSeverity());
                        label.setForeground(color);
                    }
                }
                
                return label;
            }
        });
        
        // Click to navigate to issue
        inspectionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = inspectionTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    Diagnostic diag = tableModel.getDiagnosticAt(row);
                    if (diag != null) {
                        navigateToIssue(diag);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(inspectionTable);
        scrollPane.setBorder(new LineBorder(ColorPalette.GRID_COLOR));
        scrollPane.getViewport().setBackground(ColorPalette.TABLE_BACKGROUND);
        
        // Layout
        add(statusLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Border with title
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ColorPalette.GRID_COLOR),
            "Inspections",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
                ColorPalette.TITLE_COLOR
        );
        setBorder(border);
    }
    
    private String getIconForSeverity(Diagnostic.Severity severity) {
        switch (severity) {
            case ERROR:   return "ERROR";
            case WARNING: return "WARNING";
            default:      return "INFO";
        }
    }
    
    private Color getColorForSeverity(Diagnostic.Severity severity) {
        switch (severity) {
            case ERROR:   return Color.RED;   // Red
            case WARNING: return Color.YELLOW;   // Orange
            default:      return Color.WHITE; // Default light gray
        }
    }
    
    public void updateInspections(AnalysisResult result) {
        if (result == null) {
            tableModel.clear();
            statusLabel.setText(" No analysis available");
            return;
        }
        
        List<Diagnostic> diagnostics = result.getDiagnostics();
        tableModel.setDiagnostics(diagnostics);
        
        // Update status label
        int errors = result.getErrorCount();
        int warnings = result.getWarningCount();
        
        if (errors == 0 && warnings == 0) {
            statusLabel.setText("No issues found");
            statusLabel.setForeground(Color.GREEN); // Green
        } else {
            StringBuilder sb = new StringBuilder(" ");
            if (errors > 0) {
                sb.append(errors).append(" error").append(errors > 1 ? "s" : "");
            }
            if (warnings > 0) {
                if (errors > 0) sb.append("  ");
                sb.append(warnings).append(" warning").append(warnings > 1 ? "s" : "");
            }
            statusLabel.setText(sb.toString());
            
            if (errors > 0) {
                statusLabel.setForeground(Color.RED);
            } else {
                statusLabel.setForeground(Color.YELLOW);
            }
        }
    }
    
    private void navigateToIssue(Diagnostic diag) {
        try {
            // Calculate offset from line and column
            String text = editorPane.getText();
            String[] lines = text.split("\n", -1);
            
            int line = diag.getLine();
            int column = diag.getColumn();
            
            if (line < 1 || line > lines.length) {
                return;
            }
            
            // Calculate offset
            int offset = 0;
            for (int i = 0; i < line - 1; i++) {
                offset += lines[i].length() + 1; // +1 for newline
            }
            offset += Math.max(0, Math.min(column - 1, lines[line - 1].length()));
            
            // Set caret position and highlight
            editorPane.setCaretPosition(offset);
            editorPane.requestFocusInWindow();
            
            // Highlight the line briefly
            highlightLine(line);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void highlightLine(int lineNumber) {
        try {
            String text = editorPane.getText();
            String[] lines = text.split("\n", -1);
            
            if (lineNumber < 1 || lineNumber > lines.length) {
                return;
            }
            
            int offset = 0;
            for (int i = 0; i < lineNumber - 1; i++) {
                offset += lines[i].length() + 1;
            }
            
            final int finalOffset = offset;
            int lineLength = lines[lineNumber - 1].length();
            
            editorPane.select(offset, offset + lineLength);
            
            // Remove selection after 1 second
            javax.swing.Timer timer = new javax.swing.Timer(1000, e -> editorPane.select(finalOffset, finalOffset));
            timer.setRepeats(false);
            timer.start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Table model for inspections.
     */
    private static class InspectionTableModel extends AbstractTableModel {
        private List<Diagnostic> diagnostics = new ArrayList<>();
        private final String[] columnNames = {"", "Line", "Message"};
        
        public void setDiagnostics(List<Diagnostic> diagnostics) {
            this.diagnostics = new ArrayList<>(diagnostics);
            
            // Sort by severity (errors first) then by line
            this.diagnostics.sort((a, b) -> {
                int severityCompare = a.getSeverity().compareTo(b.getSeverity());
                if (severityCompare != 0) {
                    return severityCompare;
                }
                return Integer.compare(a.getLine(), b.getLine());
            });
            
            fireTableDataChanged();
        }
        
        public void clear() {
            diagnostics.clear();
            fireTableDataChanged();
        }
        
        public Diagnostic getDiagnosticAt(int row) {
            if (row >= 0 && row < diagnostics.size()) {
                return diagnostics.get(row);
            }
            return null;
        }
        
        @Override
        public int getRowCount() {
            return diagnostics.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            if (row >= diagnostics.size()) {
                return "";
            }
            
            Diagnostic diag = diagnostics.get(row);
            switch (column) {
                case 0: return getIconForSeverity(diag.getSeverity());
                case 1: return String.valueOf(diag.getLine());
                case 2: return diag.getMessage();
                default: return "";
            }
        }
        
        private String getIconForSeverity(Diagnostic.Severity severity) {
            switch (severity) {
                case ERROR:   return "ERROR";
                case WARNING: return "WARNING";
                default:      return "•";
            }
        }
    }
}

