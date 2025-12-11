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

/**
 * Adds squiggly underlines for diagnostics (errors and warnings).
 */
public class CodeHighlighter {
    private final JTextPane textPane;
    
    // Squiggle painters
    private SquiggleHighlightPainter errorPainter;
    private SquiggleHighlightPainter warningPainter;
    private List<Object> squiggleHighlights = new ArrayList<>();
    
    public CodeHighlighter(JTextPane textPane) {
        this.textPane = textPane;
        
        // Initialize squiggle painters
        errorPainter = new SquiggleHighlightPainter(new Color(244, 67, 54)); // Red
        warningPainter = new SquiggleHighlightPainter(new Color(255, 152, 0)); // Orange
    }
    
    /**
     * Apply squiggles from analysis result.
     */
    public void applySquiggles(AnalysisResult result) {
        if (result == null) {
            clearSquiggles();
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                // Remove old squiggles
                clearSquiggles();
                
                // Add new squiggles
                StyledDocument document = textPane.getStyledDocument();
                String text = document.getText(0, document.getLength());
                Highlighter highlighter = textPane.getHighlighter();
                
                for (Diagnostic diag : result.getDiagnostics()) {
                    SquiggleHighlightPainter painter = getPainterForSeverity(diag.getSeverity());
                    
                    int startOffset = diag.getStartOffset();
                    int endOffset = diag.getEndOffset();
                    
                    // Validate offsets
                    if (startOffset >= 0 && endOffset <= text.length() && startOffset < endOffset) {
                        Object highlight = highlighter.addHighlight(startOffset, endOffset, painter);
                        squiggleHighlights.add(highlight);
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private void clearSquiggles() {
        Highlighter highlighter = textPane.getHighlighter();
        for (Object highlight : squiggleHighlights) {
            highlighter.removeHighlight(highlight);
        }
        squiggleHighlights.clear();
    }
    
    private SquiggleHighlightPainter getPainterForSeverity(Diagnostic.Severity severity) {
        switch (severity) {
            case ERROR:   return errorPainter;
            case WARNING: return warningPainter;
            default:      return warningPainter;
        }
    }
    
    /**
     * Custom highlighter painter for squiggly underlines.
     */
    private static class SquiggleHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        private Color color;
        
        public SquiggleHighlightPainter(Color color) {
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
                    drawSquiggle(g2d, rect.x, rect.y + rect.height - 2, rect.width);
                } else {
                    // Multi-line (just draw under first line)
                    int width = bounds.getBounds().x + bounds.getBounds().width - r0.x;
                    drawSquiggle(g2d, r0.x, r0.y + r0.height - 2, width);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return null;
        }
        
        private void drawSquiggle(Graphics2D g2d, int x, int y, int width) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(1.0f));
            
            // Draw wavy line
            int amplitude = 2;
            int frequency = 4;
            
            for (int i = 0; i < width; i += frequency) {
                int yOffset = (i / frequency) % 2 == 0 ? 0 : amplitude;
                int nextI = Math.min(i + frequency, width);
                int nextYOffset = ((i + frequency) / frequency) % 2 == 0 ? 0 : amplitude;
                
                g2d.drawLine(x + i, y + yOffset, x + nextI, y + nextYOffset);
            }
        }
    }
}

