import lexical.LexerEngine;
import lexical.Token;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File("/Users/demanzverev/IdeaProjects/compiler-construction/src/main/java/examples/example1.txt"));

        StringBuilder builder = new StringBuilder();
        while (scanner.hasNextLine()) {
            builder.append(scanner.nextLine());
            builder.append("\n");
        }

        String sourceCode = builder.toString();

        LexerEngine lexer = new LexerEngine(sourceCode);
        List<Token> tokens = lexer.tokenize();

        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}
