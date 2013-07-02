/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util.generated.type.builder;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.sal.binding.model.api.AnnotationType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.MethodSignatureBuilder;

final class MethodSignatureBuilderImpl extends AbstractTypeMemberBuilder implements MethodSignatureBuilder {

    private final List<MethodSignature.Parameter> parameters;
    private boolean isAbstract;

    public MethodSignatureBuilderImpl(final String name) {
        super(name);
        this.parameters = new ArrayList<>();
    }

    @Override
    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    @Override
    public void addParameter(Type type, String name) {
        parameters.add(new MethodParameterImpl(name, type));
    }

    @Override
    public MethodSignature toInstance(Type definingType) {
        final List<AnnotationType> annotations = toAnnotationTypes();
        return new MethodSignatureImpl(definingType, getName(), annotations, getComment(), getAccessModifier(),
                getReturnType(), parameters, isFinal(), isAbstract);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result + ((getReturnType() == null) ? 0 : getReturnType().hashCode());
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
        if (parameters == null) {
            if (other.parameters != null) {
                return false;
            }
        } else if (!parameters.equals(other.parameters)) {
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
        builder.append("MethodSignatureBuilderImpl [name=");
        builder.append(getName());
        builder.append(", returnType=");
        builder.append(getReturnType());
        builder.append(", parameters=");
        builder.append(parameters);
        builder.append(", annotationBuilders=");
        builder.append(getAnnotationBuilders());
        builder.append(", comment=");
        builder.append(getComment());
        builder.append("]");
        return builder.toString();
    }
}