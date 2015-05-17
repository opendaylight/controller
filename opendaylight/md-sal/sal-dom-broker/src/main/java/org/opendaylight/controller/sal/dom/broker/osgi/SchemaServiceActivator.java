/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.osgi;

import java.util.Hashtable;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.dom.broker.GlobalBundleScanningSchemaServiceImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class SchemaServiceActivator implements BundleActivator {


    private ServiceRegistration<SchemaService> schemaServiceReg;
    private GlobalBundleScanningSchemaServiceImpl schemaService;

    @Override
    public void start(final BundleContext context) {
        schemaService = GlobalBundleScanningSchemaServiceImpl.createInstance(context);
        schemaServiceReg = context.registerService(SchemaService.class, schemaService, new Hashtable<String,String>());
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        schemaServiceReg.unregister();
        schemaService.close();
    }
}
