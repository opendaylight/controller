package org.opendaylight.controller.sal.dom.broker;

import java.util.Hashtable;

import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.dom.broker.impl.HashMapDataStore;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class BrokerActivator implements BundleActivator {

    private static final InstanceIdentifier ROOT = InstanceIdentifier.builder().toInstance();
    BrokerImpl broker;
    private ServiceRegistration<Broker> brokerReg;
    private ServiceRegistration<SchemaService> schemaReg;
    private ServiceRegistration<DataBrokerService> dataReg;
    private ServiceRegistration<DataProviderService> dataProviderReg;
    private SchemaServiceImpl schemaService;
    private DataBrokerImpl dataService;
    private MountPointManagerImpl mountService;
    private ServiceRegistration<MountService> mountReg;
    private ServiceRegistration<MountProvisionService> mountProviderReg;
    private HashMapDataStore hashMapStore;

    @Override
    public void start(BundleContext context) throws Exception {
        Hashtable<String, String> emptyProperties = new Hashtable<String, String>();
        broker = new BrokerImpl();
        broker.setBundleContext(context);
        

        schemaService = new SchemaServiceImpl();
        schemaService.setContext(context);
        schemaService.setParser(new YangParserImpl());
        schemaService.start();
        schemaReg = context.registerService(SchemaService.class, schemaService, new Hashtable<String, String>());
        
        dataService = new DataBrokerImpl();
        dataService.setExecutor(broker.getExecutor());
        
        dataReg = context.registerService(DataBrokerService.class, dataService, emptyProperties);
        dataProviderReg = context.registerService(DataProviderService.class, dataService, emptyProperties);
        
        hashMapStore = new HashMapDataStore();
        
        dataService.registerConfigurationReader(ROOT, hashMapStore);
        dataService.registerCommitHandler(ROOT, hashMapStore);
        dataService.registerOperationalReader(ROOT, hashMapStore);
        
        mountService = new MountPointManagerImpl();
        mountService.setDataBroker(dataService);
        
        mountReg = context.registerService(MountService.class, mountService, emptyProperties);
        mountProviderReg =  context.registerService(MountProvisionService.class, mountService, emptyProperties);
        
        brokerReg = context.registerService(Broker.class, broker, emptyProperties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (brokerReg != null) {
            brokerReg.unregister();
        }
    }
}
