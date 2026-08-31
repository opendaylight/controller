/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import java.util.List;
import org.opendaylight.mdsal.binding.api.DataObjectDeleted;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.Cars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a basic DTCL implementation for performance testing reasons.  Emits a rudimentary
 * summary of the changes that occurred.
 *
 * @author Ryan Goulding (ryandgoulding@gmail.com)
 */
public final class CarDataTreeChangeListener implements DataTreeChangeListener<Cars> {
    private static final Logger LOG = LoggerFactory.getLogger(CarDataTreeChangeListener.class);

    @Override
    public void onDataTreeChanged(final List<DataTreeModification<Cars>> changes) {
        if (LOG.isTraceEnabled()) {
            for (var change : changes) {
                final var path = change.path();
                switch (change.getRootNode()) {
                    case DataObjectDeleted<Cars> unused ->
                        LOG.trace("onDataTreeChanged - Cars config with path {} was deleted", path);
                    case DataObjectModification.WithDataAfter<Cars> present ->
                        LOG.trace("onDataTreeChanged - Cars config with path {} was added or changed from {} to {}",
                            path, present.dataBefore(), present.dataAfter());
                }
            }
        }
    }
}
