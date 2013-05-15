/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.java.api.generator;

import static org.opendaylight.controller.sal.java.api.generator.Constants.CLASS;
import static org.opendaylight.controller.sal.java.api.generator.Constants.COMMA;
import static org.opendaylight.controller.sal.java.api.generator.Constants.ENUM;
import static org.opendaylight.controller.sal.java.api.generator.Constants.FINAL;
import static org.opendaylight.controller.sal.java.api.generator.Constants.GAP;
import static org.opendaylight.controller.sal.java.api.generator.Constants.IFC;
import static org.opendaylight.controller.sal.java.api.generator.Constants.LB;
import static org.opendaylight.controller.sal.java.api.generator.Constants.LCB;
import static org.opendaylight.controller.sal.java.api.generator.Constants.NL;
import static org.opendaylight.controller.sal.java.api.generator.Constants.PKG;
import static org.opendaylight.controller.sal.java.api.generator.Constants.PRIVATE;
import static org.opendaylight.controller.sal.java.api.generator.Constants.PUBLIC;
import static org.opendaylight.controller.sal.java.api.generator.Constants.RB;
import static org.opendaylight.controller.sal.java.api.generator.Constants.RCB;
import static org.opendaylight.controller.sal.java.api.generator.Constants.SC;
import static org.opendaylight.controller.sal.java.api.generator.Constants.STATIC;
import static org.opendaylight.controller.sal.java.api.generator.Constants.TAB;
import static org.opendaylight.controller.sal.java.api.generator.Constants.EXTENDS;
import static org.opendaylight.controller.sal.java.api.generator.Constants.IMPLEMENTS;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.sal.binding.model.api.AnnotationType;
import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.Enumeration.Pair;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature.Parameter;
import org.opendaylight.controller.sal.binding.model.api.ParameterizedType;
import org.opendaylight.controller.sal.binding.model.api.Type;

public class GeneratorUtil {

    private GeneratorUtil() {
    }

    public static String createIfcDeclarationWithPkgName(
            final GeneratedType genType, final String indent) {
        return createFileDeclarationWithPkgName(IFC, genType, indent);
    }

    public static String createClassDeclarationWithPkgName(
            final GeneratedTransferObject genTransferObject, final String indent) {
        return createFileDeclarationWithPkgName(CLASS, genTransferObject,
                indent);
    }

    private static String createFileDeclarationWithPkgName(final String type,
            final GeneratedType genType, final String indent) {
        final StringBuilder builder = new StringBuilder();
        builder.append(PKG + GAP + genType.getPackageName() + SC);
        builder.append(NL);
        builder.append(NL);
        createComment(builder, genType.getComment(), indent);

        if (!genType.getAnnotations().isEmpty()) {
            final List<AnnotationType> annotations = genType.getAnnotations();
            appendAnnotations(builder, annotations);
            builder.append(NL);
        }
        builder.append(PUBLIC + GAP + type + GAP + genType.getName() + GAP);

        final List<GeneratedType> genImplements = genType.getImplements();
        if (genType instanceof GeneratedTransferObject) {
            GeneratedTransferObject genTO = (GeneratedTransferObject) genType;

            if (genTO.getExtends() != null) {
                builder.append(EXTENDS + GAP);
                builder.append(genTO.getExtends() + GAP);
            }
        }
        if (!genImplements.isEmpty()) {
            if (genType instanceof GeneratedTransferObject) {
                builder.append(IMPLEMENTS + GAP);
            } else {
                builder.append(EXTENDS + GAP);
            }
            builder.append(genImplements.get(0).getPackageName()
                    + "." + genImplements.get(0).getName());
            for (int i = 1; i < genImplements.size(); ++i) {
                builder.append(", ");
                builder.append(genImplements.get(i).getPackageName()
                        + "." + genImplements.get(i).getName());
            }
        }

        builder.append(GAP + LCB);
        return builder.toString();
    }

    private static StringBuilder appendAnnotations(final StringBuilder builder,
            final List<AnnotationType> annotations) {
        if ((builder != null) && (annotations != null)) {
            for (final AnnotationType annotation : annotations) {
                builder.append("@");
                builder.append(annotation.getPackageName());
                builder.append(".");
                builder.append(annotation.getName());

                if (annotation.containsParameters()) {
                    builder.append("(");
                    final List<AnnotationType.Parameter> parameters = annotation
                            .getParameters();
                    appendAnnotationParams(builder, parameters);
                    builder.append(")");
                }
            }
        }
        return builder;
    }

