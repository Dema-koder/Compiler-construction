package sintax;

import ast.ASTNode;
import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class SintaxisAnalyzer {

    private List<Token> tokens;
    private int currentIndex;

    public SintaxisAnalyzer(List<Token> tokens) {
        this.tokens = tokens;
        this.currentIndex = 0;
    }

    private Token currentToken() {
        return tokens.get(currentIndex);
    }

    private Token nextToken() {
        if (currentIndex + 1 < tokens.size()) {
            return tokens.get(++currentIndex);
        } else {
            ++currentIndex;
            return null;
        }
    }

    private boolean check(TokenType type) {
        return currentToken().type() == type;
    }

    private Token expect(TokenType type) {
        if (check(type)) {
            return nextToken();
        }
        throw new RuntimeException("Expected token " + type + ", but found " + currentToken().type() + ", position " + currentToken().position());
    }

    public ASTNode parse() {
        return parseProgram();
    }

    private ASTNode parseProgram() {
        ASTNode programNode = new ASTNode("Program");

        while (currentIndex < tokens.size()) {
            if (check(TokenType.CLASS)) {
                programNode.addChild(parseClass());
            } else {
                throw new RuntimeException("Unexpected token: " + currentToken());
            }
        }

        return programNode;
    }

    private ASTNode parseClass() {
        expect(TokenType.CLASS);
        String className = currentToken().value();
        expect(TokenType.IDENTIFIER);
        expect(TokenType.IS);

        ASTNode classNode = new ASTNode("Class", className);

        // Парсим конструкторы и методы
        while (!check(TokenType.END)) {
            if (check(TokenType.VAR)) {
                classNode.addChild(parseVarDeclaration());
            }else if (check(TokenType.THIS)) {
                classNode.addChild(parseConstructor());
            } else if (check(TokenType.METHOD)) {
                classNode.addChild(parseMethod());
            } else {
                throw new RuntimeException("Unexpected token: " + currentToken());
            }
        }

        expect(TokenType.END);
        return classNode;
    }

    private ASTNode parseConstructor() {
        expect(TokenType.THIS);
        expect(TokenType.LEFT_PAREN);
        List<ASTNode> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                String argName = currentToken().value();
                expect(TokenType.IDENTIFIER);
                expect(TokenType.COLON);
                String argType = parseType();
                arguments.add(new ASTNode("Argument", argName, argType));

                if (check(TokenType.COMMA)) {
                    nextToken();
                } else {
                    break;
                }
            } while (true);
        }

        expect(TokenType.RIGHT_PAREN);
        expect(TokenType.IS);

        ASTNode constructorNode = new ASTNode("Constructor");
        constructorNode.addChildren(arguments);

        while (!check(TokenType.END)) {
            constructorNode.addChild(parseStatement());
        }

        expect(TokenType.END);
        return constructorNode;
    }

    private ASTNode parseIfStatement() {
        expect(TokenType.IF);

        ASTNode condition = parseExpression();

        expect(TokenType.THEN);

        ASTNode ifNode = new ASTNode("IfStatement");
        ifNode.addChild(condition);

        ASTNode thenBlock = new ASTNode("ThenBlock");
        while (!check(TokenType.ELSE) && !check(TokenType.END)) {
            thenBlock.addChild(parseStatement());
        }
        ifNode.addChild(thenBlock);

        if (check(TokenType.ELSE)) {
            expect(TokenType.ELSE);
            ASTNode elseBlock = new ASTNode("ElseBlock");
            while (!check(TokenType.END)) {
                elseBlock.addChild(parseStatement());
            }
            ifNode.addChild(elseBlock);
        }

        expect(TokenType.END);

        return ifNode;
    }

    private ASTNode parseMethod() {
        expect(TokenType.METHOD);
        String methodName = currentToken().value();
        expect(TokenType.IDENTIFIER);
        expect(TokenType.LEFT_PAREN);

        List<ASTNode> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                String argName = currentToken().value();
                expect(TokenType.IDENTIFIER);
                expect(TokenType.COLON);
                String argType = parseType();
                arguments.add(new ASTNode("Argument", argName, argType));

                if (check(TokenType.COMMA)) {
                    nextToken();
                } else {
                    break;
                }
            } while (true);
        }

        expect(TokenType.RIGHT_PAREN);

        String returnType = null;
        if (!check(TokenType.IS)) {
            returnType = parseType();
        }

        expect(TokenType.IS);

        ASTNode methodNode = new ASTNode("Method", methodName);
        methodNode.addChildren(arguments);
        if (returnType != null) {
            methodNode.addChild(new ASTNode("ReturnType", returnType));
        }

        // Парсим тело метода
        while (!check(TokenType.END)) {
            methodNode.addChild(parseStatement());
        }

        expect(TokenType.END);
        return methodNode;
    }


    private ASTNode parseConstructorCall(String className) {
        List<ASTNode> arguments = new ArrayList<>();

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(parseExpression());

                if (check(TokenType.COMMA)) {
                    nextToken();
                } else {
                    break;
                }
            } while (true);
        }

        ASTNode constructorCallNode = new ASTNode("ConstructorCall", className);
        constructorCallNode.addChildren(arguments);

        return constructorCallNode;
    }


    private String parseType() {
        String typeName;

        if (check(TokenType.ARRAY)) {
            typeName = currentToken().value();
            expect(TokenType.ARRAY);
        } else if (check(TokenType.INTEGER)) {
            typeName = currentToken().value();
            expect(TokenType.INTEGER);
        } else if (check(TokenType.BOOLEAN)) {
            typeName = currentToken().value();
            expect(TokenType.BOOLEAN);
        } else if (check(TokenType.REAL)) {
            typeName = currentToken().value();
            expect(TokenType.REAL);
        } else if (check(TokenType.IDENTIFIER)) {
            typeName = currentToken().value();
            expect(TokenType.IDENTIFIER);
        } else {
            throw new RuntimeException("Expected type but found: " + currentToken());
        }

        if (check(TokenType.LEFT_BRACKET)) {
            nextToken();
            String innerType = parseType();
            expect(TokenType.RIGHT_BRACKET);
            return typeName + "[" + innerType + "]";
        }

        return typeName;
    }

    private ASTNode parseWhileStatement() {
        expect(TokenType.WHILE);

        ASTNode condition = parseExpression();

        expect(TokenType.LOOP);

        ASTNode whileNode = new ASTNode("WhileStatement", "while", null);

        while (!check(TokenType.END)) {
            whileNode.addChild(parseStatement());
        }

        expect(TokenType.END);

        whileNode.addChild(condition);

        return whileNode;
    }

    private ASTNode parseReturnStatement() {
        expect(TokenType.RETURN);
        ASTNode returnExpr = parseExpression();
        ASTNode returnStatement = new ASTNode("ReturnStatement");
        returnStatement.addChild(returnExpr);
        return returnStatement;

    }


    private ASTNode parseStatement() {
        if (check(TokenType.VAR)) {
            return parseVarDeclaration();
        } else if (check(TokenType.IDENTIFIER)) {
            return parseAssignmentOrMethodCall();
        } else if (check(TokenType.WHILE)) {
            return parseWhileStatement();
        } else if (check(TokenType.THIS)) {
            return parseConcstructorVarDeclaration();
        } else if (check(TokenType.IF)) {
            return parseIfStatement();
        } else if (check(TokenType.RETURN)) {
            return parseReturnStatement();
        } else {
            throw new RuntimeException("Unexpected statement: " + currentToken());
        }
    }



    private ASTNode parseConcstructorVarDeclaration() {
        expect(TokenType.THIS);
        expect(TokenType.DOT);
        String varName = currentToken().value();
        expect(TokenType.IDENTIFIER);
        ASTNode value = null;
        if (check(TokenType.ASSIGN)) {
            expect(TokenType.ASSIGN);
            value = parseExpression();
        } else if (check(TokenType.LEFT_PAREN)) {
            expect(TokenType.LEFT_PAREN);
            value = parseExpression();
            expect(TokenType.RIGHT_PAREN);
        }

        ASTNode varNode = new ASTNode("Assignment", varName, "this");
        if (value != null) {
            varNode.addChild(value);
        }

        return varNode;
    }

    private ASTNode parseVarDeclaration() {
        expect(TokenType.VAR);
        String varName = currentToken().value();
        expect(TokenType.IDENTIFIER);
        expect(TokenType.COLON);

        String varType = parseType();

        ASTNode value = null;
        if (check(TokenType.LEFT_PAREN)) {
            expect(TokenType.LEFT_PAREN);
            value = parseConstructorCall(varType);
            expect(TokenType.RIGHT_PAREN);
        } else if (check(TokenType.ASSIGN)) {
            expect(TokenType.ASSIGN);
            value = parseExpression();
        }

        ASTNode varNode = new ASTNode("Declaration", varName, varType);
        if (value != null) {
            varNode.addChild(value);
        }

        return varNode;
    }

    private ASTNode parseAssignmentOrMethodCall() {
        String identifier = currentToken().value();
        expect(TokenType.IDENTIFIER);

        ASTNode leftHandSide = new ASTNode("Identifier", identifier);

        while (check(TokenType.DOT)) {
            nextToken();
            String memberName = currentToken().value();
            expect(TokenType.IDENTIFIER);
            ASTNode memberAccessNode = new ASTNode("MemberAccess", memberName);
            memberAccessNode.addChild(leftHandSide);
            leftHandSide = memberAccessNode;
        }

        if (check(TokenType.ASSIGN)) {
            expect(TokenType.ASSIGN);
            ASTNode value = parseExpression();
            ASTNode assignmentNode = new ASTNode("Assignment", null);
            assignmentNode.addChild(leftHandSide);
            assignmentNode.addChild(value);
            return assignmentNode;
        } else if (check(TokenType.LEFT_PAREN)) {
            return parseMethodCall(identifier);
        } else {
            throw new RuntimeException("Unexpected token in assignment or method call: " + currentToken());
        }
    }


    private ASTNode parseMethodCall(String methodName) {
        expect(TokenType.LEFT_PAREN);

        List<ASTNode> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(parseExpression());

                if (check(TokenType.COMMA)) {
                    nextToken();
                } else {
                    break;
                }
            } while (true);
        }

        expect(TokenType.RIGHT_PAREN);
        ASTNode methodCallNode = new ASTNode("MethodCall", methodName);
        methodCallNode.addChildren(arguments);

        return methodCallNode;
    }

    private ASTNode parseExpression() {
        ASTNode expr = parsePrimary();

        while (check(TokenType.DOT)) {
            nextToken();
            String memberName = currentToken().value();
            expect(TokenType.IDENTIFIER);

            if (check(TokenType.LEFT_PAREN)) {
                List<ASTNode> arguments = parseArguments();
                ASTNode methodCallNode = new ASTNode("MethodCall", memberName);
                methodCallNode.addChild(expr);
                methodCallNode.addChildren(arguments);
                expr = methodCallNode;
            } else {
                ASTNode fieldAccessNode = new ASTNode("FieldAccess", memberName);
                fieldAccessNode.addChild(expr);
                expr = fieldAccessNode;
            }
        }

        return expr;
    }


    private ASTNode parsePrimary() {
        if (check(TokenType.IDENTIFIER)) {
            String name = currentToken().value();
            nextToken();

            if (check(TokenType.LEFT_PAREN)) {
                List<ASTNode> arguments = parseArguments();
                ASTNode methodCallNode = new ASTNode("MethodCall", name);
                methodCallNode.addChildren(arguments);
                return methodCallNode;
            } else {
                return new ASTNode("Identifier", name);
            }
        } else if (check(TokenType.INTEGER_LITERAL)) {
            String number = currentToken().value();
            nextToken();
            return new ASTNode("NumberLiteral", number);
        } else if (check(TokenType.STRING_LITERAL)) {
            String string = currentToken().value();
            nextToken();
            return new ASTNode("StringLiteral", string);
        }
        return null;
    }


    private List<ASTNode> parseArguments() {
        List<ASTNode> arguments = new ArrayList<>();
        expect(TokenType.LEFT_PAREN);
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                ASTNode arg = parseExpression();
                arguments.add(arg);
                if (check(TokenType.COMMA)) {
                    nextToken();
                } else {
                    break;
                }
            } while (true);
        }
        expect(TokenType.RIGHT_PAREN);
        return arguments;
    }

}
