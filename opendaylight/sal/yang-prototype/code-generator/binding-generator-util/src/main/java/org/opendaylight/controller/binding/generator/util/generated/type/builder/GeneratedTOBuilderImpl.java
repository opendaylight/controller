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

import org.opendaylight.controller.sal.binding.model.api.AccessModifier;
import org.opendaylight.controller.sal.binding.model.api.AnnotationType;
import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.AnnotationTypeBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.ConstantBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.EnumBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedPropertyBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTOBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.MethodSignatureBuilder;

public final class GeneratedTOBuilderImpl implements GeneratedTOBuilder {
    private String packageName;
    private final String name;
    private String comment = "";
    
    private GeneratedTransferObject extendsType;
    private final List<GeneratedType> implementsTypes = new ArrayList<GeneratedType>();
    private final List<EnumBuilder> enumerations = new ArrayList<EnumBuilder>();
    private final List<GeneratedPropertyBuilder> properties = new ArrayList<GeneratedPropertyBuilder>();
    private final List<GeneratedPropertyBuilder> equalsProperties = new ArrayList<GeneratedPropertyBuilder>();
    private final List<GeneratedPropertyBuilder> hashProperties = new ArrayList<GeneratedPropertyBuilder>();
    private final List<GeneratedPropertyBuilder> toStringProperties = new ArrayList<GeneratedPropertyBuilder>();

    private final List<ConstantBuilder> constantDefintions = new ArrayList<ConstantBuilder>();
    private final List<MethodSignatureBuilder> methodDefinitions = new ArrayList<MethodSignatureBuilder>();
    private final List<AnnotationTypeBuilder> annotationBuilders = new ArrayList<AnnotationTypeBuilder>();

