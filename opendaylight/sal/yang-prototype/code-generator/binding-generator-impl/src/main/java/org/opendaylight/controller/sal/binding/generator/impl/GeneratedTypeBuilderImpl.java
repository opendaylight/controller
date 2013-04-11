/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.generator.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.sal.binding.model.api.AccessModifier;
import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.ConstantBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.EnumBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.MethodSignatureBuilder;

public final class GeneratedTypeBuilderImpl implements GeneratedTypeBuilder {

	private static final String[] SET_VALUES = new String[] { "abstract",
			"assert", "boolean", "break", "byte", "case", "catch", "char",
			"class", "const", "continue", "default", "double", "do", "else",
			"enum", "extends", "false", "final", "finally", "float", "for",
			"goto", "if", "implements", "import", "instanceof", "int",
			"interface", "long", "native", "new", "null", "package", "private",
			"protected", "public", "return", "short", "static", "strictfp",
			"super", "switch", "synchronized", "this", "throw", "throws",
			"transient", "true", "try", "void", "volatile", "while" };

	public static final Set<String> JAVA_RESERVED_WORDS = new HashSet<String>(
			Arrays.asList(SET_VALUES));

	private String packageName;
	private String comment;
	private final String name;
	private final List<EnumBuilder> enumDefinitions = new ArrayList<EnumBuilder>();
	private final List<ConstantBuilder> constantDefintions = new ArrayList<ConstantBuilder>();
	private final List<MethodSignatureBuilder> methodDefinitions = new ArrayList<MethodSignatureBuilder>();

	public GeneratedTypeBuilderImpl(final String packageName, final String name) {
		this.packageName = validatePackage(packageName);
		this.name = name;
	}

	public static String validatePackage(final String packageName) {
		if (packageName != null) {
			final String[] packNameParts = packageName.split("\\.");
			if (packNameParts != null) {
				final StringBuilder builder = new StringBuilder();
				for (int i = 0; i < packNameParts.length; ++i) {
					if (JAVA_RESERVED_WORDS.contains(packNameParts[i])) {
						packNameParts[i] = "_" + packNameParts[i];
					}
					if (i > 0) {
						builder.append(".");
					}

					builder.append(packNameParts[i]);
				}
				return builder.toString();
			}
		}
		return packageName;
	}

	@Override
	public Type getParentType() {
		return this;
	}

