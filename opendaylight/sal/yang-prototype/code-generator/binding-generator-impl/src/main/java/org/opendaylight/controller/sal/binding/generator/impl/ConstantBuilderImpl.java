/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.generator.impl;

import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.ConstantBuilder;

final class ConstantBuilderImpl implements ConstantBuilder {

    private final Type type;
    private final String name;
    private Object value;

    public ConstantBuilderImpl(Type type, String name, Object value) {
        super();
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public ConstantBuilderImpl(Type type, String name) {
        super();
        this.type = type;
        this.name = name;
    }

    @Override
    public void assignValue(Object value) {
        this.value = value;
    }

    @Override
    public Constant toInstance(final Type definingType) {
        return new ConstantImpl(definingType, type, name, value);
    }

    private static final class ConstantImpl implements Constant {

        final Type definingType;
        private final Type type;
        private final String name;
        private final Object value;

        public ConstantImpl(final Type definingType, final Type type,
                final String name, final Object value) {
            super();
            this.definingType = definingType;
            this.type = type;
            this.name = name;
            this.value = value;
        }

        @Override
        public Type getDefiningType() {
            return definingType;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public String toFormattedString() {
            StringBuilder builder = new StringBuilder();
            builder.append(type);
            builder.append(" ");
            builder.append(name);
            builder.append(" ");
            builder.append(value);
            return builder.toString();
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
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            if (definingType != null) {
                result = prime
                        * result
                        + ((definingType.getPackageName() == null) ? 0
                                : definingType.getPackageName().hashCode());
                result = prime
                        * result
                        + ((definingType.getName() == null) ? 0 : definingType
                                .getName().hashCode());
            }
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
            ConstantImpl other = (ConstantImpl) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (type == null) {
                if (other.type != null) {
                    return false;
                }
            } else if (!type.equals(other.type)) {
                return false;
            }
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            if (definingType == null) {
                if (other.definingType != null) {
                    return false;
                }
            } else if ((definingType != null) && (other.definingType != null)) {
                if (!definingType.getPackageName().equals(
                        other.definingType.getPackageName())
                        && !definingType.getName().equals(
                                other.definingType.getName())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ConstantImpl [type=");
            builder.append(type);
            builder.append(", name=");
            builder.append(name);
            builder.append(", value=");
            builder.append(value);
            if (definingType != null) {
                builder.append(", definingType=");
                builder.append(definingType.getPackageName());
                builder.append(".");
                builder.append(definingType.getName());
            } else {
                builder.append(", definingType= null");
            }
            builder.append("]");
            return builder.toString();
        }
    }
}
