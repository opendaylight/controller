package org.opendaylight.controller.sal.binding.test.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javassist.ClassPool;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.dom.serializer.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.controller.sal.binding.impl.BindingAwareBrokerImpl;
import org.opendaylight.controller.sal.binding.impl.DataBrokerImpl;
import org.opendaylight.controller.sal.binding.impl.NotificationBrokerImpl;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentMappingService;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.data.DataStore;
import org.opendaylight.controller.sal.dom.broker.BrokerImpl;
import org.opendaylight.controller.sal.dom.broker.impl.DataStoreStatsWrapper;
import org.opendaylight.controller.sal.dom.broker.impl.HashMapDataStore;
import org.opendaylight.controller.sal.dom.broker.impl.RpcRouterImpl;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareDataStoreAdapter;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import static com.google.common.base.Preconditions.*;

public class BindingTestContext implements AutoCloseable {
    
    
    public static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier TREE_ROOT = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
            .builder().toInstance();

    private static final Logger LOG = LoggerFactory.getLogger(BindingTestContext.class);
    
    private RuntimeGeneratedMappingServiceImpl mappingServiceImpl;
    
    
    private BindingAwareBrokerImpl baBrokerImpl;
    private DataBrokerImpl baDataImpl;
    private NotificationBrokerImpl baNotifyImpl;
    private BindingIndependentConnector baConnectDataServiceImpl;

    private org.opendaylight.controller.sal.dom.broker.DataBrokerImpl biDataImpl;
    private BrokerImpl biBrokerImpl;
    private HashMapDataStore rawDataStore;
    private SchemaAwareDataStoreAdapter schemaAwareDataStore;
    private DataStoreStatsWrapper dataStoreStats;
    private DataStore dataStore;

    
    private boolean dataStoreStatisticsEnabled = false;
    
    private final ListeningExecutorService executor;
    private final ClassPool classPool;

    private final boolean startWithSchema;

    
    protected BindingTestContext(ListeningExecutorService executor, ClassPool classPool, boolean startWithSchema) {
        this.executor = executor;
        this.classPool = classPool;
        this.startWithSchema = startWithSchema;
    }

    public void startDomDataStore() {
        checkState(dataStore == null, "DataStore already started.");
        checkState(biDataImpl != null, "Dom Data Broker not present");
        rawDataStore = new HashMapDataStore();
        schemaAwareDataStore = new SchemaAwareDataStoreAdapter();
        schemaAwareDataStore.changeDelegate(rawDataStore);
        if(dataStoreStatisticsEnabled) {
        dataStoreStats = new DataStoreStatsWrapper(schemaAwareDataStore);
        dataStore = dataStoreStats;
        } else {
            dataStore = schemaAwareDataStore;
        }
        
        biDataImpl.registerConfigurationReader(TREE_ROOT, dataStore);
        biDataImpl.registerOperationalReader(TREE_ROOT, dataStore);
        biDataImpl.registerCommitHandler(TREE_ROOT, dataStore);
    }
    
    public void startDomDataBroker() {
        checkState(executor != null,"Executor needs to be set");
        biDataImpl = new org.opendaylight.controller.sal.dom.broker.DataBrokerImpl();
        biDataImpl.setExecutor(executor);
    }
    
    public void startBindingDataBroker() {
        checkState(executor != null,"Executor needs to be set");
        baDataImpl = new DataBrokerImpl();
        baDataImpl.setExecutor(executor);
    }
    
    public void startBindingBroker() {
        checkState(executor != null,"Executor needs to be set");
        checkState(baDataImpl != null,"Binding Data Broker must be started");
        checkState(baNotifyImpl != null, "Notification Service must be started");
        baBrokerImpl = new BindingAwareBrokerImpl("test",null);
        
        baBrokerImpl.setDataBroker(baDataImpl);
        baBrokerImpl.setNotifyBroker(baNotifyImpl);
        
        baBrokerImpl.start();
    }
    
    public void startBindingToDomDataConnector() {
        checkState(baDataImpl != null,"Binding Data Broker needs to be started");
        checkState(biDataImpl != null,"DOM Data Broker needs to be started.");
        checkState(mappingServiceImpl != null,"DOM Mapping Service needs to be started.");
        baConnectDataServiceImpl = new BindingIndependentConnector();
        baConnectDataServiceImpl.setRpcRegistry(baBrokerImpl);
        baConnectDataServiceImpl.setDomRpcRegistry(getDomRpcRegistry());
        baConnectDataServiceImpl.setBaDataService(baDataImpl);
        baConnectDataServiceImpl.setBiDataService(biDataImpl);
        baConnectDataServiceImpl.setMappingService(mappingServiceImpl);
        baConnectDataServiceImpl.start();
    }
    
