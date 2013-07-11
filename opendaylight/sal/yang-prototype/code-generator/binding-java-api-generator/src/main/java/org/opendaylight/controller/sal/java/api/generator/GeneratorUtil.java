/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.java.api.generator;

import static org.opendaylight.controller.sal.java.api.generator.Constants.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.binding.generator.util.TypeConstants;
import org.opendaylight.controller.sal.binding.model.api.*;
import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.sal.binding.model.api.Enumeration.Pair;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature.Parameter;

public final class GeneratorUtil {

    private GeneratorUtil() {
    }

    public static String createIfcDeclaration(final GeneratedType genType, final String indent,
            final Map<String, String> availableImports) {
        return createFileDeclaration(IFC, genType, indent, availableImports, false, false);
    }

    public static String createClassDeclaration(final GeneratedTransferObject genTransferObject, final String indent,
            final Map<String, String> availableImports, boolean isIdentity, boolean isInnerClass) {
        return createFileDeclaration(CLASS, genTransferObject, indent, availableImports, isIdentity, isInnerClass);
    }

    public static String createPackageDeclaration(final String packageName) {
        return PKG + GAP + packageName + SC;
    }

    private static String createFileDeclaration(final String type, final GeneratedType genType, final String indent,
            final Map<String, String> availableImports, boolean isIdentity, boolean innerClass) {
        final StringBuilder builder = new StringBuilder();
        final String currentPkg = genType.getPackageName();

        createComment(builder, genType.getComment(), indent);

        if (!genType.getAnnotations().isEmpty()) {
            final List<AnnotationType> annotations = genType.getAnnotations();
            appendAnnotations(builder, annotations);
            builder.append(NL);
        }

        if (innerClass) {
            builder.append(indent + PUBLIC + GAP + STATIC + GAP + FINAL + GAP + type + GAP + genType.getName() + GAP);
        } else if (isIdentity) {
            if (!(CLASS.equals(type))) {
                throw new IllegalArgumentException("'identity' has to be generated as a class");
            }
            builder.append(indent + PUBLIC + GAP + ABSTRACT + GAP + type + GAP + genType.getName() + GAP);
        } else {
            builder.append(indent + PUBLIC + GAP + type + GAP + genType.getName() + GAP);
        }

        if (genType instanceof GeneratedTransferObject) {
            GeneratedTransferObject genTO = (GeneratedTransferObject) genType;

            if (genTO.getExtends() != null) {
                builder.append(EXTENDS + GAP);
                String gtoString = getExplicitType(genTO.getExtends(), availableImports, currentPkg);
                builder.append(gtoString + GAP);
            }
        }

        final List<Type> genImplements = genType.getImplements();
        if (!genImplements.isEmpty()) {
            if (genType instanceof GeneratedTransferObject) {
                builder.append(IMPLEMENTS + GAP);
            } else {
                builder.append(EXTENDS + GAP);
            }
            builder.append(getExplicitType(genImplements.get(0), availableImports, currentPkg));

            for (int i = 1; i < genImplements.size(); ++i) {
                builder.append(", ");
                builder.append(getExplicitType(genImplements.get(i), availableImports, currentPkg));
            }
        }
        builder.append(GAP + LCB);
        return builder.toString();
    }

    private static StringBuilder appendAnnotations(final StringBuilder builder, final List<AnnotationType> annotations) {
        if ((builder != null) && (annotations != null)) {
            for (final AnnotationType annotation : annotations) {
                builder.append("@");
                builder.append(annotation.getPackageName());
                builder.append(".");
                builder.append(annotation.getName());

                if (annotation.containsParameters()) {
                    builder.append("(");
                    final List<AnnotationType.Parameter> parameters = annotation.getParameters();
                    appendAnnotationParams(builder, parameters);
                    builder.append(")");
                }
            }
        }
        return builder;
    }

