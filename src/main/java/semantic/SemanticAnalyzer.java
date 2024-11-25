package semantic;

import ast.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticAnalyzer {

    private final Map<String, String> symbolTable = new HashMap<>();
    private final Map<String, ClassDefinition> classTable = new HashMap<>();
    private final Map<String, String> globalSymbolTable = new HashMap<>();  // Global symbol table for global variables and methods

    public void analyze(ASTNode root) {
        if (!root.getNodeType().equals("Program")) {
            throw new RuntimeException("Root node must be of type Program");
        }

        // Pass 1: Collect class definitions
        for (ASTNode child : root.getChildren()) {
            if (child.getNodeType().equals("class")) {
                collectClassDefinition(child);
            } else {
                throw new RuntimeException("Unexpected node type: " + child.getNodeType());
            }
        }

        // Pass 2: Analyze class members
        for (ASTNode child : root.getChildren()) {
            if (child.getNodeType().equals("class")) {
                child.setParent(root);
                analyzeClass(child);
            }
        }
    }

    private void collectClassDefinition(ASTNode classNode) {
        String className = classNode.getNodeName();
        List<String> constructorArgTypes = List.of();
        String parentClass = null;

        // Check for inheritance
        for (ASTNode child : classNode.getChildren()) {
            if (child.getNodeType().equals("extends")) {
                parentClass = child.getNodeName();
                if (!classTable.containsKey(parentClass)) {
                    throw new RuntimeException("Class " + parentClass + " not defined before being extended.");
                }
            } else if (child.getNodeType().equals("constructor")) {
                constructorArgTypes = extractConstructorArgTypes(child);
            }
        }

        // Register the class in the class table
        classTable.put(className, new ClassDefinition(className, parentClass, constructorArgTypes));
        globalSymbolTable.put(className, "class");
    }

    private List<String> extractConstructorArgTypes(ASTNode constructorNode) {
        List<String> argTypes = new ArrayList<>();
        for (ASTNode arg : constructorNode.getChildren()) {
            if ("argument".equals(arg.getNodeType())) {
                argTypes.add(arg.getNodeTypeInfo()); // Assuming nodeTypeInfo stores the argument type
            }
        }
        return argTypes;
    }

    private void analyzeClass(ASTNode classNode) {
        String className = classNode.getNodeName();
        ClassDefinition classDef = classTable.get(className);

        if (classDef == null) {
            throw new RuntimeException("Class " + className + " not found in class table.");
        }

        for (ASTNode child : classNode.getChildren()) {
            switch (child.getNodeType()) {
                case "declaration":
                    child.setParent(classNode);
                    analyzeVarDeclaration(child); // Analyze class field declarations
                    break;
                case "constructor":
                    child.setParent(classNode);
                    analyzeConstructor(child);
                    break;
                case "method":
                    child.setParent(classNode);
                    analyzeMethod(child, classDef);
                    break;
                case "extends":
                    // Already processed in the first pass
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

    private void analyzeMethod(ASTNode methodNode, ClassDefinition classDef) {
        String methodName = methodNode.getNodeName();
        System.out.println("Analyzing method: " + methodName);

        symbolTable.clear(); // New symbol table for method scope

        String returnType = null; // Track the return type

        // Check for overridden methods
        if (classDef.getParentClass() != null) {
            ClassDefinition parentDef = classTable.get(classDef.getParentClass());
            if (parentDef != null && parentDef.hasMethod(methodName)) {
                String parentReturnType = parentDef.getMethodReturnType(methodName);
                if (!parentReturnType.equals(returnType)) {
                    throw new RuntimeException("Return type of method " + methodName +
                            " in class " + classDef.getName() +
                            " does not match the parent class method return type.");
                }
            }
        }

        System.out.println("Analyzing method: " + methodName + " in class " + classDef.getName());

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
                case "IfStatement":
                    analyzeIfStatement(child, returnType);
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
                case "identifier": // New case for identifier
                    analyzeIdentifier(child);
                    break;
                default:
                    throw new RuntimeException("Unexpected method element: " + child.getNodeType());
            }
        }
    }

    private void analyzeIdentifier(ASTNode identifierNode) {
        String identifierName = identifierNode.getNodeName();
        System.out.println("Analyzing identifier: " + identifierName);

        // Check if the identifier is declared in the current scope
        if (!symbolTable.containsKey(identifierName)) {
            throw new RuntimeException("Undeclared identifier: " + identifierName);
        }

        // Retrieve the type of the identifier from the symbol table
        String identifierType = symbolTable.get(identifierName);
        System.out.println("Identifier " + identifierName + " has type: " + identifierType);
    }

    private void analyzeIfStatement(ASTNode ifNode, String methodReturnType) {
        System.out.println("Analyzing IfStatement");

        ASTNode thenBranch = null;
        ASTNode elseBranch = null;

        // Find specific parts of the IfStatement
        for (ASTNode child : ifNode.getChildren()) {
            switch (child.getNodeType()) {
                case "ThenBlock":
                    thenBranch = child;
                    break;
                case "ElseBlock":
                    elseBranch = child;
                    break;
            }
        }

        // Analyze then branch
        if (thenBranch != null) {
            for (ASTNode child : thenBranch.getChildren()) {
                analyzeChildNode(child, methodReturnType);
            }
        } else {
            throw new RuntimeException("IfStatement is missing a then branch");
        }

        // Analyze else branch (if present)
        if (elseBranch != null) {
            for (ASTNode child : elseBranch.getChildren()) {
                analyzeChildNode(child, methodReturnType);
            }
        }
    }

    private void analyzeChildNode(ASTNode child, String methodReturnType) {
        switch (child.getNodeType()) {
            case "ReturnStatement":
                analyzeReturnStatement(child, methodReturnType);
                break;
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
            case "RealLiteral":
                return "Real";
            case "identifier":
                // Check global symbol table first, then method-specific symbol table
                String identifierType = symbolTable.get(expressionNode.getNodeName());
                if (identifierType == null) {
                    identifierType = globalSymbolTable.get(expressionNode.getNodeName());
                    if (identifierType == null) {
                        throw new RuntimeException("Undefined identifier: " + expressionNode.getNodeName());
                    }
                }
                return identifierType;
            case "MethodCall":
                // This will handle the recursive method call analysis
                return analyzeMethodCall(expressionNode); // Returns the return type of the method
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

        // Register the variable in the symbol table
        symbolTable.put(varName, varType);

        if (isGlobal(varDeclNode)) {
            globalSymbolTable.put(varName, varType);
        }

        // Analyze initializer if present
        for (ASTNode child : varDeclNode.getChildren()) {
            analyzeExpression(child);
        }
    }

    private void analyzeConstructorCall(ASTNode constructorCallNode) {
        System.out.println("Analyzing constructor call for type: " + constructorCallNode.getNodeName());

        String className = constructorCallNode.getNodeName();
        if (!classTable.containsKey(className)) {
            throw new RuntimeException("Class " + className + " is not defined.");
        }

        ClassDefinition classDef = classTable.get(className);
        List<String> expectedArgTypes = classDef.getConstructorArgTypes(); // Expected argument types
        int expectedArgs = expectedArgTypes.size();
        int actualArgs = constructorCallNode.getChildren().size();

        if (expectedArgs != actualArgs) {
            throw new RuntimeException("Constructor for class " + className + " expects " +
                    expectedArgs + " arguments, but got " + actualArgs);
        }

        // Validate each argument
        for (int i = 0; i < actualArgs; i++) {
            ASTNode arg = constructorCallNode.getChildren().get(i);
            String actualArgType = getExpressionType(arg);
            String expectedArgType = expectedArgTypes.get(i);

            System.out.println("Validating argument " + (i + 1) + ": expected " + expectedArgType + ", got " + actualArgType);

            if (!actualArgType.equals(expectedArgType)) {
                throw new RuntimeException("Type mismatch in constructor for class " + className +
                        ": expected " + expectedArgType + ", but got " + actualArgType + " for argument " + (i + 1));
            }
        }
    }

    private void analyzeWhile(ASTNode whileNode) {
        System.out.println("Analyzing WHILE loop");

        // Process the rest of the children as the loop body
        for (int i = 1; i < whileNode.getChildren().size(); i++) {
            ASTNode child = whileNode.getChildren().get(i);
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

    private String analyzeMethodCall(ASTNode methodCallNode) {
        System.out.println("Analyzing method call: " + methodCallNode.getNodeName());

        // Extract method name and determine the target type
        String methodName = methodCallNode.getNodeName();
        String methodReturnType = null;
        ASTNode targetNode = methodCallNode.getChildren().get(0);
        String targetType = getExpressionType(targetNode);

        System.out.println("Target type: " + targetType);

        // Handle method calls based on the target type
        switch (targetType) {
            case "Real":
            case "Integer":
                methodReturnType = analyzeNumericMethod(methodName, targetType, methodCallNode);
                break;

            case "String":
                methodReturnType = analyzeStringMethod(methodName, methodCallNode);
                break;

            case "Boolean":
                methodReturnType = analyzeBooleanMethod(methodName, methodCallNode);
                break;

            default:
                // Handle Array[T] types
                if (targetType.startsWith("Array[")) {
                    methodReturnType = analyzeArrayMethod(methodName, targetType, methodCallNode);
                } else {
                    throw new RuntimeException("Invalid method call on type: " + targetType);
                }
        }

        // Recursively analyze nested method calls in arguments
        for (ASTNode child : methodCallNode.getChildren()) {
            if (child.getNodeType().equals("MethodCall")) {
                analyzeMethodCall(child);
            }
        }

        System.out.println("Method " + methodName + " returns type: " + methodReturnType);
        return methodReturnType;
    }

    private String analyzeNumericMethod(String methodName, String targetType, ASTNode methodCallNode) {
        if (!List.of("Mult", "Plus", "Minus", "Divide", "LessEqual", "Rem", "Equal").contains(methodName)) {
            throw new RuntimeException("Unknown method " + methodName + " for numeric type " + targetType);
        }

        List<ASTNode> args = methodCallNode.getChildren();

        switch (methodName) {
            case "LessEqual":
                // Ensure LessEqual has two numeric arguments and returns Boolean
                if (args.size() != 2) {
                    throw new RuntimeException("LessEqual expects 2 arguments, but got " + args.size());
                }
                if (!targetType.equals(getExpressionType(args.get(0))) || !targetType.equals(getExpressionType(args.get(1)))) {
                    throw new RuntimeException("Argument types for LessEqual must match and be of type " + targetType);
                }
                return "Boolean";

            case "Rem":
                // Ensure Rem has two numeric arguments of the same type
                if (args.size() != 2) {
                    throw new RuntimeException("Rem expects 2 arguments, but got " + args.size());
                }
                if (!targetType.equals(getExpressionType(args.get(0))) || !targetType.equals(getExpressionType(args.get(1)))) {
                    throw new RuntimeException("Argument types for Rem must match and be of type " + targetType);
                }
                return targetType;

            case "Equal":
                // Ensure Equal has two arguments of the same type and returns Boolean
                if (args.size() != 2) {
                    throw new RuntimeException("Equal expects 2 arguments, but got " + args.size());
                }
                String leftType = getExpressionType(args.get(0));
                String rightType = getExpressionType(args.get(1));
                if (!leftType.equals(rightType)) {
                    throw new RuntimeException("Argument types for Equal must match, but got " + leftType + " and " + rightType);
                }
                return "Boolean";

            default:
                // Validate arguments for other numeric methods (e.g., Mult, Plus, Minus, Divide)
                validateArguments(methodCallNode, targetType);
                return targetType;
        }
    }

    private String analyzeStringMethod(String methodName, ASTNode methodCallNode) {
        if (!List.of("Concat", "Substring").contains(methodName)) {
            throw new RuntimeException("Unknown method " + methodName + " for type String");
        }

        if ("Concat".equals(methodName)) {
            validateArguments(methodCallNode, "String");
            return "String";
        } else if ("Substring".equals(methodName)) {
            List<ASTNode> args = methodCallNode.getChildren();
            if (args.size() != 3) {
                throw new RuntimeException("Substring method expects 3 arguments, but got " + args.size());
            }
            // Validate index arguments
            if (!"Integer".equals(getExpressionType(args.get(1))) || !"Integer".equals(getExpressionType(args.get(2)))) {
                throw new RuntimeException("Substring index arguments must be of type Integer");
            }
            return "String";
        }

        throw new RuntimeException("Unhandled String method: " + methodName);
    }

    private String analyzeBooleanMethod(String methodName, ASTNode methodCallNode) {
        if (!List.of("And", "Or", "Not").contains(methodName)) {
            throw new RuntimeException("Unknown method " + methodName + " for type Boolean");
        }

        List<ASTNode> args = methodCallNode.getChildren();
        if ("Not".equals(methodName)) {
            if (args.size() != 2) {
                throw new RuntimeException("Not method expects 1 argument, but got " + args.size());
            }
            if (!"Boolean".equals(getExpressionType(args.get(1)))) {
                throw new RuntimeException("Not argument must be of type Boolean");
            }
        } else {
            validateArguments(methodCallNode, "Boolean");
        }
        return "Boolean";
    }

    private String analyzeArrayMethod(String methodName, String targetType, ASTNode methodCallNode) {
        // Extract the element type from Array[T]
        String elementType = targetType.substring(6, targetType.length() - 1);

        switch (methodName) {
            case "get":
                // Ensure `get` method has one Integer argument
                List<ASTNode> getArgs = methodCallNode.getChildren();
                if (getArgs.size() != 2) {
                    throw new RuntimeException("Array get method expects 1 argument, but got " + (getArgs.size() - 1));
                }
                if (!"Integer".equals(getExpressionType(getArgs.get(1)))) {
                    throw new RuntimeException("Array get method index must be of type Integer");
                }
                return elementType;

            case "set":
                // Ensure `set` method has two arguments: Integer (index) and elementType (value)
                List<ASTNode> setArgs = methodCallNode.getChildren();
                if (setArgs.size() != 3) {
                    throw new RuntimeException("Array set method expects 2 arguments, but got " + (setArgs.size() - 1));
                }
                if (!"Integer".equals(getExpressionType(setArgs.get(1)))) {
                    throw new RuntimeException("Array set method index must be of type Integer");
                }
                if (!elementType.equals(getExpressionType(setArgs.get(2)))) {
                    throw new RuntimeException("Array set method value must be of type " + elementType);
                }
                return "Void";

            default:
                throw new RuntimeException("Unknown array method: " + methodName);
        }
    }

    private void validateArguments(ASTNode methodCallNode, String expectedType) {
        // Validate that all arguments match the expected type
        List<ASTNode> args = methodCallNode.getChildren();
        for (int i = 1; i < args.size(); i++) {
            String argType = getExpressionType(args.get(i));
            if (!expectedType.equals(argType)) {
                throw new RuntimeException("Argument type mismatch: expected " + expectedType + ", but got " + argType);
            }
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
            case "RealLiteral":
                System.out.println("RealLiteral: " + expressionNode.getNodeName());
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
            case "BinaryOperation":
                analyzeBinaryOperation(expressionNode);
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

    private void analyzeBinaryOperation(ASTNode binaryNode) {
        if (!binaryNode.getNodeName().equals("Mult")) {
            throw new RuntimeException("Unsupported binary operation: " + binaryNode.getNodeName());
        }

        ASTNode leftOperand = binaryNode.getChildren().get(0);
        ASTNode rightOperand = binaryNode.getChildren().get(1);

        String leftType = getExpressionType(leftOperand);
        String rightType = getExpressionType(rightOperand);

        if (!"Real".equals(leftType) || !"Real".equals(rightType)) {
            throw new RuntimeException("Operands of Mult must be of type Real, got: " + leftType + " and " + rightType);
        }

        System.out.println("Binary operation " + binaryNode.getNodeName() + " validated with Real operands.");
    }

    private boolean isGlobal(ASTNode varDeclNode) {
        ASTNode parent = varDeclNode.getParent();

        // If the parent node is a "Program" node, the variable is global
        if (parent != null && parent.getNodeType().equals("Program")) {
            return true; // It's a global variable
        }

        // If the parent node is a "Method" or "Constructor" node, the variable is local
        if (parent != null && (parent.getNodeType().equals("method") || parent.getNodeType().equals("constructor"))) {
            return false; // It's a local variable
        }

        // If the parent node is a "Class" node, it could be a class-level variable, treat it as global if it's outside methods
        if (parent != null && parent.getNodeType().equals("class")) {
            return true; // It's a global variable within the class
        }

        // If we can't determine the scope, assume it might be local or needs more checks
        return false;
    }

}
