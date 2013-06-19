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
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.Type;

import java.util.List;

final class GeneratedPropertyImpl extends AbstractTypeMember implements GeneratedProperty {

    private boolean isReadOnly;

    public GeneratedPropertyImpl(Type definingType, String name, List<AnnotationType> annotations, String comment,
                                 AccessModifier accessModifier, Type returnType, boolean isFinal, boolean isReadOnly) {
        super(definingType, name, annotations, comment, accessModifier, returnType, isFinal);
        this.isReadOnly = isReadOnly;
    }

    @Override
    public boolean isReadOnly() {
        return isReadOnly;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GeneratedPropertyImpl [name=");
        builder.append(getName());
        builder.append(", annotations=");
        builder.append(getAnnotations());
        builder.append(", comment=");
        builder.append(getComment());
        if (getDefiningType() != null) {
            builder.append(", parent=");
            builder.append(getDefiningType().getPackageName());
            builder.append(".");
            builder.append(getDefiningType().getName());
        } else {
            builder.append(", parent=null");
        }
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
