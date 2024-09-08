import lexical.LexerEngine;
import lexical.Token;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String sourceCode = "class Test extends Parent is { var x := 10 } end";

        LexerEngine lexer = new LexerEngine(sourceCode);
        List<Token> tokens = lexer.tokenize();

        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}
