/**
 * Represents a diagnostic issue (error or warning).
 */
public class Diagnostic {
    public enum Severity {
        ERROR,    // Red squiggle
        WARNING   // Yellow squiggle
    }
    
    private final Severity severity;
    private final String message;
    private final int line;
    private final int column;
    private final int startOffset;
    private final int endOffset;
    
    public Diagnostic(Severity severity, String message, int line, int column, int startOffset, int endOffset) {
        this.severity = severity;
        this.message = message;
        this.line = line;
        this.column = column;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public String getMessage() {
        return message;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    public int getStartOffset() {
        return startOffset;
    }
    
    public int getEndOffset() {
        return endOffset;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] Line %d:%d - %s", severity, line, column, message);
    }
}

