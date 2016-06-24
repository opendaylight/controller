/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a basic DCL implementation for performance testing reasons.  Emits "onDataChanged"
 * upon invocation of the <code>onDataChanged(...)</code> method at <code>TRACE</code> level,
 * so one can ensure that the DCL has actually been added through observation of the log.
 *
 * @author Ryan Goulding (ryandgoulding@gmail.com)
 */
public class CarDataChangeListener implements DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(CarDataChangeListener.class);

    @java.lang.Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        LOG.trace("onDataChanged");
    }
}
