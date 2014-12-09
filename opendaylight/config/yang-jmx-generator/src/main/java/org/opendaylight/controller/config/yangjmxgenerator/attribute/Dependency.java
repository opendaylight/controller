/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;

public class Dependency {
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Dependency that = (Dependency) o;

        if (mandatory != that.mandatory) {
            return false;
        }
        if (!sie.equals(that.sie)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = sie.hashCode();
        result = 31 * result + (mandatory ? 1 : 0);
        return result;
    }
}
