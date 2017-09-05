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
    private final String factoryName;
    private final String instanceName;

    public ModuleIdentifier(final String factoryName, final String instanceName) {
        if (factoryName == null) {
            throw new IllegalArgumentException("Parameter 'factoryName' is null");
        }
        if (instanceName == null) {
            throw new IllegalArgumentException("Parameter 'instanceName' is null");
        }
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
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        ModuleIdentifier that = (ModuleIdentifier) object;

        if (!factoryName.equals(that.factoryName)) {
            return false;
        }
        if (!instanceName.equals(that.instanceName)) {
            return false;
        }

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
