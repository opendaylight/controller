/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import org.opendaylight.yangtools.yang.binding.BaseIdentity;

public final class IdentityAttributeRef {

    private final String qNameOfIdentity;

    public IdentityAttributeRef(String qNameOfIdentity) {
        if (qNameOfIdentity == null)
            throw new NullPointerException("Parameter 'qNameOfIdentity' is null");
        this.qNameOfIdentity = qNameOfIdentity;
    }

    public String getqNameOfIdentity() {
        return qNameOfIdentity;
    }

    <T extends BaseIdentity> Class<? extends T> resolveIdentity(DependencyResolver resolver, Class<T> baseIdentity) {
        return resolver.resolveIdentity(this, baseIdentity);
    }

    <T extends BaseIdentity> void validateIdentity(DependencyResolver resolver, Class<T> baseIdentity) {
        resolver.validateIdentity(this, baseIdentity);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("IdentityAttributeRef{");
        sb.append("qNameOfIdentity='").append(qNameOfIdentity).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityAttributeRef)) return false;

        IdentityAttributeRef that = (IdentityAttributeRef) o;

        if (!qNameOfIdentity.equals(that.qNameOfIdentity)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return qNameOfIdentity.hashCode();
    }

}