    private static StringBuilder appendAnnotationParams(final StringBuilder builder,
            final List<AnnotationType.Parameter> parameters) {
        if (parameters != null) {
            int i = 0;
            for (final AnnotationType.Parameter param : parameters) {
                if (param == null) {
                    continue;
                }
                if (i > 0) {
                    builder.append(", ");
                }
                final String paramName = param.getName();
                if (param.getValue() != null) {
                    builder.append(paramName);
                    builder.append(" = ");
                    builder.append(param.getValue());
                } else {
                    builder.append(paramName);
                    builder.append(" = {");
                    final List<String> values = param.getValues();
                    builder.append(values.get(0));
                    for (int j = 1; j < values.size(); ++j) {
                        builder.append(", ");
                        builder.append(values.get(j));
                    }
                    builder.append("}");
                }
                i++;
            }
        }
        return builder;
    }

    public static String createConstant(final Constant constant, final String indent,
            final Map<String, String> availableImports, final String currentPkg) {
        final StringBuilder builder = new StringBuilder();
        if (constant == null)
            throw new IllegalArgumentException();
        builder.append(indent + PUBLIC + GAP + STATIC + GAP + FINAL + GAP);
        builder.append(getExplicitType(constant.getType(), availableImports, currentPkg) + GAP + constant.getName());
        builder.append(GAP + "=" + GAP);

        if (constant.getName().equals(TypeConstants.PATTERN_CONSTANT_NAME)) {
            return "";
        } else {
            builder.append(constant.getValue());
        }
        builder.append(SC);

        return builder.toString();
    }

    public static String createField(final GeneratedProperty property, final String indent,
            final Map<String, String> availableImports, final String currentPkg) {
        final StringBuilder builder = new StringBuilder();
        if (!property.getAnnotations().isEmpty()) {
            final List<AnnotationType> annotations = property.getAnnotations();
            appendAnnotations(builder, annotations);
            builder.append(NL);
        }
        builder.append(indent + PRIVATE + GAP);
        builder.append(getExplicitType(property.getReturnType(), availableImports, currentPkg) + GAP
                + property.getName());
        builder.append(SC);
        return builder.toString();
    }

    /**
     * Create method declaration in interface.
     * 
     * @param method
     * @param indent
     * @return
     */
    public static String createMethodDeclaration(final MethodSignature method, final String indent,
            Map<String, String> availableImports, final String currentPkg) {
        final StringBuilder builder = new StringBuilder();

        if (method == null) {
            throw new IllegalArgumentException("Method Signature parameter MUST be specified and cannot be NULL!");
        }

        final String comment = method.getComment();
        final String name = method.getName();
        if (name == null) {
            throw new IllegalStateException("Method Name cannot be NULL!");
        }

        final Type type = method.getReturnType();
        if (type == null) {
            throw new IllegalStateException("Method Return type cannot be NULL!");
        }

        final List<Parameter> parameters = method.getParameters();

        createComment(builder, comment, indent);
        builder.append(NL);

        if (!method.getAnnotations().isEmpty()) {
            builder.append(indent);
            final List<AnnotationType> annotations = method.getAnnotations();
            appendAnnotations(builder, annotations);
            builder.append(NL);
        }

        builder.append(indent + getExplicitType(type, availableImports, currentPkg) + GAP + name);
        builder.append(LB);
        for (int i = 0; i < parameters.size(); i++) {
            Parameter p = parameters.get(i);
            String separator = COMMA;
            if (i + 1 == parameters.size()) {
                separator = "";
            }
            builder.append(getExplicitType(p.getType(), availableImports, currentPkg) + GAP + p.getName() + separator);
        }
        builder.append(RB);
        builder.append(SC);

        return builder.toString();
    }

