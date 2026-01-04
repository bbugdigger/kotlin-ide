import javax.swing.*;
import javax.swing.text.*;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.BasicStroke;
import java.util.*;
import java.util.List;

public class CodeHighlighter {
    private final JTextPane textPane;
    
    private UnderlineHighlightPainter errorPainter;
    private UnderlineHighlightPainter warningPainter;
    private List<Object> underlineHighlights = new ArrayList<>();
    
    public CodeHighlighter(JTextPane textPane) {
        this.textPane = textPane;
        errorPainter = new UnderlineHighlightPainter(Color.RED);
        warningPainter = new UnderlineHighlightPainter(Color.YELLOW);
    }
    
    public void applyUnderlines(AnalysisResult result) {
        if (result == null) {
            clearUnderlines();
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                // Remove old underlines
                clearUnderlines();
                
                // Add new underlines
                StyledDocument document = textPane.getStyledDocument();
                String text = document.getText(0, document.getLength());
                Highlighter highlighter = textPane.getHighlighter();
                
                for (Diagnostic diag : result.getDiagnostics()) {
                    UnderlineHighlightPainter painter = getPainterForSeverity(diag.getSeverity());
                    
                    int startOffset = diag.getStartOffset();
                    int endOffset = diag.getEndOffset();
                    
                    // Validate offsets
                    if (startOffset >= 0 && endOffset <= text.length() && startOffset < endOffset) {
                        Object highlight = highlighter.addHighlight(startOffset, endOffset, painter);
                        underlineHighlights.add(highlight);
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private void clearUnderlines() {
        Highlighter highlighter = textPane.getHighlighter();
        for (Object highlight : underlineHighlights) {
            highlighter.removeHighlight(highlight);
        }
        underlineHighlights.clear();
    }
    
    private UnderlineHighlightPainter getPainterForSeverity(Diagnostic.Severity severity) {
        switch (severity) {
            case ERROR:   return errorPainter;
            case WARNING: return warningPainter;
            default:      return warningPainter;
        }
    }
    
    /**
     * Custom highlighter painter for straight underlines.
     */
    private static class UnderlineHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        private Color color;
        
        public UnderlineHighlightPainter(Color color) {
            super(null);
            this.color = color;
        }
        
        @Override
        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
            Graphics2D g2d = (Graphics2D) g;
            
            try {
                // Get the rectangle for the text region
                Rectangle r0 = c.modelToView2D(offs0).getBounds();
                Rectangle r1 = c.modelToView2D(offs1).getBounds();
                
                if (r0.y == r1.y) {
                    // Same line
                    Rectangle rect = r0.union(r1);
                    drawUnderline(g2d, rect.x, rect.y + rect.height - 2, rect.width);
                } else {
                    // Multi-line (just draw under first line)
                    int width = bounds.getBounds().x + bounds.getBounds().width - r0.x;
                    drawUnderline(g2d, r0.x, r0.y + r0.height - 2, width);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return null;
        }
        
        private void drawUnderline(Graphics2D g2d, int x, int y, int width) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawLine(x, y, x + width, y);
        }
    }
}