    private static StringBuilder appendAnnotationParams(
            final StringBuilder builder,
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

    public static String createConstant(final Constant constant,
            final String indent) {
        final StringBuilder builder = new StringBuilder();
        builder.append(indent + PUBLIC + GAP + STATIC + GAP + FINAL + GAP);
        builder.append(getExplicitType(constant.getType()) + GAP
                + constant.getName());
        builder.append(GAP + "=" + GAP);
        builder.append(constant.getValue() + SC);
        return builder.toString();
    }

    public static String createField(final GeneratedProperty property,
            final String indent) {
        final StringBuilder builder = new StringBuilder();
        builder.append(indent);
        if (!property.getAnnotations().isEmpty()) {
            final List<AnnotationType> annotations = property.getAnnotations();
            appendAnnotations(builder, annotations);
            builder.append(NL);
        }
        builder.append(indent + PRIVATE + GAP);
        builder.append(getExplicitType(property.getReturnType()) + GAP
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
    public static String createMethodDeclaration(final MethodSignature method,
            final String indent) {
        final String comment = method.getComment();
        final Type type = method.getReturnType();
        final String name = method.getName();
        final List<Parameter> parameters = method.getParameters();

        final StringBuilder builder = new StringBuilder();
        createComment(builder, comment, indent);
        builder.append(NL);
        builder.append(indent);

        if (!method.getAnnotations().isEmpty()) {
            final List<AnnotationType> annotations = method.getAnnotations();
            appendAnnotations(builder, annotations);
            builder.append(NL);
        }

        builder.append(indent + getExplicitType(type) + GAP + name);
        builder.append(LB);
        for (int i = 0; i < parameters.size(); i++) {
            Parameter p = parameters.get(i);
            String separator = COMMA;
            if (i + 1 == parameters.size()) {
                separator = "";
            }
            builder.append(getExplicitType(p.getType()) + GAP + p.getName()
                    + separator);
        }
        builder.append(RB);
        builder.append(SC);

        return builder.toString();
    }

    public static String createConstructor(
            GeneratedTransferObject genTransferObject, final String indent) {
        final StringBuilder builder = new StringBuilder();

        final List<GeneratedProperty> properties = genTransferObject
                .getProperties();
        final List<GeneratedProperty> ctorParams = new ArrayList<GeneratedProperty>();
        for (final GeneratedProperty property : properties) {
            if (property.isReadOnly()) {
                ctorParams.add(property);
            }
        }
        
        builder.append(indent);
        builder.append(PUBLIC);
        builder.append(GAP);
        builder.append(genTransferObject.getName());
        builder.append(LB);
        
        if (!ctorParams.isEmpty()) {
            builder.append(getExplicitType(ctorParams.get(0).getReturnType()));
            builder.append(" ");
            builder.append(ctorParams.get(0).getName());
            for (int i = 1; i < ctorParams.size(); ++i) {
                final GeneratedProperty param = ctorParams.get(i);
                builder.append(", ");
                builder.append(getExplicitType(param.getReturnType()));
                builder.append(GAP);
                builder.append(param.getName());
            }
        }
        builder.append(RB + GAP + LCB + NL + indent + TAB + "super();" + NL);
        if (!ctorParams.isEmpty()) {
            for (final GeneratedProperty property : ctorParams) {
                builder.append(indent);
                builder.append(TAB);
                builder.append("this.");
                builder.append(property.getName());
                builder.append(" = ");
                builder.append(property.getName());
                builder.append(SC);
                builder.append(NL);
            }
        }
        builder.append(indent);
        builder.append(RCB);
        return builder.toString();
    }

    public static String createGetter(final GeneratedProperty property,
            final String indent) {
        final StringBuilder builder = new StringBuilder();

        final Type type = property.getReturnType();
        final String varName = property.getName();
        final char first = Character.toUpperCase(varName.charAt(0));
        final String methodName = "get" + first + varName.substring(1);

        builder.append(indent + PUBLIC + GAP + getExplicitType(type) + GAP
                + methodName);
        builder.append(LB + RB + LCB + NL);

        String currentIndent = indent + TAB;

        builder.append(currentIndent + "return " + varName + SC + NL);

        builder.append(indent + RCB);
        return builder.toString();
    }
    
    public static String createSetter(final GeneratedProperty property,
            final String indent) {
        final StringBuilder builder = new StringBuilder();

        final Type type = property.getReturnType();
        final String varName = property.getName();
        final char first = Character.toUpperCase(varName.charAt(0));
        final String methodName = "set" + first + varName.substring(1);

        builder.append(indent + PUBLIC + GAP + "void" + GAP
                + methodName);
        builder.append(LB + getExplicitType(type) + GAP + varName + RB + LCB + NL);
        String currentIndent = indent + TAB;
        builder.append(currentIndent + "this." + varName + " = " + varName + SC + NL);
        builder.append(indent + RCB);
        return builder.toString();
    }

    public static String createHashCode(
            final List<GeneratedProperty> properties, final String indent) {
        StringBuilder builder = new StringBuilder();
        builder.append(indent + "public int hashCode() {" + NL);
        builder.append(indent + TAB + "final int prime = 31;" + NL);
        builder.append(indent + TAB + "int result = 1;" + NL);
        
        for (GeneratedProperty property : properties) {
            String fieldName = property.getName();
            builder.append(indent + TAB + "result = prime * result + (("
                    + fieldName + " == null) ? 0 : " + fieldName
                    + ".hashCode());" + NL);
        }

        builder.append(indent + TAB + "return result;" + NL);
        builder.append(indent + RCB + NL);
        return builder.toString();
    }

    public static String createEquals(final GeneratedTransferObject type,
            final List<GeneratedProperty> properties, final String indent) {
        StringBuilder builder = new StringBuilder();
        final String indent1 = indent + TAB;
        final String indent2 = indent + TAB + TAB;
        final String indent3 = indent + TAB + TAB + TAB;

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

        String typeStr = type.getPackageName() + "." + type.getName();
        builder.append(indent1 + typeStr + " other = (" + typeStr + ") obj;"
                + NL);

        for (GeneratedProperty property : properties) {
            String fieldName = property.getName();
            builder.append(indent1 + "if (" + fieldName + " == null) {" + NL);
            builder.append(indent2 + "if (other." + fieldName + " != null) {"
                    + NL);
            builder.append(indent3 + "return false;" + NL);
            builder.append(indent2 + "}" + NL);
            builder.append(indent1 + "} else if (!" + fieldName
                    + ".equals(other." + fieldName + ")) {" + NL);
            builder.append(indent2 + "return false;" + NL);
            builder.append(indent1 + "}" + NL);
        }

        builder.append(indent1 + "return true;" + NL);

        builder.append(indent + RCB + NL);
        return builder.toString();
    }

    public static String createToString(final GeneratedTransferObject type,
            final List<GeneratedProperty> properties, final String indent) {
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
        for (GeneratedProperty property : properties) {
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
                builder.append("builder.append(\", ");
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

    public static String createEnum(final Enumeration enumeration,
            final String indent) {
        final StringBuilder builder = new StringBuilder(indent + ENUM + GAP
                + enumeration.getName() + GAP + LCB + NL);

        String separator = COMMA;
        final List<Pair> values = enumeration.getValues();
        builder.append(indent + TAB);
        for (int i = 0; i < values.size(); i++) {
            if (i + 1 == values.size()) {
                separator = SC;
            }
            builder.append(values.get(i).getName() + separator);
        }
        builder.append(NL);
        builder.append(indent + RCB);
        return builder.toString();
    }

    private static String getExplicitType(final Type type) {
        String packageName = type.getPackageName();
        if (packageName.endsWith(".")) {
            packageName = packageName.substring(0, packageName.length() - 1);
        }
        final StringBuilder builder = new StringBuilder(packageName + "."
                + type.getName());
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] pTypes = pType.getActualTypeArguments();
            builder.append("<");
            builder.append(getParameters(pTypes));
            builder.append(">");
        }
        if (builder.toString().equals("java.lang.Void")) {
            return "void";
        }
        return builder.toString();
    }

    private static String getParameters(final Type[] pTypes) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pTypes.length; i++) {
            Type t = pTypes[i];

            String separator = COMMA;
            if (i + 1 == pTypes.length) {
                separator = "";
            }
            builder.append(getExplicitType(t) + separator);
        }
        return builder.toString();
    }

    private static void createComment(final StringBuilder builder,
            final String comment, final String indent) {
        if (comment != null && comment.length() > 0) {
            builder.append(indent + "/*" + NL);
            builder.append(indent + comment + NL);
            builder.append(indent + "*/" + NL);
        }
    }

}
