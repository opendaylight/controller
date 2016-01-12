/*
 * Copyright (c) 2016 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dsbenchmark.listener;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DsbenchmarkListener implements DataTreeChangeListener<TestExec> {
    private static final Logger LOG = LoggerFactory.getLogger(DsbenchmarkListener.class);
    private AtomicInteger numEvents = new AtomicInteger(0);

    @Override
    public void onDataTreeChanged(
            Collection<DataTreeModification<TestExec>> changes) {
        // Since we're registering the same DsbenchmarkListener object for both
        // OPERATIONAL and CONFIG, the onDataTreeChanged() method can be called
        // from different threads, and we need to use atomic counters.

        final int eventNum = numEvents.incrementAndGet();
        if(LOG.isDebugEnabled()){
            logDataTreeChangeEvent(eventNum, changes);
        }
    }

    private static synchronized void logDataTreeChangeEvent(int eventNum,
            Collection<DataTreeModification<TestExec>> changes) {
        LOG.debug("DsbenchmarkListener-onDataTreeChanged: Event {}", eventNum);

        for(DataTreeModification<TestExec> change : changes) {
            final DataObjectModification<TestExec> rootNode = change.getRootNode();
            final ModificationType modType = rootNode.getModificationType();
            final PathArgument changeId = rootNode.getIdentifier();
            final Collection<DataObjectModification<? extends DataObject>> modifications = rootNode.getModifiedChildren();

            LOG.debug("    changeId {}, modType {}, mods: {}", changeId, modType, modifications.size());

            for (DataObjectModification<? extends DataObject> mod : modifications) {
                LOG.debug("      mod-getDataAfter: {}", mod.getDataAfter());
            }
        }
    }

    public int getNumEvents() {
        return numEvents.get();
    }

}
