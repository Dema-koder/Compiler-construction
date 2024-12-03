package bytecode;

import ast.ASTNode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class BytecodeGenerator {
    private final Logger log = Logger.getLogger(this.getClass().getName());

    private final StringBuilder bytecode;

    private String className;

    private Map<String, Method> methods = new HashMap<>();

    private List<Param> params = new ArrayList<>();

    private String ifNum = "A";

    private String whileNum = "B";

    public BytecodeGenerator() {
        this.bytecode = new StringBuilder();
        methods.put("getValue", new Method("getValue", "Integer", "", "LInteger;"));
        methods.put("Plus", new Method("Plus", "Integer", "LInteger;", "LInteger;"));
        methods.put("Minus", new Method("Minus", "Integer", "LInteger;", "LInteger;"));
        methods.put("Mult", new Method("Mult", "Integer", "LInteger;", "LInteger;"));
        methods.put("Div", new Method("Div", "Integer", "LInteger;", "LInteger;"));
        methods.put("Rem", new Method("Rem", "Integer", "LInteger;", "LInteger;"));
        methods.put("Less", new Method("Less", "Integer", "LInteger;", "LBoolean;"));
        methods.put("LessEqual", new Method("LessEqual", "Integer", "LInteger;", "LBoolean;"));
        methods.put("Greater", new Method("Greater", "Integer", "LInteger;", "LBoolean;"));
        methods.put("GreaterEqual", new Method("GreaterEqual", "Integer", "LInteger;", "LBoolean;"));
        methods.put("Equal", new Method("Equal", "Integer", "LInteger;", "LBoolean;"));
        methods.put("toReal", new Method("toReal", "Integer", "", "LReal;"));
        methods.put("toBoolean", new Method("toBoolean", "Integer", "", "LBoolean;"));
        methods.put("UnaryMinus", new Method("UnaryMinus", "Integer", "", "LInteger;"));
        methods.put("toInteger", new Method("toInteger", "Boolean", "", "LInteger;"));
        methods.put("Or", new Method("Or", "Boolean", "LBoolean;", "LBoolean;"));
        methods.put("And", new Method("And", "Boolean", "LBoolean;", "LBoolean;"));
        methods.put("Xor", new Method("Xor", "Boolean", "LBoolean;", "LBoolean;"));
        methods.put("Not", new Method("Not", "Boolean", "", "LBoolean;"));
        methods.put("getBool", new Method("getBool", "Boolean", "", "Ljava/lang/String;"));
    }

    public void generate(ASTNode root) throws Exception {
        if (!"Program".equals(root.getNodeType())) {
            throw new IllegalArgumentException("Root node must be of type 'Program'");
        }

        for (ASTNode child : root.getChildren()) {
            if ("class".equals(child.getNodeType())) {
                generateClass(child);
            }
        }
    }

    private void generateClass(ASTNode classNode) {
        className = classNode.getNodeName();
        bytecode.append(".class public ").append(className).append("\n");
        bytecode.append(".super java/lang/Object\n\n");

        for (ASTNode child : classNode.getChildren()) {
            switch (child.getNodeType()) {
                case "declaration":
                    generateClassDeclaration(child);
                    break;
                case "constructor":
                    generateConstructor(child);
                    break;
                case "method":
                    generateMethod(child);
                    break;
                case "extends":
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown class element: " + child.getNodeType());
            }
        }

        try (FileOutputStream fos = new FileOutputStream("/Users/demanzverev/IdeaProjects/compiler-construction/src/main/java/examples/" + className + ".j")) {
            fos.write(bytecode.toString().getBytes());
            log.info("Generated class " + className);
            bytecode.setLength(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateClassDeclaration(ASTNode varNode) {
        String fieldName = varNode.getNodeName();
        String fieldType = mapType(varNode.getNodeTypeInfo());
        var param = new Param(fieldName, varNode.getNodeTypeInfo(), className, true, null, false, null);
        bytecode.append(".field private ").append(fieldName).append(" ").append(fieldType).append("\n");
        param.setType(fieldType);
        params.add(param);
    }

    private void generateConstructor(ASTNode constructorNode) {
        bytecode.append("\n.method public <init>(");

        Map types = new HashMap<String, String>();
        int k = 1;
        for (ASTNode arg : constructorNode.getChildren()) {
            if ("argument".equals(arg.getNodeType())) {
                bytecode.append(mapType(arg.getNodeTypeInfo()));
                types.put(arg.getNodeName(), Integer.toString(k++));
            }
        }

        bytecode.append(")V\n");
        bytecode.append("    .limit stack 1000\n"); ////////////////////////
        bytecode.append("    .limit locals 1000\n");
        bytecode.append("    aload_0\n");
        bytecode.append("    invokespecial java/lang/Object/<init>()V\n");

        for (ASTNode child : constructorNode.getChildren()) {
            if ("assignment".equals(child.getNodeType())) {
                generateAssignmentInConstructor(child, types);
            }
        }

        bytecode.append("    return\n");
        bytecode.append(".end method\n");
    }

    private void generateMethod(ASTNode methodNode) {
        String methodName = methodNode.getNodeName();
        Method method = new Method();
        method.setName(methodName);
        method.setClassName(className);
        int k = 1;
        String returnType = "V";
        if (methodName.equals("main"))
            bytecode.append(".method public static main([Ljava/lang/String;)V\n");
        else {
            bytecode.append("\n.method public ").append(methodName).append("(");
            StringBuilder methodSignature = new StringBuilder();
            for (ASTNode child : methodNode.getChildren()) {
                if ("argument".equals(child.getNodeType())) {
                    var param = new Param(child.getNodeName(), null, className, false, methodName, true, Integer.toString(k++));
                    bytecode.append(mapType(child.getNodeTypeInfo()));
                    methodSignature.append(mapType(child.getNodeTypeInfo()));
                    param.setType(mapType(child.getNodeTypeInfo()));
                    params.add(param);
                }
                if ("ReturnType".equals(child.getNodeType())) {
                    returnType = mapType(child.getNodeName());
                }
            }
            method.setSignature(methodSignature.toString());
            method.setReturnType(returnType);
            methods.put(methodName, method);
            bytecode.append(")").append(returnType).append("\n");
        }

        bytecode.append("    .limit stack 1000\n");
        bytecode.append("    .limit locals 1000\n");

        bytecode.append("aload_0\n");

        for (ASTNode child : methodNode.getChildren()) {
            switch (child.getNodeType()) {
                case "assignment":
                    generateAssignmentInMethod(child);
                    break;
                case "argument", "ReturnType":
                    break;
                case "MethodCall":
                    generateMethodCall(child);
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
                    var param = new Param(child.getNodeName(), mapType(child.getNodeTypeInfo()), className, false, methodName, true, Integer.toString(k++));
                    params.add(param);
                    generateDeclarationInMethod(param, child);
                    break;
                case "identifier":
                    //generateMethodIdentifier(child);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown method element: " + child.getNodeType());
            }
        }
        bytecode.append(".end method\n");
    }

    private void generateDeclarationInMethod(Param param, ASTNode child) {
        if (child.getChildren().isEmpty()) {
            return;
        }
        var target = child.getChildren().get(0);
        if (target.getNodeType().equals("ConstructorCall")) {
            bytecode.append("new ").append(target.getNodeName()).append("\ndup\n");

            StringBuilder constructorType = new StringBuilder();
            for (ASTNode child2 : target.getChildren()) {
                switch (child2.getNodeType()) {
                    case "NumberLiteral":
                        bytecode.append("ldc ").append(child2.getNodeName()).append("\n");
                        constructorType.append("I");
                        break;
                    case "StringLiteral":
                        bytecode.append("ldc ").append(child2.getNodeName()).append("\n");
                        constructorType.append("Ljava/lang/String;");
                        break;
                    case "identifier":
                        var param2 = findParam(child2.getNodeName(), className);
                        bytecode.append("aload").append((Integer.parseInt(param2.getLocalPosition()) > 3 ? " " : "_")).append(param2.getLocalPosition()).append("\n");
                        constructorType.append(param2.getType());
                        break;
                }
            }
            bytecode.append("invokespecial ").append(target.getNodeName()).append("/<init>(").append(constructorType).append(")V\n");
            bytecode.append("astore").append((Integer.parseInt(param.getLocalPosition()) > 3 ? " " : "_")).append(param.getLocalPosition()).append("\n");
        }
    }

    private void generateMethodIdentifier(ASTNode child, Map types) {
        if (types.get(child.getNodeName()) != null) {
            //bytecode.append("new ").append(className).append("\n").
        }
    }

    private void generateReturnStatement(ASTNode returnNode, String returnType) {
        if (!"V".equals(returnType)) {
            switch (returnNode.getChildren().get(0).getNodeType()) {
                case "identifier":
                    var param = findParam(returnNode.getChildren().get(0).getNodeName(), className);
                    assert param != null;
                    bytecode.append("aload");
                    bytecode.append((Integer.parseInt(param.getLocalPosition()) > 3 ? " " : "_")).append(param.getLocalPosition()).append("\n");
                    break;
                case "StringLiteral", "NumberLiteral":
                    bytecode.append("ldc ").append(returnNode.getChildren().get(0).getNodeName()).append("\n");
                    break;
                case "FieldAccess":
                    var cur = returnNode.getChildren().get(0).getChildren().get(0);
                    var param2 = findParam(cur.getNodeName(), className);
                    bytecode.append("getfield ").append(className).append("/").append(cur.getNodeName()).append(" ").append(param2.getType()).append("\n");
                    break;
                default:
                    break;
            }

            if (returnType.equals("I")) {
                bytecode.append("    ireturn\n");
            } else {
                bytecode.append("    areturn\n");
            }
        } else {
            bytecode.append("    return\n");
        }
    }

    private void generateAssignmentInConstructor(ASTNode assignNode, Map types) {
        for (ASTNode child : assignNode.getChildren()) {
            var param = findParam(assignNode.getNodeName(), className);
            switch (child.getNodeType()) {
                case "StringLiteral":
                    assert param != null;
                    bytecode.append("aload_0\n").append("ldc ")
                            .append(child.getNodeName()).append("\n")
                            .append("putfield ").append(className)
                            .append("/").append(assignNode.getNodeName()).append(" ")
                            .append(param.getType())
                            .append("\n");
                    break;
                case "NumberLiteral":
                    bytecode.append("new Integer\ndup\n")
                            .append("ldc ").append(child.getNodeName()).append("\n")
                            .append("invokespecial Integer/<init>(I)V\n")
                            .append("putfield ").append(className)
                            .append("/").append(assignNode.getNodeName()).append(" ")
                            .append(param.getType())
                            .append("\n");
                    break;
                case "identifier":
                    assert param != null;
                    bytecode.append("aload_0\n").append("aload")
                            .append((Integer.parseInt((String) types.get(child.getNodeName())) > 3 ? " " : "_"))
                            .append(types.get(child.getNodeName()))
                            .append("\n").append("putfield ").append(className)
                            .append("/").append(assignNode.getNodeName()).append(" ")
                            .append(param.getType())
                            .append("\n");
                case "MethodCall":
                    break;
            }
        }
    }

    private void generateAssignmentInMethod(ASTNode assignNode) {
        var identifier = assignNode.getChildren().get(0);
        var value = assignNode.getChildren().get(1);
        var param = findParam(identifier.getNodeName(), className);
        assert param != null;

        switch (value.getNodeType()) {
            case "NumberLiteral", "StringLiteral":
                bytecode.append("new Integer\n" +
                                "dup\n").append("ldc ").append(value.getNodeName()).append("\n")
                        .append("invokespecial Integer/<init>(I)V\n")
                        .append("astore").append((Integer.parseInt(param.getLocalPosition()) > 3 ? " " : "_")).append(param.getLocalPosition()).append("\n");
                break;
            case "MethodCall":
                generateMethodCall(value);
                bytecode.append("astore").append((Integer.parseInt(param.getLocalPosition()) > 3 ? " " : "_")).append(param.getLocalPosition()).append("\n");
                break;
        }
    }

    private void generateMethodCall(ASTNode methodCallNode) {
        String methodName = methodCallNode.getNodeName();
        if (methodName.equals("Plus") || methodName.equals("Minus") || methodName.equals("Mult")
                || methodName.equals("Div") || methodName.equals("Rem") || methodName.equals("Less")
                || methodName.equals("LessEqual") || methodName.equals("Greater") || methodName.equals("GreaterEqual")
                || methodName.equals("Equal")) {
            for (ASTNode child : methodCallNode.getChildren()) {
                switch (child.getNodeType()) {
                    case "identifier":
                        var localParam = findParam(child.getNodeName(), className);
                        assert localParam != null;
                        if (localParam.getIsMethodParam())
                            bytecode.append("aload").append((Integer.parseInt(localParam.getLocalPosition()) > 3 ? " " : "_")).append(localParam.getLocalPosition())
                                    .append("\n");
                        else
                            bytecode.append("getfield ").append(className).append("/")
                                    .append(localParam.getName()).append(localParam.getType()).append("\n");
                        break;
                    case "NumberLiteral":
                        bytecode.append("ldc ").append(child.getNodeName()).append("\n");
                        break;
                }
            }
            bytecode.append("invokevirtual ").append(methods.get(methodName).getClassName()).append("/").append(methodName)
                    .append("(").append(methods.get(methodName).getSignature()).append(")")
                    .append(methods.get(methodName).getReturnType()).append("\n");
        } else
        if (methodName.equals("print")) {

            bytecode.append("getstatic java/lang/System/out Ljava/io/PrintStream;\n");

            var param = findParam(methodCallNode.getChildren().get(0).getNodeName(), className);

            var returnType = "";
            switch (methodCallNode.getChildren().get(0).getNodeType()) {
                case "StringLiteral":
                    bytecode.append("ldc ").append(methodCallNode.getChildren().get(0).getNodeName()).append("\n");
                    returnType = "Ljava/lang/String;";
                    break;
                case "NumberLiteral":
                    bytecode.append("ldc ").append(methodCallNode.getChildren().get(0).getNodeName()).append("\n");
                    returnType = "I";
                    break;
                case "identifier":
                    bytecode.append("aload").append((Integer.parseInt(param.getLocalPosition()) > 3 ? " " : "_")).append(param.getLocalPosition()).append("\n");
                    var param2 = findParam(methodCallNode.getChildren().get(0).getNodeName(), className);
                    if (param2.getType().equals("LInteger;"))
                        bytecode.append("invokevirtual Integer/getValue()I\n");
                    if (param2.getType().equals("LBoolean;"))
                        bytecode.append("invokevirtual Boolean/getBool()Ljava/lang/String;\n");
                    break;
                case "MethodCall":
                    break;
            }
            if (param != null) {
                if (Objects.equals(param.getType(), "LInteger;"))
                    returnType = "I";
                else
                    if (Objects.equals(param.getType(), "LBoolean;"))
                        returnType = "Ljava/lang/String;";
                    else
                        returnType = param.getType();
            }
            bytecode.append("invokevirtual java/io/PrintStream/println(").append(returnType).append(")V\n");
        } else {
            for (ASTNode child : methodCallNode.getChildren()) {
                var param = findParam(child.getNodeName(), className);
                switch (child.getNodeType()) {
                    case "StringLiteral", "NumberLiteral":
                        bytecode.append("ldc ").append(child.getNodeName()).append("\n");
                        break;
                    case "identifier":
                        bytecode.append("aload").append((Integer.parseInt(param.getLocalPosition()) > 3 ? " " : "_")).append(param.getLocalPosition()).append("\n");
                        break;
                }
            }
            bytecode.append("invokevirtual ").append(methods.get(methodName).getClassName()).append("/").append(methodName)
                    .append("(").append(methods.get(methodName).getSignature()).append(")")
                    .append(methods.get(methodName).getReturnType()).append("\n");
        }
    }

    private void generateIfStatement(ASTNode ifStatementNode) {
        String elseLabel = ifNum + "C";
        String endLabel = ifNum + "D";
        for (ASTNode child : ifStatementNode.getChildren()) {
            switch (child.getNodeType()) {
                case "identifier":
                    var param2 = findParam(child.getNodeName(), className);
                    bytecode.append("aload").append((Integer.parseInt(param2.getLocalPosition()) > 3 ? " " : "_")).append(param2.getLocalPosition()).append("\n");
                    bytecode.append("invokevirtual Boolean/getBool()Ljava/lang/String;\nldc \"true\"\n" +
                            "invokevirtual java/lang/String/equals(Ljava/lang/Object;)Z\n" +
                            "ifeq ").append(elseLabel).append("\n");
                    break;
                case "ThenBlock", "ElseBlock":
                    if (child.getNodeType().equals("ElseBlock"))
                        bytecode.append(elseLabel).append(":\n");
                    for (ASTNode child2 : child.getChildren()) {
                        switch (child2.getNodeType()) {
                            case "assignment":
                                generateAssignmentInMethod(child2);
                                break;
                            case "MethodCall":
                                generateMethodCall(child2);
                                break;
                            case "IfStatement":
                                generateIfStatement(child2);
                                break;
                            case "WhileStatement":
                                generateWhileStatement(child2);
                                break;
                            default:
                                throw new UnsupportedOperationException("Unknown ifStatement element: " + child2.getNodeType());
                        }
                    }
                    if (child.getNodeType().equals("ThenBlock"))
                        bytecode.append("goto ").append(endLabel).append("\n");
                    else
                        bytecode.append(endLabel).append(":\n");
                    break;
            }
        }
        ifNum += "A";
    }

    private void generateWhileStatement(ASTNode whileStatementNode) {
        bytecode.append(whileNum).append(":\n");
        var whileEnd = whileNum + "E";
        for (ASTNode child : whileStatementNode.getChildren()) {
            if (child.getNodeType().equals("identifier") && child.getChildren().isEmpty()) {
                var param2 = findParam(child.getNodeName(), className);
                bytecode.append("aload").append((Integer.parseInt(param2.getLocalPosition()) > 3 ? " " : "_")).append(param2.getLocalPosition()).append("\n");
                bytecode.append("invokevirtual Boolean/getBool()Ljava/lang/String;\nldc \"true\"\n" +
                        "invokevirtual java/lang/String/equals(Ljava/lang/Object;)Z\n" +
                        "ifeq ").append(whileEnd).append("\n");
                break;
            }
        }
        for (ASTNode child : whileStatementNode.getChildren()) {
            switch (child.getNodeType()) {
                case "assignment":
                    generateAssignmentInMethod(child);
                    break;
                case "MethodCall":
                    generateMethodCall(child);
                    break;
                case "IfStatement":
                    generateIfStatement(child);
                    break;
                case "WhileStatement":
                    generateWhileStatement(child);
                    break;
                case "identifier":
                    if (child.getChildren().isEmpty())
                        break;
                    var localParam = findParam(child.getNodeName(), className);
                    assert localParam != null;
                    if (localParam.getIsMethodParam())
                        bytecode.append("aload").append((Integer.parseInt(localParam.getLocalPosition()) > 3 ? " " : "_")).append(localParam.getLocalPosition())
                                .append("\n");
                    else
                        bytecode.append("getfield ").append(className).append("/")
                                .append(localParam.getName()).append(localParam.getType()).append("\n");
                    generateMethodCall(child.getChildren().get(0));
                    break;
                default:
                    break;
            }
        }
        bytecode.append("goto ").append(whileNum).append("\n").append(whileEnd).append(":\n");
    }

    private String mapType(String type) {
        switch (type) {
            case "Integer":
                return "LInteger;";
            case "String":
                return "Ljava/lang/String;";
            case "Real":
                return "D";
            case "Boolean":
                return "LBoolean;";
            case "Array[Integer]":
                return "[LInteger;";
            default:
                return "L" + type + ";";
        }
    }

    private Param findParam(String paramName, String className) {
        for (Param param : params) {
            if (param.getName().equals(paramName) && param.getClassOwner().equals(className)) {
                return param;
            }
        }
        return null;
    }
}