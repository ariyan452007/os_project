import java.util.ArrayList;
import java.util.List;

/**
 * OS CONCEPT: Lexical Analysis and Tokenization
 * The shell must parse raw input strings into distinct arguments based on IFS
 * (Internal Field Separator).
 * It implements a state machine to correctly handle single and double quoting
 * rules,
 * ensuring that special characters are either interpreted or treated as
 * literals.
 */
public class Parser {

    static class Token {
        String value;
        boolean isOperator;

        Token(String v, boolean op) {
            this.value = v;
            this.isOperator = op;
        }
    }

    public static List<Command> parse(String input) {
        List<Token> tokens = tokenize(input);
        List<Command> commands = new ArrayList<>();
        Command current = new Command();

        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.isOperator) {
                if (t.value.equals("|")) {
                    commands.add(current);
                    current = new Command();
                } else if (t.value.equals(">") || t.value.equals("1>")) {
                    if (i + 1 < tokens.size())
                        current.stdoutRedirect = tokens.get(++i).value;
                    current.stdoutAppend = false;
                } else if (t.value.equals(">>") || t.value.equals("1>>")) {
                    if (i + 1 < tokens.size())
                        current.stdoutRedirect = tokens.get(++i).value;
                    current.stdoutAppend = true;
                } else if (t.value.equals("2>")) {
                    if (i + 1 < tokens.size())
                        current.stderrRedirect = tokens.get(++i).value;
                    current.stderrAppend = false;
                } else if (t.value.equals("2>>")) {
                    if (i + 1 < tokens.size())
                        current.stderrRedirect = tokens.get(++i).value;
                    current.stderrAppend = true;
                } else if (t.value.equals("&")) {
                    current.isBackground = true;
                } else {
                    current.args.add(t.value);
                }
            } else {
                current.args.add(t.value);
            }
        }
        if (!current.args.isEmpty() || current.stdoutRedirect != null || current.stderrRedirect != null) {
            commands.add(current);
        }
        return commands;
    }

    private static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inSingle = false, inDouble = false, escapeNext = false, hasToken = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escapeNext) {
                if (inDouble) {
                    if (c == '$' || c == '"' || c == '\\' || c == '\n') {
                        currentToken.append(c);
                    } else {
                        currentToken.append('\\').append(c);
                    }
                } else {
                    currentToken.append(c);
                }
                escapeNext = false;
                hasToken = true;
                continue;
            }

            if (inSingle) {
                if (c == '\'')
                    inSingle = false;
                else
                    currentToken.append(c);
                hasToken = true;
            } else if (inDouble) {
                if (c == '"')
                    inDouble = false;
                else if (c == '\\')
                    escapeNext = true;
                else
                    currentToken.append(c);
                hasToken = true;
            } else {
                if (c == '\\') {
                    escapeNext = true;
                    hasToken = true;
                } else if (c == '\'') {
                    inSingle = true;
                    hasToken = true;
                } else if (c == '"') {
                    inDouble = true;
                    hasToken = true;
                } else if (Character.isWhitespace(c)) {
                    if (hasToken) {
                        tokens.add(new Token(currentToken.toString(), false));
                        currentToken.setLength(0);
                        hasToken = false;
                    }
                } else if (c == '|' || c == '&' || c == '>' || c == '<') {
                    if (hasToken) {
                        if (c == '>' && (currentToken.toString().equals("1") || currentToken.toString().equals("2"))) {
                            currentToken.append(c);
                            if (i + 1 < input.length() && input.charAt(i + 1) == '>') {
                                currentToken.append('>');
                                i++;
                            }
                            tokens.add(new Token(currentToken.toString(), true));
                            currentToken.setLength(0);
                            hasToken = false;
                            continue;
                        } else {
                            tokens.add(new Token(currentToken.toString(), false));
                            currentToken.setLength(0);
                            hasToken = false;
                        }
                    }

                    StringBuilder op = new StringBuilder();
                    op.append(c);
                    if (c == '>' && i + 1 < input.length() && input.charAt(i + 1) == '>') {
                        op.append('>');
                        i++;
                    }
                    tokens.add(new Token(op.toString(), true));
                } else {
                    currentToken.append(c);
                    hasToken = true;
                }
            }
        }
        if (hasToken) {
            tokens.add(new Token(currentToken.toString(), false));
        }
        return tokens;
    }
}
