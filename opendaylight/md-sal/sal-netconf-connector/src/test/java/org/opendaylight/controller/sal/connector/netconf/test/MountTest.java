package org.opendaylight.controller.sal.connector.netconf.test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import io.netty.channel.ChannelFuture;
import io.netty.util.HashedWheelTimer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yang.store.impl.HardcodedYangStoreService;
import org.opendaylight.controller.config.yang.test.impl.DepTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.TestImplModuleFactory;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.NetconfOperationServiceFactoryImpl;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionListenerFactory;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.sal.connect.netconf.InventoryUtils;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.NetconfDeviceManager;
import org.opendaylight.controller.sal.connect.netconf.NetconfInventoryUtils;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.dom.broker.DataBrokerImpl;
import org.opendaylight.controller.sal.dom.broker.MountPointManagerImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class MountTest extends AbstractConfigTest {

    private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 12023);
    private static final InetSocketAddress tlsAddress = new InetSocketAddress("127.0.0.1", 12024);
    private static final URI NETCONF_MONITORING_NS = URI.create("urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring");
    
    private static final QName NETCONF_MONITORING = new QName(NETCONF_MONITORING_NS, new Date(2010,10,04), "ietf-netconf-monitoring");
    private static final QName NETCONF_MONITORING_STATE = new QName(NETCONF_MONITORING,"netconf-state");
    

    private NetconfMessage getConfig, getConfigCandidate, editConfig, closeSession;
    private DefaultCommitNotificationProducer commitNot;
    private NetconfServerDispatcher dispatch;
    private DataProviderService dataBroker;
    private MountPointManagerImpl mountManager;
    private NetconfDeviceManager netconfManager;

    private static QName CONFIG_MODULES = new QName(
            URI.create("urn:opendaylight:params:xml:ns:yang:controller:config"), null, "modules");
    private static QName CONFIG_SERVICES = new QName(
            URI.create("urn:opendaylight:params:xml:ns:yang:controller:config"), null, "modules");

    private NetconfClient createSession(final InetSocketAddress address, NetconfClientDispatcher dispatcher) throws InterruptedException {
        final NetconfClient netconfClient = new NetconfClient("test " + address.toString(), address, 5000, dispatcher);
        return netconfClient;
    }
    
    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(getModuleFactories().toArray(
                new ModuleFactory[0])));

        loadMessages();

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(getYangStore()));

        commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        dispatch = createDispatcher(Optional.<SSLContext> absent(), factoriesListener);
        ChannelFuture s = dispatch.createServer(tcpAddress);
        s.await();

        dataBroker = new DataBrokerImpl();
        mountManager = new MountPointManagerImpl();
        mountManager.setDataBroker(dataBroker);
        netconfManager = new NetconfDeviceManager();

        netconfManager.setMountService(mountManager);
        netconfManager.setDataService(dataBroker);
        netconfManager.start();

        try (NetconfClient netconfClient = createSession(tcpAddress, netconfManager.getDispatcher())) {
            // send edit_config.xml
            final Document rpcReply = netconfClient.sendMessage(this.editConfig).getDocument();
            assertNotNull(rpcReply);
        }
    }


    protected List<ModuleFactory> getModuleFactories() {
        return getModuleFactoriesS();
    }

    static List<ModuleFactory> getModuleFactoriesS() {
        return Lists.newArrayList(new TestImplModuleFactory(), new DepTestImplModuleFactory(),
                new NetconfTestImplModuleFactory());
    }

    private void loadMessages() throws IOException, SAXException, ParserConfigurationException {
        this.editConfig = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/edit_config.xml");
        this.getConfig = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig.xml");
        this.getConfigCandidate = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig_candidate.xml");
        this.closeSession = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/closeSession.xml");
    }

    private NetconfServerDispatcher createDispatcher(Optional<SSLContext> sslC,
            NetconfOperationServiceFactoryListenerImpl factoriesListener) {
        SessionIdProvider idProvider = new SessionIdProvider();
        NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                new HashedWheelTimer(5000, TimeUnit.MILLISECONDS), factoriesListener, idProvider);

        NetconfServerSessionListenerFactory listenerFactory = new NetconfServerSessionListenerFactory(
                factoriesListener, commitNot, idProvider);

        return new NetconfServerDispatcher(sslC, serverNegotiatorFactory, listenerFactory);
    }

    private HardcodedYangStoreService getYangStore() throws YangStoreException, IOException {
        final Collection<InputStream> yangDependencies = getBasicYangs();
        return new HardcodedYangStoreService(yangDependencies);
    }

    private Collection<InputStream> getBasicYangs() throws IOException {
        List<String> paths = Arrays.asList("/META-INF/yang/config.yang", "/META-INF/yang/rpc-context.yang",
                "/META-INF/yang/config-test.yang", "/META-INF/yang/config-test-impl.yang",
                "/META-INF/yang/ietf-inet-types.yang");
        final Collection<InputStream> yangDependencies = new ArrayList<>();
        for (String path : paths) {
            final InputStream is = Preconditions
                    .checkNotNull(getClass().getResourceAsStream(path), path + " not found");
            yangDependencies.add(is);
        }
        return yangDependencies;
    }

    @Test
    public void test() {
        // MountProvisionInstance mount =
        // Mockito.mock(MountProvisionInstance.class);
        InstanceIdentifier path = InstanceIdentifier.builder(InventoryUtils.INVENTORY_PATH)
                .node(InventoryUtils.INVENTORY_NODE).toInstance();
        netconfManager.netconfNodeAdded(path, tcpAddress);
        InstanceIdentifier mountPointPath = path;
        MountProvisionInstance mountPoint = mountManager.getMountPoint(mountPointPath);

        CompositeNode data = mountPoint.readOperationalData(InstanceIdentifier.builder().node(CONFIG_MODULES)
                .toInstance());
        assertNotNull(data);
        assertEquals(CONFIG_MODULES, data.getNodeType());

        CompositeNode data2 = mountPoint.readOperationalData(InstanceIdentifier.builder().toInstance());
        assertNotNull(data2);

        InstanceIdentifier fullPath = InstanceIdentifier.builder(mountPointPath).node(CONFIG_MODULES).toInstance();

        CompositeNode data3 = dataBroker.readOperationalData(fullPath);
        assertNotNull(data3);
        assertEquals(CONFIG_MODULES, data.getNodeType());
    }

}
