import ast.ASTNode;
import lexical.LexerEngine;
import semantic.SemanticAnalyzer;
import sintax.SintaxisAnalyzer;
import token.Token;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        try {

            Scanner scanner = new Scanner(new File("/Users/alenamaksimova/Desktop/Compiler-construction/src/main/java/examples/example15.txt"));

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

            SintaxisAnalyzer parser = new SintaxisAnalyzer(tokens);
            ASTNode root = parser.parse();
            System.out.println(root.toString());

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            semanticAnalyzer.analyze(root);

            System.out.println("Semantic analysis completed successfully.");

        } catch (FileNotFoundException e) {
            System.err.println("Source file not found: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
