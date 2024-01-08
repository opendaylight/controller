/*
 * Copyright (c) 2016 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dsbenchmark.listener;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DsbenchmarkListener implements DataTreeChangeListener<TestExec> {
    private static final Logger LOG = LoggerFactory.getLogger(DsbenchmarkListener.class);
    private final AtomicInteger numEvents = new AtomicInteger(0);
    private final AtomicInteger numDataChanges = new AtomicInteger(0);

    @Override
    public void onDataTreeChanged(final List<DataTreeModification<TestExec>> changes) {
        // Since we're registering the same DsbenchmarkListener object for both
        // OPERATIONAL and CONFIG, the onDataTreeChanged() method can be called
        // from different threads, and we need to use atomic counters.

        final int eventNum = numEvents.incrementAndGet();
        numDataChanges.addAndGet(changes.size());

        if (LOG.isDebugEnabled()) {
            logDataTreeChangeEvent(eventNum, changes);
        }
    }

    private static synchronized void logDataTreeChangeEvent(final int eventNum,
            final List<DataTreeModification<TestExec>> changes) {
        LOG.debug("DsbenchmarkListener-onDataTreeChanged: Event {}", eventNum);

        for (var change : changes) {
            final var rootNode = change.getRootNode();
            final var modType = rootNode.modificationType();
            final var changeId = rootNode.step();
            final var modifications = rootNode.modifiedChildren();

            LOG.debug("    changeId {}, modType {}, mods: {}", changeId, modType, modifications.size());

            for (var mod : modifications) {
                LOG.debug("      mod-getDataAfter: {}", mod.dataAfter());
            }
        }
    }

    public int getNumEvents() {
        return numEvents.get();
    }

    public int getNumDataChanges() {
        return numDataChanges.get();
    }
}
