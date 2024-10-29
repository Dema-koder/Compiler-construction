package semantic;

import ast.ASTNode;
import java.util.*;

public class SemanticAnalyzer {
    private final Stack<Set<String>> symbolTable;
    private final List<String> errors;
    private final ASTNode root;

    public SemanticAnalyzer(ASTNode root) {
        this.root = root;
        this.symbolTable = new Stack<>();
        this.errors = new ArrayList<>();
    }

    public void analyze() {
        enterScope();
        checkDeclarationsBeforeUsage(root);
        checkKeywordUsage(root);
        removeUnusedVariables(root);
        simplifyConstantExpressions(root);
        exitScope();
        reportErrors();
    }

    private void enterScope() {
        symbolTable.push(new HashSet<>());
    }

    private void exitScope() {
        if (!symbolTable.isEmpty()) {
            symbolTable.pop();
        }
    }

    private void addToSymbolTable(String name) {
        if (!symbolTable.isEmpty()) {
            symbolTable.peek().add(name);
        }
    }

    private boolean isDeclared(String name) {
        for (Set<String> scope : symbolTable) {
            if (scope.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private void reportError(ASTNode node, String message) {
        errors.add("Error at line " + node.getNodeName() + ": " + message);
    }

    private void reportErrors() {
        for (String error : errors) {
            System.out.println(error);
        }
    }

    private void checkKeywordUsage(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            if ("Break".equals(child.getNodeType()) && !isInsideLoop(child)) {
                reportError(child, "Break statement used outside of a loop.");
            } else if ("Return".equals(child.getNodeType()) && !isInsideFunction(child)) {
                reportError(child, "Return statement used outside of a function.");
            }
            checkKeywordUsage(child);
        }
    }

    private boolean isInsideLoop(ASTNode node) {
        ASTNode parent = findParent(node);
        while (parent != null) {
            if ("Loop".equals(parent.getNodeType())) {
                return true;
            }
            parent = findParent(parent);
        }
        return false;
    }

    private boolean isInsideFunction(ASTNode node) {
        ASTNode parent = findParent(node);
        while (parent != null) {
            if ("FunctionDeclaration".equals(parent.getNodeType())) {
                return true;
            }
            parent = findParent(parent);
        }
        return false;
    }

    private ASTNode findParent(ASTNode node) {
        return node.getParent();
    }

    private void checkDeclarationsBeforeUsage(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            if ("Declaration".equals(child.getNodeType())) {
                addToSymbolTable(child.getNodeName());
            } else if ("Usage".equals(child.getNodeType())) {
                if (!isDeclared(child.getNodeName())) {
                    reportError(child, "Undeclared variable/function/class used: " + child.getNodeName());
                }
            }
            checkDeclarationsBeforeUsage(child);
        }
    }

    private void removeUnusedVariables(ASTNode node) {
        Set<String> usedVariables = new HashSet<>();
        collectUsedVariables(node, usedVariables);
        List<ASTNode> toRemove = new ArrayList<>();

        for (ASTNode child : node.getChildren()) {
            if ("VariableDeclaration".equals(child.getNodeType()) && !usedVariables.contains(child.getNodeName())) {
                toRemove.add(child);
            }
            removeUnusedVariables(child);
        }

        for (ASTNode unused : toRemove) {
            node.getChildren().remove(unused);
        }
    }

    private void collectUsedVariables(ASTNode node, Set<String> usedVariables) {
        for (ASTNode child : node.getChildren()) {
            if ("VariableUsage".equals(child.getNodeType())) {
                usedVariables.add(child.getNodeName());
            }
            collectUsedVariables(child, usedVariables);
        }
    }

    private void simplifyConstantExpressions(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            if ("BinaryExpression".equals(child.getNodeType()) && areChildrenConstants(child)) {
                int result = evaluateExpression(child);
                replaceNodeWithConstant(child, result);
            } else {
                simplifyConstantExpressions(child);
            }
        }
    }

    private boolean areChildrenConstants(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            if (!"Constant".equals(child.getNodeType())) {
                return false;
            }
        }
        return true;
    }

    private int evaluateExpression(ASTNode node) {
        if ("BinaryExpression".equals(node.getNodeType()) && node.getChildren().size() == 2) {
            ASTNode left = node.getChildren().get(0);
            ASTNode right = node.getChildren().get(1);

            int leftValue = Integer.parseInt(left.getNodeName());
            int rightValue = Integer.parseInt(right.getNodeName());

            return switch (node.getNodeName()) {
                case "+" -> leftValue + rightValue;
                case "-" -> leftValue - rightValue;
                case "*" -> leftValue * rightValue;
                case "/" -> leftValue / rightValue;
                case "<" -> leftValue < rightValue ? 1 : 0;
                default -> 0;
            };
        }
        return 0;
    }

    private void replaceNodeWithConstant(ASTNode node, int value) {
        ASTNode constantNode = new ASTNode("Constant", String.valueOf(value));
        ASTNode parent = findParent(node);

        if (parent != null) {
            parent.getChildren().remove(node);
            parent.addChild(constantNode);
        }
    }
}
