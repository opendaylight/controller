/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.sample.pingdiscovery;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sample.pingdiscovery.util.AutoCloseableManager;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyManager implements SchemaContextListener,
                                                     Provider,
                                                     AutoCloseable {

    private final Logger log = LoggerFactory.getLogger( DependencyManager.class );

    private MountProvisionService mountService;
    private volatile SchemaContext globalSchemaContext;

    private volatile BindingIndependentMappingService mappingService;
    private final AutoCloseableManager closeables = new AutoCloseableManager();

    private volatile ServiceReference<BindingIndependentMappingService> mappingServiceRef;

    private volatile BundleContext bundleContext;

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet(); //Why does this work?
    }

    @Override
    public void onSessionInitiated(ProviderSession session) {

        mountService = session.getService(MountProvisionService.class);
        SchemaService schemaService = session.getService( SchemaService.class );

        ListenerRegistration<SchemaServiceListener> schemaListenerRegistration =
                schemaService.registerSchemaServiceListener( this );

        closeables.add( schemaListenerRegistration );
    }

    @Override
    public void close() throws Exception {
        closeables.close();
        if( bundleContext != null && mappingServiceRef != null ) {
            bundleContext.ungetService( mappingServiceRef );
        }
    }

    public SchemaContext getGlobalSchemaContext() {
        return globalSchemaContext;
    }

    public MountProvisionService getMountService() {
        return mountService;
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext context) {
        this.globalSchemaContext = context;
    }

    public void initDependenciesFromBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        mappingServiceRef = bundleContext.getServiceReference( BindingIndependentMappingService.class );
        mappingService = bundleContext.getService( mappingServiceRef );
    }

    public BindingIndependentMappingService getMappingService() {
        return mappingService;
    }

//  private SchemaContext getSchemaContext()
//  {
//      YangParserImpl parser = new YangParserImpl();
//      try {
//          InputStream icmpdataStream =  new FileInputStream( "cache/schema/icmpdata@2014-05-15.yang");
//          InputStream ietfNettypes =  new FileInputStream( "cache/schema/ietf-inet-types@2010-09-24.yang");
//          Preconditions.checkNotNull( icmpdataStream );
//          Set<Module> models = parser.parseYangModelsFromStreams( Arrays.asList( icmpdataStream, ietfNettypes ));
//          return parser.resolveSchemaContext(models);
//      } catch (Exception e) {
//          e.printStackTrace();
//          return null;
//      }
//  }
}
