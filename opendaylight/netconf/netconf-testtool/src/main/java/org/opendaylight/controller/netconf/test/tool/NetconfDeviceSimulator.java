/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreException;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreService;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreSnapshot;
import org.opendaylight.controller.netconf.confignetconfconnector.util.Util;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringOperationService;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NetconfDeviceSimulator implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceSimulator.class);

    private static final FileFilter YANG_FILE_FILTER = new SuffixFileFilter("yang");

    public static final int CONNECTION_TIMEOUT_MILLIS = 20000;

    private final NioEventLoopGroup nettyThreadgroup;
    private final HashedWheelTimer hashedWheelTimer;
    private final List<Channel> devicesChannels = Lists.newArrayList();

    public NetconfDeviceSimulator() {
        this(new NioEventLoopGroup(), new HashedWheelTimer());
    }

    public NetconfDeviceSimulator(final NioEventLoopGroup eventExecutors, final HashedWheelTimer hashedWheelTimer) {
        this.nettyThreadgroup = eventExecutors;
        this.hashedWheelTimer = hashedWheelTimer;
    }

    private static Collection<File> getFiles(final File schemasDir) {
        return Arrays.asList(schemasDir.listFiles(YANG_FILE_FILTER));
    }

    private NetconfServerDispatcher createDispatcher(final YangStoreService yangStoreService) {

        final SessionIdProvider idProvider = new SessionIdProvider();


        final SimulatedOperationProvider simulatedOperationProvider = new SimulatedOperationProvider(idProvider, yangStoreService);
        final NetconfMonitoringOperationService monitoringService = new NetconfMonitoringOperationService(new NetconfMonitoringServiceImpl(simulatedOperationProvider));
        simulatedOperationProvider.addService(monitoringService);

        final DefaultCommitNotificationProducer commitNotifier = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        final NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                hashedWheelTimer, simulatedOperationProvider, idProvider, CONNECTION_TIMEOUT_MILLIS, commitNotifier, new LoggingMonitoringService());

        final NetconfServerDispatcher.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerChannelInitializer(
                serverNegotiatorFactory);
        return new NetconfServerDispatcher(serverChannelInitializer, nettyThreadgroup, nettyThreadgroup);
    }

    public List<Integer> start(final Main.Params params) {
        final StaticYangStoreService yangStoreService;

        yangStoreService = new StaticYangStoreService(getFiles(params.schemasDir));
        // Init schema context before connections arrive
        try {
            LOG.debug("Parsing schemas in {}", params.schemasDir);
            yangStoreService.getYangStoreSnapshot();
        } catch (final YangStoreException e) {
            throw new RuntimeException(e);
        }

        final NetconfServerDispatcher dispatcher = createDispatcher(yangStoreService);

        int currentPort = params.startingPort;

        final List<Integer> openDevices = Lists.newArrayList();
        for (int i = 0; i < params.deviceCount; i++) {
            final InetSocketAddress address = getAddress(currentPort);
            final LocalAddress tcpLocalAddress = new LocalAddress(address.toString());
            final ChannelFuture server = dispatcher.createLocalServer(tcpLocalAddress);

            try {
                NetconfSSHServer.start(currentPort, tcpLocalAddress, new AcceptingAuthProvider(), nettyThreadgroup);
            } catch (final Exception e) {
                LOG.warn("Cannot start simulated device on {}, skipping", address, e);
                // Close local server and continue
                server.channel().close();
                continue;
            } finally {
                currentPort++;
            }

            try {
                server.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            } catch (final ExecutionException e) {
                LOG.warn("Cannot start simulated device on {}, skipping", address, e);
                continue;
            }

            devicesChannels.add(server.channel());

            openDevices.add(currentPort - 1);
            LOG.debug("Simulated device started on {}", address);
        }

        if(openDevices.size() == params.deviceCount) {
            LOG.info("All simulated devices started successfully from port {} to {}", params.startingPort, currentPort);
        } else {
            LOG.warn("Not all simulated devices started successfully. Started devices ar on ports {}", openDevices);
        }

        return openDevices;
    }

    private static InetSocketAddress getAddress(final int port) {
        try {
            // TODO make address configurable
            return new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), port);
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        for (final Channel deviceCh : devicesChannels) {
            deviceCh.close();
        }
        nettyThreadgroup.shutdownGracefully();
        // close Everything
    }

    private static class SimulatedOperationProvider implements NetconfOperationProvider {
        private final SessionIdProvider idProvider;
        private final Set<NetconfOperationService> netconfOperationServices;


        public SimulatedOperationProvider(final SessionIdProvider idProvider, final YangStoreService yangStoreService) {
            this.idProvider = idProvider;
            final SimulatedOperationService simulatedOperationService = new SimulatedOperationService(yangStoreService, idProvider.getCurrentSessionId());
            this.netconfOperationServices = Sets.<NetconfOperationService>newHashSet(simulatedOperationService);
        }

        @Override
        public NetconfOperationServiceSnapshot openSnapshot(final String sessionIdForReporting) {
            return new SimulatedServiceSnapshot(idProvider, netconfOperationServices);
        }

        public void addService(final NetconfOperationService monitoringService) {
            netconfOperationServices.add(monitoringService);
        }

        private static class SimulatedServiceSnapshot implements NetconfOperationServiceSnapshot {
            private final SessionIdProvider idProvider;
            private final Set<NetconfOperationService> netconfOperationServices;

            public SimulatedServiceSnapshot(final SessionIdProvider idProvider, final Set<NetconfOperationService> netconfOperationServices) {
                this.idProvider = idProvider;
                this.netconfOperationServices = netconfOperationServices;
            }

            @Override
            public String getNetconfSessionIdForReporting() {
                return String.valueOf(idProvider.getCurrentSessionId());
            }

            @Override
            public Set<NetconfOperationService> getServices() {
                return netconfOperationServices;
            }

            @Override
            public void close() throws Exception {}
        }

        static class SimulatedOperationService implements NetconfOperationService {
            private final YangStoreSnapshot yangStoreSnapshot;
            private static SimulatedGet sGet;

            public SimulatedOperationService(final YangStoreService yangStoreService, final long currentSessionId) {
                try {
                    this.yangStoreSnapshot = yangStoreService.getYangStoreSnapshot();
                } catch (final YangStoreException e) {
                    // Should not happen
                    throw new RuntimeException(e);
                }

                sGet = new SimulatedGet(String.valueOf(currentSessionId));
            }

            @Override
            public Set<Capability> getCapabilities() {

                return Sets.newHashSet(Collections2.transform(yangStoreSnapshot.getModules(), new Function<Module, Capability>() {
                    @Override
                    public Capability apply(final Module input) {
                        return new YangStoreCapability(input, yangStoreSnapshot.getModuleSource(input));
                    }
                }));
            }

            @Override
            public Set<NetconfOperation> getNetconfOperations() {
                return Sets.<NetconfOperation>newHashSet(sGet);
            }

            @Override
            public void close() {

            }
        }
    }

    private static class SimulatedGet extends AbstractConfigNetconfOperation {

        protected SimulatedGet(final String netconfSessionIdForReporting) {
            super(null, netconfSessionIdForReporting);
        }

        @Override
        protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement) throws NetconfDocumentedException {
            return XmlUtil.createElement(document, XmlNetconfConstants.DATA_KEY, Optional.<String>absent());
        }

        @Override
        protected String getOperationName() {
            return XmlNetconfConstants.GET;
        }
    }

    // FIXME move BasicCapability + YangStoreCapability to netconf-util from config-netconf-connector
    private static class BasicCapability implements Capability {

        private final String capability;

        private BasicCapability(final String capability) {
            this.capability = capability;
        }

        @Override
        public String getCapabilityUri() {
            return capability;
        }

        @Override
        public Optional<String> getModuleNamespace() {
            return Optional.absent();
        }

        @Override
        public Optional<String> getModuleName() {
            return Optional.absent();
        }

        @Override
        public Optional<String> getRevision() {
            return Optional.absent();
        }

        @Override
        public Optional<String> getCapabilitySchema() {
            return Optional.absent();
        }

        @Override
        public Optional<List<String>> getLocation() {
            return Optional.absent();
        }

        @Override
        public String toString() {
            return capability;
        }
    }


    private static final class YangStoreCapability extends BasicCapability {

        private final String content;
        private final String revision;
        private final String moduleName;
        private final String moduleNamespace;

        public YangStoreCapability(final Module module, final String moduleContent) {
            super(toCapabilityURI(module));
            this.content = moduleContent;
            this.moduleName = module.getName();
            this.moduleNamespace = module.getNamespace().toString();
            this.revision = Util.writeDate(module.getRevision());
        }

        @Override
        public Optional<String> getCapabilitySchema() {
            return Optional.of(content);
        }

        private static String toCapabilityURI(final Module module) {
            return String.valueOf(module.getNamespace()) + "?module="
                    + module.getName() + "&revision=" + Util.writeDate(module.getRevision());
        }

        @Override
        public Optional<String> getModuleName() {
            return Optional.of(moduleName);
        }

        @Override
        public Optional<String> getModuleNamespace() {
            return Optional.of(moduleNamespace);
        }

        @Override
        public Optional<String> getRevision() {
            return Optional.of(revision);
        }
    }

    private static class AcceptingAuthProvider implements AuthProvider {
        private final String privateKeyPEMString;

        public AcceptingAuthProvider() {
            try {
                this.privateKeyPEMString = PEMGenerator.readOrGeneratePK(new File("PK"));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public synchronized boolean authenticated(final String username, final String password) {
            return true;
        }

        @Override
        public char[] getPEMAsCharArray() {
            return privateKeyPEMString.toCharArray();
        }
    }

    private class LoggingMonitoringService implements SessionMonitoringService {
        @Override
        public void onSessionUp(final NetconfManagementSession session) {
            LOG.debug("Session {} established", session);
        }

        @Override
        public void onSessionDown(final NetconfManagementSession session) {
            LOG.debug("Session {} down", session);
        }
    }

}
