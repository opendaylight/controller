/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util.generated.type.builder;

import org.opendaylight.controller.sal.binding.model.api.*;
import org.opendaylight.controller.sal.binding.model.api.type.builder.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GeneratedTOBuilderImpl extends AbstractGeneratedTypeBuilder implements GeneratedTOBuilder {

    private GeneratedTransferObject extendsType;
    private final List<GeneratedPropertyBuilder> properties = new ArrayList<>();
    private final List<GeneratedPropertyBuilder> equalsProperties = new ArrayList<>();
    private final List<GeneratedPropertyBuilder> hashProperties = new ArrayList<>();
    private final List<GeneratedPropertyBuilder> toStringProperties = new ArrayList<>();
    private boolean isUnionType = false;

    public GeneratedTOBuilderImpl(String packageName, String name) {
        super(packageName, name);
        setAbstract(false);
    }

    @Override
    public void setExtendsType(final GeneratedTransferObject genTransObj) {
        if (genTransObj == null) {
            throw new IllegalArgumentException("Generated Transfer Object cannot be null!");
        }
        extendsType = genTransObj;
    }

    @Override
    public GeneratedPropertyBuilder addProperty(String name) {
        final GeneratedPropertyBuilder builder = new GeneratedPropertyBuilderImpl(name);
        builder.setAccessModifier(AccessModifier.PUBLIC);
        properties.add(builder);
        return builder;
    }

    @Override
    public boolean containsProperty(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name can't be null");
        }
        for (GeneratedPropertyBuilder property : properties) {
            if (name.equals(property.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add new Method Signature definition for Generated Type Builder and
     * returns Method Signature Builder for specifying all Method parameters. <br>
     * Name of Method cannot be <code>null</code>, if it is <code>null</code>
     * the method SHOULD throw {@link IllegalArgumentException} <br>
     * By <i>Default</i> the MethodSignatureBuilder SHOULD be pre-set as
     * {@link MethodSignatureBuilder#setAbstract(false)},
     * {@link MethodSignatureBuilder#setFinal(false)} and
     * {@link MethodSignatureBuilder#setAccessModifier(PUBLIC)}
     * 
     * @param name
     *            Name of Method
     * @return <code>new</code> instance of Method Signature Builder.
     */
    @Override
    public MethodSignatureBuilder addMethod(String name) {
        final MethodSignatureBuilder builder = super.addMethod(name);
        builder.setAbstract(false);
        return builder;
    }

    @Override
    public boolean addEqualsIdentity(GeneratedPropertyBuilder property) {
        return equalsProperties.add(property);
    }

    @Override
    public boolean addHashIdentity(GeneratedPropertyBuilder property) {
        return hashProperties.add(property);
    }

    @Override
    public boolean addToStringProperty(GeneratedPropertyBuilder property) {
        return toStringProperties.add(property);
    }

    @Override
    public GeneratedTransferObject toInstance() {
        return new GeneratedTransferObjectImpl(null, getPackageName(), getName(), getComment(), getAnnotations(),
                isAbstract(), extendsType, getImplementsTypes(), getEnclosedTypes(), getEnclosedTransferObjects(),
                getConstants(), getEnumerations(), getMethodDefinitions(), properties, equalsProperties,
                hashProperties, toStringProperties, isUnionType);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GeneratedTransferObject [packageName=");
        builder.append(getPackageName());
        builder.append(", name=");
        builder.append(getName());
        builder.append(", comment=");
        builder.append(getComment());
        builder.append(", constants=");
        builder.append(getConstants());
        builder.append(", enumerations=");
        builder.append(getEnumerations());
        builder.append(", properties=");
        builder.append(properties);
        builder.append(", equalsProperties=");
        builder.append(equalsProperties);
        builder.append(", hashCodeProperties=");
        builder.append(hashProperties);
        builder.append(", stringProperties=");
        builder.append(toStringProperties);
        builder.append(", annotations=");
        builder.append(getAnnotations());
        builder.append(", methods=");
        builder.append(getMethodDefinitions());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public void setIsUnion(boolean isUnion) {
        this.isUnionType = isUnion;
    }

    private static final class GeneratedTransferObjectImpl extends AbstractGeneratedType implements
            GeneratedTransferObject {

        private final List<GeneratedProperty> properties;
        private final List<GeneratedProperty> equalsProperties;
        private final List<GeneratedProperty> hashCodeProperties;
        private final List<GeneratedProperty> stringProperties;
        private final GeneratedTransferObject extendsType;
        private final boolean isUnionType;

        GeneratedTransferObjectImpl(final Type parent, final String packageName, final String name,
                final String comment, final List<AnnotationTypeBuilder> annotationBuilders, final boolean isAbstract,
                final GeneratedTransferObject extendsType, final List<Type> implementsTypes,
                final List<GeneratedTypeBuilder> enclosedGenTypeBuilders,
                final List<GeneratedTOBuilder> enclosedGenTOBuilders, final List<Constant> constants,
                final List<EnumBuilder> enumBuilders, final List<MethodSignatureBuilder> methodBuilders,
                final List<GeneratedPropertyBuilder> propBuilders, final List<GeneratedPropertyBuilder> equalsBuilders,
                final List<GeneratedPropertyBuilder> hashCodeBuilders,
                final List<GeneratedPropertyBuilder> stringBuilders, final boolean isUnionType) {
            super(parent, packageName, name, comment, annotationBuilders, isAbstract, implementsTypes,
                    enclosedGenTypeBuilders, enclosedGenTOBuilders, enumBuilders, constants, methodBuilders);
            this.extendsType = extendsType;
            this.properties = toUnmodifiableProperties(propBuilders);
            this.equalsProperties = toUnmodifiableProperties(equalsBuilders);
            this.hashCodeProperties = toUnmodifiableProperties(hashCodeBuilders);
            this.stringProperties = toUnmodifiableProperties(stringBuilders);
            this.isUnionType = isUnionType;
        }

        @Override
        public boolean isUnionType() {
            return isUnionType;
        }

        private List<GeneratedProperty> toUnmodifiableProperties(final List<GeneratedPropertyBuilder> propBuilders) {
            final List<GeneratedProperty> properties = new ArrayList<>();
            for (final GeneratedPropertyBuilder builder : propBuilders) {
                properties.add(builder.toInstance(this));
            }
            return Collections.unmodifiableList(properties);
        }

        @Override
        public GeneratedTransferObject getExtends() {
            return extendsType;
        }

        @Override
        public List<GeneratedProperty> getProperties() {
            return properties;
        }

        @Override
        public List<GeneratedProperty> getEqualsIdentifiers() {
            return equalsProperties;
        }

        @Override
        public List<GeneratedProperty> getHashCodeIdentifiers() {
            return hashCodeProperties;
        }

        @Override
        public List<GeneratedProperty> getToStringIdentifiers() {
            return stringProperties;
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
            builder.append(", extends=");
            builder.append(getExtends());
            builder.append(", implements=");
            builder.append(getImplements());
            builder.append(", enclosedTypes=");
            builder.append(getEnclosedTypes());
            builder.append(", constants=");
            builder.append(getConstantDefinitions());
            builder.append(", enumerations=");
            builder.append(getEnumerations());
            builder.append(", properties=");
            builder.append(properties);
            builder.append(", equalsProperties=");
            builder.append(equalsProperties);
            builder.append(", hashCodeProperties=");
            builder.append(hashCodeProperties);
            builder.append(", stringProperties=");
            builder.append(stringProperties);
            builder.append(", methods=");
            builder.append(getMethodDefinitions());
            builder.append("]");
            return builder.toString();
        }
    }
}
