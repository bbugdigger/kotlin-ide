import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {
    private final JTextPane textPane;
    private final StyledDocument document;
    private final Style defaultStyle;
    private final Style keywordStyle;
    private final Style commentStyle;
    private final Style stringStyle;

    // Kotlin keywords to highlight
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "annotation", "as", "break", "by",
            "catch", "class", "companion", "const", "constructor", "continue",
            "crossinline", "data", "delegate", "do",
            "else", "enum", "expect",
            "false", "final", "finally", "for", "fun",
            "get",
            "if", "import", "in", "infix", "init", "inline", "inner", "interface", "internal", "is",
            "lateinit",
            "noinline", "null",
            "object", "open", "operator", "out", "override",
            "package", "private", "protected", "public",
            "return", "reified",
            "sealed", "set", "super", "suspend",
            "tailrec", "this", "throw", "true", "try", "typealias",
            "val", "var", "vararg",
            "when", "where", "while"
    ));

    // Pattern to match words (potential keywords)
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w+\\b");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("//.*");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");

    public SyntaxHighlighter(JTextPane textPane) {
        this.textPane = textPane;
        this.document = textPane.getStyledDocument();

        // Create styles for dark theme
        defaultStyle = textPane.addStyle("Default", null);
        StyleConstants.setForeground(defaultStyle, Color.LIGHT_GRAY);

        keywordStyle = textPane.addStyle("Keyword", null);
        StyleConstants.setForeground(keywordStyle, Color.BLUE);
        StyleConstants.setBold(keywordStyle, true);

        commentStyle = textPane.addStyle("Comment", null);
        StyleConstants.setForeground(commentStyle, Color.GREEN);
        StyleConstants.setItalic(commentStyle, true);

        stringStyle = textPane.addStyle("String", null);
        StyleConstants.setForeground(stringStyle, Color.ORANGE);

        // Attach document listener
        document.addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                scheduleHighlight();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                scheduleHighlight();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                scheduleHighlight();
            }
        });
    }

    private javax.swing.Timer highlightTimer;

    private void scheduleHighlight() {
        if (highlightTimer != null && highlightTimer.isRunning()) {
            highlightTimer.restart();
        } else {
            highlightTimer = new javax.swing.Timer(200, e -> highlightAll());
            highlightTimer.setRepeats(false);
            highlightTimer.start();
        }
    }

    private void highlightAll() {
        SwingUtilities.invokeLater(() -> {
            try {
                String text = document.getText(0, document.getLength());

                // Save caret position
                int caretPosition = textPane.getCaretPosition();

                // Reset all to default style
                document.setCharacterAttributes(0, text.length(), defaultStyle, true);

                // Highlight strings first (so keywords in strings don't get highlighted)
                highlightPattern(text, STRING_PATTERN, stringStyle);

                // Highlight comments (so keywords in comments don't get highlighted)
                highlightPattern(text, COMMENT_PATTERN, commentStyle);

                // Highlight keywords (but check they're not in strings/comments)
                highlightKeywords(text);

                // Restore caret position
                textPane.setCaretPosition(Math.min(caretPosition, text.length()));

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void highlightPattern(String text, Pattern pattern, Style style) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int length = matcher.end() - start;
            document.setCharacterAttributes(start, length, style, false);
        }
    }

    private void highlightKeywords(String text) throws BadLocationException {
        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            String word = matcher.group();
            int start = matcher.start();
            int length = word.length();

            // Check if this word is a keyword
            if (KEYWORDS.contains(word)) {
                // Check if it's not already styled (i.e., not in a comment or string)
                AttributeSet attrs = document.getCharacterElement(start).getAttributes();
                Color fgColor = (Color) attrs.getAttribute(StyleConstants.Foreground);
                // Check if it's the default light gray color (not in string/comment)
                if (fgColor != null && fgColor.getRed() == 220 && fgColor.getGreen() == 220) {
                    document.setCharacterAttributes(start, length, keywordStyle, false);
                }
            }
        }
    }
}
