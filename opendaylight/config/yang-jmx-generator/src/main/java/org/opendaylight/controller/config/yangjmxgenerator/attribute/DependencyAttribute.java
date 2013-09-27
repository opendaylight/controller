/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import javax.management.ObjectName;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.yangtools.binding.generator.util.Types;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class DependencyAttribute extends AbstractAttribute implements
        TypedAttribute {

    private final Dependency dependency;
    private final String nullableDescription, nullableDefault;

    public DependencyAttribute(DataSchemaNode attrNode,
            ServiceInterfaceEntry sie, boolean mandatory,
            String nullableDescription) {
        super(attrNode);
        dependency = new Dependency(sie, mandatory);
        this.nullableDescription = nullableDescription;
        nullableDefault = null;
    }

    @Override
    public Type getType() {
        return Types.typeForClass(ObjectName.class);
    }

    public Dependency getDependency() {
        return dependency;
    }

    @Override
    public String getNullableDescription() {
        return nullableDescription;
    }

    @Override
    public String getNullableDefault() {
        return nullableDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        DependencyAttribute that = (DependencyAttribute) o;

        if (dependency != null ? !dependency.equals(that.dependency)
                : that.dependency != null)
            return false;
        if (nullableDefault != null ? !nullableDefault
                .equals(that.nullableDefault) : that.nullableDefault != null)
            return false;
        if (nullableDescription != null ? !nullableDescription
                .equals(that.nullableDescription)
                : that.nullableDescription != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (dependency != null ? dependency.hashCode() : 0);
        result = 31
                * result
                + (nullableDescription != null ? nullableDescription.hashCode()
                        : 0);
        result = 31 * result
                + (nullableDefault != null ? nullableDefault.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DependencyAttribute{" + getAttributeYangName() + ","
                + "dependency=" + dependency + '}';
    }

    @Override
    public OpenType<?> getOpenType() {
        return SimpleType.OBJECTNAME;
    }

    public static class Dependency {
        private final ServiceInterfaceEntry sie;
        private final boolean mandatory;

        public Dependency(ServiceInterfaceEntry sie, boolean mandatory) {
            this.sie = sie;
            this.mandatory = mandatory;
        }

        public ServiceInterfaceEntry getSie() {
            return sie;
        }

        public boolean isMandatory() {
            return mandatory;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            Dependency that = (Dependency) o;

            if (mandatory != that.mandatory)
                return false;
            if (!sie.equals(that.sie))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = sie.hashCode();
            result = 31 * result + (mandatory ? 1 : 0);
            return result;
        }
    }

}
