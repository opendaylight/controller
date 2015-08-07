/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.sal.core.spi.entityownership.EntityOwnershipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The distributed implementation of the EntityOwnershipService.
 *
 * @author Thomas Pantelis
 */
public class DistributedEntityOwnershipService implements EntityOwnershipService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedEntityOwnershipService.class);

    private final DistributedDataStore datastore;

    public DistributedEntityOwnershipService(DistributedDataStore datastore) {
        this.datastore = datastore;
    }

    public void start() {
        LOG.info("DistributedEntityOwnershipService starting");
    }

    @Override
    public void close() throws Exception {
    }
}
