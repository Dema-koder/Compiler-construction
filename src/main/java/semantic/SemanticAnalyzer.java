package semantic;

import ast.ASTNode;

import java.util.HashMap;
import java.util.Map;

public class SemanticAnalyzer {

    private final Map<String, String> symbolTable = new HashMap<>();

    public void analyze(ASTNode root) {
        if (!root.getNodeType().equals("Program")) {
            throw new RuntimeException("Root node must be of type Program");
        }

        for (ASTNode child : root.getChildren()) {
            if (child.getNodeType().equals("class")) {
                analyzeClass(child);
            } else {
                throw new RuntimeException("Unexpected node type: " + child.getNodeType());
            }
        }
    }

    private void analyzeClass(ASTNode classNode) {
        String className = classNode.getNodeName();
        System.out.println("Analyzing class: " + className);

        for (ASTNode child : classNode.getChildren()) {
            switch (child.getNodeType()) {
                case "declaration":
                    analyzeVarDeclaration(child); // Analyze class field declarations
                    break;
                case "constructor":
                    analyzeConstructor(child);
                    break;
                case "method":
                    analyzeMethod(child);
                    break;
                default:
                    throw new RuntimeException("Unexpected class member: " + child.getNodeType());
            }
        }
    }

    private void analyzeConstructor(ASTNode constructorNode) {
        System.out.println("Analyzing constructor");
        // Constructor-specific checks if needed
    }

    private void analyzeMethod(ASTNode methodNode) {
        String methodName = methodNode.getNodeName();
        System.out.println("Analyzing method: " + methodName);

        symbolTable.clear(); // New symbol table for method scope

        String returnType = null; // Track the return type

        for (ASTNode child : methodNode.getChildren()) {
            switch (child.getNodeType()) {
                case "ReturnType":
                    returnType = analyzeReturnType(child);
                    break;
                case "argument":
                    analyzeArgument(child);
                    break;
                case "declaration":
                    analyzeVarDeclaration(child);
                    break;
                case "WhileStatement":
                    analyzeWhile(child);
                    break;
                case "MethodCall":
                    analyzeMethodCall(child);
                    break;
                case "ReturnStatement":
                    analyzeReturnStatement(child, returnType);
                    break;
                case "assignment":
                    analyzeAssignment(child);
                    break;
                default:
                    throw new RuntimeException("Unexpected method element: " + child.getNodeType());
            }
        }
    }

    private String analyzeReturnType(ASTNode returnTypeNode) {
        String type = returnTypeNode.getNodeName();
        System.out.println("Method return type: " + type);
        return type;
    }

    private void analyzeReturnStatement(ASTNode returnNode, String expectedType) {
        System.out.println("Analyzing return statement");
        if (expectedType == null) {
            throw new RuntimeException("Method return type not declared before return statement");
        }

        if (returnNode.getChildren().isEmpty()) {
            throw new RuntimeException("Return statement must have a value");
        }

        ASTNode returnValue = returnNode.getChildren().get(0);
        String returnValueType = getExpressionType(returnValue);

        if (!expectedType.equals(returnValueType)) {
            throw new RuntimeException("Return type mismatch: expected " + expectedType + ", got " + returnValueType);
        }
    }

    private String getExpressionType(ASTNode expressionNode) {
        switch (expressionNode.getNodeType()) {
            case "StringLiteral":
                return "String";
            case "NumberLiteral":
                return "Integer";
            case "BooleanLiteral":
                return "Boolean";
            case "identifier":
                String identifierType = symbolTable.get(expressionNode.getNodeName());
                if (identifierType == null) {
                    throw new RuntimeException("Undefined identifier: " + expressionNode.getNodeName());
                }
                return identifierType;
            default:
                throw new RuntimeException("Unknown expression type: " + expressionNode.getNodeType());
        }
    }

    private void analyzeArgument(ASTNode argumentNode) {
        String argName = argumentNode.getNodeName();
        String argType = argumentNode.getNodeTypeInfo();

        if (symbolTable.containsKey(argName)) {
            throw new RuntimeException("Argument already declared: " + argName);
        }

        symbolTable.put(argName, argType);
        System.out.println("Declared argument: " + argName + " of type " + argType);
    }

