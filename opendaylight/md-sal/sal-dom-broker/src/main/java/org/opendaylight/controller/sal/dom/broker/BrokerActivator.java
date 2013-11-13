package org.opendaylight.controller.sal.dom.broker;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.datastore.ClusteredDataStore;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

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
    private ClusteredDataStore dataStore;
    private Logger logger = LoggerFactory.getLogger(BrokerActivator.class);

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

        initializeDataService(context);

        mountService = new MountPointManagerImpl();
        mountService.setDataBroker(dataService);
        
        mountReg = context.registerService(MountService.class, mountService, emptyProperties);
        mountProviderReg =  context.registerService(MountProvisionService.class, mountService, emptyProperties);
        
        brokerReg = context.registerService(Broker.class, broker, emptyProperties);
    }

    private void initializeDataService(BundleContext context) {
        DependencyManager dm = new DependencyManager(context);
        Component c = dm.createComponent();
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("dataService", dataService);
        c.setServiceProperties(props);
        c.add(dm.createServiceDependency().setService(
                ClusteredDataStore.class).setCallbacks(
                "setClusteredDataStore",
                "unsetClusteredDataStore").setRequired(true));

        c.setImplementation(DataServiceInitializer.class);
        dm.add(c);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (brokerReg != null) {
            brokerReg.unregister();
        }
    }


    public static class DataServiceInitializer {
        private ClusteredDataStore dataStore;
        private Logger logger = LoggerFactory.getLogger(DataServiceInitializer.class);

        public DataServiceInitializer(){

        }

        public void init(Component c) {
            logger.info("Initializing data service for sal-broker");
            DataBrokerImpl dataService = (DataBrokerImpl) c.getServiceProperties().get("dataService");
            dataService.registerConfigurationReader(ROOT, dataStore);
            dataService.registerCommitHandler(ROOT, dataStore);
            dataService.registerOperationalReader(ROOT, dataStore);
        }

        public void setClusteredDataStore(ClusteredDataStore dataStore){
            this.dataStore = dataStore;
        }

        public void unsetClusteredDataStore(ClusteredDataStore dataStore){
        }

    }
}
