/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.generator.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.EnumBuilder;

final class EnumerationBuilderImpl implements EnumBuilder {
    private final String packageName;
    private final String name;
    private final List<Enumeration.Pair> values;

    public EnumerationBuilderImpl(final String packageName, final String name) {
        super();
        this.packageName = GeneratedTypeBuilderImpl.validatePackage(packageName);
        this.name = name;
        values = new ArrayList<Enumeration.Pair>();
    }

    @Override
    public void addValue(final String name, final Integer value) {
        values.add(new EnumPairImpl(name, value));
    }

    @Override
    public Enumeration toInstance(final Type definingType) {
        return new EnumerationImpl(definingType, packageName, name, values);
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
        result = prime * result
                + ((packageName == null) ? 0 : packageName.hashCode());
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
        EnumerationBuilderImpl other = (EnumerationBuilderImpl) obj;
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EnumerationBuilderImpl [packageName=");
        builder.append(packageName);
        builder.append(", name=");
        builder.append(name);
        builder.append(", values=");
        builder.append(values);
        builder.append("]");
        return builder.toString();
    }

    private static final class EnumPairImpl implements Enumeration.Pair {

        private final String name;
        private final Integer value;

        public EnumPairImpl(String name, Integer value) {
            super();
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Integer getValue() {
            return value;
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
            result = prime * result + ((value == null) ? 0 : value.hashCode());
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
            EnumPairImpl other = (EnumPairImpl) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
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
            builder.append("EnumPairImpl [name=");
            builder.append(name);
            builder.append(", value=");
            builder.append(value);
            builder.append("]");
            return builder.toString();
        }
    }

    private static final class EnumerationImpl implements Enumeration {

        private final Type definingType;
        private final String packageName;
        private final String name;
        private final List<Pair> values;

        public EnumerationImpl(final Type definingType,
                final String packageName, final String name,
                final List<Pair> values) {
            super();
            this.definingType = definingType;
            this.packageName = packageName;
            this.name = name;
            this.values = Collections.unmodifiableList(values);
        }

        @Override
        public Type getDefiningType() {
            return definingType;
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
        public List<Pair> getValues() {
            return values;
        }

        @Override
        public String toFormattedString() {
            StringBuilder builder = new StringBuilder();
            builder.append("public enum");
            builder.append(" ");
            builder.append(name);
            builder.append(" {");
            builder.append("\n");

            int i = 0;
            for (final Enumeration.Pair valPair : values) {
                builder.append("\t");
                builder.append(" ");
                builder.append(valPair.getName());
                builder.append(" (");
                builder.append(valPair.getValue());

                if (i == (values.size() - 1)) {
                    builder.append(" );");
                } else {
                    builder.append(" ),");
                }
                ++i;
            }
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
            result = prime * result
                    + ((packageName == null) ? 0 : packageName.hashCode());
            result = prime * result
                    + ((values == null) ? 0 : values.hashCode());

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
            EnumerationImpl other = (EnumerationImpl) obj;
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
            if (values == null) {
                if (other.values != null) {
                    return false;
                }
            } else if (!values.equals(other.values)) {
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

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("EnumerationImpl [packageName=");
            builder.append(packageName);
            if (definingType != null) {
                builder.append(", definingType=");
                builder.append(definingType.getPackageName());
                builder.append(".");
                builder.append(definingType.getName());
            } else {
                builder.append(", definingType= null");
            }
            builder.append(", name=");
            builder.append(name);
            builder.append(", values=");
            builder.append(values);
            builder.append("]");
            return builder.toString();
        }
    }
}
