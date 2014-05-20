/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.sample.pingdiscovery.dependencies.mgrs;

import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * This is responsible for managing and interacting with all services fetched via the bundle context.
 * <br><br>DEMOSTRATES: How to get services from OSGi's bundle context when you can't get to them
*  via the BrokerService (config sub system).
 * @author Devin Avery
 * @author Greg Hall
 */
public class BundleContextDependencyManager implements AutoCloseable{

    private volatile BundleContext bundleContext;
    private volatile BindingIndependentMappingService mappingService;
    private volatile ServiceReference<BindingIndependentMappingService> mappingServiceRef;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() {
        mappingServiceRef =
                bundleContext.getServiceReference( BindingIndependentMappingService.class );
        mappingService = bundleContext.getService( mappingServiceRef );
    }

    public BindingIndependentMappingService getMappingService() {
        return mappingService;
    }

    @Override
    public void close() throws Exception {
        //Return your reference to the service back to the bundle context. This allows OSGi to know
        //that we are no longer using that service.
        if( bundleContext != null && mappingServiceRef != null )
        {
            bundleContext.ungetService( mappingServiceRef );
        }
    }
}
