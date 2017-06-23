/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider;

import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.Cars;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a basic DTCL implementation for performance testing reasons.  Emits a rudimentary
 * summary of the changes that occurred.
 *
 * @author Ryan Goulding (ryandgoulding@gmail.com)
 */
public class CarDataTreeChangeListener implements DataTreeChangeListener<Cars> {
    private static final Logger LOG = LoggerFactory.getLogger(CarDataTreeChangeListener.class);

    @java.lang.Override
    public void onDataTreeChanged(@Nonnull final java.util.Collection<DataTreeModification<Cars>> changes) {
        if (LOG.isTraceEnabled()) {
            for (DataTreeModification<Cars> change : changes) {
                ouputChanges(change);
            }
        }
    }

    private static void ouputChanges(final DataTreeModification<Cars> change) {
        final DataObjectModification<Cars> rootNode = change.getRootNode();
        final ModificationType modificationType = rootNode.getModificationType();
        final InstanceIdentifier<Cars> rootIdentifier = change.getRootPath().getRootIdentifier();
        switch (modificationType) {
            case WRITE:
            case SUBTREE_MODIFIED: {
                final Cars dataBefore = rootNode.getDataBefore();
                final Cars dataAfter = rootNode.getDataAfter();
                LOG.trace("onDataTreeChanged - Cars config with path {} was added or changed from {} to {}",
                        rootIdentifier, dataBefore, dataAfter);
                break;
            }
            case DELETE: {
                LOG.trace("onDataTreeChanged - Cars config with path {} was deleted", rootIdentifier);
                break;
            }
            default: {
                LOG.trace("onDataTreeChanged called with unknown modificationType: {}", modificationType);
                break;
            }
        }
    }
}
