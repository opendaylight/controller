/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory metadata corresponding to the "static-reference" element that obtains an OSGi service and
 * returns the actual instance. This differs from the standard "reference" element that returns a dynamic
 * proxy whose underlying service instance can come and go.
 *
 * @author Thomas Pantelis
 */
class StaticReferenceMetadata extends AbstractDependentComponentFactoryMetadata {
    private static final Logger LOG = LoggerFactory.getLogger(StaticReferenceMetadata.class);

    private final String interfaceName;
    private volatile Object retrievedService;

    StaticReferenceMetadata(String id, String interfaceName) {
        super(id);
        this.interfaceName = interfaceName;
    }

    @Override
    protected void startTracking() {
        retrieveService(interfaceName, interfaceName, service -> {
            retrievedService = service;
            setSatisfied();
        });
    }

    @Override
    public Object create() throws ComponentDefinitionException {
        super.onCreate();

        LOG.debug("{}: create returning service {}", logName(), retrievedService);

        return retrievedService;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StaticReferenceMetadata [interfaceName=").append(interfaceName).append("]");
        return builder.toString();
    }
}