    private void analyzeVarDeclaration(ASTNode varDeclNode) {
        if (!"declaration".equals(varDeclNode.getNodeType())) {
            throw new RuntimeException("Unexpected declaration type: " + varDeclNode.getNodeType());
        }

        String varName = varDeclNode.getNodeName();
        String varType = varDeclNode.getNodeTypeInfo();

        System.out.println("Declared variable: " + varName + " of type " + varType);

        // Analyze initializer if present
        for (ASTNode child : varDeclNode.getChildren()) {
            analyzeExpression(child);
        }
    }

    private void analyzeConstructorCall(ASTNode constructorCallNode) {
        System.out.println("Analyzing constructor call for type: " + constructorCallNode.getNodeName());

        for (ASTNode child : constructorCallNode.getChildren()) {
            if (child.getNodeType().equals("NumberLiteral")) {
                analyzeLiteral(child);
            } else {
                throw new RuntimeException("Unexpected constructor call child: " + child.getNodeType());
            }
        }
    }

    private void analyzeWhile(ASTNode whileNode) {
        System.out.println("Analyzing WHILE loop");

        for (ASTNode child : whileNode.getChildren()) {
            switch (child.getNodeType()) {
                case "declaration":
                    analyzeVarDeclaration(child);
                    break;
                case "MethodCall":
                    analyzeMethodCall(child);
                    break;
                case "assignment":
                    analyzeAssignment(child);
                    break;
                default:
                    analyzeExpression(child);
            }
        }
    }

    private void analyzeAssignment(ASTNode assignmentNode) {
        if (!"assignment".equals(assignmentNode.getNodeType())) {
            throw new RuntimeException("Unexpected assignment type: " + assignmentNode.getNodeType());
        }

        System.out.println("Assignment:");

        // Analyze left-hand side
        ASTNode lhs = assignmentNode.getChildren().get(0); // Assuming first child is the LHS
        analyzeExpression(lhs);

        // Analyze right-hand side
        ASTNode rhs = assignmentNode.getChildren().get(1); // Assuming second child is the RHS
        analyzeExpression(rhs);
    }

    private void analyzeMethodCall(ASTNode methodCallNode) {
        System.out.println("Analyzing method call: " + methodCallNode.getNodeName());

        for (ASTNode child : methodCallNode.getChildren()) {
            analyzeExpression(child);
        }
    }

    private void analyzeExpression(ASTNode expressionNode) {
        switch (expressionNode.getNodeType()) {
            case "StringLiteral":
                System.out.println("StringLiteral: " + expressionNode.getNodeName());
                break;
            case "NumberLiteral":
                System.out.println("NumberLiteral: " + expressionNode.getNodeName());
                break;
            case "BooleanLiteral":
                System.out.println("BooleanLiteral: " + expressionNode.getNodeName());
                break;
            case "identifier":
                // Check if the identifier is valid (could enhance with a symbol table)
                System.out.println("Identifier: " + expressionNode.getNodeName());
                break;
            case "ConstructorCall":
                analyzeConstructorCall(expressionNode); // Explicit call to analyzeConstructorCall
                break;
            case "MethodCall":
                System.out.println("MethodCall: " + expressionNode.getNodeName());
                for (ASTNode child : expressionNode.getChildren()) {
                    analyzeExpression(child); // Analyze method arguments
                }
                break;
            case "FieldAccess":
                analyzeFieldAccess(expressionNode); // Explicit call to analyzeFieldAccess
                break;
            default:
                throw new RuntimeException("Unexpected expression type: " + expressionNode.getNodeType());
        }
    }

    private void analyzeLiteral(ASTNode literalNode) {
        System.out.println("Analyzing literal: " + literalNode.getNodeName());
    }

    private void analyzeFieldAccess(ASTNode fieldAccessNode) {
        System.out.println("Analyzing field access: " + fieldAccessNode.getNodeName());
        if (!symbolTable.containsKey(fieldAccessNode.getNodeName())) {
            throw new RuntimeException("Undefined field: " + fieldAccessNode.getNodeName());
        }
    }
}
