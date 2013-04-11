/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.generator.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.sal.binding.model.api.AccessModifier;
import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.ConstantBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.EnumBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedPropertyBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTOBuilder;

final class GeneratedTOBuilderImpl implements GeneratedTOBuilder {
    
    private static final String[] SET_VALUES = new String[] { "abstract",
        "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "double", "do", "else",
        "enum", "extends", "false", "final", "finally", "float", "for",
        "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "null", "package", "private",
        "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws",
        "transient", "true", "try", "void", "volatile", "while" };

    public static final Set<String> JAVA_RESERVED_WORDS = new HashSet<String>(
            Arrays.asList(SET_VALUES));
    
    private String packageName;
    private final String name;

    private final List<EnumBuilder> enumerations = new ArrayList<EnumBuilder>();
    private final List<GeneratedPropertyBuilder> properties = new ArrayList<GeneratedPropertyBuilder>();
    private final List<GeneratedPropertyBuilder> equalsProperties = new ArrayList<GeneratedPropertyBuilder>();
    private final List<GeneratedPropertyBuilder> hashProperties = new ArrayList<GeneratedPropertyBuilder>();
    private final List<GeneratedPropertyBuilder> toStringProperties = new ArrayList<GeneratedPropertyBuilder>();

    public GeneratedTOBuilderImpl(String packageName, String name) {
        super();
        this.packageName = GeneratedTypeBuilderImpl.validatePackage(packageName);
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
    public EnumBuilder addEnumeration(String name) {
        final EnumBuilder builder = new EnumerationBuilderImpl(packageName,
                name);
        enumerations.add(builder);
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
       
        return new GeneratedTransferObjectImpl(packageName, name, enumerations,
                properties, equalsProperties, hashProperties,
                toStringProperties);
    }

    private static final class GeneratedPropertyBuilderImpl implements
            GeneratedPropertyBuilder {

        private final String name;
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
            return new GeneratedPropertyImpl(name, comment, definingType,
                    returnType, isFinal, isReadOnly, parameters, accessModifier);
        }
    }

    private static final class GeneratedPropertyImpl implements
            GeneratedProperty {

        private final String name;
        private final String comment;
        private final Type parent;
        private final Type returnType;
        private final boolean isFinal;
        private final boolean isReadOnly;
        private final List<MethodSignature.Parameter> parameters;
        private final AccessModifier modifier;

        public GeneratedPropertyImpl(final String name, final String comment,
                final Type parent, final Type returnType,
                final boolean isFinal, final boolean isReadOnly,
                final List<Parameter> parameters, final AccessModifier modifier) {
            super();
            this.name = name;
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
            result = prime * result
                    + ((comment == null) ? 0 : comment.hashCode());
            result = prime * result + (isFinal ? 1231 : 1237);
            result = prime * result + (isReadOnly ? 1231 : 1237);
            result = prime * result
                    + ((modifier == null) ? 0 : modifier.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result
                    + ((parameters == null) ? 0 : parameters.hashCode());

            if (parent != null) {
                result = prime
                        * result
                        + ((parent.getPackageName() == null) ? 0 : parent
                                .getPackageName().hashCode());
                result = prime
                        * result
                        + ((parent.getName() == null) ? 0 : parent.getName()
                                .hashCode());
            }

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
            if (comment == null) {
                if (other.comment != null) {
                    return false;
                }
            } else if (!comment.equals(other.comment)) {
                return false;
            }
            if (isFinal != other.isFinal) {
                return false;
            }
            if (isReadOnly != other.isReadOnly) {
                return false;
            }
            if (modifier != other.modifier) {
                return false;
            }
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
            if (parent == null) {
                if (other.parent != null) {
                    return false;
                }
            } else if ((parent != null) && (other.parent != null)) {
                if (!parent.getPackageName().equals(
                        other.parent.getPackageName())
                        && !parent.getName().equals(other.parent.getName())) {
                    return false;
                }
            }
            if (returnType == null) {
                if (other.returnType != null) {
                    return false;
                }
            } else if (!returnType.equals(other.returnType)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("GeneratedPropertyImpl [name=");
            builder.append(name);
            builder.append(", comment=");
            builder.append(comment);
            if (parent != null) {
                builder.append(", parent=");
                builder.append(parent.getPackageName());
                builder.append(".");
                builder.append(parent.getName());
            } else {
                builder.append(", parent= null");
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
        // private final List<Constant> constants;
        private final List<Enumeration> enumerations;
        private final List<GeneratedProperty> properties;
        private final List<GeneratedProperty> equalsProperties;
        private final List<GeneratedProperty> hashCodeProperties;
        private final List<GeneratedProperty> stringProperties;

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

        private List<GeneratedProperty> toUnmodifiableProperties(
                final List<GeneratedPropertyBuilder> propBuilders) {
            final List<GeneratedProperty> constants = new ArrayList<GeneratedProperty>();
            for (final GeneratedPropertyBuilder builder : propBuilders) {
                constants.add(builder.toInstance(this));
            }
            return Collections.unmodifiableList(constants);
        }

        public GeneratedTransferObjectImpl(String packageName, String name,
                List<EnumBuilder> enumBuilders,
                List<GeneratedPropertyBuilder> propBuilers,
                List<GeneratedPropertyBuilder> equalsBuilers,
                List<GeneratedPropertyBuilder> hashCodeBuilers,
                List<GeneratedPropertyBuilder> stringBuilers) {
            super();
            this.packageName = packageName;
            this.name = name;
            this.enumerations = toUnmodifiableEnumerations(enumBuilders);
            this.properties = toUnmodifiableProperties(propBuilers);
            this.equalsProperties = toUnmodifiableProperties(equalsBuilers);
            this.hashCodeProperties = toUnmodifiableProperties(hashCodeBuilers);
            this.stringProperties = toUnmodifiableProperties(stringBuilers);
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
        public List<Enumeration> getEnumDefintions() {
            return enumerations;
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

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((enumerations == null) ? 0 : enumerations.hashCode());
            result = prime
                    * result
                    + ((equalsProperties == null) ? 0 : equalsProperties
                            .hashCode());
            result = prime
                    * result
                    + ((hashCodeProperties == null) ? 0 : hashCodeProperties
                            .hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result
                    + ((packageName == null) ? 0 : packageName.hashCode());
            result = prime * result
                    + ((properties == null) ? 0 : properties.hashCode());
            result = prime
                    * result
                    + ((stringProperties == null) ? 0 : stringProperties
                            .hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
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
            if (enumerations == null) {
                if (other.enumerations != null) {
                    return false;
                }
            } else if (!enumerations.equals(other.enumerations)) {
                return false;
            }
            if (equalsProperties == null) {
                if (other.equalsProperties != null) {
                    return false;
                }
            } else if (!equalsProperties.equals(other.equalsProperties)) {
                return false;
            }
            if (hashCodeProperties == null) {
                if (other.hashCodeProperties != null) {
                    return false;
                }
            } else if (!hashCodeProperties.equals(other.hashCodeProperties)) {
                return false;
            }
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
            if (properties == null) {
                if (other.properties != null) {
                    return false;
                }
            } else if (!properties.equals(other.properties)) {
                return false;
            }
            if (stringProperties == null) {
                if (other.stringProperties != null) {
                    return false;
                }
            } else if (!stringProperties.equals(other.stringProperties)) {
                return false;
            }
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("GeneratedTransferObjectImpl [packageName=");
            builder.append(packageName);
            builder.append(", name=");
            builder.append(name);
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
            builder.append("]");
            return builder.toString();
        }
    }
}
