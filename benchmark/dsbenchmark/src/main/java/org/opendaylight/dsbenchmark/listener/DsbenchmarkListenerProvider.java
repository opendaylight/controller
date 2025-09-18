/*
 * Copyright (c) 2016 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dsbenchmark.listener;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DsbenchmarkListenerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DsbenchmarkListenerProvider.class);
    private static final DataObjectIdentifier<TestExec> TEST_EXEC_IID =
        DataObjectIdentifier.builder(TestExec.class).build();
    private final List<DsbenchmarkListener> listeners = new ArrayList<>();
    private final List<Registration> registrations = new ArrayList<>();
    private final DataBroker dataBroker;

    public DsbenchmarkListenerProvider(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
        LOG.debug("DsbenchmarkListenerProvider created");
    }

    public void createAndRegisterListeners(final int numListeners) {
        for (int i = 0; i < numListeners; i++) {
            var listener = new DsbenchmarkListener();
            listeners.add(listener);
            registrations.add(dataBroker.registerTreeChangeListener(
                    LogicalDatastoreType.CONFIGURATION, TEST_EXEC_IID, listener));
            registrations.add(dataBroker.registerTreeChangeListener(
                    LogicalDatastoreType.OPERATIONAL, TEST_EXEC_IID, listener));

        }
        LOG.debug("DsbenchmarkListenerProvider created {} listeneres", numListeners);
    }

    public long getDataChangeCount() {
        long dataChanges = 0;

        for (var listener : listeners) {
            dataChanges += listener.getNumDataChanges();
        }
        LOG.debug("DsbenchmarkListenerProvider , total data changes {}", dataChanges);
        return dataChanges;
    }

    public long getEventCountAndDestroyListeners() {
        long totalEvents = 0;

        registrations.forEach(Registration::close);
        registrations.clear();

        for (var listener : listeners) {
            totalEvents += listener.getNumEvents();
        }
        listeners.clear();

        LOG.debug("DsbenchmarkListenerProvider destroyed listeneres, total events {}", totalEvents);
        return totalEvents;
    }
}
