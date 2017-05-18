/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import java.beans.ConstructorProperties;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;

public final class IdentityAttributeRef {

    public static final String QNAME_ATTR_NAME = "qNameOfIdentity";

    private final String qNameOfIdentity;

    @ConstructorProperties(QNAME_ATTR_NAME)
    public IdentityAttributeRef(final String qNameOfIdentity) {
        if (qNameOfIdentity == null) {
            throw new NullPointerException("Parameter " + QNAME_ATTR_NAME + " is null");
        }
        this.qNameOfIdentity = qNameOfIdentity;
    }

    public String getqNameOfIdentity() {
        return qNameOfIdentity;
    }

    public <T extends BaseIdentity> Class<? extends T> resolveIdentity(final DependencyResolver resolver, final Class<T> baseIdentity) {
        return resolver.resolveIdentity(this, baseIdentity);
    }

    public <T extends BaseIdentity> void validateIdentity(final DependencyResolver resolver, final Class<T> baseIdentity, final JmxAttribute jmxAttribute) {
        resolver.validateIdentity(this, baseIdentity, jmxAttribute);
    }

    @Override
    public String toString() {
        return "IdentityAttributeRef{" + "qNameOfIdentity='" + qNameOfIdentity + "'}";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IdentityAttributeRef)) {
            return false;
        }

        IdentityAttributeRef that = (IdentityAttributeRef) o;

        if (!qNameOfIdentity.equals(that.qNameOfIdentity)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return qNameOfIdentity.hashCode();
    }

}
