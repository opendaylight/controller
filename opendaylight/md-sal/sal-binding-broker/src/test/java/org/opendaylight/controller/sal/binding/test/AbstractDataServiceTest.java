package org.opendaylight.controller.sal.binding.test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.impl.DataBrokerImpl;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentDataServiceConnector;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentMappingService;
import org.opendaylight.controller.sal.binding.impl.connect.dom.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.controller.sal.binding.test.connect.dom.MappingServiceTest;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.dom.broker.impl.HashMapDataStore;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public  abstract class AbstractDataServiceTest {
    protected DataBrokerService biDataService;
    protected DataProviderService baDataService;
    
    protected RuntimeGeneratedMappingServiceImpl mappingServiceImpl;
    protected BindingIndependentMappingService mappingService;
    protected DataBrokerImpl baDataImpl;
    protected org.opendaylight.controller.sal.dom.broker.DataBrokerImpl biDataImpl;
    protected ListeningExecutorService executor;
    protected BindingIndependentDataServiceConnector connectorServiceImpl;
    protected HashMapDataStore dataStore;
    
    
    @Before
    public void setUp() {
        executor = MoreExecutors.sameThreadExecutor();
        baDataImpl = new DataBrokerImpl();
        baDataService = baDataImpl;
        baDataImpl.setExecutor(executor);
        
        biDataImpl = new org.opendaylight.controller.sal.dom.broker.DataBrokerImpl();
        biDataService =  biDataImpl;
        biDataImpl.setExecutor(executor);
        
        dataStore = new HashMapDataStore();
        org.opendaylight.yangtools.yang.data.api.InstanceIdentifier treeRoot = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder().toInstance();
        biDataImpl.registerConfigurationReader(treeRoot, dataStore);
        biDataImpl.registerOperationalReader(treeRoot, dataStore);
        biDataImpl.registerCommitHandler(treeRoot, dataStore);
        
        mappingServiceImpl = new RuntimeGeneratedMappingServiceImpl();
        mappingService = mappingServiceImpl;
        File pathname = new File("target/gen-classes-debug");
        //System.out.println("Generated classes are captured in " + pathname.getAbsolutePath());
        mappingServiceImpl.start();
        //mappingServiceImpl.getBinding().setClassFileCapturePath(pathname);
        
        connectorServiceImpl = new BindingIndependentDataServiceConnector();
        connectorServiceImpl.setBaDataService(baDataService);
        connectorServiceImpl.setBiDataService(biDataService);
        connectorServiceImpl.setMappingService(mappingServiceImpl);
        connectorServiceImpl.start();
        
        String[] yangFiles= getModelFilenames();
        mappingServiceImpl.onGlobalContextUpdated(getContext(yangFiles));
    }


    protected  String[] getModelFilenames() {
        return getModelFilenamesImpl();
    }
    
    public static String[] getModelFilenamesImpl() {
        Predicate<String> predicate = new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.endsWith(".yang");
            }
        };
        Reflections reflection= new Reflections("META-INF.yang", new ResourcesScanner());
        Set<String> result = reflection.getResources(predicate);
        return (String[]) result.toArray(new String[result.size()]);
    }
    
    public static SchemaContext getContext(String[] yangFiles) {

        ClassLoader loader = AbstractDataServiceTest.class.getClassLoader();

        List<InputStream> streams = new ArrayList<>();
        for (String string : yangFiles) {
            InputStream stream = loader.getResourceAsStream(string);
            streams.add(stream);

        }
        YangParserImpl parser = new YangParserImpl();

        Set<Module> modules = parser.parseYangModelsFromStreams(streams);
        return parser.resolveSchemaContext(modules);
    }
}
