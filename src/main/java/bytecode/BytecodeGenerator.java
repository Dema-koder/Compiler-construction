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
                case "Declaration":
                    generateField(className, child);
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

    private void generateField(String className, ASTNode varNode) {
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
        bytecode.append("    .limit stack 2\n");
        bytecode.append("    .limit locals ").append(constructorNode.getChildren().size() + 1).append("\n");
        bytecode.append("    aload_0\n");
        bytecode.append("    invokespecial java/lang/Object/<init>()V\n");

        int localIndex = 1;
        for (ASTNode child : constructorNode.getChildren()) {
            if ("Assignment".equals(child.getNodeType())) {
                ASTNode targetNode = child.getChildren().get(0);
                if ("this".equals(child.getNodeTypeInfo())) {
                    bytecode.append("    aload_0\n");
                    bytecode.append("    iload_").append(localIndex++).append("\n");
                    bytecode.append("    putfield ").append(className).append("/").append(targetNode.getNodeName()).append(" I\n");
                }
            }
        }

        bytecode.append("    return\n");
        bytecode.append(".end method\n");
    }

    private void generateMethod(String className, ASTNode methodNode) {
        String returnType = "V";
        String methodName = methodNode.getNodeName();
        bytecode.append("\n.method public ").append(methodName).append("(");

        for (ASTNode arg : methodNode.getChildren()) {
            if ("Argument".equals(arg.getNodeType())) {
                bytecode.append(mapType(arg.getNodeTypeInfo()));
            }
        }
        for (ASTNode child : methodNode.getChildren()) {
            if ("ReturnType".equals(child.getNodeType())) {
                returnType = mapType(child.getNodeName());
            }
        }
        bytecode.append(")").append(returnType).append("\n");
        bytecode.append("    .limit stack 3\n");
        bytecode.append("    .limit locals 3\n");

        for (ASTNode child : methodNode.getChildren()) {
            switch (child.getNodeType()) {
                case "Assignment":
                    generateAssignment(child, className);
                    break;
                case "MethodCall":
                    generateMethodCall(child);
                    break;
                case "Argument":
                    break;
                case "ReturnType":
                    break;
                case "ReturnStatement":
                    generateReturnStatement(child, returnType);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown method element: " + child.getNodeType());
            }
        }
        bytecode.append("    return\n");
        bytecode.append(".end method\n");
    }

    private void generateReturnStatement(ASTNode returnNode, String returnType) {
        if (!"V".equals(returnType)) {
            ASTNode returnExpression = returnNode.getChildren().get(0);
            generateExpression(returnExpression);

            switch (returnType) {
                case "I":
                    bytecode.append("    ireturn\n");
                    break;
                case "Ljava/lang/String;":
                case "[I":
                    bytecode.append("    areturn\n");
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported return type: " + returnType);
            }
        }
    }

    private void generateAssignment(ASTNode assignNode, String className) {
        ASTNode targetNode = assignNode.getChildren().get(0);

        if (assignNode.getChildren().size() == 1) {
            if ("Identifier".equals(targetNode.getNodeType()) && "this".equals(assignNode.getNodeTypeInfo())) {
                String fieldName = targetNode.getNodeName();

                bytecode.append("    aload_0\n");
                bytecode.append("    aload_1\n");
                bytecode.append("    putfield ").append(className).append("/").append(fieldName).append(" I\n");
            } else {
                throw new IllegalArgumentException("Unexpected single child in assignment node: " + targetNode.getNodeType());
            }
        }
        else if (assignNode.getChildren().size() == 2) {
            ASTNode expressionNode = assignNode.getChildren().get(1);

            if ("Identifier".equals(targetNode.getNodeType())) {
                String varName = targetNode.getNodeName();

                generateExpression(expressionNode);

                bytecode.append("    astore_").append(varName).append("\n");
            } else if ("Declaration".equals(targetNode.getNodeType()) && "this".equals(assignNode.getNodeTypeInfo())) {
                String fieldName = targetNode.getNodeName();

                generateExpression(expressionNode);

                bytecode.append("    aload_0\n");
                bytecode.append("    swap\n");
                bytecode.append("    putfield ").append(className).append("/").append(fieldName).append(" I\n");
            } else {
                throw new IllegalArgumentException("Unknown assignment target type: " + targetNode.getNodeType());
            }
        } else {
            throw new IllegalArgumentException("Assignment node must have 1 or 2 children, but has " + assignNode.getChildren().size());
        }
    }

    private void generateMethodCall(ASTNode methodCallNode) {
        String methodName = methodCallNode.getNodeName();

        bytecode.append("    aload_0\n");

        for (ASTNode argumentNode : methodCallNode.getChildren()) {
            generateExpression(argumentNode);
        }

        if (methodName.equals("Plus")) {

        }
        bytecode.append("    invokevirtual ").append(methodName).append("()I\n");
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
            case "Array[Integer]":
                return "[I";
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}