/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.mdsal.dstracer.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

/**
 * Aggregator of org.opendaylight.controller.mdsal.dstracer.spi.DataStorePersister instances to which data store
 * change events are passed.
 */
public interface DataStorePersisterController {

    /**
     * Trigger tracing into configured data store persisters.
     *
     * @see org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker#registerDataChangeListener
     *
     * @param storeType selected store type
     * @param path path in XML tree on which to listen
     * @param triggeringScope scope filtering depth of events
     * @return registration controlling end of tracing
     * @throws java.lang.IllegalStateException if underlying data store service is not available
     */
    Registration startTracing(LogicalDatastoreType storeType, InstanceIdentifier path, DataChangeScope triggeringScope);
}
