package org.opendaylight.controller.sal.java.api.generator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.ParameterizedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.yang.binding.Augmentable;

public class BuilderClassDescriptor {

    private static final String GET_PREFIX = "get";
    private static final String IMPORT_JAVA_UTIL_HASH_MAP = "java.util.HashMap";
    private static final String IMPORT_JAVA_UTIL_MAP = "java.util.Map";
    private static final String GET_AUGMENTATION_METHOD_NAME = "getAugmentation";

    private final GeneratedType genType;
    private Map<String, String> imports;
    private final String packageName;
    private final String className;
    private final Set<MethodDeclaration> methods;
    private final Set<FieldDeclaration> fields;
    private final List<String> importsNames;
    private FieldDeclaration augmentField;

    private class TypeDeclaration {

        private final static String JAVA_LANG_PREFIX = "java.lang";
        private final String name;
        private final TypeDeclaration[] generics;

        public TypeDeclaration(String fullyQualifiedName, TypeDeclaration... generics) {
            this.name = removeJavaLangPkgName(getRightTypeName(fullyQualifiedName));
            if (generics != null && generics.length > 0) {
                this.generics = generics;
            } else {
                this.generics = null;
            }
        }
        
        public TypeDeclaration(Type type) {
            if (type == null) {
                throw new IllegalArgumentException("type cannot be NULL");
            }
            this.name = removeJavaLangPkgName(getRightTypeName(type.getFullyQualifiedName()));
            TypeDeclaration[] generics = null;
            if (type instanceof ParameterizedType) {
                final ParameterizedType pType = (ParameterizedType) type;
                Type[] actualTypeArguments = pType.getActualTypeArguments();
                generics = new TypeDeclaration[actualTypeArguments.length];
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    generics[i] = new TypeDeclaration(actualTypeArguments[i].getFullyQualifiedName());
                }
            }
            if (generics != null && generics.length > 0) {
                this.generics = generics;
            } else {
                this.generics = null;
            }
        }

        private String removeJavaLangPkgName(String typeName) {
            if (typeName.startsWith(JAVA_LANG_PREFIX)) {
                return typeName.substring(typeName.lastIndexOf(Constants.DOT) + 1);
            }
            return typeName;
        }
        
        private String getRightTypeName(String fullyQualifiedName) {
            if (fullyQualifiedName == null) {
                throw new IllegalArgumentException("fullyQualifiedName cannot be NULL!");
            }
            if (!fullyQualifiedName.contains(Constants.DOT)) {
                throw new IllegalArgumentException(fullyQualifiedName + " is not fully qualified name!");
            }

            int lastDotIndex = fullyQualifiedName.lastIndexOf(Constants.DOT);
            String pkg = fullyQualifiedName.substring(0, lastDotIndex);
            String name = fullyQualifiedName.substring(lastDotIndex + 1);
            if (imports == null) {
                return name;
            }
            String pkgFromImports = imports.get(name);
            if (pkgFromImports == null || pkgFromImports.equals(pkg)) {
                return name;
            }
            return fullyQualifiedName;
        }

        @SuppressWarnings("unused")
        public String getName() {
            return name;
        }

