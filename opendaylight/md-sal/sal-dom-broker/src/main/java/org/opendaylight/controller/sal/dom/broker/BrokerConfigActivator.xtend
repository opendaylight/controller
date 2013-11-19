package org.opendaylight.controller.sal.dom.broker

import org.osgi.framework.ServiceRegistration
import org.opendaylight.controller.sal.core.api.model.SchemaService
import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.controller.sal.core.api.data.DataProviderService
import org.opendaylight.controller.sal.dom.broker.impl.HashMapDataStore
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService
import org.opendaylight.controller.sal.core.api.mount.MountService
import org.osgi.framework.BundleContext
import java.util.Hashtable
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.controller.sal.core.api.data.DataStore

class BrokerConfigActivator implements AutoCloseable {
    
    
    private static val ROOT = InstanceIdentifier.builder().toInstance();
    
    private var ServiceRegistration<SchemaService> schemaReg;
    private var ServiceRegistration<DataBrokerService> dataReg;
    private var ServiceRegistration<DataProviderService> dataProviderReg;
    private var ServiceRegistration<MountService> mountReg;
    private var ServiceRegistration<MountProvisionService> mountProviderReg;
    
    private var SchemaServiceImpl schemaService;
    private var DataBrokerImpl dataService;
    private var MountPointManagerImpl mountService;

    public def void start(BrokerImpl broker,DataStore store,BundleContext context) {
        val emptyProperties = new Hashtable<String, String>();
        broker.setBundleContext(context);
        

        schemaService = new SchemaServiceImpl();
        schemaService.setContext(context);
        schemaService.setParser(new YangParserImpl());
        schemaService.start();
        schemaReg = context.registerService(SchemaService, schemaService, emptyProperties);
        
        dataService = new DataBrokerImpl();
        dataService.setExecutor(broker.getExecutor());
        
        dataReg = context.registerService(DataBrokerService, dataService, emptyProperties);
        dataProviderReg = context.registerService(DataProviderService, dataService, emptyProperties);

        dataService.registerConfigurationReader(ROOT, store);
        dataService.registerCommitHandler(ROOT, store);
        dataService.registerOperationalReader(ROOT, store);
        
        mountService = new MountPointManagerImpl();
        mountService.setDataBroker(dataService);
        
        mountReg = context.registerService(MountService, mountService, emptyProperties);
        mountProviderReg =  context.registerService(MountProvisionService, mountService, emptyProperties);
    }

    override def close() {
        schemaReg?.unregister();
        dataReg?.unregister();
        dataProviderReg?.unregister();
        mountReg?.unregister();
        mountProviderReg?.unregister();
    }
    
}