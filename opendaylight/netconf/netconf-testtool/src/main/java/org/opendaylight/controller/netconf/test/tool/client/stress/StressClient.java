/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool.client.stress;

import ch.qos.logback.classic.Level;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.CommitInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.EditConfigInput;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public final class StressClient {

    private static final Logger LOG = LoggerFactory.getLogger(StressClient.class);

    static final QName COMMIT_QNAME = QName.create(CommitInput.QNAME, "commit");
    public static final NetconfMessage COMMIT_MSG;

    static {
        try {
            COMMIT_MSG = new NetconfMessage(XmlUtil.readXmlToDocument("<rpc message-id=\"commit-batch\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                    "    <commit/>\n" +
                    "</rpc>"));
        } catch (SAXException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static final QName EDIT_QNAME = QName.create(EditConfigInput.QNAME, "edit-config");
    static final org.w3c.dom.Document editBlueprint;

    static {
        try {
            editBlueprint = XmlUtil.readXmlToDocument(
                    "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                            "    <edit-config xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                            "        <target>\n" +
                            "            <candidate/>\n" +
                            "        </target>\n" +
                            "        <config/>\n" +
                            "    </edit-config>\n" +
                            "</rpc>");
        } catch (SAXException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(final String[] args) {
        final Parameters params = parseArgs(args, Parameters.getParser());
        params.validate();

        // TODO remove
        try {
            Thread.sleep(10000);
        } catch (final InterruptedException e) {
//            e.printStackTrace();
        }

        final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(params.debug ? Level.DEBUG : Level.INFO);

        LOG.info("Preparing messages");
        // Prepare all msgs up front
        final List<NetconfMessage> preparedMessages = Lists.newArrayListWithCapacity(params.editCount);
        for (int i = 0; i < params.editCount; i++) {
            final Document msg = XmlUtil.createDocumentCopy(editBlueprint);
            msg.getDocumentElement().setAttribute("message-id", Integer.toString(i));
            final NetconfMessage netconfMessage = new NetconfMessage(msg);


            final Element editContent;
            try {
                editContent = XmlUtil.readXmlToElement(params.editContent);
                final Node config = ((Element) msg.getDocumentElement().getElementsByTagName("edit-config").item(0)).
                        getElementsByTagName("config").item(0);
                config.appendChild(msg.importNode(editContent, true));
            } catch (final IOException | SAXException e) {
                throw new IllegalArgumentException("Edit content file is unreadable", e);
            }

            preparedMessages.add(netconfMessage);

        }


        final NioEventLoopGroup nioGroup = new NioEventLoopGroup();
        final Timer timer = new HashedWheelTimer();

        final NetconfClientDispatcherImpl netconfClientDispatcher = configureClientDispatcher(params, nioGroup, timer);

        final NetconfDeviceCommunicator sessionListener = getSessionListener(params.getInetAddress());

        final NetconfClientConfiguration cfg = getNetconfClientConfiguration(params, sessionListener);

        LOG.info("Connecting to netconf server {}:{}", params.ip, params.port);
        final NetconfClientSession netconfClientSession;
        try {
            netconfClientSession = netconfClientDispatcher.createClient(cfg).get();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        } catch (final ExecutionException e) {
            throw new RuntimeException("Unable to connect", e);
        }

        LOG.info("Starting stress test");
        final Stopwatch started = Stopwatch.createStarted();
        getExecutionStrategy(params, preparedMessages, sessionListener).invoke();
        started.stop();

        LOG.info("FINISHED. Execution time: {}", started);
        LOG.info("Requests per second: {}", (params.editCount * 1000.0 / started.elapsed(TimeUnit.MILLISECONDS)));

        // Cleanup
        netconfClientSession.close();
        timer.stop();
        try {
            nioGroup.shutdownGracefully().get(20L, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Unable to close executor properly", e);
        }
    }

    private static ExecutionStrategy getExecutionStrategy(final Parameters params, final List<NetconfMessage> preparedMessages, final NetconfDeviceCommunicator sessionListener) {
        if(params.async) {
            return new AsyncExecutionStrategy(params, preparedMessages, sessionListener);
        } else {
            return new SyncExecutionStrategy(params, preparedMessages, sessionListener);
        }
    }

    private static NetconfClientDispatcherImpl configureClientDispatcher(final Parameters params, final NioEventLoopGroup nioGroup, final Timer timer) {
        final NetconfClientDispatcherImpl netconfClientDispatcher;
        if(params.exi) {
            if(params.legacyFraming) {
                netconfClientDispatcher= ConfigurableClientDispatcher.createLegacyExi(nioGroup, nioGroup, timer);
            } else {
                netconfClientDispatcher = ConfigurableClientDispatcher.createChunkedExi(nioGroup, nioGroup, timer);
            }
        } else {
            if(params.legacyFraming) {
                netconfClientDispatcher = ConfigurableClientDispatcher.createLegacy(nioGroup, nioGroup, timer);
            } else {
                netconfClientDispatcher = ConfigurableClientDispatcher.createChunked(nioGroup, nioGroup, timer);
            }
        }
        return netconfClientDispatcher;
    }

    private static NetconfClientConfiguration getNetconfClientConfiguration(final Parameters params, final NetconfDeviceCommunicator sessionListener) {
        final NetconfClientConfigurationBuilder netconfClientConfigurationBuilder = NetconfClientConfigurationBuilder.create();
        netconfClientConfigurationBuilder.withSessionListener(sessionListener);
        netconfClientConfigurationBuilder.withAddress(params.getInetAddress());
        netconfClientConfigurationBuilder.withProtocol(params.ssh ? NetconfClientConfiguration.NetconfClientProtocol.SSH : NetconfClientConfiguration.NetconfClientProtocol.TCP);
        netconfClientConfigurationBuilder.withConnectionTimeoutMillis(20000L);
        netconfClientConfigurationBuilder.withReconnectStrategy(new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000));
        return netconfClientConfigurationBuilder.build();
    }

    static NetconfDeviceCommunicator getSessionListener(final InetSocketAddress inetAddress) {
        final RemoteDevice<NetconfSessionPreferences, NetconfMessage, NetconfDeviceCommunicator> loggingRemoteDevice = new LoggingRemoteDevice();
        return new NetconfDeviceCommunicator(new RemoteDeviceId("secure-test", inetAddress), loggingRemoteDevice);
    }

    private static Parameters parseArgs(final String[] args, final ArgumentParser parser) {
        final Parameters opt = new Parameters();
        try {
            parser.parseArgs(args, opt);
            return opt;
        } catch (final ArgumentParserException e) {
            parser.handleError(e);
        }

        System.exit(1);
        return null;
    }


    private static class LoggingRemoteDevice implements RemoteDevice<NetconfSessionPreferences, NetconfMessage, NetconfDeviceCommunicator> {
        @Override
        public void onRemoteSessionUp(final NetconfSessionPreferences remoteSessionCapabilities, final NetconfDeviceCommunicator netconfDeviceCommunicator) {
            LOG.info("Session established");
        }

        @Override
        public void onRemoteSessionDown() {
            LOG.info("Session down");
        }

        @Override
        public void onRemoteSessionFailed(final Throwable throwable) {
            LOG.info("Session failed");
        }

        @Override
        public void onNotification(final NetconfMessage notification) {
            LOG.info("Notification received: {}", notification.toString());
        }
    }

}
