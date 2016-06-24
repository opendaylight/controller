/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider;

import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.Cars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a basic DTCL implementation for performance testing reasons.  Emits "onDataTreeChanged"
 * upon invocation of the <code>onDataTreeChanged(...)</code> method at <code>TRACE</code> level,
 * so one can ensure that the DTCL has actually been added through observation of the log.
 *
 * @author Ryan Goulding (ryandgoulding@gmail.com)
 */
public class CarDataTreeChangeListener implements DataTreeChangeListener<Cars> {
    private static final Logger LOG = LoggerFactory.getLogger(CarDataTreeChangeListener.class);

    @java.lang.Override
    public void onDataTreeChanged(@Nonnull java.util.Collection<DataTreeModification<Cars>> changes) {
        LOG.trace("onDataTreeChanged");
    }
}