        @SuppressWarnings("unused")
        public TypeDeclaration[] getGenerics() {
            return generics;
        }

    }

    private class ParameterDeclaration {

        private final TypeDeclaration type;
        private final String name;

        public ParameterDeclaration(TypeDeclaration type, String name) {
            this.type = type;
            this.name = name;
        }

        @SuppressWarnings("unused")
        public TypeDeclaration getType() {
            return type;
        }

        @SuppressWarnings("unused")
        public String getName() {
            return name;
        }

    }

    private class MethodDeclaration {

        private final TypeDeclaration returnType;
        private final String name;
        private final List<ParameterDeclaration> parameters;

        public MethodDeclaration(TypeDeclaration returnType, String name, List<ParameterDeclaration> parameters) {
            this.returnType = returnType;
            this.name = name;
            if (parameters != null && !parameters.isEmpty()) {
                this.parameters = parameters;
            } else {
                this.parameters = null;
            }
        }

        public TypeDeclaration getReturnType() {
            return returnType;
        }

        public String getName() {
            return name;
        }

        @SuppressWarnings("unused")
        public List<ParameterDeclaration> getParameters() {
            return parameters;
        }

    }

    private class FieldDeclaration extends ParameterDeclaration {

        public FieldDeclaration(TypeDeclaration type, String name) {
            super(type, name);
        }

    }
    

    public BuilderClassDescriptor(GeneratedType genType) {
        if (genType == null) {
            throw new IllegalArgumentException("GeneratedType reference cannot be NULL!");
        }
        this.genType = genType;
        this.imports = GeneratorUtil.createImports(genType);
        addToImports(genType.getFullyQualifiedName());
        packageName = genType.getPackageName();
        className = genType.getName();
        methods = createMethods();
        fields = createFieldsFromMethods();
        importsNames = createImportsNames();
    }
    
    private Set<MethodDeclaration> createMethods() {
        final Set<MethodDeclaration> methods = new LinkedHashSet<>();
        storeMethodsOfIfc(methods, genType);
        storeMethodsOfImplementedIfcs(methods, genType.getImplements());
        return methods;
    }

    private void storeMethodsOfIfc(Set<MethodDeclaration> methodStorage, GeneratedType ifc) {
        for (MethodSignature methodSignature : ifc.getMethodDefinitions()) {
            List<ParameterDeclaration> parameterDeclarations = getParameterDeclarationsFrom(methodSignature
                    .getParameters());
            methodStorage.add(new MethodDeclaration(new TypeDeclaration(methodSignature.getReturnType()),
                    methodSignature.getName(), parameterDeclarations));
        }
        if (ifc.getEnclosedTypes() != null && !ifc.getEnclosedTypes().isEmpty()) {
            addToImports(ifc.getFullyQualifiedName() + ".*");
        }
    }
    
    private List<ParameterDeclaration> getParameterDeclarationsFrom(List<MethodSignature.Parameter> parameters) {
        List<ParameterDeclaration> parameterDeclarations = new ArrayList<>();
        for (MethodSignature.Parameter mp : parameters) {
            parameterDeclarations.add(new ParameterDeclaration(new TypeDeclaration(mp.getType()), mp.getName()));
        }
        return parameterDeclarations;
    }

    private void storeMethodsOfImplementedIfcs(Set<MethodDeclaration> methodStorage, List<Type> implementedIfcs) {
        if (implementedIfcs == null || implementedIfcs.isEmpty()) {
            return;
        }
        for (Type implementedIfc : implementedIfcs) {
            if ((implementedIfc instanceof GeneratedType && !(implementedIfc instanceof GeneratedTransferObject))) {
                final GeneratedType ifc = ((GeneratedType) implementedIfc);
                storeMethodsOfIfc(methodStorage, ifc);
                storeMethodsOfImplementedIfcs(methodStorage, ifc.getImplements());
            } else if (implementedIfc.getFullyQualifiedName().equals(Augmentable.class.getName())) {
                for (Method m : Augmentable.class.getMethods()) {
                    if (m.getName().equals(GET_AUGMENTATION_METHOD_NAME)) {
                        addToImports(IMPORT_JAVA_UTIL_HASH_MAP);
                        addToImports(IMPORT_JAVA_UTIL_MAP);
                        java.lang.reflect.Type returnType = m.getReturnType();
                        addToImports(((Class<?>) returnType).getName());
                        TypeDeclaration augmentMethodType = new TypeDeclaration(((Class<?>) returnType).getName(), 
                                new TypeDeclaration(genType.getFullyQualifiedName()));
                        augmentField = createFieldFromGetMethod(new MethodDeclaration(augmentMethodType, m.getName(), null));
                    }
                }
            }
        }
    }
    
    private void addToImports(String fullyQualifiedName) {
        int lastDotIndex = fullyQualifiedName.lastIndexOf(Constants.DOT);
        String pkg = fullyQualifiedName.substring(0, lastDotIndex);
        String name = fullyQualifiedName.substring(lastDotIndex + 1);
        if (imports == null) {
            imports = new LinkedHashMap<>();
        }
        if (imports.get(name) == null) {
            imports.put(name, pkg);
        }
    }

    private Set<FieldDeclaration> createFieldsFromMethods() {
        if (methods == null || methods.isEmpty()) {
            return null;
        }

        final Set<FieldDeclaration> result = new LinkedHashSet<>();

        for (MethodDeclaration m : methods) {
            FieldDeclaration createdField = createFieldFromGetMethod(m);
            if (createdField != null) {
                result.add(createdField);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private FieldDeclaration createFieldFromGetMethod(MethodDeclaration method) {
        if (method == null || method.getName() == null || method.getName().isEmpty()) {
            return null;
        } else if (method.getName().startsWith(GET_PREFIX)) {
            String fieldName = method.getName().substring(GET_PREFIX.length());
            fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
            return new FieldDeclaration(method.getReturnType(), fieldName);
        }
        return null;
    }
    
    private List<String> createImportsNames() {
        if (imports == null || imports.isEmpty()) {
            return null;
        }

        final List<String> result = new ArrayList<>();

        for (Map.Entry<String, String> entry : imports.entrySet()) {
            final String typeName = entry.getKey();
            final String packageName = entry.getValue();
            result.add(packageName + Constants.DOT + typeName);
        }
        return result;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<String> getImportsNames() {
        return importsNames;
    }

    public String getClassName() {
        return className;
    }

    public Set<FieldDeclaration> getFields() {
        return fields;
    }

    public Set<MethodDeclaration> getMethods() {
        return methods;
    }

    public FieldDeclaration getAugmentField() {
        return augmentField;
    }

}
