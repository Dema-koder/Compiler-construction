import ast.ASTNode;
import bytecode.BytecodeGenerator;
import lexical.LexerEngine;
import semantic.SemanticAnalyzer;
import sintax.SintaxisAnalyzer;
import token.Token;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        try {

            Scanner scanner = new Scanner(new File("/Users/demanzverev/IdeaProjects/compiler-construction/src/main/java/examples/example3.txt"));

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

            BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();
            var bytecode = bytecodeGenerator.generate(root);

            System.out.println("Bytecode: " + bytecode);

        } catch (FileNotFoundException e) {
            System.err.println("Source file not found: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
