/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.mdsal;

import org.opendaylight.controller.config.yang.messagebus.app.impl.NamespaceToStream;
import org.opendaylight.controller.messagebus.app.impl.EventAggregator;
import org.opendaylight.controller.messagebus.app.impl.EventSourceManager;
import org.opendaylight.controller.messagebus.app.impl.NetconfEventSourceTopology;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.api.Broker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class InitializationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitializationContext.class);

    private final MdSAL mdSal;
    private final DataStore dataStore;
    private final NetconfEventSourceTopology eventSourceTopology;
    private final EventSourceManager eventSourceManager;
    private final EventAggregator eventAggregator;

    public InitializationContext(List<NamespaceToStream> namespaceMapping) {
        this.mdSal = new MdSAL();
        this.dataStore = new DataStore(mdSal);
        this.eventSourceTopology = new NetconfEventSourceTopology(dataStore, mdSal);
        this.eventSourceManager = new EventSourceManager(dataStore, mdSal, eventSourceTopology, namespaceMapping);
        this.eventAggregator = new EventAggregator(mdSal, eventSourceTopology);
    }

    public synchronized void set(BindingAwareBroker.ProviderContext session) {
        mdSal.setBindingAwareContext(session);

        if (mdSal.isReady()) {
            initialize();
        }
    }

    public synchronized void set(Broker.ProviderSession session) {
        mdSal.setBindingIndependentContext(session);

        if (mdSal.isReady()) {
            initialize();
        }
    }

    private void initialize() {
        eventSourceTopology.mdsalReady();
        eventSourceManager.mdsalReady();
        eventAggregator.mdsalReady();

        LOGGER.info("InitializationContext started.");
    }
}
