/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util.generated.type.builder;

import org.opendaylight.controller.sal.binding.model.api.AnnotationType;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedPropertyBuilder;

import java.util.List;

final class GeneratedPropertyBuilderImpl extends AbstractTypeMemberBuilder implements GeneratedPropertyBuilder {

    private boolean isReadOnly;

    public GeneratedPropertyBuilderImpl(String name) {
        super(name);
        this.isReadOnly = true;
    }

    @Override
    public void setReadOnly(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    @Override
    public GeneratedProperty toInstance(Type definingType) {
        final List<AnnotationType> annotations = toAnnotationTypes();
        return new GeneratedPropertyImpl(definingType, getName(), annotations, getComment(), getAccessModifier(),
                getReturnType(), isFinal(), isReadOnly);
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
        builder.append(", isReadOnly=");
        builder.append(isReadOnly);
        builder.append(", modifier=");
        builder.append(getAccessModifier());
        builder.append("]");
        return builder.toString();
    }
}