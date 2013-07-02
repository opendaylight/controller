/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util.generated.type.builder;

import org.opendaylight.controller.binding.generator.util.AbstractBaseType;
import org.opendaylight.controller.sal.binding.model.api.AccessModifier;
import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.*;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractGeneratedTypeBuilder extends AbstractBaseType implements GeneratedTypeBuilder {

    private final String packageName;
    private String comment = "";
    private final String name;

    private final List<AnnotationTypeBuilder> annotationBuilders = new ArrayList<>();
    private final List<Type> implementsTypes = new ArrayList<>();
    private final List<EnumBuilder> enumDefinitions = new ArrayList<>();
    private final List<Constant> constants = new ArrayList<>();
    private final List<MethodSignatureBuilder> methodDefinitions = new ArrayList<>();
    private final List<GeneratedTypeBuilder> enclosedTypes = new ArrayList<>();
    private final List<GeneratedTOBuilder> enclosingTransferObjects = new ArrayList<>();
    private boolean isAbstract;

    public AbstractGeneratedTypeBuilder(final String packageName, final String name) {
        super(packageName, name);
        if (packageName == null) {
            throw new IllegalArgumentException("Package Name for Generated Type cannot be null!");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name of Generated Type cannot be null!");
        }
        this.packageName = packageName;
        this.name = name;
    }

    protected String getComment() {
        return comment;
    }

    protected List<AnnotationTypeBuilder> getAnnotations() {
        return annotationBuilders;
    }

    protected boolean isAbstract() {
        return isAbstract;
    }

    protected List<Type> getImplementsTypes() {
        return implementsTypes;
    }

    protected List<EnumBuilder> getEnumerations() {
        return enumDefinitions;
    }

    protected List<Constant> getConstants() {
        return constants;
    }

    protected List<MethodSignatureBuilder> getMethodDefinitions() {
        return methodDefinitions;
    }

    protected List<GeneratedTypeBuilder> getEnclosedTypes() {
        return enclosedTypes;
    }

    protected List<GeneratedTOBuilder> getEnclosedTransferObjects() {
        return enclosingTransferObjects;
    }

    @Override
    public GeneratedTypeBuilder addEnclosingType(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name for Enclosing Generated Type cannot be null!");
        }
        GeneratedTypeBuilder builder = new GeneratedTOBuilderImpl(getFullyQualifiedName(), name);
        enclosedTypes.add(builder);
        return builder;
    }

    @Override
    public GeneratedTOBuilder addEnclosingTransferObject(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name for Enclosing Generated Transfer Object cannot be null!");
        }
        GeneratedTOBuilder builder = new GeneratedTOBuilderImpl(getFullyQualifiedName(), name);
        enclosingTransferObjects.add(builder);
        return builder;
    }

    @Override
    public void addEnclosingTransferObject(final GeneratedTOBuilder genTOBuilder) {
        if (genTOBuilder == null) {
            throw new IllegalArgumentException("Parameter genTOBuilder cannot be null!");
        }
        enclosingTransferObjects.add(genTOBuilder);
    }

    @Override
    public void addComment(String comment) {
        this.comment = comment;
    }

    @Override
    public AnnotationTypeBuilder addAnnotation(final String packageName, final String name) {
        if (packageName == null) {
            throw new IllegalArgumentException("Package Name for Annotation Type cannot be null!");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name of Annotation Type cannot be null!");
        }

        final AnnotationTypeBuilder builder = new AnnotationTypeBuilderImpl(packageName, name);
        annotationBuilders.add(builder);
        return builder;
    }

    @Override
    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    @Override
    public boolean addImplementsType(Type genType) {
        if (genType == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        return implementsTypes.add(genType);
    }

    @Override
    public Constant addConstant(Type type, String name, Object value) {
        if (type == null) {
            throw new IllegalArgumentException("Returning Type for Constant cannot be null!");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name of constant cannot be null!");
        }

        final Constant constant = new ConstantImpl(this, type, name, value);
        constants.add(constant);
        return constant;
    }

    @Override
    public EnumBuilder addEnumeration(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name of enumeration cannot be null!");
        }
        final EnumBuilder builder = new EnumerationBuilderImpl(getFullyQualifiedName(), name);
        enumDefinitions.add(builder);
        return builder;
    }

    @Override
    public MethodSignatureBuilder addMethod(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name of method cannot be null!");
        }
        final MethodSignatureBuilder builder = new MethodSignatureBuilderImpl(name);
        builder.setAccessModifier(AccessModifier.PUBLIC);
        builder.setAbstract(true);
        methodDefinitions.add(builder);
        return builder;
    }

    @Override
    public boolean containsMethod(String name) {
        for (MethodSignatureBuilder methodDefinition : methodDefinitions) {
            if (name.equals(methodDefinition.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getPackageName() == null) ? 0 : getPackageName().hashCode());
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
        AbstractGeneratedTypeBuilder other = (AbstractGeneratedTypeBuilder) obj;
        if (getName() == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!getName().equals(other.getName())) {
            return false;
        }
        if (getPackageName() == null) {
            if (other.getPackageName() != null) {
                return false;
            }
        } else if (!getPackageName().equals(other.getPackageName())) {
            return false;
        }
        return true;
    }
}
