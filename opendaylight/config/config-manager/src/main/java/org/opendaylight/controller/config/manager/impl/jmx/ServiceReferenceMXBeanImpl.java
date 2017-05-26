/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import javax.management.ObjectName;
import org.opendaylight.controller.config.api.jmx.ServiceReferenceMXBean;

public class ServiceReferenceMXBeanImpl implements ServiceReferenceMXBean {
    private ObjectName currentImplementation;

    public ServiceReferenceMXBeanImpl(ObjectName currentImplementation) {
        this.currentImplementation = currentImplementation;
    }

    @Override
    public ObjectName getCurrentImplementation() {
        return currentImplementation;
    }

    public void setCurrentImplementation(ObjectName currentImplementation) {
        this.currentImplementation = currentImplementation;
    }
}