    public static String createConstructor(final GeneratedTransferObject genTransferObject, final String indent,
            final Map<String, String> availableImports, final boolean isIdentity, final boolean oneConstructor) {
        if (genTransferObject == null) {
            throw new IllegalArgumentException("genTransferObject can't be null");
        }
        if (indent == null) {
            throw new IllegalArgumentException("indent can't be null");
        }
        if (availableImports == null) {
            throw new IllegalArgumentException("availableImports can't be null");
        }
        GeneratedTransferObject genTOTopParent = getTopParrentTransportObject(genTransferObject);
        final List<GeneratedProperty> ctorProperties = resolveReadOnlyPropertiesFromTO(genTransferObject
                .getProperties());
        final List<GeneratedProperty> ctorPropertiesAllParents = getPropertiesOfAllParents(genTransferObject
                .getExtends());

        final String currentPkg = genTransferObject.getPackageName();
        final String className = genTransferObject.getName();

        String constructorPart = "";
        if (oneConstructor) {
            if (genTOTopParent != genTransferObject && genTOTopParent.isUnionType()) {
                constructorPart = createConstructorForEveryParentProperty(indent, isIdentity, ctorProperties,
                        ctorPropertiesAllParents, availableImports, currentPkg, className);

            } else {
                constructorPart = createOneConstructor(indent, isIdentity, ctorProperties, ctorPropertiesAllParents,
                        availableImports, currentPkg, className);
            }

        } else { // union won't be extended
            constructorPart = createConstructorForEveryProperty(indent, isIdentity, ctorProperties,
                    ctorPropertiesAllParents, availableImports, currentPkg, className);
        }

        return constructorPart;
    }

    private static String createOneConstructor(final String indent, boolean isIdentity,
            final List<GeneratedProperty> properties, final List<GeneratedProperty> propertiesAllParents,
            final Map<String, String> availableImports, final String currentPkg, final String className) {
        if (indent == null) {
            throw new IllegalArgumentException("indent can't be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties can't be null");
        }
        if (propertiesAllParents == null) {
            throw new IllegalArgumentException("propertiesAllParent can't be null");
        }
        if (availableImports == null) {
            throw new IllegalArgumentException("availableImports can't be null");
        }
        if (currentPkg == null) {
            throw new IllegalArgumentException("currentPkg can't be null");
        }
        if (className == null) {
            throw new IllegalArgumentException("className can't be null");
        }

        final StringBuilder builder = new StringBuilder();

        List<GeneratedProperty> propertiesAll = new ArrayList<GeneratedProperty>(properties);
        propertiesAll.addAll(propertiesAllParents);

        builder.append(createConstructorDeclarationToLeftParenthesis(className, indent, isIdentity));
        builder.append(createMethodPropertiesDeclaration(propertiesAll, availableImports, currentPkg, COMMA + GAP));
        builder.append(createConstructorDeclarationFromRightParenthesis());
        builder.append(createConstructorSuper(propertiesAllParents, indent));
        builder.append(createClassPropertiesInitialization(propertiesAll, indent));
        builder.append(createConstructorClosingPart(indent));
        return builder.toString();
    }

    private static String createConstructorForEveryParentProperty(final String indent, final boolean isIdentity,
            final List<GeneratedProperty> properties, final List<GeneratedProperty> propertiesAllParents,
            final Map<String, String> availableImports, final String currentPkg, final String className) {
        if (indent == null) {
            throw new IllegalArgumentException("indent can't be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties can't be null");
        }
        if (propertiesAllParents == null) {
            throw new IllegalArgumentException("propertiesAllParent can't be null");
        }
        if (availableImports == null) {
            throw new IllegalArgumentException("availableImports can't be null");
        }
        if (currentPkg == null) {
            throw new IllegalArgumentException("currentPkg can't be null");
        }
        if (className == null) {
            throw new IllegalArgumentException("className can't be null");
        }
        final StringBuilder builder = new StringBuilder();
        GeneratedProperty parentProperty;
        Iterator<GeneratedProperty> parentPropertyIterator = propertiesAllParents.iterator();

        do {
            parentProperty = null;
            if (parentPropertyIterator.hasNext()) {
                parentProperty = parentPropertyIterator.next();
            }

            List<GeneratedProperty> propertiesAndParentProperties = new ArrayList<GeneratedProperty>();
            if (parentProperty != null) {
                propertiesAndParentProperties.add(parentProperty);
            }
            propertiesAndParentProperties.addAll(properties);

            builder.append(createConstructorDeclarationToLeftParenthesis(className, indent, isIdentity));
            builder.append(createMethodPropertiesDeclaration(propertiesAndParentProperties, availableImports,
                    currentPkg, COMMA + GAP));
            builder.append(createConstructorDeclarationFromRightParenthesis());
            builder.append(createConstructorSuper(parentProperty, indent));
            builder.append(createClassPropertiesInitialization(properties, indent));
            builder.append(createConstructorClosingPart(indent));
        } while (parentPropertyIterator.hasNext());

        return builder.toString();
    }

