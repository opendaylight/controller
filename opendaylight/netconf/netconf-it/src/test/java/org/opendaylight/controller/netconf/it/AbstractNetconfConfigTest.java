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

import com.google.common.io.ByteStreams;
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
import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacadeFactory;
import org.opendaylight.controller.config.facade.xml.osgi.EnumResolver;
import org.opendaylight.controller.config.facade.xml.osgi.YangStoreService;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.DepTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.IdentityTestModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.MultipleDependenciesModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.TestImplModuleFactory;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.NetconfOperationServiceFactoryImpl;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcherImpl;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.AggregatedNetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringActivator;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringOperationService;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public abstract class AbstractNetconfConfigTest extends AbstractConfigTest {

    public static final String LOOPBACK_ADDRESS = "127.0.0.1";
    public static final int SERVER_CONNECTION_TIMEOUT_MILLIS = 5000;
    private static final int RESOURCE_TIMEOUT_MINUTES = 2;

    static ModuleFactory[] FACTORIES = {new TestImplModuleFactory(),
                                        new DepTestImplModuleFactory(),
                                        new NetconfTestImplModuleFactory(),
                                        new IdentityTestModuleFactory(),
                                        new MultipleDependenciesModuleFactory() };

    protected ConfigSubsystemFacadeFactory configSubsystemFacadeFactory;
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

        final AggregatedNetconfOperationServiceFactory factoriesListener = new AggregatedNetconfOperationServiceFactory();
        final NetconfMonitoringService netconfMonitoringService = getNetconfMonitoringService(factoriesListener);
        configSubsystemFacadeFactory = new ConfigSubsystemFacadeFactory(configRegistryClient, configRegistryClient, getYangStore());
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(configSubsystemFacadeFactory));
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory(new NetconfMonitoringOperationService(netconfMonitoringService)));

        for (final NetconfOperationServiceFactory netconfOperationServiceFactory : getAdditionalServiceFactories(factoriesListener)) {
            factoriesListener.onAddNetconfOperationServiceFactory(netconfOperationServiceFactory);
        }

        serverTcpChannel = startNetconfTcpServer(factoriesListener, netconfMonitoringService);
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

    private Channel startNetconfTcpServer(final AggregatedNetconfOperationServiceFactory listener, final NetconfMonitoringService monitoring) throws Exception {
        final NetconfServerDispatcherImpl dispatch = createDispatcher(listener, monitoring);

        final ChannelFuture s;
        if(getTcpServerAddress() instanceof LocalAddress) {
            s = dispatch.createLocalServer(((LocalAddress) getTcpServerAddress()));
        } else {
            s = dispatch.createServer(((InetSocketAddress) getTcpServerAddress()));
        }
        s.await(RESOURCE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        return s.channel();
    }

    protected Iterable<NetconfOperationServiceFactory> getAdditionalServiceFactories(final AggregatedNetconfOperationServiceFactory factoriesListener) throws Exception {
        return Collections.emptySet();
    }

    protected NetconfMonitoringService getNetconfMonitoringService(final AggregatedNetconfOperationServiceFactory factoriesListener) throws Exception {
        return new NetconfMonitoringServiceImpl(factoriesListener);
    }

    protected abstract SocketAddress getTcpServerAddress();

    public NetconfClientDispatcherImpl getClientDispatcher() {
        return clientDispatcher;
    }

    private HardcodedYangStoreService getYangStore() throws IOException {
        final Collection<InputStream> yangDependencies = getBasicYangs();
        return new HardcodedYangStoreService(yangDependencies, getBindingRuntimeContext());
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

    protected NetconfServerDispatcherImpl createDispatcher(
            final AggregatedNetconfOperationServiceFactory factoriesListener, final NetconfMonitoringService sessionMonitoringService) {
        final SessionIdProvider idProvider = new SessionIdProvider();

        final NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                hashedWheelTimer, factoriesListener, idProvider, SERVER_CONNECTION_TIMEOUT_MILLIS, sessionMonitoringService);

        final NetconfServerDispatcherImpl.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcherImpl.ServerChannelInitializer(
                serverNegotiatorFactory);
        return new NetconfServerDispatcherImpl(serverChannelInitializer, nettyThreadgroup, nettyThreadgroup);
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
        public HardcodedYangStoreService(final Collection<? extends InputStream> inputStreams, final BindingRuntimeContext bindingRuntimeContext) throws IOException {
            super(new SchemaContextProvider() {
                @Override
                public SchemaContext getSchemaContext() {
                    return getSchema(inputStreams);
                }
            });

            refresh(bindingRuntimeContext);
        }

        private static SchemaContext getSchema(final Collection<? extends InputStream> inputStreams) {
            final ArrayList<InputStream> byteArrayInputStreams = new ArrayList<>();
            for (final InputStream inputStream : inputStreams) {
                assertNotNull(inputStream);
                final byte[] content;
                try {
                    content = ByteStreams.toByteArray(inputStream);
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

        @Override
        public EnumResolver getEnumResolver() {
            return new EnumResolver() {
                @Override
                public String fromYang(final String enumType, final String enumYangValue) {
                    return BindingMapping.getClassName(enumYangValue);
                }

                @Override
                public String toYang(final String enumType, final String enumJavaValue) {
                    return enumJavaValue.toLowerCase();
                }
            };
        }
    }
}
