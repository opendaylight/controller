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
    public void start(BundleContext context) throws Exception {
        schemaService = new GlobalBundleScanningSchemaServiceImpl();
        schemaService.setContext(context);
        schemaService.start();
        schemaServiceReg = context.registerService(SchemaService.class, schemaService, new Hashtable<String,String>());
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        schemaServiceReg.unregister();
        schemaService.close();
    }
}