    private static String createConstructorForEveryProperty(final String indent, final boolean isIdentity,
            final List<GeneratedProperty> properties, final List<GeneratedProperty> propertiesAllParents,
            final Map<String, String> availableImports, final String currentPkg, final String className) {
        if (indent == null) {
            throw new IllegalArgumentException("indent can't be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties can't be null");
        }
        if (propertiesAllParents == null) {
            throw new IllegalArgumentException("propertiesAllParent can't be null");
        }
        if (availableImports == null) {
            throw new IllegalArgumentException("availableImports can't be null");
        }
        if (currentPkg == null) {
            throw new IllegalArgumentException("currentPkg can't be null");
        }
        if (className == null) {
            throw new IllegalArgumentException("className can't be null");
        }

        final StringBuilder builder = new StringBuilder();

        GeneratedProperty property;
        Iterator<GeneratedProperty> propertyIterator = properties.iterator();

        do {
            property = null;
            if (propertyIterator.hasNext()) {
                property = propertyIterator.next();
            }

            List<GeneratedProperty> propertyAndTopParentProperties = new ArrayList<GeneratedProperty>();
            if (property != null) {
                propertyAndTopParentProperties.add(property);
            }
            propertyAndTopParentProperties.addAll(propertiesAllParents);

            builder.append(createConstructorDeclarationToLeftParenthesis(className, indent, isIdentity));
            builder.append(createMethodPropertiesDeclaration(propertyAndTopParentProperties, availableImports,
                    currentPkg, COMMA + GAP));
            builder.append(createConstructorDeclarationFromRightParenthesis());
            builder.append(createConstructorSuper(propertiesAllParents, indent));
            builder.append(createClassPropertyInitialization(property, indent));
            builder.append(createConstructorClosingPart(indent));
        } while (propertyIterator.hasNext());

        return builder.toString();
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
    private static List<GeneratedProperty> resolveReadOnlyPropertiesFromTO(List<GeneratedProperty> properties) {
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

    private static String createMethodPropertiesDeclaration(final List<GeneratedProperty> parameters,
            final Map<String, String> availableImports, final String currentPkg, final String parameterSeparator) {
        StringBuilder builder = new StringBuilder();
        if (parameters == null) {
            throw new IllegalArgumentException("parameters can't be null");
        }
        if (availableImports == null) {
            throw new IllegalArgumentException("availableImports can't be null");
        }
        if (currentPkg == null) {
            throw new IllegalArgumentException("currentPkg can't be null");
        }
        if (parameterSeparator == null) {
            throw new IllegalArgumentException("parameterSeparator can't be null");
        }

        for (final GeneratedProperty parameter : parameters) {
            builder.append(createMethodPropertyDeclaration(parameter, availableImports, currentPkg));
            builder.append(parameterSeparator);
        }
        if (!parameters.isEmpty()) {
            builder = builder.delete(builder.length() - parameterSeparator.length(), builder.length());
        }
        return builder.toString();
    }

    private static String createConstructorDeclarationToLeftParenthesis(final String className, final String indent,
            final boolean isIdentity) {
        if (className == null) {
            throw new IllegalArgumentException("className can't be null");
        }
        if (indent == null) {
            throw new IllegalArgumentException("indent can't be null");
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(indent);
        builder.append(isIdentity ? PROTECTED : PUBLIC);
        builder.append(GAP);
        builder.append(className);
        builder.append(LB);
        return builder.toString();
    }

    private static String createConstructorDeclarationFromRightParenthesis() {
        final StringBuilder builder = new StringBuilder();
        builder.append(RB + GAP + LCB + NL);
        return builder.toString();
    }

    private static String createConstructorSuper(final List<GeneratedProperty> superProperties, final String indent) {
        if (indent == null) {
            throw new IllegalArgumentException("indent can't be null");
        }
        if (superProperties == null) {
            throw new IllegalArgumentException("superProperties can't be null");
        }
        StringBuilder builder = new StringBuilder();
        builder.append(indent + TAB + "super(");
        String propertySeparator = COMMA + GAP;
        for (GeneratedProperty superProperty : superProperties) {
            builder.append(superProperty.getName());
            builder.append(propertySeparator);
        }
        if (!superProperties.isEmpty()) {
            builder = builder.delete(builder.length() - propertySeparator.length(), builder.length());
        }

        builder.append(");" + NL);
        return builder.toString();
    }

    private static String createConstructorSuper(final GeneratedProperty superProperty, final String indent) {
        if (indent == null) {
            throw new IllegalArgumentException("indent can't be null");
        }
        if (superProperty == null) {
            throw new IllegalArgumentException("superProperties can't be null");
        }
        StringBuilder builder = new StringBuilder();
        if (superProperty != null) {
            builder.append(indent + TAB + "super(");
            builder.append(superProperty.getName());
            builder.append(");" + NL);
        }
        return builder.toString();
    }

    private static String createConstructorClosingPart(final String indent) {
        if (indent == null) {
            throw new IllegalArgumentException("indent can't be null");
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(indent);
        builder.append(RCB);
        builder.append(NL + NL);
        return builder.toString();
    }

    private static String createClassPropertiesInitialization(final List<GeneratedProperty> properties,
            final String indent) {
        if (indent == null) {
            throw new IllegalArgumentException("indent can't be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties can't be null");
        }
        final StringBuilder builder = new StringBuilder();
        for (final GeneratedProperty property : properties) {
            createClassPropertyInitialization(property, indent);
        }
        return builder.toString();
    }

    private static String createClassPropertyInitialization(final GeneratedProperty property, final String indent) {
        if (indent == null) {
            throw new IllegalArgumentException("indent can't be null");
        }
        if (property == null) {
            throw new IllegalArgumentException("properties can't be null");
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(indent);
        builder.append(TAB);
        builder.append("this.");
        builder.append(property.getName());
        builder.append(" = ");
        builder.append(property.getName());
        builder.append(SC);
        builder.append(NL);
        return builder.toString();
    }

    private static String createMethodPropertyDeclaration(final GeneratedProperty property,
            final Map<String, String> availableImports, final String currentPkg) {
        if (property == null) {
            throw new IllegalArgumentException("property can't be null");
        }
        if (availableImports == null) {
            throw new IllegalArgumentException("availableImports can't be null");
        }
        if (currentPkg == null) {
            throw new IllegalArgumentException("currentPkg can't be null");
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(getExplicitType(property.getReturnType(), availableImports, currentPkg));
        builder.append(GAP);
        builder.append(property.getName());
        return builder.toString();
    }

    public static String createGetter(final GeneratedProperty property, final String indent,
            final Map<String, String> availableImports, final String currentPkg) {
        final StringBuilder builder = new StringBuilder();

        final Type type = property.getReturnType();
        final String varName = property.getName();
        final char first = Character.toUpperCase(varName.charAt(0));
        final String methodName = "get" + first + varName.substring(1);

        builder.append(indent + PUBLIC + GAP + getExplicitType(type, availableImports, currentPkg) + GAP + methodName);
        builder.append(LB + RB + LCB + NL);

        String currentIndent = indent + TAB;

        builder.append(currentIndent + "return " + varName + SC + NL);

        builder.append(indent + RCB);
        return builder.toString();
    }

    public static String createSetter(final GeneratedProperty property, final String indent,
            final Map<String, String> availableImports, final String currentPkg) {
        final StringBuilder builder = new StringBuilder();

        final Type type = property.getReturnType();
        final String varName = property.getName();
        final char first = Character.toUpperCase(varName.charAt(0));
        final String methodName = "set" + first + varName.substring(1);

        builder.append(indent + PUBLIC + GAP + "void" + GAP + methodName);
        builder.append(LB + getExplicitType(type, availableImports, currentPkg) + GAP + varName + RB + LCB + NL);
        String currentIndent = indent + TAB;
        builder.append(currentIndent + "this." + varName + " = " + varName + SC + NL);
        builder.append(indent + RCB);
        return builder.toString();
    }

    public static String createHashCode(final List<GeneratedProperty> properties, final String indent) {
        StringBuilder builder = new StringBuilder();
        builder.append(indent + "public int hashCode() {" + NL);
        builder.append(indent + TAB + "final int prime = 31;" + NL);
        builder.append(indent + TAB + "int result = 1;" + NL);

        for (GeneratedProperty property : properties) {
            String fieldName = property.getName();
            builder.append(indent + TAB + "result = prime * result + ((" + fieldName + " == null) ? 0 : " + fieldName
                    + ".hashCode());" + NL);
        }

        builder.append(indent + TAB + "return result;" + NL);
        builder.append(indent + RCB + NL);
        return builder.toString();
    }

    public static String createEquals(final GeneratedTransferObject type, final List<GeneratedProperty> properties,
            final String indent) {
        final StringBuilder builder = new StringBuilder();
        final String indent1 = indent + TAB;
        final String indent2 = indent1 + TAB;
        final String indent3 = indent2 + TAB;

        builder.append(indent + "public boolean equals(Object obj) {" + NL);
        builder.append(indent1 + "if (this == obj) {" + NL);
        builder.append(indent2 + "return true;" + NL);
        builder.append(indent1 + "}" + NL);
        builder.append(indent1 + "if (obj == null) {" + NL);
        builder.append(indent2 + "return false;" + NL);
        builder.append(indent1 + "}" + NL);
        builder.append(indent1 + "if (getClass() != obj.getClass()) {" + NL);
        builder.append(indent2 + "return false;" + NL);
        builder.append(indent1 + "}" + NL);

        String typeStr = type.getName();
        builder.append(indent1 + typeStr + " other = (" + typeStr + ") obj;" + NL);

        for (final GeneratedProperty property : properties) {
            String fieldName = property.getName();
            builder.append(indent1 + "if (" + fieldName + " == null) {" + NL);
            builder.append(indent2 + "if (other." + fieldName + " != null) {" + NL);
            builder.append(indent3 + "return false;" + NL);
            builder.append(indent2 + "}" + NL);
            builder.append(indent1 + "} else if (!" + fieldName + ".equals(other." + fieldName + ")) {" + NL);
            builder.append(indent2 + "return false;" + NL);
            builder.append(indent1 + "}" + NL);
        }

        builder.append(indent1 + "return true;" + NL);
        builder.append(indent + RCB + NL);
        return builder.toString();
    }

    public static String createToString(final GeneratedTransferObject type, final List<GeneratedProperty> properties,
            final String indent) {
        StringBuilder builder = new StringBuilder();
        builder.append(indent);
        builder.append("public String toString() {");
        builder.append(NL);
        builder.append(indent);
        builder.append(TAB);
        builder.append("StringBuilder builder = new StringBuilder();");
        builder.append(NL);
        builder.append(indent);
        builder.append(TAB);
        builder.append("builder.append(\"");
        builder.append(type.getName());
        builder.append(" [");

        boolean first = true;
        for (final GeneratedProperty property : properties) {
            if (first) {
                builder.append(property.getName());
                builder.append("=\");");
                builder.append(NL);
                builder.append(indent);
                builder.append(TAB);
                builder.append("builder.append(");
                builder.append(property.getName());
                builder.append(");");
                first = false;
            } else {
                builder.append(NL);
                builder.append(indent);
                builder.append(TAB);
                builder.append("builder.append(\", ");
                builder.append(property.getName());
                builder.append("=\");");
                builder.append(NL);
                builder.append(indent);
                builder.append(TAB);
                builder.append("builder.append(");
                builder.append(property.getName());
                builder.append(");");
            }
        }
        builder.append(NL);
        builder.append(indent);
        builder.append(TAB);
        builder.append("builder.append(\"]\");");
        builder.append(NL);
        builder.append(indent);
        builder.append(TAB);
        builder.append("return builder.toString();");

        builder.append(NL);
        builder.append(indent);
        builder.append(RCB);
        builder.append(NL);
        return builder.toString();
    }

    public static String createEnum(final Enumeration enumeration, final String indent) {
        if (enumeration == null || indent == null)
            throw new IllegalArgumentException();
        final StringBuilder builder = new StringBuilder(indent + PUBLIC + GAP + ENUM + GAP + enumeration.getName()
                + GAP + LCB + NL);

        String separator = COMMA + NL;
        final List<Pair> values = enumeration.getValues();

        for (int i = 0; i < values.size(); i++) {
            if (i + 1 == values.size()) {
                separator = SC;
            }
            builder.append(indent + TAB + values.get(i).getName() + LB + values.get(i).getValue() + RB + separator);
        }
        builder.append(NL);
        builder.append(NL);
        final String ENUMERATION_NAME = "value";
        final String ENUMERATION_TYPE = "int";
        builder.append(indent + TAB + ENUMERATION_TYPE + GAP + ENUMERATION_NAME + SC);
        builder.append(NL);
        builder.append(indent + TAB + PRIVATE + GAP + enumeration.getName() + LB + ENUMERATION_TYPE + GAP
                + ENUMERATION_NAME + RB + GAP + LCB + NL);
        builder.append(indent + TAB + TAB + "this." + ENUMERATION_NAME + GAP + "=" + GAP + ENUMERATION_NAME + SC + NL);
        builder.append(indent + TAB + RCB + NL);

        builder.append(indent + RCB);
        builder.append(NL);
        return builder.toString();
    }

    private static String getExplicitType(final Type type, final Map<String, String> imports, final String currentPkg) {
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
        if (typePackageName.equals(importedPackageName) || typePackageName.equals(currentPkg)) {
            final StringBuilder builder = new StringBuilder(type.getName());
            if (type instanceof ParameterizedType) {
                final ParameterizedType pType = (ParameterizedType) type;
                final Type[] pTypes = pType.getActualTypeArguments();
                builder.append("<");
                builder.append(getParameters(pTypes, imports, currentPkg));
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
                    builder.append(typePackageName + "." + type.getName());
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
                builder.append(getParameters(pTypes, imports, currentPkg));
                builder.append(">");
            }
            return builder.toString();
        }
    }

    private static String getParameters(final Type[] pTypes, Map<String, String> availableImports, String currentPkg) {
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

                builder.append(wildcardParam + getExplicitType(t, availableImports, currentPkg) + separator);
            }
        }
        return builder.toString();
    }

    private static void createComment(final StringBuilder builder, final String comment, final String indent) {
        if (comment != null && comment.length() > 0) {
            builder.append(indent + "/*" + NL);
            builder.append(indent + comment + NL);
            builder.append(indent + "*/" + NL);
        }
    }

    public static Map<String, String> createImports(GeneratedType genType) {
        if (genType == null) {
            throw new IllegalArgumentException("Generated Type cannot be NULL!");
        }
        final Map<String, String> imports = new LinkedHashMap<>();
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

    private static void putTypeIntoImports(final GeneratedType parentGenType, final Type type,
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

    public static List<String> createImportLines(final Map<String, String> imports,
            final Map<String, String> innerTypeImports) {
        final List<String> importLines = new ArrayList<>();

        for (Map.Entry<String, String> entry : imports.entrySet()) {
            final String typeName = entry.getKey();
            final String packageName = entry.getValue();
            if (innerTypeImports != null) {
                String innerTypePackageName = innerTypeImports.get(typeName);
                if (innerTypePackageName != null) {
                    if (innerTypePackageName.equals(packageName))
                        continue;
                }
            }
            importLines.add("import " + packageName + "." + typeName + SC);
        }
        return importLines;
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
    private static GeneratedTransferObject getTopParrentTransportObject(GeneratedTransferObject childTransportObject) {
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
     * The method returns the list of the properties of all extending generated
     * transfer object from <code>genTO</code> to highest parent generated
     * transfer object
     * 
     * @param genTO
     * @return the list of all properties from actual to highest parent
     *         generated transfer object. In case when extension exists the
     *         method is recursive called.
     */
    private static List<GeneratedProperty> getPropertiesOfAllParents(GeneratedTransferObject genTO) {
        List<GeneratedProperty> propertiesOfAllParents = new ArrayList<GeneratedProperty>();
        if (genTO != null) {
            final List<GeneratedProperty> allPropertiesOfTO = genTO.getProperties();
            List<GeneratedProperty> readOnlyPropertiesOfTO = resolveReadOnlyPropertiesFromTO(allPropertiesOfTO);
            propertiesOfAllParents.addAll(readOnlyPropertiesOfTO);
            if (genTO.getExtends() != null) {
                propertiesOfAllParents.addAll(getPropertiesOfAllParents(genTO.getExtends()));
            }
        }
        return propertiesOfAllParents;
    }

    public static String createStaticInicializationBlock(GeneratedTransferObject genTransferObject, String indent) {

        final StringBuilder builder = new StringBuilder();

        List<Constant> constants = genTransferObject.getConstantDefinitions();
        for (Constant constant : constants) {
            if (constant.getName() == null || constant.getType() == null || constant.getValue() == null) {
                continue;
            }
            if (constant.getName().equals(TypeConstants.PATTERN_CONSTANT_NAME)) {
                final Object constValue = constant.getValue();
                List<String> regularExpressions = new ArrayList<>();
                if (constValue instanceof List) {
                    builder.append(indent + PUBLIC + GAP + STATIC + GAP + FINAL + GAP + "List<String>" + GAP
                            + TypeConstants.PATTERN_CONSTANT_NAME + GAP + "=" + GAP + "Arrays.asList" + LB);
                    final List<?> constantValues = (List<?>) constValue;
                    int stringsCount = 0;
                    for (Object value : constantValues) {
                        if (value instanceof String) {
                            if (stringsCount > 0) {
                                builder.append(COMMA);
                            }
                            stringsCount++;
                            regularExpressions.add((String) value);
                            builder.append(DOUBLE_QUOTE + (String) value + DOUBLE_QUOTE);
                        }
                    }
                    builder.append(RB + SC + NL);
                }
                builder.append(indent + PRIVATE + GAP + STATIC + GAP + FINAL + GAP + "List<Pattern>" + GAP
                        + MEMBER_PATTERN_LIST + GAP + ASSIGN + GAP + "new ArrayList<Pattern>()" + GAP + SC + NL + NL);

                if (!regularExpressions.isEmpty()) {
                    builder.append(indent + STATIC + LCB + NL);
                    builder.append(indent + TAB + "for (String regEx : " + TypeConstants.PATTERN_CONSTANT_NAME + ") {"
                            + NL);
                    builder.append(indent + TAB + TAB + MEMBER_PATTERN_LIST + ".add(Pattern.compile(regEx))" + SC + NL);
                    builder.append(indent + TAB + RCB + NL);
                    builder.append(indent + RCB + NL + NL);
                }

            }
        }
        return builder.toString();
    }
}
