package lexical;

import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LexerEngine {
    private final Logger log = Logger.getLogger(this.getClass().getName());
    private final List<Token> tokens = new ArrayList<>();
    private final String input;

    public LexerEngine(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        Long position = 0L;
        String remainingInput = input;

        log.info(input);

        while (!remainingInput.isEmpty()) {
            boolean matched = false;

            for (TokenType tokenType : TokenType.values()) {
                Pattern pattern = Pattern.compile("^" + tokenType.pattern);
                Matcher matcher = pattern.matcher(remainingInput);

                if (matcher.find()) {
                    String lexeme = matcher.group().trim();

                    if (tokenType != TokenType.WHITESPACE) {
                        tokens.add(new Token(tokenType, lexeme, position));
                    }

                    remainingInput = remainingInput.substring(matcher.end());
                    position += matcher.end();
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                throw new RuntimeException("Unexpected token: " + remainingInput);
            }
        }

        return tokens;
    }
}