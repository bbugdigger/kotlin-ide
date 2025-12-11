import java.util.*;

/**
 * Container for analysis results.
 */
public class AnalysisResult {
    private final List<Diagnostic> diagnostics;
    
    public AnalysisResult(List<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }
    
    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }
    
    public int getErrorCount() {
        return (int) diagnostics.stream()
            .filter(d -> d.getSeverity() == Diagnostic.Severity.ERROR)
            .count();
    }
    
    public int getWarningCount() {
        return (int) diagnostics.stream()
            .filter(d -> d.getSeverity() == Diagnostic.Severity.WARNING)
            .count();
    }
}

