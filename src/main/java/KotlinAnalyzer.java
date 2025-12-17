import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.*;
import javax.swing.Timer;

public class KotlinAnalyzer {
    private ExecutorService executorService;
    private Timer analysisTimer;
    
    private static final int ANALYSIS_DELAY_MS = 500;
    
    private static final Pattern VAR_PATTERN = Pattern.compile("\\b(val|var)\\s+(\\w+)\\s*[=:]");
    private static final Pattern FUN_PATTERN = Pattern.compile("\\bfun\\s+(\\w+)\\s*\\(");
    private static final Pattern CALL_PATTERN = Pattern.compile("\\b(\\w+)\\s*\\(");
    private static final Pattern VAR_USAGE_PATTERN = Pattern.compile("\\b(\\w+)\\b");
    
    private static final Set<String> STDLIB_FUNCTIONS = new HashSet<>(Arrays.asList(
        "println", "print", "listOf", "forEach"
    ));
    
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "break", "catch", "class", "const", "constructor", "continue",
            "do", "else", "enum", "false", "finally", "for", "fun", "if", "import", "in", "inline", "interface",
            "null", "override", "private", "protected", "public", "return", "super",
            "this", "throw", "true", "try", "val", "var", "while"
    ));
    
    public KotlinAnalyzer() {
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    public AnalysisResult analyze(String code) {
        try {
            List<Diagnostic> diagnostics = new ArrayList<>();
            
            // Track declared symbols
            Map<String, Integer> declaredVariables = new HashMap<>(); // name -> line offset
            Set<String> declaredFunctions = new HashSet<>();
            Set<String> usedSymbols = new HashSet<>();
            
            String[] lines = code.split("\n", -1);
            
            // First pass: Extract declarations
            for (int lineNum = 0; lineNum < lines.length; lineNum++) {
                String line = lines[lineNum];
                int lineOffset = getLineOffset(lines, lineNum);
                
                // Skip comment lines
                String trimmedLine = line.trim();
                boolean isComment = trimmedLine.startsWith("//");
                
                if (!isComment) {
                    // Extract variable declarations
                    Matcher varMatcher = VAR_PATTERN.matcher(line);
                    while (varMatcher.find()) {
                        String varName = varMatcher.group(2);
                        declaredVariables.put(varName, lineOffset + varMatcher.start(2));
                    }
                    
                    // Extract function declarations
                    Matcher funMatcher = FUN_PATTERN.matcher(line);
                    while (funMatcher.find()) {
                        String funName = funMatcher.group(1);
                        declaredFunctions.add(funName);
                    }
                }
            }
            
            // Second pass: Find usage and errors
            for (int lineNum = 0; lineNum < lines.length; lineNum++) {
                String line = lines[lineNum];
                int lineOffset = getLineOffset(lines, lineNum);
                
                // Skip comment lines for analysis
                String trimmedLine = line.trim();
                boolean isComment = trimmedLine.startsWith("//");
                
                // Check for unclosed strings (skip in comments)
                if (!isComment) {
                    int quoteCount = 0;
                    boolean escaped = false;
                    for (int i = 0; i < line.length(); i++) {
                        char c = line.charAt(i);
                        if (c == '\\' && !escaped) {
                            escaped = true;
                        } else if (c == '"' && !escaped) {
                            quoteCount++;
                        } else {
                            escaped = false;
                        }
                    }
                    
                    // Odd number of quotes means unclosed string
                    if (quoteCount % 2 != 0) {
                        int firstQuote = line.indexOf('"');
                        if (firstQuote >= 0) {
                            diagnostics.add(new Diagnostic(
                                Diagnostic.Severity.ERROR,
                                "Unclosed string literal",
                                lineNum + 1,
                                firstQuote + 1,
                                lineOffset + firstQuote,
                                lineOffset + line.length()
                            ));
                        }
                    }
                }
                
                // Track variable usage (skip in comments)
                if (!isComment) {
                    Matcher usageMatcher = VAR_USAGE_PATTERN.matcher(line);
                    while (usageMatcher.find()) {
                        String name = usageMatcher.group(1);
                        // Don't count in variable declarations
                        if (!line.substring(Math.max(0, usageMatcher.start() - 10), usageMatcher.start()).contains("val ") &&
                            !line.substring(Math.max(0, usageMatcher.start() - 10), usageMatcher.start()).contains("var ")) {
                            usedSymbols.add(name);
                        }
                    }
                }
                
                // Check for undefined function calls (skip in comments)
                if (!isComment) {
                    Matcher callMatcher = CALL_PATTERN.matcher(line);
                    while (callMatcher.find()) {
                        String callName = callMatcher.group(1);
                        
                        // Check if it's undefined
                        if (!KEYWORDS.contains(callName) && 
                            !STDLIB_FUNCTIONS.contains(callName) &&
                            !declaredFunctions.contains(callName) &&
                            callName.matches("^[a-z].*")) { // starts with lowercase
                            
                            int startOffset = lineOffset + callMatcher.start(1);
                            int endOffset = lineOffset + callMatcher.end(1);
                            
                            diagnostics.add(new Diagnostic(
                                Diagnostic.Severity.ERROR,
                                "Undefined function: " + callName,
                                lineNum + 1,
                                callMatcher.start(1) + 1,
                                startOffset,
                                endOffset
                            ));
                        }
                    }
                }
            }
            
            // Check for unused variables
            for (Map.Entry<String, Integer> entry : declaredVariables.entrySet()) {
                String varName = entry.getKey();
                if (!usedSymbols.contains(varName) && !varName.equals("_")) {
                    int startOffset = entry.getValue();
                    int endOffset = startOffset + varName.length();
                    int[] lineCol = offsetToLineColumn(code, startOffset);
                    
                    diagnostics.add(new Diagnostic(
                        Diagnostic.Severity.WARNING,
                        "Unused variable: " + varName,
                        lineCol[0],
                        lineCol[1],
                        startOffset,
                        endOffset
                    ));
                }
            }
            
            return new AnalysisResult(diagnostics);
            
        } catch (Exception e) {
            System.err.println("Analysis error: " + e.getMessage());
            e.printStackTrace();
            return new AnalysisResult(new ArrayList<>());
        }
    }
    
    private int getLineOffset(String[] lines, int lineNum) {
        int offset = 0;
        for (int i = 0; i < lineNum; i++) {
            offset += lines[i].length() + 1; // +1 for newline
        }
        return offset;
    }
    
    private int[] offsetToLineColumn(String code, int offset) {
        int line = 1;
        int column = 1;
        
        for (int i = 0; i < offset && i < code.length(); i++) {
            if (code.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        
        return new int[]{line, column};
    }
    
    public void analyzeAsync(String code, Consumer<AnalysisResult> callback) {
        // Cancel previous timer
        if (analysisTimer != null && analysisTimer.isRunning()) {
            analysisTimer.stop();
        }
        
        // Create new debounced timer
        analysisTimer = new Timer(ANALYSIS_DELAY_MS, e -> {
            executorService.submit(() -> {
                try {
                    AnalysisResult result = analyze(code);
                    callback.accept(result);
                } catch (Exception ex) {
                    System.err.println("Async analysis error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        });
        analysisTimer.start();
    }
    
    public void shutdown() {
        if (analysisTimer != null) {
            analysisTimer.stop();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

