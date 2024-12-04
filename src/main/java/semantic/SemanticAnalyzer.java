package semantic;

import ast.ASTNode;
import java.util.*;

public class SemanticAnalyzer {

    private final Map<String, String> symbolTable = new HashMap<>();
    private final Map<String, ClassDefinition> classTable = new HashMap<>();
    private final Map<String, String> globalSymbolTable = new HashMap<>();

    public void analyze(ASTNode root) {
        if (!root.getNodeType().equals("Program")) {
            throw new RuntimeException("Root node must be of type Program");
        }

        // Collect class definitions
        for (ASTNode child : root.getChildren()) {
            if (child.getNodeType().equals("class")) {
                collectClassDefinition(child);
            } else {
                throw new RuntimeException("Unexpected node type: " + child.getNodeType());
            }
        }

        // Analyze class members
        for (ASTNode child : root.getChildren()) {
            if (child.getNodeType().equals("class")) {
                child.setParent(root);
                analyzeClass(child);
            }
        }
    }

    public void optimize(ASTNode root) {
        removeUnusedVariables(root);
        removeUnreachableCode(root);
    }

    private void removeUnusedVariables(ASTNode root) {
        Map<String, ASTNode> declaredVariables = new HashMap<>();
        Set<String> usedVariables = new HashSet<>();

        // Collect all declared variables and their nodes
        collectDeclaredVariables(root, declaredVariables);

        // Collect all used variables
        collectUsedVariables(root, usedVariables, declaredVariables);

        // Remove declarations that are not in the used set
        for (Map.Entry<String, ASTNode> entry : declaredVariables.entrySet()) {
            if (!usedVariables.contains(entry.getKey())) {
                ASTNode parent = entry.getValue().getParent();
                parent.removeChild(entry.getValue());
            }
        }
    }

    private void collectDeclaredVariables(ASTNode node, Map<String, ASTNode> declaredVariables) {
        if (node == null) return;

        if (node.getNodeType().equals("declaration") || node.getNodeType().equals("argument")) {
            String variableName = node.getNodeName();
            declaredVariables.put(variableName, node);
        }

        for (ASTNode child : node.getChildren()) {
            collectDeclaredVariables(child, declaredVariables);
        }
    }

    private void collectUsedVariables(ASTNode node, Set<String> usedVariables, Map<String, ASTNode> declaredVariables) {
        if (node == null) return;

        if (node.getNodeType().equals("identifier")) {
            String variableName = node.getNodeName();
            if (declaredVariables.containsKey(variableName)) {
                usedVariables.add(variableName); // Mark as used if it exists in declaredVariables
            }
        }

        // Recursively check all children
        for (ASTNode child : node.getChildren()) {
            collectUsedVariables(child, usedVariables, declaredVariables);
        }
    }

    private void removeUnreachableCode(ASTNode root) {
        traverseAndRemoveUnreachableCode(root);
    }

