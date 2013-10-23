/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import org.opendaylight.yangtools.concepts.Identifier;

public class ModuleIdentifier implements Identifier {
    private static final long serialVersionUID = 1L;
    private final String factoryName, instanceName;

    public ModuleIdentifier(String factoryName, String instanceName) {
        if (factoryName == null)
            throw new IllegalArgumentException(
                    "Parameter 'factoryName' is null");
        if (instanceName == null)
            throw new IllegalArgumentException(
                    "Parameter 'instanceName' is null");
        this.factoryName = factoryName;
        this.instanceName = instanceName;
    }

    public String getFactoryName() {
        return factoryName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ModuleIdentifier that = (ModuleIdentifier) o;

        if (!factoryName.equals(that.factoryName))
            return false;
        if (!instanceName.equals(that.instanceName))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = factoryName.hashCode();
        result = 31 * result + instanceName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ModuleIdentifier{" + "factoryName='" + factoryName + '\''
                + ", instanceName='" + instanceName + '\'' + '}';
    }
}
