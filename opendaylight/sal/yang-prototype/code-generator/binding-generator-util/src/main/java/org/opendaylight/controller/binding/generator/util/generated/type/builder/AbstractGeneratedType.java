/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util.generated.type.builder;


import org.opendaylight.controller.binding.generator.util.AbstractBaseType;
import org.opendaylight.controller.sal.binding.model.api.*;
import org.opendaylight.controller.sal.binding.model.api.type.builder.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class AbstractGeneratedType extends AbstractBaseType implements GeneratedType {

    private final Type parent;
    private final String comment;
    private final List<AnnotationType> annotations;
    private final List<Type> implementsTypes;
    private final List<Enumeration> enumerations;
    private final List<Constant> constants;
    private final List<MethodSignature> methodSignatures;
    private final List<GeneratedType> enclosedTypes;
    private final boolean isAbstract;

    public AbstractGeneratedType(final Type parent, final String packageName,
                             final String name, final String comment,
                             final List<AnnotationTypeBuilder> annotationBuilders,
                             final boolean isAbstract,
                             final List<Type> implementsTypes,
                             final List<GeneratedTypeBuilder> enclosedGenTypeBuilders,
                             final List<GeneratedTOBuilder> enclosedGenTOBuilders,
                             final List<EnumBuilder> enumBuilders,
                             final List<Constant> constants,
                             final List<MethodSignatureBuilder> methodBuilders) {
        super(packageName, name);
        this.parent = parent;
        this.comment = comment;
        this.annotations = toUnmodifiableAnnotations(annotationBuilders);
        this.implementsTypes = Collections.unmodifiableList(implementsTypes);
        this.constants = Collections.unmodifiableList(constants);
        this.enumerations = toUnmodifiableEnumerations(enumBuilders);
        this.methodSignatures = toUnmodifiableMethods(methodBuilders);
        this.enclosedTypes = toUnmodifiableEnclosedTypes(enclosedGenTypeBuilders, enclosedGenTOBuilders);
        this.isAbstract = isAbstract;
    }

    private List<GeneratedType> toUnmodifiableEnclosedTypes(final List<GeneratedTypeBuilder> enclosedGenTypeBuilders,
                                                            final List<GeneratedTOBuilder> enclosedGenTOBuilders) {
        final List<GeneratedType> enclosedTypes = new ArrayList<>();
        for (final GeneratedTypeBuilder builder : enclosedGenTypeBuilders) {
            if (builder != null) {
                enclosedTypes.add(builder.toInstance());
            }
        }

        for (final GeneratedTOBuilder builder : enclosedGenTOBuilders) {
            if (builder != null) {
                enclosedTypes.add(builder.toInstance());
            }
        }
        return enclosedTypes;
    }

    protected List<AnnotationType> toUnmodifiableAnnotations(
            final List<AnnotationTypeBuilder> annotationBuilders) {
        final List<AnnotationType> annotations = new ArrayList<>();
        for (final AnnotationTypeBuilder builder : annotationBuilders) {
            annotations.add(builder.toInstance());
        }
        return Collections.unmodifiableList(annotations);
    }

    protected List<MethodSignature> toUnmodifiableMethods(
            List<MethodSignatureBuilder> methodBuilders) {
        final List<MethodSignature> methods = new ArrayList<>();
        for (final MethodSignatureBuilder methodBuilder : methodBuilders) {
            methods.add(methodBuilder.toInstance(this));
        }
        return Collections.unmodifiableList(methods);
    }

    protected List<Enumeration> toUnmodifiableEnumerations(
            List<EnumBuilder> enumBuilders) {
        final List<Enumeration> enums = new ArrayList<>();
        for (final EnumBuilder enumBuilder : enumBuilders) {
            enums.add(enumBuilder.toInstance(this));
        }
        return Collections.unmodifiableList(enums);
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
    public boolean isAbstract() {
       return isAbstract;
    }

     @Override
    public List<Type> getImplements() {
        return implementsTypes;
    }

    @Override
    public List<GeneratedType> getEnclosedTypes() {
        return enclosedTypes;
    }

    @Override
    public List<Enumeration> getEnumerations() {
        return enumerations;
    }

    @Override
    public List<Constant> getConstantDefinitions() {
        return constants;
    }

    @Override
    public List<MethodSignature> getMethodDefinitions() {
        return methodSignatures;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GeneratedType [packageName=");
        builder.append(getPackageName());
        builder.append(", name=");
        builder.append(getName());
        if (parent != null) {
            builder.append(", parent=");
            builder.append(parent.getFullyQualifiedName());
        } else {
            builder.append(", parent=null");
        }
        builder.append(", comment=");
        builder.append(comment);
        builder.append(", annotations=");
        builder.append(annotations);
        builder.append(", enclosedTypes=");
        builder.append(enclosedTypes);
        builder.append(", enumerations=");
        builder.append(enumerations);
        builder.append(", constants=");
        builder.append(constants);
        builder.append(", methodSignatures=");
        builder.append(methodSignatures);
        builder.append("]");
        return builder.toString();
    }
}