    private boolean traverseAndRemoveUnreachableCode(ASTNode node) {
        if (node == null) return false;

        boolean foundReturn = false;

        Iterator<ASTNode> iterator = node.getChildren().iterator();
        while (iterator.hasNext()) {
            ASTNode child = iterator.next();

            if (foundReturn && (child.getNodeType().equals("method") || child.getNodeType().equals("class") || child.getNodeType().equals("ElseBlock"))) {
                foundReturn = false;
            }

            // If a return statement was found in this scope, mark subsequent siblings as unreachable.
            if (foundReturn) {
                iterator.remove(); // Remove unreachable node
                continue;
            }

            // Check if the child is a return statement
            if (child.getNodeType().equals("ReturnStatement")) {
                foundReturn = true;
                // Process the subtree of the return statement itself
                traverseAndRemoveUnreachableCode(child);
            } else {
                // If the child subtree has a return, this marks subsequent siblings unreachable.
                foundReturn = traverseAndRemoveUnreachableCode(child);
            }
        }

        return foundReturn;
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
                argTypes.add(arg.getNodeTypeInfo());
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
                    analyzeVarDeclaration(child);
                    break;
                case "constructor":
                    child.setParent(classNode);
                    analyzeConstructor();
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

    private void analyzeConstructor() {
        System.out.println("Analyzing constructor");
    }

    private void analyzeMethod(ASTNode methodNode, ClassDefinition classDef) {
        String methodName = methodNode.getNodeName();
        System.out.println("Analyzing method: " + methodName);

        symbolTable.clear(); // New symbol table for method scope

        String returnType = "Void";

        System.out.println("Analyzing method: " + methodName + " in class " + classDef.getName());

        for (ASTNode child : methodNode.getChildren()) {
            switch (child.getNodeType()) {
                case "ReturnType":
                    returnType = analyzeReturnType(child);
                    break;
                case "argument":
                    analyzeArgument(child, methodNode);
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
                case "identifier":
                    analyzeIdentifier(child);
                    break;
                default:
                    throw new RuntimeException("Unexpected method element: " + child.getNodeType());
            }
        }

        globalSymbolTable.put(methodName, returnType);
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

        if (thenBranch != null) {
            for (ASTNode child : thenBranch.getChildren()) {
                analyzeChildNode(child, methodReturnType);
            }
        } else {
            throw new RuntimeException("IfStatement is missing a then branch");
        }

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

        ASTNode returnValue = returnNode.getChildren().getFirst();
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
            case "Void":
                return "Void";
            case "identifier":
                // Check global symbol table first, then global symbol table
                String identifierType = symbolTable.get(expressionNode.getNodeName());
                if (identifierType == null) {
                    identifierType = globalSymbolTable.get(expressionNode.getNodeName());
                    if (identifierType == null) {
                        throw new RuntimeException("Undefined identifier: " + expressionNode.getNodeName());
                    }
                }
                return identifierType;
            case "MethodCall":
                return analyzeMethodCall(expressionNode);
            default:
                throw new RuntimeException("Unknown expression type: " + expressionNode.getNodeType());
        }
    }

