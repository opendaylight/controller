package org.opendaylight.controller.sal.binding.test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javassist.ClassPool;

import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.impl.DataBrokerImpl;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentMappingService;
import org.opendaylight.controller.sal.binding.test.util.BindingBrokerTestFactory;
import org.opendaylight.controller.sal.binding.test.util.BindingTestContext;
import org.opendaylight.controller.sal.binding.dom.serializer.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataStore;
import org.opendaylight.controller.sal.dom.broker.impl.DataStoreStatsWrapper;
import org.opendaylight.controller.sal.dom.broker.impl.HashMapDataStore;
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

public abstract class AbstractDataServiceTest {
    private static Logger log = LoggerFactory.getLogger(AbstractDataServiceTest.class);

    protected org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService;
    protected DataProviderService baDataService;
    protected BindingIndependentMappingService mappingService;
    private DataStoreStatsWrapper dataStoreStats;
    protected DataStore dataStore;
    protected BindingTestContext testContext;

    @Before
    public void setUp() {
        ListeningExecutorService executor = MoreExecutors.sameThreadExecutor();
        BindingBrokerTestFactory factory = new BindingBrokerTestFactory();
        factory.setExecutor(executor);
        factory.setStartWithParsedSchema(getStartWithSchema());
        testContext = factory.getTestContext();
        testContext.start();
        
        baDataService = testContext.getBindingDataBroker();
        biDataService = testContext.getDomDataBroker();
        dataStore = testContext.getDomDataStore();
        mappingService = testContext.getBindingToDomMappingService();
    }

    protected boolean getStartWithSchema() {
        return true;
    }

    @After
    public void afterTest() {

        testContext.logDataStoreStatistics();

    }
}
