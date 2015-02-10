/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.DepTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.IdentityTestModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.MultipleDependenciesModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.TestImplModuleFactory;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.NetconfOperationServiceFactoryImpl;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreService;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceSnapshotImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.notifications.BaseNetconfNotificationListener;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChange;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.w3c.dom.Element;

public abstract class AbstractNetconfConfigTest extends AbstractConfigTest {

    public static final String LOOPBACK_ADDRESS = "127.0.0.1";
    public static final int SERVER_CONNECTION_TIMEOUT_MILLIS = 5000;
    private static final int RESOURCE_TIMEOUT_MINUTES = 2;

    static ModuleFactory[] FACTORIES = {new TestImplModuleFactory(),
                                        new DepTestImplModuleFactory(),
                                        new NetconfTestImplModuleFactory(),
                                        new IdentityTestModuleFactory(),
                                        new MultipleDependenciesModuleFactory() };

    private EventLoopGroup nettyThreadgroup;
    private HashedWheelTimer hashedWheelTimer;

    private NetconfClientDispatcherImpl clientDispatcher;
    private Channel serverTcpChannel;

    private NetconfMessage getConfig;
    private NetconfMessage get;

    /**
     * @Before in subclasses is called after this method.
     */
    @Before
    public void setUpAbstractNetconfConfigTest() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, FACTORIES));

        nettyThreadgroup = new NioEventLoopGroup();
        hashedWheelTimer = new HashedWheelTimer();

        loadMessages();

        setUpTestInitial();

        final NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(getYangStore()));

        for (final NetconfOperationServiceFactory netconfOperationServiceFactory : getAdditionalServiceFactories()) {
            factoriesListener.onAddNetconfOperationServiceFactory(netconfOperationServiceFactory);
        }

        serverTcpChannel = startNetconfTcpServer(factoriesListener);
        clientDispatcher = new NetconfClientDispatcherImpl(getNettyThreadgroup(), getNettyThreadgroup(), getHashedWheelTimer());
    }

    /**
     * Called before setUp method is executed, so test classes can set up resources before setUpAbstractNetconfConfigTest method is called.
     */
    protected void setUpTestInitial() throws Exception {}

    private void loadMessages() throws Exception {
        this.getConfig = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig.xml");
        this.get = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/get.xml");
    }

    public NetconfMessage getGetConfig() {
        return getConfig;
    }

    public NetconfMessage getGet() {
        return get;
    }

    private Channel startNetconfTcpServer(final NetconfOperationServiceFactoryListenerImpl factoriesListener) throws Exception {
        final NetconfServerDispatcher dispatch = createDispatcher(factoriesListener, getNetconfMonitoringService(), getNotificationProducer());

        final ChannelFuture s;
        if(getTcpServerAddress() instanceof LocalAddress) {
            s = dispatch.createLocalServer(((LocalAddress) getTcpServerAddress()));
        } else {
            s = dispatch.createServer(((InetSocketAddress) getTcpServerAddress()));
        }
        s.await(RESOURCE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        return s.channel();
    }

    protected DefaultCommitNotificationProducer getNotificationProducer() {
        final DefaultCommitNotificationProducer notificationProducer = mock(DefaultCommitNotificationProducer.class);
        doNothing().when(notificationProducer).close();
        doNothing().when(notificationProducer).sendCommitNotification(anyString(), any(Element.class), anySetOf(String.class));
        return notificationProducer;
    }

    protected Iterable<NetconfOperationServiceFactory> getAdditionalServiceFactories() {
        return Collections.emptySet();
    }

    protected SessionMonitoringService getNetconfMonitoringService() throws Exception {
        final NetconfOperationProvider netconfOperationProvider = mock(NetconfOperationProvider.class);
        final NetconfOperationServiceSnapshotImpl snap = mock(NetconfOperationServiceSnapshotImpl.class);
        doReturn(Collections.<NetconfOperationService>emptySet()).when(snap).getServices();
        doReturn(snap).when(netconfOperationProvider).openSnapshot(anyString());
        return new NetconfMonitoringServiceImpl(netconfOperationProvider);
    }

    protected abstract SocketAddress getTcpServerAddress();

    public NetconfClientDispatcherImpl getClientDispatcher() {
        return clientDispatcher;
    }

    private HardcodedYangStoreService getYangStore() throws IOException {
        final Collection<InputStream> yangDependencies = getBasicYangs();
        return new HardcodedYangStoreService(yangDependencies);
    }

    static Collection<InputStream> getBasicYangs() throws IOException {

        final List<String> paths = Arrays.asList(
                "/META-INF/yang/config.yang",
                "/META-INF/yang/rpc-context.yang",
                "/META-INF/yang/config-test.yang",
                "/META-INF/yang/config-test-impl.yang",
                "/META-INF/yang/test-types.yang",
                "/META-INF/yang/test-groups.yang",
                "/META-INF/yang/ietf-inet-types.yang");

        final Collection<InputStream> yangDependencies = new ArrayList<>();
        final List<String> failedToFind = new ArrayList<>();
        for (final String path : paths) {
            final InputStream resourceAsStream = NetconfITTest.class.getResourceAsStream(path);
            if (resourceAsStream == null) {
                failedToFind.add(path);
            } else {
                yangDependencies.add(resourceAsStream);
            }
        }
        assertEquals("Some yang files were not found", Collections.<String>emptyList(), failedToFind);
        return yangDependencies;
    }

    protected NetconfServerDispatcher createDispatcher(
            final NetconfOperationServiceFactoryListenerImpl factoriesListener, final SessionMonitoringService sessionMonitoringService,
            final DefaultCommitNotificationProducer commitNotifier) {
        final SessionIdProvider idProvider = new SessionIdProvider();

        final NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                hashedWheelTimer, factoriesListener, idProvider, SERVER_CONNECTION_TIMEOUT_MILLIS, commitNotifier, sessionMonitoringService);

        final NetconfServerDispatcher.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerChannelInitializer(
                serverNegotiatorFactory);
        return new NetconfServerDispatcher(serverChannelInitializer, nettyThreadgroup, nettyThreadgroup);
    }

    protected HashedWheelTimer getHashedWheelTimer() {
        return hashedWheelTimer;
    }

    protected EventLoopGroup getNettyThreadgroup() {
        return nettyThreadgroup;
    }

    /**
     * @After in subclasses is be called before this.
     */
    @After
    public void cleanUpNetconf() throws Exception {
        serverTcpChannel.close().await(RESOURCE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        hashedWheelTimer.stop();
        nettyThreadgroup.shutdownGracefully().await(RESOURCE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    public NetconfClientConfiguration getClientConfiguration(final InetSocketAddress tcpAddress, final int timeout) {
        final NetconfClientConfigurationBuilder b = NetconfClientConfigurationBuilder.create();
        b.withAddress(tcpAddress);
        b.withSessionListener(new SimpleNetconfClientSessionListener());
        b.withReconnectStrategy(new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, timeout));
        b.withConnectionTimeoutMillis(timeout);
        return b.build();
    }

    public static final class HardcodedYangStoreService extends YangStoreService {
        public HardcodedYangStoreService(final Collection<? extends InputStream> inputStreams) throws IOException {
            super(new SchemaContextProvider() {
                @Override
                public SchemaContext getSchemaContext() {
                    return getSchema(inputStreams);
                }
            }, new BaseNetconfNotificationListener() {
                @Override
                public void onCapabilityChanged(final NetconfCapabilityChange capabilityChange) {
                    // NOOP
                }
            });
        }

        private static SchemaContext getSchema(final Collection<? extends InputStream> inputStreams) {
            final ArrayList<InputStream> byteArrayInputStreams = new ArrayList<>();
            for (final InputStream inputStream : inputStreams) {
                assertNotNull(inputStream);
                final byte[] content;
                try {
                    content = IOUtils.toByteArray(inputStream);
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot read " + inputStream, e);
                }
                final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
                byteArrayInputStreams.add(byteArrayInputStream);
            }

            for (final InputStream inputStream : byteArrayInputStreams) {
                try {
                    inputStream.reset();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }

            final YangParserImpl yangParser = new YangParserImpl();
            return yangParser.resolveSchemaContext(new HashSet<>(yangParser.parseYangModelsFromStreamsMapped(byteArrayInputStreams).values()));
        }
    }
}