    private void analyzeArgument(ASTNode argumentNode, ASTNode parent) {
        String argName = argumentNode.getNodeName();
        String argType = argumentNode.getNodeTypeInfo();

        if (symbolTable.containsKey(argName)) {
            throw new RuntimeException("Argument already declared: " + argName);
        }

        argumentNode.setParent(parent);
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
            String initializerType = child.getNodeType();

            // Check for type compatibility
            if (varType.equals("String") && !initializerType.equals("MethodCall") && !isTypeCompatible(varType, initializerType)) {
                throw new RuntimeException("Type mismatch: Cannot assign a value of type " + initializerType +
                        " to variable " + varName + " of type " + varType);
            }
        }
    }

    private boolean isTypeCompatible(String varType, String valueType) {
        if (varType.equals("Integer")) {
            varType = "NumberLiteral";
        } else if (varType.equals("String")) {
            varType = "StringLiteral";
        } else if (varType.equals("Boolean")) {
            varType = "BoolLiteral";
        }

        return varType.equals(valueType);
    }

    private void analyzeConstructorCall(ASTNode constructorCallNode) {
        System.out.println("Analyzing constructor call for type: " + constructorCallNode.getNodeName());

        String fullClassName = constructorCallNode.getNodeName();
        String baseClassName = parseBaseClassName(fullClassName);
        List<String> genericTypeParameters = parseGenericTypeParameters(fullClassName);

        // Check if the base class exists in the class table
        if (!classTable.containsKey(baseClassName) && !baseClassName.equals("Array")) {
            throw new RuntimeException("Class " + baseClassName + " is not defined.");
        }

        if (baseClassName.equals("Array")) {
            validateArrayGenericTypes(genericTypeParameters);

            if (!constructorCallNode.getChildren().isEmpty()) {
                ASTNode sizeArgument = constructorCallNode.getChildren().getFirst();
                String sizeArgType = getExpressionType(sizeArgument);
                if (!sizeArgType.equals("Integer")) {
                    throw new RuntimeException("Constructor for Array expects an Integer size argument, but got " + sizeArgType);
                }
            }
            return;
        }

        // Validate the constructor arguments
        ClassDefinition classDef = classTable.get(baseClassName);
        List<String> expectedArgTypes = classDef.getConstructorArgTypes();
        int expectedArgs = expectedArgTypes.size();
        int actualArgs = constructorCallNode.getChildren().size();

        if (expectedArgs != actualArgs) {
            throw new RuntimeException("Constructor for class " + baseClassName + " expects " +
                    expectedArgs + " arguments, but got " + actualArgs);
        }

        for (int i = 0; i < actualArgs; i++) {
            ASTNode arg = constructorCallNode.getChildren().get(i);
            String actualArgType = getExpressionType(arg);
            String expectedArgType = expectedArgTypes.get(i);

            System.out.println("Validating argument " + (i + 1) + ": expected " + expectedArgType + ", got " + actualArgType);

            if (!actualArgType.equals(expectedArgType)) {
                throw new RuntimeException("Type mismatch in constructor for class " + baseClassName +
                        ": expected " + expectedArgType + ", but got " + actualArgType + " for argument " + (i + 1));
            }
        }
    }

    private String parseBaseClassName(String fullClassName) {
        int genericStart = fullClassName.indexOf('[');
        return (genericStart == -1) ? fullClassName : fullClassName.substring(0, genericStart);
    }

    private List<String> parseGenericTypeParameters(String fullClassName) {
        int genericStart = fullClassName.indexOf('[');
        int genericEnd = fullClassName.lastIndexOf(']');
        if (genericStart == -1 || genericEnd == -1 || genericStart > genericEnd) {
            return List.of(); // No generic type parameters
        }
        String params = fullClassName.substring(genericStart + 1, genericEnd);
        return List.of(params.split(","));
    }

    private void validateArrayGenericTypes(List<String> genericTypeParameters) {
        if (genericTypeParameters.size() != 1) {
            throw new RuntimeException("Array expects exactly 1 generic type parameter, but got " + genericTypeParameters.size());
        }

        String elementType = genericTypeParameters.getFirst();
        List<String> allowedTypes = List.of("Integer", "String", "Boolean");

        System.out.println("Validating Array generic type: " + elementType);

        if (!allowedTypes.contains(elementType)) {
            throw new RuntimeException("Invalid generic type for Array: " + elementType +
                    ". Allowed types are: " + allowedTypes);
        }
    }

    private void analyzeWhile(ASTNode whileNode) {
        System.out.println("Analyzing WHILE loop");

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

        ASTNode lhs = assignmentNode.getChildren().get(0);
        analyzeExpression(lhs);

        ASTNode rhs = assignmentNode.getChildren().get(1);
        analyzeExpression(rhs);

        var expectedType = symbolTable.get(lhs.getNodeName());
        if (!rhs.getNodeType().equals("MethodCall") && !isTypeCompatible(expectedType, rhs.getNodeType())) {
            throw new RuntimeException("Type mismatch: Cannot assign a value of type " + rhs.getNodeType() +
                    " to variable " + lhs.getNodeName() + " of type " + expectedType);
        }

    }

    private String analyzeMethodCall(ASTNode methodCallNode) {
        System.out.println("Analyzing method call: " + methodCallNode.getNodeName());

        // Extract method name and determine the target type
        String methodName = methodCallNode.getNodeName();
        String methodReturnType;
        ASTNode targetNode = methodCallNode.getChildren().getFirst();
        String targetType = getExpressionType(targetNode);

        System.out.println("Target type: " + targetType);

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
        if (!List.of("Mult", "Plus", "Minus", "Divide", "LessEqual", "Rem", "Equal", "print").contains(methodName)) {
            throw new RuntimeException("Unknown method " + methodName + " for numeric type " + targetType);
        }

        List<ASTNode> args = methodCallNode.getChildren();

        if ("print".equals(methodName)) {
            // Ensure `print` has one argument of numeric type
            if (args.size() != 1) {
                throw new RuntimeException("print expects 1 argument, but got " + args.size());
            }
            if (!targetType.equals(getExpressionType(args.getFirst()))) {
                throw new RuntimeException("Argument type for print must match the numeric type " + targetType);
            }
            return "void";
        }

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
                // Validate arguments for other numeric methods
                validateArguments(methodCallNode, targetType);
                return targetType;
        }
    }

    private String analyzeStringMethod(String methodName, ASTNode methodCallNode) {
        if (!List.of("Concat", "Substring", "print").contains(methodName)) {
            throw new RuntimeException("Unknown method " + methodName + " for type String");
        }

        if ("print".equals(methodName)) {
            // Ensure `print` has one String argument
            List<ASTNode> args = methodCallNode.getChildren();
            if (args.size() != 1) {
                throw new RuntimeException("print expects 1 argument, but got " + args.size());
            }
            if (!"String".equals(getExpressionType(args.getFirst()))) {
                throw new RuntimeException("Argument type for print must be String");
            }
            return "void";
        }

        if ("Concat".equals(methodName)) {
            validateArguments(methodCallNode, "String");
            return "String";
        } else if ("Substring".equals(methodName)) {
            List<ASTNode> args = methodCallNode.getChildren();
            if (args.size() != 3) {
                throw new RuntimeException("Substring method expects 3 arguments, but got " + args.size());
            }
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
            if (args.size() != 1) {
                throw new RuntimeException("Not method expects 1 argument, but got " + args.size());
            }
            if (!"Boolean".equals(getExpressionType(args.getFirst()))) {
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
                if (globalSymbolTable.get(methodName) != null) {
                    return "Void";
                }
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
            case "BoolLiteral":
                System.out.println("BooleanLiteral: " + expressionNode.getNodeName());
                break;
            case "RealLiteral":
                System.out.println("RealLiteral: " + expressionNode.getNodeName());
                break;
            case "identifier":
                String identifierType = symbolTable.get(expressionNode.getNodeName());
                if (identifierType == null && (!expressionNode.getNodeName().equals("true") && (!expressionNode.getNodeName().equals("false")))) {
                    identifierType = globalSymbolTable.get(expressionNode.getNodeName());
                    if (identifierType == null) {
                        throw new RuntimeException("Undefined identifier: " + expressionNode.getNodeName());
                    }
                }
                System.out.println("Identifier: " + expressionNode.getNodeName());
                break;
            case "ConstructorCall":
                analyzeConstructorCall(expressionNode);
                break;
            case "MethodCall":
                System.out.println("MethodCall: " + expressionNode.getNodeName());
                for (ASTNode child : expressionNode.getChildren()) {
                    analyzeExpression(child);
                }
                break;
            case "FieldAccess":
                analyzeFieldAccess(expressionNode);
                break;
            case "BinaryOperation":
                analyzeBinaryOperation(expressionNode);
                break;
            default:
                throw new RuntimeException("Unexpected expression type: " + expressionNode.getNodeType());
        }
    }

    private void analyzeFieldAccess(ASTNode fieldAccessNode) {
        System.out.println("Analyzing field access: " + fieldAccessNode.getNodeName());
        if (!symbolTable.containsKey(fieldAccessNode.getNodeName())) {
            throw new RuntimeException("Undefined field: " + fieldAccessNode.getNodeName());
        }
    }

    private void analyzeBinaryOperation(ASTNode binaryNode) {
        System.out.println("Analyzing binary operation: " + binaryNode.getNodeName());

        if (!List.of("Mult", "Plus", "Minus", "Divide").contains(binaryNode.getNodeName())) {
            throw new RuntimeException("Unsupported binary operation: " + binaryNode.getNodeName());
        }

        // Validate both operands
        ASTNode leftOperand = binaryNode.getChildren().get(0);
        ASTNode rightOperand = binaryNode.getChildren().get(1);

        String leftType = getExpressionType(leftOperand);
        String rightType = getExpressionType(rightOperand);

        System.out.println("Left operand type: " + leftType);
        System.out.println("Right operand type: " + rightType);

        if (!leftType.equals(rightType)) {
            throw new RuntimeException("Type mismatch in binary operation: left=" + leftType + ", right=" + rightType);
        }

        // Numeric operations
        if (List.of("Mult", "Plus", "Minus", "Divide").contains(binaryNode.getNodeName())) {
            if (!List.of("Integer", "Real").contains(leftType)) {
                throw new RuntimeException("Binary operation requires numeric types, but got: " + leftType);
            }
        }

        // Print the result type for debugging
        System.out.println("Binary operation result type: " + leftType);
    }

    private boolean isGlobal(ASTNode varDeclNode) {
        ASTNode parent = varDeclNode.getParent();

        // If the parent node is a "Program" node, the variable is global
        if (parent != null && parent.getNodeType().equals("Program")) {
            return true;
        }

        // If the parent node is a "Method" or "Constructor" node, the variable is local
        if (parent != null && (parent.getNodeType().equals("method") || parent.getNodeType().equals("constructor"))) {
            return false;
        }

        // If the parent node is a "Class" node, it could be a class-level variable
        return parent != null && parent.getNodeType().equals("class");
    }
}
