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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

public final class GeneratorUtil {

	private GeneratorUtil() {
	}

	public static String createIfcDeclaration(final GeneratedType genType,
			final String indent,
			final Map<String, LinkedHashMap<String, Integer>> availableImports) {
		return createFileDeclaration(IFC, genType, indent, availableImports);
	}

	public static String createClassDeclaration(
			final GeneratedTransferObject genTransferObject,
			final String indent,
			final Map<String, LinkedHashMap<String, Integer>> availableImports) {
		return createFileDeclaration(CLASS, genTransferObject, indent,
				availableImports);
	}

	public static String createPackageDeclaration(final String packageName) {
		return PKG + GAP + packageName + SC;
	}

	private static String createFileDeclaration(final String type,
			final GeneratedType genType, final String indent,
			final Map<String, LinkedHashMap<String, Integer>> availableImports) {
		final StringBuilder builder = new StringBuilder();
		final String currentPkg = genType.getPackageName();

		createComment(builder, genType.getComment(), indent);

		if (!genType.getAnnotations().isEmpty()) {
			final List<AnnotationType> annotations = genType.getAnnotations();
			appendAnnotations(builder, annotations);
			builder.append(NL);
		}
		builder.append(PUBLIC + GAP + type + GAP + genType.getName() + GAP);

		if (genType instanceof GeneratedTransferObject) {
			GeneratedTransferObject genTO = (GeneratedTransferObject) genType;

			if (genTO.getExtends() != null) {
				builder.append(EXTENDS + GAP);
				builder.append(genTO.getExtends() + GAP);
			}
		}

		final List<Type> genImplements = genType.getImplements();
		if (!genImplements.isEmpty()) {
			if (genType instanceof GeneratedTransferObject) {
				builder.append(IMPLEMENTS + GAP);
			} else {
				builder.append(EXTENDS + GAP);
			}
			builder.append(getExplicitType(genImplements.get(0),
					availableImports, currentPkg));

			for (int i = 1; i < genImplements.size(); ++i) {
				builder.append(", ");
				builder.append(getExplicitType(genImplements.get(i),
						availableImports, currentPkg));
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
			final String indent,
			final Map<String, LinkedHashMap<String, Integer>> availableImports,
			final String currentPkg) {
		final StringBuilder builder = new StringBuilder();
		builder.append(indent + PUBLIC + GAP + STATIC + GAP + FINAL + GAP);
		builder.append(getExplicitType(constant.getType(), availableImports,
				currentPkg) + GAP + constant.getName());
		builder.append(GAP + "=" + GAP);
		builder.append(constant.getValue() + SC);
		return builder.toString();
	}

	public static String createField(final GeneratedProperty property,
			final String indent,
			Map<String, LinkedHashMap<String, Integer>> availableImports,
			final String currentPkg) {
		final StringBuilder builder = new StringBuilder();
		builder.append(indent);
		if (!property.getAnnotations().isEmpty()) {
			final List<AnnotationType> annotations = property.getAnnotations();
			appendAnnotations(builder, annotations);
			builder.append(NL);
		}
		builder.append(indent + PRIVATE + GAP);
		builder.append(getExplicitType(property.getReturnType(),
				availableImports, currentPkg) + GAP + property.getName());
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
			final String indent,
			Map<String, LinkedHashMap<String, Integer>> availableImports,
			final String currentPkg) {
		final StringBuilder builder = new StringBuilder();

		if (method == null) {
			throw new IllegalArgumentException(
					"Method Signature parameter MUST be specified and cannot be NULL!");
		}

		final String comment = method.getComment();
		final String name = method.getName();
		if (name == null) {
			throw new IllegalStateException("Method Name cannot be NULL!");
		}

		final Type type = method.getReturnType();
		if (type == null) {
			throw new IllegalStateException(
					"Method Return type cannot be NULL!");
		}

		final List<Parameter> parameters = method.getParameters();

		createComment(builder, comment, indent);
		builder.append(NL);
		builder.append(indent);

		if (!method.getAnnotations().isEmpty()) {
			final List<AnnotationType> annotations = method.getAnnotations();
			appendAnnotations(builder, annotations);
			builder.append(NL);
		}

		builder.append(indent
				+ getExplicitType(type, availableImports, currentPkg) + GAP
				+ name);
		builder.append(LB);
		for (int i = 0; i < parameters.size(); i++) {
			Parameter p = parameters.get(i);
			String separator = COMMA;
			if (i + 1 == parameters.size()) {
				separator = "";
			}
			builder.append(getExplicitType(p.getType(), availableImports,
					currentPkg) + GAP + p.getName() + separator);
		}
		builder.append(RB);
		builder.append(SC);

		return builder.toString();
	}

	public static String createConstructor(
			GeneratedTransferObject genTransferObject, final String indent,
			Map<String, LinkedHashMap<String, Integer>> availableImports) {
		final StringBuilder builder = new StringBuilder();

		final String currentPkg = genTransferObject.getPackageName();
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
			builder.append(getExplicitType(ctorParams.get(0).getReturnType(),
					availableImports, currentPkg));
			builder.append(" ");
			builder.append(ctorParams.get(0).getName());
			for (int i = 1; i < ctorParams.size(); ++i) {
				final GeneratedProperty param = ctorParams.get(i);
				builder.append(", ");
				builder.append(getExplicitType(param.getReturnType(),
						availableImports, currentPkg));
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
			final String indent,
			Map<String, LinkedHashMap<String, Integer>> availableImports,
			final String currentPkg) {
		final StringBuilder builder = new StringBuilder();

		final Type type = property.getReturnType();
		final String varName = property.getName();
		final char first = Character.toUpperCase(varName.charAt(0));
		final String methodName = "get" + first + varName.substring(1);

		builder.append(indent + PUBLIC + GAP
				+ getExplicitType(type, availableImports, currentPkg) + GAP
				+ methodName);
		builder.append(LB + RB + LCB + NL);

		String currentIndent = indent + TAB;

		builder.append(currentIndent + "return " + varName + SC + NL);

		builder.append(indent + RCB);
		return builder.toString();
	}

	public static String createSetter(final GeneratedProperty property,
			final String indent,
			Map<String, LinkedHashMap<String, Integer>> availableImports,
			String currentPkg) {
		final StringBuilder builder = new StringBuilder();

		final Type type = property.getReturnType();
		final String varName = property.getName();
		final char first = Character.toUpperCase(varName.charAt(0));
		final String methodName = "set" + first + varName.substring(1);

		builder.append(indent + PUBLIC + GAP + "void" + GAP + methodName);
		builder.append(LB + getExplicitType(type, availableImports, currentPkg)
				+ GAP + varName + RB + LCB + NL);
		String currentIndent = indent + TAB;
		builder.append(currentIndent + "this." + varName + " = " + varName + SC
				+ NL);
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

	public static String createEnum(final Enumeration enumeration,
			final String indent) {
		if (enumeration == null || indent == null)
			throw new IllegalArgumentException();
		final StringBuilder builder = new StringBuilder(indent + PUBLIC + GAP
				+ ENUM + GAP + enumeration.getName() + GAP + LCB + NL);

		String separator = COMMA + NL;
		final List<Pair> values = enumeration.getValues();

		for (int i = 0; i < values.size(); i++) {
			if (i + 1 == values.size()) {
				separator = SC;
			}
			builder.append(indent + TAB + values.get(i).getName() + LB
					+ values.get(i).getValue() + RB + separator);
		}
		builder.append(NL);
		builder.append(NL);
		final String ENUMERATION_NAME = "value";
		final String ENUMERATION_TYPE = "int";
		builder.append(indent + TAB + ENUMERATION_TYPE + GAP + ENUMERATION_NAME
				+ SC);
		builder.append(NL);
		builder.append(indent + TAB + PRIVATE + GAP + enumeration.getName()
				+ LB + ENUMERATION_TYPE + GAP + ENUMERATION_NAME + RB + GAP
				+ LCB + NL);
		builder.append(indent + TAB + TAB + "this." + ENUMERATION_NAME + GAP
				+ "=" + GAP + ENUMERATION_NAME + SC + NL);
		builder.append(indent + TAB + RCB + NL);

		builder.append(indent + RCB);
		builder.append(NL);
		return builder.toString();
	}

	private static String getExplicitType(final Type type,
			Map<String, LinkedHashMap<String, Integer>> availableImports,
			final String currentPkg) {
		if (type == null) {
			throw new IllegalArgumentException(
					"Type parameter MUST be specified and cannot be NULL!");
		}
		String packageName = type.getPackageName();

		LinkedHashMap<String, Integer> imports = availableImports.get(type
				.getName());

		if ((imports != null && packageName
				.equals(findMaxValue(imports).get(0)))
				|| packageName.equals(currentPkg)) {
			final StringBuilder builder = new StringBuilder(type.getName());
			if (type instanceof ParameterizedType) {
				ParameterizedType pType = (ParameterizedType) type;
				Type[] pTypes = pType.getActualTypeArguments();
				builder.append("<");
				builder.append(getParameters(pTypes, availableImports,
						currentPkg));
				builder.append(">");
			}
			if (builder.toString().equals("Void")) {
				return "void";
			}
			return builder.toString();
		} else {
			final StringBuilder builder = new StringBuilder();
			if (packageName.startsWith("java.lang")) {
				builder.append(type.getName());
			} else {
                if (!packageName.isEmpty()) {
                    builder.append(packageName + "." + type.getName());
                } else {
                    builder.append(type.getName());
                }

			}
			if (type instanceof ParameterizedType) {
				ParameterizedType pType = (ParameterizedType) type;
				Type[] pTypes = pType.getActualTypeArguments();
				builder.append("<");
				builder.append(getParameters(pTypes, availableImports,
						currentPkg));
				builder.append(">");
			}
			if (builder.toString().equals("Void")) {
				return "void";
			}
			return builder.toString();
		}
	}

	private static String getParameters(final Type[] pTypes,
			Map<String, LinkedHashMap<String, Integer>> availableImports,
			String currentPkg) {
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < pTypes.length; i++) {
			Type t = pTypes[i];

			String separator = COMMA;
			if (i + 1 == pTypes.length) {
				separator = "";
			}
			builder.append(getExplicitType(t, availableImports, currentPkg)
					+ separator);
		}
		return builder.toString();
	}

	private static List<String> findMaxValue(
			LinkedHashMap<String, Integer> imports) {
		final List<String> result = new ArrayList<String>();

		int maxValue = 0;
		int currentValue = 0;
		for (Map.Entry<String, Integer> entry : imports.entrySet()) {
			currentValue = entry.getValue();
			if (currentValue > maxValue) {
				result.clear();
				result.add(entry.getKey());
			} else if (currentValue == maxValue) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

	private static void createComment(final StringBuilder builder,
			final String comment, final String indent) {
		if (comment != null && comment.length() > 0) {
			builder.append(indent + "/*" + NL);
			builder.append(indent + comment + NL);
			builder.append(indent + "*/" + NL);
		}
	}

	public static Map<String, LinkedHashMap<String, Integer>> createImports(
			GeneratedType genType) {
		final Map<String, LinkedHashMap<String, Integer>> imports = new HashMap<String, LinkedHashMap<String, Integer>>();
		final String genTypePkg = genType.getPackageName();

		final List<Constant> constants = genType.getConstantDefinitions();
		final List<MethodSignature> methods = genType.getMethodDefinitions();
		List<Type> impl = genType.getImplements();

		// IMPLEMENTATIONS
		if (impl != null) {
			for (Type t : impl) {
				addTypeToImports(t, imports, genTypePkg);
			}
		}

		// CONSTANTS
		if (constants != null) {
			for (Constant c : constants) {
				Type ct = c.getType();
				addTypeToImports(ct, imports, genTypePkg);
			}
		}

		// METHODS
		if (methods != null) {
			for (MethodSignature m : methods) {
				Type ct = m.getReturnType();
				addTypeToImports(ct, imports, genTypePkg);
				for (MethodSignature.Parameter p : m.getParameters()) {
					addTypeToImports(p.getType(), imports, genTypePkg);
				}
			}
		}

		// PROPERTIES
		if (genType instanceof GeneratedTransferObject) {
			GeneratedTransferObject genTO = (GeneratedTransferObject) genType;

			List<GeneratedProperty> props = genTO.getProperties();
			if (props != null) {
				for (GeneratedProperty prop : props) {
					Type pt = prop.getReturnType();
					addTypeToImports(pt, imports, genTypePkg);
				}
			}
		}

		return imports;
	}

	private static void addTypeToImports(Type type,
			Map<String, LinkedHashMap<String, Integer>> importedTypes,
			String genTypePkg) {
		String typeName = type.getName();
		String typePkg = type.getPackageName();
		if (typePkg.startsWith("java.lang") || typePkg.equals(genTypePkg) ||
                typePkg.isEmpty()) {
			return;
		}
		LinkedHashMap<String, Integer> packages = importedTypes.get(typeName);
		if (packages == null) {
			packages = new LinkedHashMap<String, Integer>();
			packages.put(typePkg, 1);
			importedTypes.put(typeName, packages);
		} else {
			Integer occurrence = packages.get(typePkg);
			if (occurrence == null) {
				packages.put(typePkg, 1);
			} else {
				occurrence++;
				packages.put(typePkg, occurrence);
			}
		}

		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type[] params = pt.getActualTypeArguments();
			for (Type param : params) {
				addTypeToImports(param, importedTypes, genTypePkg);
			}
		}
	}

	public static List<String> createImportLines(
			Map<String, LinkedHashMap<String, Integer>> imports) {
		List<String> importLines = new ArrayList<String>();

		for (Map.Entry<String, LinkedHashMap<String, Integer>> entry : imports
				.entrySet()) {
			String typeName = entry.getKey();
			LinkedHashMap<String, Integer> typePkgMap = entry.getValue();
			String typePkg = typePkgMap.keySet().iterator().next();
			importLines.add("import " + typePkg + "." + typeName + SC);
		}
		return importLines;
	}

}
