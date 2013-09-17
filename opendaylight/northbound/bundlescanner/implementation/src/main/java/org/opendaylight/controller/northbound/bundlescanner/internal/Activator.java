/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.bundlescanner.internal;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.northbound.bundlescanner.IBundleScanService;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;

/**
 * The activator registers the BundleScanner.
 */
public class Activator extends ComponentActivatorAbstractBase {

    @Override
    protected void init() {
    }

    @Override
    protected void destroy() {
    }

    @Override
    protected Object[] getGlobalImplementations() {
        return new Object[] { BundleScanServiceImpl.class };
    }

    @Override
    protected void configureGlobalInstance(Component c, Object imp) {
        if (!imp.equals(BundleScanServiceImpl.class)) return;
        // export service
        c.setInterface(
                new String[] { IBundleScanService.class.getName() },
                null);
    }

}
