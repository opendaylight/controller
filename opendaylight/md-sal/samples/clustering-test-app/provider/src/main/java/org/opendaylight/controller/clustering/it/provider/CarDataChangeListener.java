/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a basic DCL implementation for performance testing reasons.  Emits a summary
 * of the changes that occurred.
 *
 * @author Ryan Goulding (ryandgoulding@gmail.com)
 */
public class CarDataChangeListener implements DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(CarDataChangeListener.class);

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("onDataChanged invoked");
            outputChanges(change);
        }
    }

    private static void outputChanges(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        final Map<InstanceIdentifier<?>, DataObject> originalData = change.getOriginalData() != null ?
                change.getOriginalData(): Collections.<InstanceIdentifier<?>, DataObject>emptyMap();
        final Map<InstanceIdentifier<?>, DataObject> updatedData = change.getUpdatedData() != null ?
                change.getUpdatedData(): Collections.<InstanceIdentifier<?>, DataObject>emptyMap();
        final Map<InstanceIdentifier<?>, DataObject> createdData = change.getCreatedData() != null ?
                change.getCreatedData(): Collections.<InstanceIdentifier<?>, DataObject>emptyMap();
        final Set<InstanceIdentifier<?>> removedPaths = change.getRemovedPaths() != null ?
                change.getRemovedPaths(): Collections.<InstanceIdentifier<?>>emptySet();
        LOG.trace("AsyncDataChangeEvent - originalData={} updatedData={} createdData={} removedPaths={}",
                originalData, updatedData, createdData, removedPaths);
    }
}