    public GeneratedTOBuilderImpl(String packageName, String name) {
        super();
        this.packageName = packageName;
        this.name = name;
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addComment(final String comment) {
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
    public boolean addImplementsType(final GeneratedType genType) {
        if (genType != null) {
            return implementsTypes.add(genType);
        }
        return false;
    }

    @Override
    public boolean addExtendsType(final GeneratedTransferObject genTransObj) {
        if (genTransObj != null) {
            extendsType = genTransObj;
            return true;
        }
        return false;
    }
    
    @Override
    public EnumBuilder addEnumeration(String name) {
        final EnumBuilder builder = new EnumerationBuilderImpl(packageName,
                name);
        enumerations.add(builder);
        return builder;
    }

    @Override
    public ConstantBuilder addConstant(Type type, String name, Object value) {
        final ConstantBuilder builder = new ConstantBuilderImpl(type, name,
                value);
        constantDefintions.add(builder);
        return builder;
    }

    @Override
    public MethodSignatureBuilder addMethod(String name) {
        final MethodSignatureBuilder builder = new MethodSignatureBuilderImpl(
                this, name);
        methodDefinitions.add(builder);
        return builder;
    }

    @Override
    public GeneratedPropertyBuilder addProperty(String name) {
        final GeneratedPropertyBuilder builder = new GeneratedPropertyBuilderImpl(
                name);
        properties.add(builder);
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
        return new GeneratedTransferObjectImpl(packageName, name, comment, 
                annotationBuilders, extendsType, implementsTypes, constantDefintions, enumerations,
                methodDefinitions, properties, equalsProperties,
                hashProperties, toStringProperties);
    }

    private static final class GeneratedPropertyBuilderImpl implements
            GeneratedPropertyBuilder {

        private final String name;
        private final List<AnnotationTypeBuilder> annotationBuilders = new ArrayList<AnnotationTypeBuilder>();
        private Type returnType;
        private final List<MethodSignature.Parameter> parameters;
        private String comment = "";
        private AccessModifier accessModifier;
        private boolean isFinal;
        private boolean isReadOnly;

        public GeneratedPropertyBuilderImpl(final String name) {
            super();
            this.name = name;
            parameters = new ArrayList<MethodSignature.Parameter>();
            isFinal = true;
            this.isReadOnly = true;
            accessModifier = AccessModifier.PUBLIC;
        }

        public String getName() {
            return name;
        }

        @Override
        public AnnotationTypeBuilder addAnnotation(String packageName,
                String name) {
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
        public boolean addReturnType(Type returnType) {
            if (returnType != null) {
                this.returnType = returnType;
                this.parameters.add(new MethodParameterImpl(name, returnType));
                return true;
            }
            return false;
        }

        @Override
        public void accessorModifier(final AccessModifier modifier) {
            accessModifier = modifier;
        }

        @Override
        public void addComment(String comment) {
            if (comment != null) {
                this.comment = comment;
            }
        }

        @Override
        public void setFinal(boolean isFinal) {
            this.isFinal = isFinal;
        }

        @Override
        public void setReadOnly(boolean isReadOnly) {
            this.isReadOnly = isReadOnly;
        }

        @Override
        public GeneratedProperty toInstance(final Type definingType) {
            return new GeneratedPropertyImpl(name, comment, annotationBuilders, definingType,
                    returnType, isFinal, isReadOnly, parameters, accessModifier);
        }
    }

    private static final class GeneratedPropertyImpl implements
            GeneratedProperty {

        private final String name;
        private List<AnnotationType> annotations;
        private final String comment;
        private final Type parent;
        private final Type returnType;
        private final boolean isFinal;
        private final boolean isReadOnly;
        private final List<MethodSignature.Parameter> parameters;
        private final AccessModifier modifier;
        
        public GeneratedPropertyImpl(final String name, final String comment,
                final List<AnnotationTypeBuilder> annotationBuilders, final Type parent, final Type returnType,
                final boolean isFinal, final boolean isReadOnly,
                final List<Parameter> parameters, final AccessModifier modifier) {
            super();
            this.name = name;
            this.annotations = new ArrayList<AnnotationType>();
            for (final AnnotationTypeBuilder builder : annotationBuilders) {
                this.annotations.add(builder.toInstance());
            }
            this.annotations = Collections.unmodifiableList(this.annotations);
            this.comment = comment;
            this.parent = parent;
            this.returnType = returnType;
            this.isFinal = isFinal;
            this.isReadOnly = isReadOnly;
            this.parameters = Collections.unmodifiableList(parameters);
            this.modifier = modifier;
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
            return parent;
        }

        @Override
        public List<AnnotationType> getAnnotations() {
            return annotations;
        }
        
        @Override
        public Type getReturnType() {
            return returnType;
        }

        @Override
        public List<Parameter> getParameters() {
            return parameters;
        }

        @Override
        public AccessModifier getAccessModifier() {
            return modifier;
        }

        @Override
        public boolean isReadOnly() {
            return isReadOnly;
        }

        @Override
        public boolean isFinal() {
            return isFinal;
        }

        

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result
                    + ((parameters == null) ? 0 : parameters.hashCode());
            result = prime * result
                    + ((returnType == null) ? 0 : returnType.hashCode());
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
            GeneratedPropertyImpl other = (GeneratedPropertyImpl) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (parameters == null) {
                if (other.parameters != null) {
                    return false;
                }
            } else if (!parameters.equals(other.parameters)) {
                return false;
            }
            if (returnType == null) {
                if (other.returnType != null) {
                    return false;
                }
            } else if (!returnType.getPackageName().equals(other.returnType.getPackageName())) {
                return false;
            } else if (!returnType.getName().equals(other.returnType.getName())) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("GeneratedPropertyImpl [name=");
            builder.append(name);
            builder.append(", annotations=");
            builder.append(annotations);
            builder.append(", comment=");
            builder.append(comment);
            if (parent != null) {
                builder.append(", parent=");
                builder.append(parent.getPackageName());
                builder.append(".");
                builder.append(parent.getName());
            } else {
                builder.append(", parent=null");
            }
            builder.append(", returnType=");
            builder.append(returnType);
            builder.append(", isFinal=");
            builder.append(isFinal);
            builder.append(", isReadOnly=");
            builder.append(isReadOnly);
            builder.append(", parameters=");
            builder.append(parameters);
            builder.append(", modifier=");
            builder.append(modifier);
            builder.append("]");
            return builder.toString();
        }
    }

    private static final class GeneratedTransferObjectImpl implements
            GeneratedTransferObject {

        private final String packageName;
        private final String name;
        private final String comment;
        private final List<Constant> constants;
        private final List<Enumeration> enumerations;
        private final List<GeneratedProperty> properties;
        private final List<GeneratedProperty> equalsProperties;
        private final List<GeneratedProperty> hashCodeProperties;
        private final List<GeneratedProperty> stringProperties;
        private final List<AnnotationType> annotations;
        private final List<MethodSignature> methods;
        private final GeneratedTransferObject extendsType;
        private final List<GeneratedType> implementsTypes;

        public GeneratedTransferObjectImpl(final String packageName,
                final String name,
                final String comment,
                final List<AnnotationTypeBuilder> annotationBuilders,
                final GeneratedTransferObject extendsType,
                final List<GeneratedType> implementsTypes,
                final List<ConstantBuilder> constantBuilders,
                final List<EnumBuilder> enumBuilders,
                final List<MethodSignatureBuilder> methodBuilders,
                final List<GeneratedPropertyBuilder> propBuilers,
                final List<GeneratedPropertyBuilder> equalsBuilers,
                final List<GeneratedPropertyBuilder> hashCodeBuilers,
                final List<GeneratedPropertyBuilder> stringBuilers) {
            super();
            this.packageName = packageName;
            this.name = name;
            this.comment = comment;
            this.annotations = toUnmodifiableAnnotations(annotationBuilders);
            this.extendsType = extendsType;
            this.implementsTypes = Collections.unmodifiableList(implementsTypes);
            this.constants = toUnmodifiableConstant(constantBuilders);
            this.enumerations = toUnmodifiableEnumerations(enumBuilders);
            this.properties = toUnmodifiableProperties(propBuilers);
            this.methods = toUnmodifiableMethods(methodBuilders);
            this.equalsProperties = toUnmodifiableProperties(equalsBuilers);
            this.hashCodeProperties = toUnmodifiableProperties(hashCodeBuilers);
            this.stringProperties = toUnmodifiableProperties(stringBuilers);
        }

        private List<AnnotationType> toUnmodifiableAnnotations(
                final List<AnnotationTypeBuilder> annotationBuilders) {
            final List<AnnotationType> annotations = new ArrayList<AnnotationType>();
            for (final AnnotationTypeBuilder builder : annotationBuilders) {
                annotations.add(builder.toInstance());
            }
            return Collections.unmodifiableList(annotations);
        }

        private List<Enumeration> toUnmodifiableEnumerations(
                final List<EnumBuilder> enumBuilders) {
            final List<Enumeration> enumerations = new ArrayList<Enumeration>();
            for (final EnumBuilder builder : enumBuilders) {
                enumerations.add(builder.toInstance(this));
            }
            return Collections.unmodifiableList(enumerations);
        }

        private List<Constant> toUnmodifiableConstant(
                final List<ConstantBuilder> constBuilders) {
            final List<Constant> constants = new ArrayList<Constant>();
            for (final ConstantBuilder builder : constBuilders) {
                constants.add(builder.toInstance(this));
            }
            return Collections.unmodifiableList(constants);
        }

        private List<MethodSignature> toUnmodifiableMethods(
                final List<MethodSignatureBuilder> methodBuilders) {
            final List<MethodSignature> methods = new ArrayList<MethodSignature>();
            for (final MethodSignatureBuilder builder : methodBuilders) {
                methods.add(builder.toInstance(this));
            }
            return Collections.unmodifiableList(methods);
        }

        private List<GeneratedProperty> toUnmodifiableProperties(
                final List<GeneratedPropertyBuilder> propBuilders) {
            final List<GeneratedProperty> constants = new ArrayList<GeneratedProperty>();
            for (final GeneratedPropertyBuilder builder : propBuilders) {
                constants.add(builder.toInstance(this));
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
            return null;
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
        public List<GeneratedType> getImplements() {
            return implementsTypes;
        }

        @Override
        public GeneratedTransferObject getExtends() {
            return extendsType;
        }
        
        @Override
        public List<Enumeration> getEnumDefintions() {
            return enumerations;
        }

        @Override
        public List<Constant> getConstantDefinitions() {
            return constants;
        }

        @Override
        public List<MethodSignature> getMethodDefinitions() {
            return methods;
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
        public int hashCode() {
            final int prime = 31;
            int result = 1;
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
            GeneratedTransferObjectImpl other = (GeneratedTransferObjectImpl) obj;
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
            builder.append("GeneratedTransferObject [packageName=");
            builder.append(packageName);
            builder.append(", name=");
            builder.append(name);
            builder.append(", comment=");
            builder.append(comment);
            builder.append(", constants=");
            builder.append(constants);
            builder.append(", enumerations=");
            builder.append(enumerations);
            builder.append(", properties=");
            builder.append(properties);
            builder.append(", equalsProperties=");
            builder.append(equalsProperties);
            builder.append(", hashCodeProperties=");
            builder.append(hashCodeProperties);
            builder.append(", stringProperties=");
            builder.append(stringProperties);
            builder.append(", annotations=");
            builder.append(annotations);
            builder.append(", methods=");
            builder.append(methods);
            builder.append("]");
            return builder.toString();
        }
    }
}