    public void startBindingToDomMappingService() {
        checkState(classPool != null,"ClassPool needs to be present");
        mappingServiceImpl = new RuntimeGeneratedMappingServiceImpl();
        mappingServiceImpl.setPool(classPool);
        mappingServiceImpl.start(null);
    }
    
    
    public void updateYangSchema(String[] files) {
        SchemaContext context = getContext(files);
        if(schemaAwareDataStore != null) {
            schemaAwareDataStore.onGlobalContextUpdated(context);
        }
        if(mappingServiceImpl != null) {
            mappingServiceImpl.onGlobalContextUpdated(context);
        }
    }
    
    
    public static String[] getAllYangFilesOnClasspath() {
        Predicate<String> predicate = new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.endsWith(".yang");
            }
        };
        Reflections reflection = new Reflections("META-INF.yang", new ResourcesScanner());
        Set<String> result = reflection.getResources(predicate);
        return (String[]) result.toArray(new String[result.size()]);
    }
    
    private static SchemaContext getContext(String[] yangFiles) {
        ClassLoader loader = BindingTestContext.class.getClassLoader();
        List<InputStream> streams = new ArrayList<>();
        for (String string : yangFiles) {
            InputStream stream = loader.getResourceAsStream(string);
            streams.add(stream);
        }
        YangParserImpl parser = new YangParserImpl();
        Set<Module> modules = parser.parseYangModelsFromStreams(streams);
        return parser.resolveSchemaContext(modules);
    }
    
    public void start() {
        startBindingDataBroker();
        startBindingNotificationBroker();
        startBindingBroker();
        startDomDataBroker();
        startDomDataStore();
        startDomBroker();
        startBindingToDomMappingService();
        startBindingToDomDataConnector();
        if(startWithSchema) {
            loadYangSchemaFromClasspath();
        }
    }

    private void startDomBroker() {
        checkState(executor != null);
        biBrokerImpl = new BrokerImpl();
        biBrokerImpl.setExecutor(executor);
        biBrokerImpl.setRouter(new RpcRouterImpl("test"));
    }

    public void startBindingNotificationBroker() {
        checkState(executor != null);
        baNotifyImpl = new NotificationBrokerImpl(executor);
        
    }

    public void loadYangSchemaFromClasspath() {
        String[] files = getAllYangFilesOnClasspath();
        updateYangSchema(files);
    }

    public DataProviderService getBindingDataBroker() {
        return baDataImpl;
    }

    public org.opendaylight.controller.sal.core.api.data.DataProviderService getDomDataBroker() {
        return biDataImpl;
    }

    public DataStore getDomDataStore() {
        return dataStore;
    }

    public BindingIndependentMappingService getBindingToDomMappingService() {
        return mappingServiceImpl;
    }

    public void logDataStoreStatistics() {
        if(dataStoreStats == null) {
            return;
        }
        
        LOG.info("BIDataStore Statistics: Configuration Read Count: {} TotalTime: {} ms AverageTime (ns): {} ms",
                dataStoreStats.getConfigurationReadCount(), dataStoreStats.getConfigurationReadTotalTime(),
                dataStoreStats.getConfigurationReadAverageTime());

        LOG.info("BIDataStore Statistics: Operational Read Count: {} TotalTime: {} ms AverageTime (ns): {} ms",
                dataStoreStats.getOperationalReadCount(), dataStoreStats.getOperationalReadTotalTime(),
                dataStoreStats.getOperationalReadAverageTime());

        LOG.info("BIDataStore Statistics: Request Commit Count: {} TotalTime: {} ms AverageTime (ns): {} ms",
                dataStoreStats.getRequestCommitCount(), dataStoreStats.getRequestCommitTotalTime(),
                dataStoreStats.getRequestCommitAverageTime());
    }

    public RpcProviderRegistry getBindingRpcRegistry() {
        return baBrokerImpl;
    }

    public RpcProvisionRegistry getDomRpcRegistry() {
        if(biBrokerImpl == null) {
            return null;
        }
        return biBrokerImpl.getRouter();
    }
    
    public RpcImplementation getDomRpcInvoker() {
        return biBrokerImpl.getRouter();
    }
    
    @Override
    public void close() throws Exception {
        
    }
}
