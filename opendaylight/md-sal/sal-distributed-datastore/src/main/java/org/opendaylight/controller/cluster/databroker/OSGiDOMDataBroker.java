/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.annotations.Beta;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.spi.ForwardingDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@Component(service = DOMDataBroker.class, property = "type=default")
public final class OSGiDOMDataBroker extends ForwardingDOMDataBroker {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiDOMDataBroker.class);

    private final @NonNull ConcurrentDOMDataBroker delegate;

    @Activate
    public OSGiDOMDataBroker(@Reference final DataBrokerCommitExecutor commitExecutor,
            @Reference(target = "(type=distributed-config)") final DOMStore configDatastore,
            @Reference(target = "(type=distributed-operational)") final DOMStore operDatastore) {
        delegate = new ConcurrentDOMDataBroker(Map.of(
            LogicalDatastoreType.CONFIGURATION, configDatastore, LogicalDatastoreType.OPERATIONAL, operDatastore),
            commitExecutor.executor(), commitExecutor.commitStatsTracker());
        LOG.info("DOM Data Broker started");
    }

    @Override
    protected DOMDataBroker delegate() {
        return delegate;
    }

    @Deactivate
    void deactivate() {
        LOG.info("DOM Data Broker stopping");
        delegate.close();
        LOG.info("DOM Data Broker stopped");
    }
}
