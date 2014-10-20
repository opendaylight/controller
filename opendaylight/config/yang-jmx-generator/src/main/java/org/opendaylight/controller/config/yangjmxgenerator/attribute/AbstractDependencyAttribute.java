/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public abstract class AbstractDependencyAttribute extends AbstractAttribute  implements TypedAttribute  {

    protected final Dependency dependency;
    protected final String nullableDescription, nullableDefault;

    public AbstractDependencyAttribute(DataSchemaNode attrNode,
                                       ServiceInterfaceEntry sie, boolean mandatory,
                                       String nullableDescription) {
        super(attrNode);
        dependency = new Dependency(sie, mandatory);
        this.nullableDescription = nullableDescription;
        nullableDefault = null;
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        AbstractDependencyAttribute that = (AbstractDependencyAttribute) o;

        if (dependency != null ? !dependency.equals(that.dependency)
                : that.dependency != null) {
            return false;
        }
        if (nullableDefault != null ? !nullableDefault
                .equals(that.nullableDefault) : that.nullableDefault != null) {
            return false;
        }
        if (nullableDescription != null ? !nullableDescription
                .equals(that.nullableDescription)
                : that.nullableDescription != null) {
            return false;
        }

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
        return getClass().getName() + "{" + getAttributeYangName() + ","
                + "dependency=" + dependency + '}';
    }


}
