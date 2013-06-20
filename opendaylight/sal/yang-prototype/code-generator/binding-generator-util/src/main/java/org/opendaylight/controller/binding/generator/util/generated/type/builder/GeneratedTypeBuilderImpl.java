/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util.generated.type.builder;

import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.*;

import java.util.List;

public final class GeneratedTypeBuilderImpl extends AbstractGeneratedTypeBuilder {

    public GeneratedTypeBuilderImpl(String packageName, String name) {
        super(packageName, name);
        setAbstract(true);
    }

    @Override
    public GeneratedType toInstance() {
        return new GeneratedTypeImpl(null, getPackageName(), getName(), getComment(), getAnnotations(), isAbstract(),
                getImplementsTypes(), getEnclosedTypes(), getEnclosedTransferObjects(), getEnumerations(),
                getConstants(), getMethodDefinitions());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GeneratedTransferObject [packageName=");
        builder.append(getPackageName());
        builder.append(", name=");
        builder.append(getName());
        builder.append(", comment=");
        builder.append(", annotations=");
        builder.append(getAnnotations());
        builder.append(getComment());
        builder.append(", implements=");
        builder.append(getImplementsTypes());
        builder.append(", enclosedTypes=");
        builder.append(getEnclosedTypes());
        builder.append(", constants=");
        builder.append(getConstants());
        builder.append(", enumerations=");
        builder.append(getEnumerations());
        builder.append(", properties=");
        builder.append(", methods=");
        builder.append(getMethodDefinitions());
        builder.append("]");
        return builder.toString();
    }

    private static final class GeneratedTypeImpl extends AbstractGeneratedType {

        public GeneratedTypeImpl(final Type parent, final String packageName, final String name, final String comment,
                                 final List<AnnotationTypeBuilder> annotationBuilders, final boolean isAbstract,
                                 final List<Type> implementsTypes,
                                 final List<GeneratedTypeBuilder> enclosedGenTypeBuilders,
                                 final List<GeneratedTOBuilder> enclosedGenTOBuilders,
                                 final List<EnumBuilder> enumBuilders, final List<Constant> constants,
                                 final List<MethodSignatureBuilder> methodBuilders) {
            super(parent, packageName, name, comment, annotationBuilders, isAbstract, implementsTypes,
                    enclosedGenTypeBuilders, enclosedGenTOBuilders, enumBuilders, constants, methodBuilders);
        }
    }
}