	@Override
	public String getPackageName() {
		return packageName;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void addComment(String comment) {
		this.comment = comment;
	}

	@Override
	public ConstantBuilder addConstant(Type type, String name, Object value) {
		final ConstantBuilder builder = new ConstantBuilderImpl(type, name,
				value);
		constantDefintions.add(builder);

		return builder;
	}

	@Override
	public EnumBuilder addEnumeration(final String name) {
		final EnumBuilder builder = new EnumerationBuilderImpl(packageName,
				name);
		enumDefinitions.add(builder);
		return builder;
	}

	@Override
	public MethodSignatureBuilder addMethod(final String name) {
		final MethodSignatureBuilder builder = new MethodSignatureBuilderImpl(
				this, name);
		methodDefinitions.add(builder);
		return builder;
	}

	@Override
	public GeneratedType toInstance() {
		packageName = (packageName);

		return new GeneratedTypeImpl(this, packageName, name, enumDefinitions,
				constantDefintions, methodDefinitions);
	}

	private static final class MethodSignatureBuilderImpl implements
			MethodSignatureBuilder {
		private final String name;
		private Type returnType;
		private final List<MethodSignature.Parameter> parameters;
		private String comment = "";
		private final Type parent;

		public MethodSignatureBuilderImpl(final Type parent, final String name) {
			super();
			this.name = name;
			this.parent = parent;
			parameters = new ArrayList<MethodSignature.Parameter>();
			// TODO: move implementation elsewhere!

		}

		@Override
		public void addReturnType(Type returnType) {
			if (returnType != null) {
				this.returnType = returnType;
			}
		}

		@Override
		public void addParameter(Type type, String name) {
			parameters.add(new MethodParameterImpl(name, type));
		}

		@Override
		public void addComment(String comment) {
			this.comment = comment;
		}

		@Override
		public MethodSignature toInstance(Type definingType) {
			return new MethodSignatureImpl(definingType, name, comment,
					returnType, parameters);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			MethodSignatureBuilderImpl other = (MethodSignatureBuilderImpl) obj;
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("MethodBuilderImpl [name=");
			builder.append(name);
			builder.append(", returnType=");
			builder.append(returnType);
			builder.append(", parameters=");
			builder.append(parameters);
			builder.append(", comment=");
			builder.append(comment);
			builder.append(", parent=");
			builder.append(parent.getName());
			builder.append("]");
			return builder.toString();
		}

	}

	private static final class MethodSignatureImpl implements MethodSignature {

		private final String name;
		private final String comment;
		private final Type definingType;
		private final Type returnType;
		private final List<Parameter> params;

		public MethodSignatureImpl(final Type definingType, final String name,
				final String comment, final Type returnType,
				final List<Parameter> params) {
			super();
			this.name = name;
			this.comment = comment;
			this.definingType = definingType;
			this.returnType = returnType;
			this.params = Collections.unmodifiableList(params);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getComment() {
			return comment;
		}

		@Override
		public Type getDefiningType() {
			return definingType;
		}

		@Override
		public Type getReturnType() {
			return returnType;
		}

		@Override
		public List<Parameter> getParameters() {
			return params;
		}

		@Override
		public AccessModifier getAccessModifier() {
			return AccessModifier.PUBLIC;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((comment == null) ? 0 : comment.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result
					+ ((params == null) ? 0 : params.hashCode());
			result = prime * result
					+ ((returnType == null) ? 0 : returnType.hashCode());

			if (definingType != null) {
				result = prime
						* result
						+ ((definingType.getPackageName() == null) ? 0
								: definingType.getPackageName().hashCode());
				result = prime
						* result
						+ ((definingType.getName() == null) ? 0 : definingType
								.getName().hashCode());
			}

			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			MethodSignatureImpl other = (MethodSignatureImpl) obj;
			if (comment == null) {
				if (other.comment != null) {
					return false;
				}
			} else if (!comment.equals(other.comment)) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			if (params == null) {
				if (other.params != null) {
					return false;
				}
			} else if (!params.equals(other.params)) {
				return false;
			}
			if (definingType == null) {
				if (other.definingType != null) {
					return false;
				}
			} else if ((definingType != null) && (other.definingType != null)) {
				if (!definingType.getPackageName().equals(
						other.definingType.getPackageName())
						&& !definingType.getName().equals(
								other.definingType.getName())) {
					return false;
				}
			}
			if (returnType == null) {
				if (other.returnType != null) {
					return false;
				}
			} else if (!returnType.equals(other.returnType)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("MethodImpl [name=");
			builder.append(name);
			builder.append(", comment=");
			builder.append(comment);
			if (definingType != null) {
				builder.append(", definingType=");
				builder.append(definingType.getPackageName());
				builder.append(".");
				builder.append(definingType.getName());
			} else {
				builder.append(", definingType= null");
			}
			builder.append(", returnType=");
			builder.append(returnType);
			builder.append(", params=");
			builder.append(params);
			builder.append("]");
			return builder.toString();
		}
	}

	private static final class GeneratedTypeImpl implements GeneratedType {

		private final Type parent;
		private final String packageName;
		private final String name;
		private final List<Enumeration> enumDefinitions;
		private final List<Constant> constantDefintions;
		private final List<MethodSignature> methodDefinitions;

		public GeneratedTypeImpl(final Type parent, final String packageName,
				final String name, final List<EnumBuilder> enumBuilders,
				final List<ConstantBuilder> constantBuilders,
				final List<MethodSignatureBuilder> methodBuilders) {
			super();
			this.parent = parent;
			this.packageName = packageName;
			this.name = name;

			this.constantDefintions = toUnmodifiableConstants(constantBuilders);
			this.enumDefinitions = toUnmodifiableEnums(enumBuilders);
			this.methodDefinitions = toUnmodifiableMethods(methodBuilders);
		}

		private List<MethodSignature> toUnmodifiableMethods(
				List<MethodSignatureBuilder> methodBuilders) {
			final List<MethodSignature> methods = new ArrayList<MethodSignature>();
			for (final MethodSignatureBuilder methodBuilder : methodBuilders) {
				methods.add(methodBuilder.toInstance(this));
			}
			return Collections.unmodifiableList(methods);
		}

		private List<Enumeration> toUnmodifiableEnums(
				List<EnumBuilder> enumBuilders) {
			final List<Enumeration> enums = new ArrayList<Enumeration>();
			for (final EnumBuilder enumBuilder : enumBuilders) {
				enums.add(enumBuilder.toInstance(this));
			}
			return Collections.unmodifiableList(enums);
		}

		private List<Constant> toUnmodifiableConstants(
				List<ConstantBuilder> constantBuilders) {
			final List<Constant> constants = new ArrayList<Constant>();
			for (final ConstantBuilder enumBuilder : constantBuilders) {
				constants.add(enumBuilder.toInstance(this));
			}
			return Collections.unmodifiableList(constants);
		}

		@Override
		public String getPackageName() {
			return packageName;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Type getParentType() {
			return parent;
		}

		@Override
		public List<Enumeration> getEnumDefintions() {
			return enumDefinitions;
		}

		@Override
		public List<Constant> getConstantDefinitions() {
			return constantDefintions;
		}

		@Override
		public List<MethodSignature> getMethodDefinitions() {
			return methodDefinitions;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime
					* result
					+ ((constantDefintions == null) ? 0 : constantDefintions
							.hashCode());
			result = prime
					* result
					+ ((enumDefinitions == null) ? 0 : enumDefinitions
							.hashCode());
			result = prime
					* result
					+ ((methodDefinitions == null) ? 0 : methodDefinitions
							.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result
					+ ((packageName == null) ? 0 : packageName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			GeneratedTypeImpl other = (GeneratedTypeImpl) obj;
			if (constantDefintions == null) {
				if (other.constantDefintions != null) {
					return false;
				}
			} else if (!constantDefintions.equals(other.constantDefintions)) {
				return false;
			}
			if (enumDefinitions == null) {
				if (other.enumDefinitions != null) {
					return false;
				}
			} else if (!enumDefinitions.equals(other.enumDefinitions)) {
				return false;
			}
			if (methodDefinitions == null) {
				if (other.methodDefinitions != null) {
					return false;
				}
			} else if (!methodDefinitions.equals(other.methodDefinitions)) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			if (packageName == null) {
				if (other.packageName != null) {
					return false;
				}
			} else if (!packageName.equals(other.packageName)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("GeneratedTypeImpl [parent=");
			builder.append(parent.getName());
			builder.append(", packageName=");
			builder.append(packageName);
			builder.append(", name=");
			builder.append(name);
			builder.append(", enumDefinitions=");
			builder.append(enumDefinitions);
			builder.append(", constantDefintions=");
			builder.append(constantDefintions);
			builder.append(", methodDefinitions=");
			builder.append(methodDefinitions);
			builder.append("]");
			return builder.toString();
		}
	}
}
