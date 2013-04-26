/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util.generated.type.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.sal.binding.model.api.AnnotationType;
import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.AnnotationTypeBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.ConstantBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.EnumBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.MethodSignatureBuilder;

public final class GeneratedTypeBuilderImpl implements GeneratedTypeBuilder {

    private final String packageName;
    private String comment = "";
    private final String name;
    private final List<AnnotationTypeBuilder> annotationBuilders = new ArrayList<AnnotationTypeBuilder>();
    private final List<EnumBuilder> enumDefinitions = new ArrayList<EnumBuilder>();
    private final List<ConstantBuilder> constantDefintions = new ArrayList<ConstantBuilder>();
    private final List<MethodSignatureBuilder> methodDefinitions = new ArrayList<MethodSignatureBuilder>();

    public GeneratedTypeBuilderImpl(final String packageName, final String name) {
        this.packageName = packageName;
        this.name = name;
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
    public AnnotationTypeBuilder addAnnotation(String packageName, String name) {
        if (packageName != null && name != null) {
            final AnnotationTypeBuilder builder = new AnnotationTypeBuilderImpl(
                    packageName, name);
            if (annotationBuilders.add(builder)) {
                return builder;
            }
        }
        return null;
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
        final String innerPackageName = packageName + "." + this.name;
        final EnumBuilder builder = new EnumerationBuilderImpl(
                innerPackageName, name);
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
        return new GeneratedTypeImpl(this, packageName, name, comment,
                annotationBuilders, enumDefinitions, constantDefintions,
                methodDefinitions);
    }

    private static final class GeneratedTypeImpl implements GeneratedType {

        private final Type parent;
        private final String packageName;
        private final String name;
        private final String comment;
        private final List<AnnotationType> annotations;
        private final List<Enumeration> enumDefinitions;
        private final List<Constant> constantDefintions;
        private final List<MethodSignature> methodDefinitions;

        public GeneratedTypeImpl(final Type parent, final String packageName,
                final String name, final String comment,
                final List<AnnotationTypeBuilder> annotationBuilders,
                final List<EnumBuilder> enumBuilders,
                final List<ConstantBuilder> constantBuilders,
                final List<MethodSignatureBuilder> methodBuilders) {
            super();
            this.parent = parent;
            this.packageName = packageName;
            this.name = name;
            this.comment = comment;
            this.annotations = toUnmodifiableAnnotations(annotationBuilders);
            this.constantDefintions = toUnmodifiableConstants(constantBuilders);
            this.enumDefinitions = toUnmodifiableEnums(enumBuilders);
            this.methodDefinitions = toUnmodifiableMethods(methodBuilders);
        }

        private List<AnnotationType> toUnmodifiableAnnotations(
                final List<AnnotationTypeBuilder> annotationBuilders) {
            final List<AnnotationType> annotations = new ArrayList<AnnotationType>();
            for (final AnnotationTypeBuilder builder : annotationBuilders) {
                annotations.add(builder.toInstance());
            }
            return Collections.unmodifiableList(annotations);
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
        public String getComment() {
            return comment;
        }

        @Override
        public List<AnnotationType> getAnnotations() {
            return annotations;
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
            builder.append("GeneratedType [packageName=");
            builder.append(packageName);
            builder.append(", name=");
            builder.append(name);
            if (parent != null) {
                builder.append(", parent=");
                builder.append(parent.getPackageName());
                builder.append(".");
                builder.append(parent.getName());
            } else {
                builder.append(", parent=null");
            }
            builder.append(", comment=");
            builder.append(comment);
            builder.append(", annotations=");
            builder.append(annotations);
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
