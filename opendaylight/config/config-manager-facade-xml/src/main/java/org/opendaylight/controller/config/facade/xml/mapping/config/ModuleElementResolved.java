/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.config;

public class ModuleElementResolved {

    private final String instanceName;
    private final InstanceConfigElementResolved instanceConfigElementResolved;

    public ModuleElementResolved(final String instanceName, final InstanceConfigElementResolved instanceConfigElementResolved) {
        this.instanceName = instanceName;
        this.instanceConfigElementResolved = instanceConfigElementResolved;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public InstanceConfigElementResolved getInstanceConfigElementResolved() {
        return instanceConfigElementResolved;
    }

}
