/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.java.api.generator;

import static org.opendaylight.controller.sal.java.api.generator.Constants.COMMA;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.binding.generator.util.TypeConstants;
import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.ParameterizedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.WildcardType;


public final class GeneratorUtil {

    private GeneratorUtil() {
    }
    
    public static Map<String, String> createImports(GeneratedType genType) {
        if (genType == null) {
            throw new IllegalArgumentException("Generated Type cannot be NULL!");
        }
        final Map<String, String> imports = new LinkedHashMap<>();
        imports.put(genType.getName(), genType.getPackageName());
        List<GeneratedType> childGeneratedTypes = genType.getEnclosedTypes();
        if (!childGeneratedTypes.isEmpty()) {
            for (GeneratedType genTypeChild : childGeneratedTypes) {
                imports.putAll(createImports(genTypeChild));
            }
        }

        final List<Constant> constants = genType.getConstantDefinitions();
        final List<MethodSignature> methods = genType.getMethodDefinitions();
        final List<Type> impl = genType.getImplements();

        // IMPLEMENTATIONS
        if (impl != null) {
            for (final Type type : impl) {
                putTypeIntoImports(genType, type, imports);
            }
        }

        // CONSTANTS
        if (constants != null) {
            for (final Constant constant : constants) {
                final Type constantType = constant.getType();
                putTypeIntoImports(genType, constantType, imports);
            }
        }

        // REGULAR EXPRESSION
        if (genType instanceof GeneratedTransferObject) {
            if (isConstantInTO(TypeConstants.PATTERN_CONSTANT_NAME, (GeneratedTransferObject) genType)) {
                putTypeIntoImports(genType, Types.typeForClass(java.util.regex.Pattern.class), imports);
                putTypeIntoImports(genType, Types.typeForClass(java.util.Arrays.class), imports);
                putTypeIntoImports(genType, Types.typeForClass(java.util.ArrayList.class), imports);
            }
        }

        // METHODS
        if (methods != null) {
            for (final MethodSignature method : methods) {
                final Type methodReturnType = method.getReturnType();
                putTypeIntoImports(genType, methodReturnType, imports);
                for (final MethodSignature.Parameter methodParam : method.getParameters()) {
                    putTypeIntoImports(genType, methodParam.getType(), imports);
                }
            }
        }

        // PROPERTIES
        if (genType instanceof GeneratedTransferObject) {
            final GeneratedTransferObject genTO = (GeneratedTransferObject) genType;
            final List<GeneratedProperty> properties = genTO.getProperties();
            if (properties != null) {
                for (GeneratedProperty property : properties) {
                    final Type propertyType = property.getReturnType();
                    putTypeIntoImports(genType, propertyType, imports);
                }
            }
        }

        return imports;
    }
    
    public static void putTypeIntoImports(final GeneratedType parentGenType, final Type type,
            final Map<String, String> imports) {
        if (parentGenType == null) {
            throw new IllegalArgumentException("Parent Generated Type parameter MUST be specified and cannot be "
                    + "NULL!");
        }
        if (parentGenType.getName() == null) {
            throw new IllegalArgumentException("Parent Generated Type name cannot be NULL!");
        }
        if (parentGenType.getPackageName() == null) {
            throw new IllegalArgumentException("Parent Generated Type cannot have Package Name referenced as NULL!");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type parameter MUST be specified and cannot be NULL!");
        }
        if (type.getName() == null) {
            throw new IllegalArgumentException("Type name cannot be NULL!");
        }
        if (type.getPackageName() == null) {
            throw new IllegalArgumentException("Type cannot have Package Name referenced as NULL!");
        }

        final String typeName = type.getName();
        final String typePackageName = type.getPackageName();
        final String parentTypeName = parentGenType.getName();
        final String parentTypePackageName = parentGenType.getPackageName();
        if (typeName.equals(parentTypeName) || typePackageName.startsWith("java.lang")
                || typePackageName.equals(parentTypePackageName) || typePackageName.isEmpty()) {
            return;
        }
        if (!imports.containsKey(typeName)) {
            imports.put(typeName, typePackageName);
        }
        if (type instanceof ParameterizedType) {
            final ParameterizedType paramType = (ParameterizedType) type;
            final Type[] params = paramType.getActualTypeArguments();
            for (Type param : params) {
                putTypeIntoImports(parentGenType, param, imports);
            }
        }
    }
    
    public static boolean isConstantInTO(String constName, GeneratedTransferObject genTO) {
        if (constName == null || genTO == null)
            throw new IllegalArgumentException();
        List<Constant> consts = genTO.getConstantDefinitions();
        for (Constant cons : consts) {
            if (cons.getName().equals(constName)) {
                return true;
            }

        }
        return false;
    }
    
    public static Map<String, String> createChildImports(GeneratedType genType) {
        Map<String, String> childImports = new LinkedHashMap<>();
        List<GeneratedType> childGeneratedTypes = genType.getEnclosedTypes();
        if (!childGeneratedTypes.isEmpty()) {
            for (GeneratedType genTypeChild : childGeneratedTypes) {
                createChildImports(genTypeChild);
                childImports.put(genTypeChild.getName(), genTypeChild.getPackageName());
            }
        }
        return childImports;
    }

