package bytecode;

import ast.ASTNode;

public class BytecodeGenerator {
    private final StringBuilder bytecode;

    public BytecodeGenerator() {
        this.bytecode = new StringBuilder();
    }

    public String generate(ASTNode root) {
        if (!"Program".equals(root.getNodeType())) {
            throw new IllegalArgumentException("Root node must be of type 'Program'");
        }

        for (ASTNode child : root.getChildren()) {
            if ("Class".equals(child.getNodeType())) {
                generateClass(child);
            }
        }

        return bytecode.toString();
    }

    private void generateClass(ASTNode classNode) {
        String className = classNode.getNodeName();
        bytecode.append(".class public ").append(className).append("\n");
        bytecode.append(".super java/lang/Object\n\n");

        for (ASTNode child : classNode.getChildren()) {
            switch (child.getNodeType()) {
                case "VarDeclaration":
                    generateField(child);
                    break;
                case "Constructor":
                    generateConstructor(className, child);
                    break;
                case "Method":
                    generateMethod(className, child);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown class element: " + child.getNodeType());
            }
        }
    }

    private void generateField(ASTNode varNode) {
        String fieldName = varNode.getNodeName();
        String fieldType = mapType(varNode.getNodeTypeInfo());
        bytecode.append(".field private ").append(fieldName).append(" ").append(fieldType).append("\n");
    }

    private void generateConstructor(String className, ASTNode constructorNode) {
        bytecode.append("\n.method public <init>(");

        for (ASTNode arg : constructorNode.getChildren()) {
            if ("Argument".equals(arg.getNodeType())) {
                bytecode.append(mapType(arg.getNodeTypeInfo()));
            }
        }

        bytecode.append(")V\n");
        bytecode.append("    aload_0\n");
        bytecode.append("    invokespecial java/lang/Object/<init>()V\n");

        for (ASTNode child : constructorNode.getChildren()) {
            if ("VarDeclaration".equals(child.getNodeType())) {
                generateAssignment(child);
            }
        }

        bytecode.append("    return\n");
        bytecode.append(".end method\n");
    }

    private void generateMethod(String className, ASTNode methodNode) {
        String methodName = methodNode.getNodeName();
        bytecode.append("\n.method public ").append(methodName).append("(");

        for (ASTNode arg : methodNode.getChildren()) {
            if ("Argument".equals(arg.getNodeType())) {
                bytecode.append(mapType(arg.getNodeTypeInfo()));
            }
        }

        bytecode.append(")V\n");

        for (ASTNode child : methodNode.getChildren()) {
            switch (child.getNodeType()) {
                case "Assignment":
                    generateAssignment(child.getChildren().get(0));
                    break;
                case "MethodCall":
                    generateMethodCall(child);
                    break;
                case "Argument":
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown method element: " + child.getNodeType());
            }
        }

        bytecode.append("    return\n");
        bytecode.append(".end method\n");
    }

    private void generateAssignment(ASTNode assignNode) {


        // Первый потомок - это цель (переменная или поле объекта)
        ASTNode targetNode = assignNode;

        // Если это идентификатор, просто используем его
        if ("Identifier".equals(targetNode.getNodeType())) {
            String varName = targetNode.getNodeName();

            // Второй потомок - это выражение (например, метод вызова или литерал)
            ASTNode expressionNode = assignNode.getChildren().get(1);
            generateExpression(expressionNode);

            // Генерация байткода для присваивания переменной
            bytecode.append("    putfield ").append(varName).append("\n");
        }
        // Если это VarDeclaration с "this", то нужно обрабатывать как поле объекта
        else if ("VarDeclaration".equals(targetNode.getNodeType()) && "this".equals(targetNode.getNodeTypeInfo())) {
            String varName = targetNode.getNodeName();

            // Второй потомок - выражение (например, вызов метода)
            ASTNode expressionNode = assignNode.getChildren().get(0);
            generateExpression(expressionNode);

            // Генерация байткода для присваивания полю объекта
            bytecode.append("    putfield this.").append(varName).append("\n");
        } else {
            throw new IllegalArgumentException("Unknown assignment target type: " + targetNode.getNodeType());
        }
    }

    private void generateMethodCall(ASTNode methodCallNode) {
        ASTNode target = methodCallNode.getChildren().get(0);
        bytecode.append("    aload_0\n");
        for (int i = 1; i < methodCallNode.getChildren().size(); i++) {
            generateExpression(methodCallNode.getChildren().get(i));
        }
        bytecode.append("    invokevirtual ").append(target.getNodeName()).append("\n");
    }

    private void generateExpression(ASTNode exprNode) {
        switch (exprNode.getNodeType()) {
            case "NumberLiteral":
                bytecode.append("    ldc ").append(exprNode.getNodeName()).append("\n");
                break;
            case "Identifier":
                bytecode.append("    aload_").append(exprNode.getNodeName()).append("\n");
                break;
            case "MethodCall":
                generateMethodCall(exprNode);
                break;
            default:
                throw new UnsupportedOperationException("Unknown expression type: " + exprNode.getNodeType());
        }
    }

    private String mapType(String type) {
        switch (type) {
            case "Integer":
                return "I";
            case "String":
                return "Ljava/lang/String;";
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}