package org.opendaylight.controller.binding.generator.util.generated.type.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.sal.binding.model.api.AccessModifier;
import org.opendaylight.controller.sal.binding.model.api.AnnotationType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.AnnotationTypeBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.MethodSignatureBuilder;

final class MethodSignatureBuilderImpl implements MethodSignatureBuilder {
    private final String name;
    private Type returnType;
    private final List<MethodSignature.Parameter> parameters;
    private final List<AnnotationTypeBuilder> annotationBuilders;
    private String comment = "";
    private final Type parent;

    public MethodSignatureBuilderImpl(final Type parent, final String name) {
        super();
        this.name = name;
        this.parent = parent;
        this.parameters = new ArrayList<MethodSignature.Parameter>();
        this.annotationBuilders = new ArrayList<AnnotationTypeBuilder>();
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
    public void addReturnType(Type returnType) {
        if (returnType != null) {
            this.returnType = returnType;
        }
    }

    @Override
    public void addParameter(Type type, String name) {
        parameters.add(new MethodParameterImpl(name, type));
    }

    @Override
    public void addComment(String comment) {
        this.comment = comment;
    }

    @Override
    public MethodSignature toInstance(Type definingType) {
        return new MethodSignatureImpl(definingType, name, annotationBuilders,
                comment, returnType, parameters);
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
        MethodSignatureBuilderImpl other = (MethodSignatureBuilderImpl) obj;
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
        } else if (!returnType.equals(other.returnType)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MethodSignatureBuilderImpl [name=");
        builder.append(name);
        builder.append(", returnType=");
        builder.append(returnType);
        builder.append(", parameters=");
        builder.append(parameters);
        builder.append(", annotationBuilders=");
        builder.append(annotationBuilders);
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
        builder.append("]");
        return builder.toString();
    }

    private static final class MethodSignatureImpl implements MethodSignature {

        private final String name;
        private final String comment;
        private final Type definingType;
        private final Type returnType;
        private final List<Parameter> params;
        private List<AnnotationType> annotations;

        public MethodSignatureImpl(final Type definingType, final String name,
                final List<AnnotationTypeBuilder> annotationBuilders,
                final String comment, final Type returnType,
                final List<Parameter> params) {
            super();
            this.name = name;
            this.comment = comment;
            this.definingType = definingType;
            this.returnType = returnType;
            this.params = Collections.unmodifiableList(params);
            
            this.annotations = new ArrayList<AnnotationType>();
            for (final AnnotationTypeBuilder builder : annotationBuilders) {
                this.annotations.add(builder.toInstance());
            }
            this.annotations = Collections.unmodifiableList(this.annotations);
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
        public Type getReturnType() {
            return returnType;
        }

        @Override
        public List<Parameter> getParameters() {
            return params;
        }

        @Override
        public AccessModifier getAccessModifier() {
            return AccessModifier.PUBLIC;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result
                    + ((params == null) ? 0 : params.hashCode());
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
            MethodSignatureImpl other = (MethodSignatureImpl) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (params == null) {
                if (other.params != null) {
                    return false;
                }
            } else if (!params.equals(other.params)) {
                return false;
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
            builder.append("MethodSignatureImpl [name=");
            builder.append(name);
            builder.append(", comment=");
            builder.append(comment);
            if (definingType != null) {
                builder.append(", definingType=");
                builder.append(definingType.getPackageName());
                builder.append(".");
                builder.append(definingType.getName());
            } else {
                builder.append(", definingType= null");
            }
            builder.append(", returnType=");
            builder.append(returnType);
            builder.append(", params=");
            builder.append(params);
            builder.append(", annotations=");
            builder.append(annotations);
            builder.append("]");
            return builder.toString();
        }
    }
}