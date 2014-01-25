package org.opendaylight.controller.sal.binding.osgi;

import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Hashtable;

public class Activator implements BundleActivator {

    private ServiceRegistration<?> reg;
    private ServiceRegistration<SchemaServiceListener> listenerReg;
    private ServiceRegistration<BindingIndependentMappingService> mappingReg;

    @Override
    public void start(BundleContext context) throws Exception {
                RuntimeGeneratedMappingServiceImpl service = new RuntimeGeneratedMappingServiceImpl();
        service.setPool(SingletonHolder.CLASS_POOL);
        service.init();
        startRuntimeMappingService(service, context);
    }

    private void startRuntimeMappingService(RuntimeGeneratedMappingServiceImpl service, BundleContext context) {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        listenerReg = context.registerService(SchemaServiceListener.class, service, properties);
        mappingReg = context.registerService(BindingIndependentMappingService.class, service, properties);
        
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if(listenerReg != null) {
            listenerReg.unregister();
        }
        if(mappingReg != null) {
            mappingReg.unregister();
        }
    }
}
