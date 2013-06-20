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
import org.opendaylight.controller.sal.binding.model.api.TypeMember;

import java.util.Collections;
import java.util.List;

abstract class AbstractTypeMember implements TypeMember {

    private final String name;
    private final String comment;
    private final Type definingType;
    private final Type returnType;
    private final List<AnnotationType> annotations;
    private final boolean isFinal;
    private final AccessModifier accessModifier;

    public AbstractTypeMember(final Type definingType, final String name,  final List<AnnotationType> annotations,
                              final String comment, final AccessModifier accessModifier, final Type returnType,
                              boolean isFinal) {
        super();
        this.definingType = definingType;
        this.name = name;
        this.annotations = Collections.unmodifiableList(annotations);
        this.comment = comment;
        this.accessModifier = accessModifier;
        this.returnType = returnType;
        this.isFinal = isFinal;
    }

    @Override
    public List<AnnotationType> getAnnotations() {
        return annotations;
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
    public AccessModifier getAccessModifier() {
        return accessModifier;
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    @Override
    public boolean isFinal() {
        return isFinal;
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
        MethodSignatureImpl other = (MethodSignatureImpl) obj;
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
        builder.append("MethodSignatureImpl [name=");
        builder.append(getName());
        builder.append(", comment=");
        builder.append(getComment());
        if (getDefiningType() != null) {
            builder.append(", definingType=");
            builder.append(getDefiningType().getPackageName());
            builder.append(".");
            builder.append(getDefiningType().getName());
        } else {
            builder.append(", definingType= null");
        }
        builder.append(", returnType=");
        builder.append(getReturnType());
        builder.append(", annotations=");
        builder.append(getAnnotations());
        builder.append("]");
        return builder.toString();
    }
}