    public static String getExplicitType(final GeneratedType parentGenType, final Type type, final Map<String, String> imports) {
        if (type == null) {
            throw new IllegalArgumentException("Type parameter MUST be specified and cannot be NULL!");
        }
        if (type.getName() == null) {
            throw new IllegalArgumentException("Type name cannot be NULL!");
        }
        if (type.getPackageName() == null) {
            throw new IllegalArgumentException("Type cannot have Package Name referenced as NULL!");
        }
        if (imports == null) {
            throw new IllegalArgumentException("Imports Map cannot be NULL!");
        }

        final String typePackageName = type.getPackageName();
        final String typeName = type.getName();
        final String importedPackageName = imports.get(typeName);
        if (typePackageName.equals(importedPackageName) || typePackageName.equals(parentGenType.getPackageName())) {
            final StringBuilder builder = new StringBuilder(type.getName());
            if (type instanceof ParameterizedType) {
                final ParameterizedType pType = (ParameterizedType) type;
                final Type[] pTypes = pType.getActualTypeArguments();
                builder.append("<");
                builder.append(getParameters(parentGenType, pTypes, imports));
                builder.append(">");
            }
            if (builder.toString().equals("Void")) {
                return "void";
            }
            return builder.toString();
        } else {
            final StringBuilder builder = new StringBuilder();
            if (typePackageName.startsWith("java.lang")) {
                builder.append(type.getName());
            } else {
                if (!typePackageName.isEmpty()) {
                    builder.append(typePackageName + Constants.DOT + type.getName());
                } else {
                    builder.append(type.getName());
                }
            }
            if (type.equals(Types.voidType())) {
                return "void";
            }
            if (type instanceof ParameterizedType) {
                final ParameterizedType pType = (ParameterizedType) type;
                final Type[] pTypes = pType.getActualTypeArguments();
                builder.append("<");
                builder.append(getParameters(parentGenType, pTypes, imports));
                builder.append(">");
            }
            return builder.toString();
        }
    }
    
    private static String getParameters(final GeneratedType parentGenType, final Type[] pTypes, Map<String, String> availableImports) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pTypes.length; i++) {
            final Type t = pTypes[i];

            String separator = COMMA;
            if (i == (pTypes.length - 1)) {
                separator = "";
            }

            String wildcardParam = "";
            if (t.equals(Types.voidType())) {
                builder.append("java.lang.Void" + separator);
                continue;
            } else {

                if (t instanceof WildcardType) {
                    wildcardParam = "? extends ";
                }

                builder.append(wildcardParam + getExplicitType(parentGenType, t, availableImports) + separator);
            }
        }
        return builder.toString();
    }
    
    /**
     * The method returns reference to highest (top parent) Generated Transfer
     * Object.
     * 
     * @param childTransportObject
     *            is generated transfer object which can be extended by other
     *            generated transfer object
     * @return in first case that <code>childTransportObject</code> isn't
     *         extended then <code>childTransportObject</code> is returned. In
     *         second case the method is recursive called until first case.
     */
    public static GeneratedTransferObject getTopParrentTransportObject(GeneratedTransferObject childTransportObject) {
        if (childTransportObject == null) {
            throw new IllegalArgumentException("Parameter childTransportObject can't be null.");
        }
        if (childTransportObject.getExtends() == null) {
            return childTransportObject;
        } else {
            return getTopParrentTransportObject(childTransportObject.getExtends());
        }
    }
    
    /**
     * The method selects from input list of properties only those which have
     * read only attribute set to true.
     * 
     * @param properties
     *            contains list of properties of generated transfer object
     * @return subset of <code>properties</code> which have read only attribute
     *         set to true
     */
    public static List<GeneratedProperty> resolveReadOnlyPropertiesFromTO(List<GeneratedProperty> properties) {
        List<GeneratedProperty> readOnlyProperties = new ArrayList<GeneratedProperty>();
        if (properties != null) {
            for (final GeneratedProperty property : properties) {
                if (property.isReadOnly()) {
                    readOnlyProperties.add(property);
                }
            }
        }
        return readOnlyProperties;
    }
    
    /**
     * The method returns the list of the properties of all extending generated
     * transfer object from <code>genTO</code> to highest parent generated
     * transfer object
     * 
     * @param genTO
     * @return the list of all properties from actual to highest parent
     *         generated transfer object. In case when extension exists the
     *         method is recursive called.
     */
    public static List<GeneratedProperty> getPropertiesOfAllParents(GeneratedTransferObject genTO) {
        List<GeneratedProperty> propertiesOfAllParents = new ArrayList<GeneratedProperty>();
        if (genTO.getExtends() != null) {
            final List<GeneratedProperty> allPropertiesOfTO = genTO.getExtends().getProperties();
            List<GeneratedProperty> readOnlyPropertiesOfTO = resolveReadOnlyPropertiesFromTO(allPropertiesOfTO);
            propertiesOfAllParents.addAll(readOnlyPropertiesOfTO);
            propertiesOfAllParents.addAll(getPropertiesOfAllParents(genTO.getExtends()));
        }
        return propertiesOfAllParents;
    }
    
}
