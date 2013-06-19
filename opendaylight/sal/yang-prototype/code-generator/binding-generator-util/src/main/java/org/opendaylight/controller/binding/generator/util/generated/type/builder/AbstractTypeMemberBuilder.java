/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util.generated.type.builder;

import org.opendaylight.controller.sal.binding.model.api.AccessModifier;
import org.opendaylight.controller.sal.binding.model.api.AnnotationType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.AnnotationTypeBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.TypeMemberBuilder;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractTypeMemberBuilder implements TypeMemberBuilder {
    private final String name;
    private Type returnType;
    private final List<AnnotationTypeBuilder> annotationBuilders;
    private String comment = "";
    private boolean isFinal;
    private AccessModifier accessModifier;

    public AbstractTypeMemberBuilder(final String name) {
        this.name = name;
        this.annotationBuilders = new ArrayList<>();
    }

    @Override
    public AnnotationTypeBuilder addAnnotation(String packageName, String name) {
        if (packageName == null) {
            throw new IllegalArgumentException("Annotation Type cannot have package name null!");
        }
        if (name == null) {
            throw new IllegalArgumentException("Annotation Type cannot have name as null!");
        }
        final AnnotationTypeBuilder builder = new AnnotationTypeBuilderImpl(
                    packageName, name);
        annotationBuilders.add(builder);
        return builder;
    }

    protected Type getReturnType() {
        return returnType;
    }

    protected List<AnnotationTypeBuilder> getAnnotationBuilders() {
        return annotationBuilders;
    }

    protected String getComment() {
        return comment;
    }

    protected boolean isFinal() {
        return isFinal;
    }

    protected AccessModifier getAccessModifier() {
        return accessModifier;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setReturnType(Type returnType) {
        if (returnType == null) {
            throw new IllegalArgumentException("Return Type of member cannot be null!");
        }
        this.returnType = returnType;
    }

    @Override
    public void setAccessModifier(AccessModifier modifier) {
        if (modifier == null) {
            throw new IllegalArgumentException("Access Modifier for member type cannot be null!");
        }
        this.accessModifier = modifier;
    }

    @Override
    public void setComment(String comment) {
        if (comment == null) {
            throw new IllegalArgumentException("Comment string cannot be null!");
        }
        this.comment = comment;
    }

    @Override
    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    protected List<AnnotationType> toAnnotationTypes() {
        final List<AnnotationType> annotations = new ArrayList<>();
        for (final AnnotationTypeBuilder annotBuilder : getAnnotationBuilders()) {
            if (annotBuilder != null) {
                annotations.add(annotBuilder.toInstance());
            }
        }
        return annotations;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result
                + ((getReturnType() == null) ? 0 : getReturnType().hashCode());
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
        if (getName() == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!getName().equals(other.getName())) {
            return false;
        }
        if (getReturnType() == null) {
            if (other.getReturnType() != null) {
                return false;
            }
        } else if (!getReturnType().equals(other.getReturnType())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GeneratedPropertyImpl [name=");
        builder.append(getName());
        builder.append(", annotations=");
        builder.append(getAnnotationBuilders());
        builder.append(", comment=");
        builder.append(getComment());
        builder.append(", returnType=");
        builder.append(getReturnType());
        builder.append(", isFinal=");
        builder.append(isFinal());
        builder.append(", modifier=");
        builder.append(getAccessModifier());
        builder.append("]");
        return builder.toString();
    }
}
