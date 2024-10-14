package ast;

import java.util.ArrayList;
import java.util.List;

public class ASTNode {
    private String nodeType;
    private String nodeName;
    private String nodeTypeInfo;
    private List<ASTNode> children;

    public ASTNode(String nodeType) {
        this(nodeType, null, null);
    }

    public ASTNode(String nodeType, String nodeName) {
        this(nodeType, nodeName, null);
    }

    public ASTNode(String nodeType, String nodeName, String nodeTypeInfo) {
        this.nodeType = nodeType;
        this.nodeName = nodeName;
        this.nodeTypeInfo = nodeTypeInfo;
        this.children = new ArrayList<>();
    }

    public void addChild(ASTNode child) {
        this.children.add(child);
    }

    public void addChildren(List<ASTNode> children) {
        this.children.addAll(children);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    private String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        String prefix = " ".repeat(indent * 2);
        sb.append(prefix).append(nodeType);

        if (nodeName != null) {
            sb.append(": ").append(nodeName);
        }

        if (nodeTypeInfo != null) {
            sb.append(" (").append(nodeTypeInfo).append(")");
        }

        sb.append("\n");

        for (ASTNode child : children) {
            sb.append(child.toString(indent + 1));
        }

        return sb.toString();
    }
}
