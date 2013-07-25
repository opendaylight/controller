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
    private static final String JAVA_UTIL = "java.util";
    private static final String HASH_MAP = "HashMap";
    private static final String MAP = "Map";
    private static final String GET_AUGMENTATION_METHOD_NAME = "getAugmentation";

    private final GeneratedType genType;
    private Map<String, String> imports;
    private final String packageName;
    private final String className;
    private final Set<MethodDeclaration> methods;
    private final Set<FieldDeclaration> fields;
    private final List<String> importsNames;
    private FieldDeclaration augmentField;

    class TypeDeclaration {

        private final static String JAVA_LANG_PREFIX = "java.lang";
        private final String name;
        private final TypeDeclaration[] generics;

        public TypeDeclaration(String pkg, String name, TypeDeclaration... generics) {
            this.name = removeJavaLangPkgName(getRightTypeName(pkg, name));
            if (generics != null && generics.length > 0) {
                this.generics = generics;
            } else {
                this.generics = null;
            }
        }

        public TypeDeclaration(final Type type) {
            if (type == null) {
                throw new IllegalArgumentException("Type cannot be NULL");
            }

            this.name = removeJavaLangPkgName(getRightTypeName(type.getPackageName(), type.getName()));
            TypeDeclaration[] generics = null;
            if (type instanceof ParameterizedType) {
                final ParameterizedType pType = (ParameterizedType) type;
                final Type[] actualTypeArguments = pType.getActualTypeArguments();
                generics = new TypeDeclaration[actualTypeArguments.length];
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    generics[i] = new TypeDeclaration(actualTypeArguments[i].getPackageName(),
                            actualTypeArguments[i].getName());
                }
            }
            if (generics != null && generics.length > 0) {
                this.generics = generics;
            } else {
                this.generics = null;
            }
        }

        private String removeJavaLangPkgName(final String typeName) {
            if (typeName.startsWith(JAVA_LANG_PREFIX)) {
                return typeName.substring(typeName.lastIndexOf(Constants.DOT) + 1);
            }
            return typeName;
        }

        private String getRightTypeName(final String pkg, final String name) {
            if (name == null) {
                throw new IllegalArgumentException("Name cannot be NULL!");
            }

            if (imports == null) {
                return name;
            }
            final String pkgFromImports = imports.get(name);
            if (pkgFromImports == null || pkgFromImports.equals(pkg)) {
                return name;
            }
            return (pkg == null ? "" : pkg) + Constants.DOT + name;
        }

        public String getName() {
            return name;
        }

        public TypeDeclaration[] getGenerics() {
            return generics;
        }

    }

    class ParameterDeclaration {

        private final TypeDeclaration type;
        private final String name;

        public ParameterDeclaration(TypeDeclaration type, String name) {
            this.type = type;
            this.name = name;
        }

        public TypeDeclaration getType() {
            return type;
        }

        public String getName() {
            return name;
        }

    }

    class MethodDeclaration {

        private final TypeDeclaration returnType;
        private final String name;
        private final List<ParameterDeclaration> parameters;

        public MethodDeclaration(final TypeDeclaration returnType, final String name,
                final List<ParameterDeclaration> parameters) {
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

        public List<ParameterDeclaration> getParameters() {
            return parameters;
        }

    }

    class FieldDeclaration extends ParameterDeclaration {

        public FieldDeclaration(final TypeDeclaration type, final String name) {
            super(type, name);
        }

    }

    public BuilderClassDescriptor(final GeneratedType genType) {
        if (genType == null) {
            throw new IllegalArgumentException("Generated type reference cannot be NULL!");
        }
        this.genType = genType;
        this.imports = GeneratorUtil.createImports(genType);
        addToImports(genType.getPackageName(), genType.getName());
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

    private void storeMethodsOfIfc(final Set<MethodDeclaration> methodStorage, final GeneratedType ifc) {
        for (MethodSignature methodSignature : ifc.getMethodDefinitions()) {
            final List<ParameterDeclaration> parameterDeclarations = getParameterDeclarationsFrom(methodSignature
                    .getParameters());
            methodStorage.add(new MethodDeclaration(new TypeDeclaration(methodSignature.getReturnType()),
                    methodSignature.getName(), parameterDeclarations));
        }
        if (ifc.getEnclosedTypes() != null && !ifc.getEnclosedTypes().isEmpty()) {
            addToImports(ifc.getPackageName(), ifc.getName() + ".*");
        }
    }

    private List<ParameterDeclaration> getParameterDeclarationsFrom(final List<MethodSignature.Parameter> parameters) {
        final List<ParameterDeclaration> parameterDeclarations = new ArrayList<>();
        for (MethodSignature.Parameter mp : parameters) {
            parameterDeclarations.add(new ParameterDeclaration(new TypeDeclaration(mp.getType()), mp.getName()));
        }
        return parameterDeclarations;
    }

    private void storeMethodsOfImplementedIfcs(final Set<MethodDeclaration> methodStorage,
            final List<Type> implementedIfcs) {
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
                        addToImports(JAVA_UTIL, HASH_MAP);
                        addToImports(JAVA_UTIL, MAP);
                        java.lang.reflect.Type returnType = m.getReturnType();
                        final String fullyQualifiedName = ((Class<?>) returnType).getName();
                        addToImports(getPackageFrom(fullyQualifiedName), getNameFrom(fullyQualifiedName));
                        TypeDeclaration augmentMethodType = new TypeDeclaration(getPackageFrom(fullyQualifiedName),
                                getNameFrom(fullyQualifiedName), new TypeDeclaration(genType));
                        augmentField = createFieldFromGetMethod(new MethodDeclaration(augmentMethodType, m.getName(),
                                null));
                    }
                }
            }
        }
    }

    private void addToImports(final String pkg, final String name) {
        if (imports == null) {
            imports = new LinkedHashMap<>();
        }
        if (imports.get(name) == null) {
            imports.put(name, pkg);
        }
    }

    private String getPackageFrom(final String fullyQualifiedName) {
        final int lastDotIndex = fullyQualifiedName.lastIndexOf(Constants.DOT);
        return lastDotIndex == -1 ? "" : fullyQualifiedName.substring(0, lastDotIndex);
    }

    private String getNameFrom(final String fullyQualifiedName) {
        final int lastDotIndex = fullyQualifiedName.lastIndexOf(Constants.DOT);
        return lastDotIndex == -1 ? fullyQualifiedName : fullyQualifiedName.substring(lastDotIndex + 1);
    }

    private Set<FieldDeclaration> createFieldsFromMethods() {
        final Set<FieldDeclaration> result = new LinkedHashSet<>();

        if (methods == null || methods.isEmpty()) {
            return result;
        }

        for (MethodDeclaration m : methods) {
            final FieldDeclaration createdField = createFieldFromGetMethod(m);
            if (createdField != null) {
                result.add(createdField);
            }
        }
        return result;
    }

    private FieldDeclaration createFieldFromGetMethod(final MethodDeclaration method) {
        if (method == null || method.getName() == null || method.getName().isEmpty()) {
            return null;
        } else if (method.getName().startsWith(GET_PREFIX)) {
            final String fieldNameFromMethod = method.getName().substring(GET_PREFIX.length());
            final String fieldName = Character.toLowerCase(fieldNameFromMethod.charAt(0))
                    + fieldNameFromMethod.substring(1);
            return new FieldDeclaration(method.getReturnType(), fieldName);
        }
        return null;
    }

    private List<String> createImportsNames() {
        final List<String> result = new ArrayList<>();

        if (imports == null || imports.isEmpty()) {
            return result;
        }

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

    /**
     * @return list of imports or empty list
     */
    public List<String> getImportsNames() {
        return importsNames;
    }

    public String getClassName() {
        return className;
    }

    /**
     * @return set of methods or empty set
     */
    public Set<FieldDeclaration> getFields() {
        return fields;
    }

    /**
     * @return set of methods or empty set
     */
    public Set<MethodDeclaration> getMethods() {
        return methods;
    }

    /**
     * @return declaration of augment field or NULL
     */
    public FieldDeclaration getAugmentField() {
        return augmentField;
    }

}
