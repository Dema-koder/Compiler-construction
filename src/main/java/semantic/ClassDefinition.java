package semantic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassDefinition {
    private final String name;
    private final String parentClass;
    private final Map<String, String> methods = new HashMap<>();
    private final List<String> constructorArgTypes;

    public ClassDefinition(String name, String parentClass, List<String> constructorArgTypes) {
        this.name = name;
        this.parentClass = parentClass;
        this.constructorArgTypes = constructorArgTypes;
    }

    public String getName() {
        return name;
    }

    public String getParentClass() {
        return parentClass;
    }

    public boolean hasMethod(String methodName) {
        return methods.containsKey(methodName);
    }

    public void addMethod(String methodName, String returnType) {
        methods.put(methodName, returnType);
    }

    public String getMethodReturnType(String methodName) {
        return methods.get(methodName);
    }

    public List<String> getConstructorArgTypes() {
        return constructorArgTypes;
    }
}
