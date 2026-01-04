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

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "break", "catch", "class", "const", "constructor", "continue",
            "do", "else", "enum", "false", "finally", "for", "fun", "if", "import", "in", "inline", "interface",
            "null", "override", "private", "protected", "public", "return", "super",
            "this", "throw", "true", "try", "val", "var", "while"
    ));

    // We will be using regex to match words (potential keywords) since building the lexer, parser and then generating the AST is a bit too much work...
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w+\\b");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("//.*");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");

    public SyntaxHighlighter(JTextPane textPane) {
        this.textPane = textPane;
        this.document = textPane.getStyledDocument();

        defaultStyle = textPane.addStyle("Default", null);
        StyleConstants.setForeground(defaultStyle, Color.WHITE);

        keywordStyle = textPane.addStyle("Keyword", null);
        StyleConstants.setForeground(keywordStyle, Color.ORANGE);
        StyleConstants.setBold(keywordStyle, true);

        commentStyle = textPane.addStyle("Comment", null);
        StyleConstants.setForeground(commentStyle, Color.GREEN);
        StyleConstants.setItalic(commentStyle, true);

        stringStyle = textPane.addStyle("String", null);
        StyleConstants.setForeground(stringStyle, Color.ORANGE);

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
            highlightTimer = new javax.swing.Timer(500, e -> highlightAll());
            highlightTimer.start();
        }
    }

    private void highlightAll() {
            try {
                String text = document.getText(0, document.getLength());

                // Reset all to default style
                document.setCharacterAttributes(0, text.length(), defaultStyle, true);

                // Highlight strings first (so keywords in strings don't get highlighted)
                highlightPattern(text, STRING_PATTERN, stringStyle);

                // Highlight comments (so keywords in comments don't get highlighted)
                highlightPattern(text, COMMENT_PATTERN, commentStyle);

                // Highlight keywords (but check they're not in strings/comments)
                highlightKeywords(text);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
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

            if (KEYWORDS.contains(word)) {
                // We need to check if it's not already styled (not in a comment or string)
                AttributeSet attrs = document.getCharacterElement(start).getAttributes();
                Color fgColor = (Color) attrs.getAttribute(StyleConstants.Foreground);
                if (fgColor != null && fgColor.getRed() == 255 && fgColor.getGreen() == 255) {
                    document.setCharacterAttributes(start, length, keywordStyle, false);
                }
            }
        }
    }
}
