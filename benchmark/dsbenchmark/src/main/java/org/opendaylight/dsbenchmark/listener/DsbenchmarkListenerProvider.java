/*
 * Copyright (c) 2016 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dsbenchmark.listener;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DsbenchmarkListenerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DsbenchmarkListenerProvider.class);
    private static final InstanceIdentifier<TestExec> TEST_EXEC_IID = InstanceIdentifier.builder(TestExec.class).build();
    private final List<ListenerRegistration<DsbenchmarkListener>> listeners = 
            new ArrayList<ListenerRegistration<DsbenchmarkListener>>();
    private DataBroker dataBroker;

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        LOG.info("DsbenchmarkListenerProvider created");
    }

    public void createAndRegisterListeners(int numListeners) {
        for(int i = 0; i < numListeners; i++) {
            DsbenchmarkListener listener = new DsbenchmarkListener();
            listeners.add(dataBroker.registerDataTreeChangeListener(
                    new DataTreeIdentifier<TestExec>(LogicalDatastoreType.CONFIGURATION, TEST_EXEC_IID), listener));
            listeners.add(dataBroker.registerDataTreeChangeListener(
                    new DataTreeIdentifier<TestExec>(LogicalDatastoreType.OPERATIONAL, TEST_EXEC_IID), listener));

        }
        LOG.info("DsbenchmarkListenerProvider created {} listeneres", numListeners);
    }

    public long getEventCountAndDestroyListeners() {
        long totalEvents = 0;

        for (ListenerRegistration<DsbenchmarkListener> listenerRegistration : listeners) {
            totalEvents = totalEvents + listenerRegistration.getInstance().getNumEvents();
            listenerRegistration.close();
        }

        listeners.clear();
        totalEvents = totalEvents / 2;  // Each listener is registered 2x: adjust for event double-counting
        LOG.info("DsbenchmarkListenerProvider destroyed listeneres, total events {}", totalEvents);
        return totalEvents;
    }
}
