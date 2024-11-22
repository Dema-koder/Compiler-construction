package bytecode;

import ast.ASTNode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BytecodeGenerator {
    private final Logger log = Logger.getLogger(this.getClass().getName());

    private final StringBuilder bytecode;

    private String className;

    public BytecodeGenerator() {
        this.bytecode = new StringBuilder();
    }

    public void generate(ASTNode root) {
        if (!"Program".equals(root.getNodeType())) {
            throw new IllegalArgumentException("Root node must be of type 'Program'");
        }

        for (ASTNode child : root.getChildren()) {
            if ("class".equals(child.getNodeType())) {
                generateClass(child);
            }
        }

        //return bytecode.toString();
    }

    private void generateClass(ASTNode classNode) {
        className = classNode.getNodeName();
        bytecode.append(".class public ").append(className).append("\n");
        bytecode.append(".super java/lang/Object\n\n");

        for (ASTNode child : classNode.getChildren()) {
            switch (child.getNodeType()) {
                case "declaration":
                    generateField(className, child);
                    break;
                case "constructor":
                    generateConstructor(className, child);
                    break;
                case "method":
                    generateMethod(className, child);
                    break;
                case "extends":
                    continue;
                default:
                    throw new UnsupportedOperationException("Unknown class element: " + child.getNodeType());
            }
        }

        try (FileOutputStream fos = new FileOutputStream("/Users/demanzverev/IdeaProjects/compiler-construction/src/main/java/examples/" + className + ".j")) {
            fos.write(bytecode.toString().getBytes());
            log.info("Generated class " + className);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateField(String className, ASTNode varNode) {
        String fieldName = varNode.getNodeName();
        String fieldType = mapType(varNode.getNodeTypeInfo());
        if (fieldType == null) {
            bytecode.append("new ").append(className).append("\n");
            bytecode.append("dup\n");
            if (varNode.getChildren().get(0).getNodeType().equals("ConstructorCall")) {
                bytecode.append("iconst_1\n").append("bipush 10\n")
                        .append("ldc \"text\"\n")
                        .append("invokespecial ").append(className).append("/<init>(ILjava/lang/String;)V\n");
            }
        } else {
            bytecode.append(".field private ").append(fieldName).append(" ").append(fieldType).append("\n");
        }
    }

    private void generateConstructor(String className, ASTNode constructorNode) {
        bytecode.append("\n.method public <init>(");

        Map types = new HashMap<String, String>();
        for (ASTNode arg : constructorNode.getChildren()) {
            if ("argument".equals(arg.getNodeType())) {
                bytecode.append(mapType(arg.getNodeTypeInfo()));
                types.put(arg.getNodeName(), mapType(arg.getNodeTypeInfo()));
            }
        }

        bytecode.append(")V\n");
        bytecode.append("    .limit stack 2\n");
        bytecode.append("    .limit locals ").append(constructorNode.getChildren().size() + 1).append("\n");
        bytecode.append("    aload_0\n");
        bytecode.append("    invokespecial java/lang/Object/<init>()V\n");

        int localIndex = 1;
        for (ASTNode child : constructorNode.getChildren()) {
            if ("assignment".equals(child.getNodeType())) {
                ASTNode targetNode = child.getChildren().get(0);
                if ("this".equals(child.getNodeTypeInfo())) {
                    bytecode.append("    aload_0\n");
                    bytecode.append("    iload_").append(localIndex++).append("\n");
                    bytecode.append("    putfield ").append(className).append("/").append(targetNode.getNodeName()).append(" ").append(types.get(targetNode.getNodeName())).append("\n");
                }
            }
        }

        bytecode.append("    return\n");
        bytecode.append(".end method\n");
    }

    private void generateMethod(String className, ASTNode methodNode) {
        String returnType = "V";
        String methodName = methodNode.getNodeName();
        if (methodName.equals("main"))
            bytecode.append(".method public static main([Ljava/lang/String;)V\n");
        else
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
        if (!methodName.equals("main"))
            bytecode.append(")").append(returnType).append("\n");
        bytecode.append("    .limit stack 3\n");
        bytecode.append("    .limit locals 3\n");

        for (ASTNode child : methodNode.getChildren()) {
            switch (child.getNodeType()) {
                case "assignment":
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
                case "IfStatement":
                    generateIfStatement(child);
                    break;
                case "WhileStatement":
                    generateWhileStatement(child);
                    break;
                case "declaration":
                    generateField(className, child);
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
            if ("identifier".equals(targetNode.getNodeType()) && "this".equals(assignNode.getNodeTypeInfo())) {
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

            if ("identifier".equals(targetNode.getNodeType())) {
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

        if (methodName.equals("print")) {

            bytecode.append("getstatic java/lang/System/out Ljava/io/PrintStream;\n");

            for (ASTNode argumentNode : methodCallNode.getChildren()) {
                generateExpression(argumentNode);
            }
            bytecode.append("invokevirtual java/io/PrintStream/println(I)V\n");
            return;
        }

        bytecode.append("    invokevirtual ").append(className).append("/").append(methodName).append("()V\n");
    }

    private void generateExpression(ASTNode exprNode) {
        switch (exprNode.getNodeType()) {
            case "NumberLiteral":
                bytecode.append("    ldc ").append(exprNode.getNodeName()).append("\n");
                break;

            case "identifier":
                bytecode.append("aload_0\n").append("getfield ").append(className).append("/").append(exprNode.getNodeName()).append(" I\n");
                break;

            case "MethodCall":
                generateMethodCall(exprNode);
                break;

            default:
                throw new UnsupportedOperationException("Unknown expression type: " + exprNode.getNodeType());
        }
    }

    private void generateIfStatement(ASTNode ifStatementNode) {
        String labelTrue = "L" + System.nanoTime();
        String labelEnd = "L" + System.nanoTime();

        ASTNode conditionNode = ifStatementNode.getChildren().get(0);
        generateExpression(conditionNode);

        bytecode.append("  ifne ").append(labelTrue).append("\n");
        bytecode.append("  goto ").append(labelEnd).append("\n");

        bytecode.append(labelTrue).append(":\n");
        ASTNode trueBranchNode = ifStatementNode.getChildren().get(1);
        generateExpression(trueBranchNode);

        bytecode.append(labelEnd).append(":\n");
    }

    private void generateWhileStatement(ASTNode whileStatementNode) {
        String labelStart = "L" + System.nanoTime();
        String labelEnd = "L" + System.nanoTime();

        bytecode.append(labelStart).append(":\n");
        ASTNode conditionNode = whileStatementNode.getChildren().get(0);
        generateExpression(conditionNode);

        bytecode.append("  ifeq ").append(labelEnd).append("\n");

        ASTNode bodyNode = whileStatementNode.getChildren().get(1);
        generateExpression(bodyNode);

        bytecode.append("  goto ").append(labelStart).append("\n");
        bytecode.append(labelEnd).append(":\n");
    }

    private String mapType(String type) {
        switch (type) {
            case "Integer":
                return "I";
            case "String":
                return "Ljava/lang/String;";
            case "Real":
                return "D";
            case "Boolean":
                return "Z";
            case "Array[Integer]":
                return "[I";
            default:
                return null;
        }
    }
}