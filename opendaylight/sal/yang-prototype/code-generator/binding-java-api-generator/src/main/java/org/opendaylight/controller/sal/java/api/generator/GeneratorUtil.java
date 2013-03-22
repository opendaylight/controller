/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.java.api.generator;

import static org.opendaylight.controller.sal.java.api.generator.Constants.*;

import java.util.List;

import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.ParameterizedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.Enumeration.Pair;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature.Parameter;

public class GeneratorUtil {

    private GeneratorUtil() {
    }

    public static String createIfcDeclarationWithPkgName(String packageName,
            String name, String indent) {
        return createFileDeclarationWithPkgName(IFC, packageName, name, indent);
    }

    public static String createClassDeclarationWithPkgName(String packageName,
            String name, String indent) {
        return createFileDeclarationWithPkgName(CLASS, packageName, name,
                indent);
    }

    private static String createFileDeclarationWithPkgName(String type,
            String packageName, String name, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(PKG + GAP + packageName + SC);
        sb.append(NL);
        sb.append(NL);
        sb.append(PUBLIC + GAP + type + GAP + name + GAP + LCB);
        return sb.toString();
    }

    public static String createConstant(Constant constant, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent + PUBLIC + GAP + STATIC + GAP + FINAL + GAP);
        sb.append(getExplicitType(constant.getType()) + GAP
                + constant.getName());
        sb.append(GAP + "=" + GAP);
        sb.append(constant.getValue() + SC);
        return sb.toString();
    }

    public static String createField(Constant field, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent + PRIVATE + GAP);
        sb.append(getExplicitType(field.getType()) + GAP + field.getName());
        sb.append(GAP + "=" + GAP);
        sb.append(field.getValue() + SC);
        return sb.toString();
    }

    /**
     * Create method declaration in interface.
     * 
     * @param method
     * @param indent
     * @return
     */
    public static String createMethodDeclaration(MethodSignature method,
            String indent) {
        String comment = method.getComment();
        Type type = method.getReturnType();
        String name = method.getName();
        List<Parameter> parameters = method.getParameters();

        StringBuilder sb = new StringBuilder();
        createComment(sb, comment, indent);

        sb.append(indent + getExplicitType(type) + GAP + name);
        sb.append(LB);
        for (int i = 0; i < parameters.size(); i++) {
            Parameter p = parameters.get(i);
            String separator = COMMA;
            if (i + 1 == parameters.size()) {
                separator = "";
            }
            sb.append(getExplicitType(p.getType()) + GAP + p.getName()
                    + separator);
        }
        sb.append(RB);
        sb.append(SC);

        return sb.toString();
    }

    public static String createGetter(Constant field, String indent) {
        StringBuilder sb = new StringBuilder();

        Type type = field.getType();
        String varName = field.getName();
        char first = Character.toUpperCase(varName.charAt(0));
        String methodName = "get" + first + varName.substring(1);

        sb.append(indent + PUBLIC + GAP + getExplicitType(type) + GAP
                + methodName);
        sb.append(LB + RB + LCB + NL);

        String currentIndent = indent + TAB;

        sb.append(currentIndent + "return " + varName + SC + NL);

        sb.append(indent + RCB);
        return sb.toString();
    }

    public static String createHashCode(List<Constant> fields, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent + "public int hashCode() {" + NL);
        sb.append(indent + TAB + "final int prime = 31;" + NL);
        sb.append(indent + TAB + "int result = 1;" + NL);

        for (Constant field : fields) {
            String fieldName = field.getName();
            sb.append(indent + TAB + "result = prime * result + ((" + fieldName
                    + " == null) ? 0 : " + fieldName + ".hashCode());" + NL);
        }

        sb.append(indent + TAB + "return result;" + NL);
        sb.append(indent + RCB + NL);
        return sb.toString();
    }

    public static String createEquals(Type type, List<Constant> fields,
            String indent) {
        StringBuilder sb = new StringBuilder();
        final String indent1 = indent + TAB;
        final String indent2 = indent + TAB + TAB;
        final String indent3 = indent + TAB + TAB + TAB;

        sb.append(indent + "public boolean equals(Object obj) {" + NL);
        sb.append(indent1 + "if (this == obj) {" + NL);
        sb.append(indent2 + "return true;" + NL);
        sb.append(indent1 + "}" + NL);
        sb.append(indent1 + "if (obj == null) {" + NL);
        sb.append(indent2 + "return false;" + NL);
        sb.append(indent1 + "}" + NL);
        sb.append(indent1 + "if (getClass() != obj.getClass()) {" + NL);
        sb.append(indent2 + "return false;" + NL);
        sb.append(indent1 + "}" + NL);

        String typeStr = type.getPackageName() + "." + type.getName();
        sb.append(indent1 + typeStr + " other = (" + typeStr + ") obj;" + NL);

        for (Constant field : fields) {
            String fieldName = field.getName();
            sb.append(indent1 + "if (" + fieldName + " == null) {" + NL);
            sb.append(indent2 + "if (other." + fieldName + " != null) {" + NL);
            sb.append(indent3 + "return false;" + NL);
            sb.append(indent2 + "}" + NL);
            sb.append(indent1 + "} else if (!" + fieldName + ".equals(other."
                    + fieldName + ")) {" + NL);
            sb.append(indent2 + "return false;" + NL);
            sb.append(indent1 + "}" + NL);
        }

        sb.append(indent1 + "return true;" + NL);

        sb.append(indent + RCB + NL);
        return sb.toString();
    }

    public static String createToString(Type type, List<Constant> fields,
            String indent) {
        StringBuilder sb = new StringBuilder();
        String typeStr = type.getPackageName() + "." + type.getName();

        sb.append(indent + "public String toString() {" + NL);
        sb.append(indent + TAB + "return \"" + typeStr + "[");

        boolean first = true;
        for (Constant field : fields) {
            String fieldName = field.getName();
            String fieldType = field.getType().getPackageName() + "."
                    + field.getType().getName();
            if (first) {
                if (fieldType.equals("java.lang.String")) {
                    sb.append(fieldName + "=\\\""
                            + parseStringValue((String) field.getValue())
                            + "\\\"");
                } else {
                    sb.append(fieldName + "=" + field.getValue() + "");
                }
            } else {
                if (fieldType.equals("java.lang.String")) {
                    sb.append(", " + fieldName + "=\\\""
                            + parseStringValue((String) field.getValue())
                            + "\\\"");
                } else {
                    sb.append(", " + fieldName + "=" + field.getValue() + "");
                }

            }
            first = false;
        }
        sb.append("]\"" + SC + NL);

        sb.append(indent + RCB + NL);
        return sb.toString();
    }

    /**
     * Remove starting and ending quote sign
     * 
     * @param o
     * @return
     */
    private static String parseStringValue(String str) {
        return str.substring(1, str.length() - 1);
    }

    public static String createEnum(Enumeration e, String indent) {
        StringBuilder sb = new StringBuilder(indent + ENUM + GAP + e.getName()
                + GAP + LCB + NL);

        String separator = COMMA;
        List<Pair> values = e.getValues();
        sb.append(indent + TAB);
        for (int i = 0; i < values.size(); i++) {
            if (i + 1 == values.size()) {
                separator = SC;
            }
            sb.append(values.get(i).getName() + separator);
        }
        sb.append(NL);
        sb.append(indent + RCB);
        return sb.toString();
    }

    private static String getExplicitType(Type type) {
        String packageName = type.getPackageName();
        if (packageName.endsWith(".")) {
            packageName = packageName.substring(0, packageName.length() - 1);
        }
        StringBuilder sb = new StringBuilder(packageName + "." + type.getName());
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] pTypes = pType.getActualTypeArguments();
            sb.append("<");
            sb.append(getParameters(pTypes));
            sb.append(">");
        }
        if (sb.toString().equals("java.lang.Void")) {
            return "void";
        }
        return sb.toString();
    }

    private static String getParameters(Type[] pTypes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pTypes.length; i++) {
            Type t = pTypes[i];

            String separator = COMMA;
            if (i + 1 == pTypes.length) {
                separator = "";
            }
            sb.append(getExplicitType(t) + separator);
        }
        return sb.toString();
    }

    private static void createComment(StringBuilder sb, String comment,
            String indent) {
        if (comment != null && comment.length() > 0) {
            sb.append(indent + "/*" + NL);
            sb.append(indent + comment + NL);
            sb.append(indent + "*/" + NL);
        }
    }

}
