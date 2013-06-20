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

import org.opendaylight.controller.sal.binding.model.api.AnnotationType;
import org.opendaylight.controller.sal.binding.model.api.type.builder.AnnotationTypeBuilder;

final class AnnotationTypeBuilderImpl implements AnnotationTypeBuilder {
    
    private final String packageName;
    private final String name;
    private final List<AnnotationTypeBuilder> annotationBuilders;
    private final List<AnnotationType.Parameter> parameters;
    
    public AnnotationTypeBuilderImpl(final String packageName, final String name) {
        super();
        this.packageName = packageName;
        this.name = name;
        annotationBuilders = new ArrayList<>();
        parameters = new ArrayList<>();
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
    public String getFullyQualifiedName() {
        return packageName + "." + name;
    }

    @Override
    public AnnotationTypeBuilder addAnnotation(final String packageName, final String name) {
        if (packageName != null && name != null) {
            final AnnotationTypeBuilder builder = new AnnotationTypeBuilderImpl(packageName, name);
            if (annotationBuilders.add(builder)) {
                return builder;
            }
        }
        return null;
    }

    @Override
    public boolean addParameter(String paramName, String value) {
        if ((paramName != null) && (value != null)) {
            return parameters.add(new ParameterImpl(paramName, value));
        }
        return false;
    }

    @Override
    public boolean addParameters(String paramName, List<String> values) {
        if ((paramName != null) && (values != null)) {
            return parameters.add(new ParameterImpl(paramName, values));
        }
        return false;
    }

    @Override
    public AnnotationType toInstance() {
        return new AnnotationTypeImpl(packageName, name, annotationBuilders, parameters);
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
        AnnotationTypeBuilderImpl other = (AnnotationTypeBuilderImpl) obj;
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
        builder.append("AnnotationTypeBuilder [packageName=");
        builder.append(packageName);
        builder.append(", name=");
        builder.append(name);
        builder.append(", annotationBuilders=");
        builder.append(annotationBuilders);
        builder.append(", parameters=");
        builder.append(parameters);
        builder.append("]");
        return builder.toString();
    }
    
    private static final class AnnotationTypeImpl implements AnnotationType {
        
        private final String packageName;
        private final String name;
        private List<AnnotationType> annotations;
        private final List<AnnotationType.Parameter> parameters;
        private List<String> paramNames;
        
        public AnnotationTypeImpl(String packageName, String name,
                List<AnnotationTypeBuilder> annotationBuilders,
                List<AnnotationType.Parameter> parameters) {
            super();
            this.packageName = packageName;
            this.name = name;
            
            this.annotations = new ArrayList<>();
            for (final AnnotationTypeBuilder builder : annotationBuilders) {
                annotations.add(builder.toInstance());
            }
            
            this.annotations = Collections.unmodifiableList(annotations); 
            this.parameters = Collections.unmodifiableList(parameters);
            
            paramNames = new ArrayList<>();
            for (final AnnotationType.Parameter parameter : parameters) {
                paramNames.add(parameter.getName());
            }
            this.paramNames = Collections.unmodifiableList(paramNames);
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
        public String getFullyQualifiedName() {
            return packageName + "." + name;
        }

        @Override
        public List<AnnotationType> getAnnotations() {
            return annotations;
        }

        @Override
        public Parameter getParameter(final String paramName) {
            if (paramName != null) {
                for (final AnnotationType.Parameter parameter : parameters) {
                    if (parameter.getName().equals(paramName)) {
                        return parameter;
                    }
                }
            }
            return null;
        }

        @Override
        public List<Parameter> getParameters() {
            return parameters;
        }

        @Override
        public List<String> getParameterNames() {
            return paramNames;
        }
        
        @Override
        public boolean containsParameters() {
            return !parameters.isEmpty();
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
            AnnotationTypeImpl other = (AnnotationTypeImpl) obj;
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
            builder.append("AnnotationType [packageName=");
            builder.append(packageName);
            builder.append(", name=");
            builder.append(name);
            builder.append(", annotations=");
            builder.append(annotations);
            builder.append(", parameters=");
            builder.append(parameters);
            builder.append("]");
            return builder.toString();
        }
    }
    
    private static final class ParameterImpl implements AnnotationType.Parameter {
        
        private final String name;
        private final String value;
        private final List<String> values;
        
        public ParameterImpl(String name, String value) {
            super();
            this.name = name;
            this.value = value;
            this.values = Collections.emptyList();
        }
        
        public ParameterImpl(String name, List<String> values) {
            super();
            this.name = name;
            this.values = values;
            this.value = null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public List<String> getValues() {
            return values;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
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
            ParameterImpl other = (ParameterImpl) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ParameterImpl [name=");
            builder.append(name);
            builder.append(", value=");
            builder.append(value);
            builder.append(", values=");
            builder.append(values);
            builder.append("]");
            return builder.toString();
        }
    }
}
