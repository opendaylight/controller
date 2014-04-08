/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

public class ServiceReference {
    private final String serviceInterfaceName, refName;

    public ServiceReference(String serviceInterfaceName, String refName) {
        this.serviceInterfaceName = serviceInterfaceName;
        this.refName = refName;
    }

    public String getServiceInterfaceQName() {
        return serviceInterfaceName;
    }

    public String getRefName() {
        return refName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServiceReference that = (ServiceReference) o;

        if (!refName.equals(that.refName)) {
            return false;
        }
        if (!serviceInterfaceName.equals(that.serviceInterfaceName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = serviceInterfaceName.hashCode();
        result = 31 * result + refName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ServiceReference{" +
                "serviceInterfaceName='" + serviceInterfaceName + '\'' +
                ", refName='" + refName + '\'' +
                '}';
    }
}
