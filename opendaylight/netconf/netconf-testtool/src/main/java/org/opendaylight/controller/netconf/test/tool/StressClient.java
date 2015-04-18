/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

import ch.qos.logback.classic.Level;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.EditConfigInput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public final class StressClient {

    private static final Logger LOG = LoggerFactory.getLogger(StressClient.class);

    public static class Params {

        @Arg(dest = "ip")
        public String ip;

        @Arg(dest = "port")
        public int port;

        @Arg(dest = "edit-count")
        public int editCount;

        @Arg(dest = "edit-content")
        public File editContent;

        @Arg(dest = "edit-batch-size")
        public int editBatchSize;

        @Arg(dest = "debug")
        public boolean debug;


        static ArgumentParser getParser() {
            final ArgumentParser parser = ArgumentParsers.newArgumentParser("netconf stress client");

            parser.description("Netconf stress client");

            parser.addArgument("--ip")
                    .type(String.class)
                    .setDefault("127.0.0.1")
                    .type(String.class)
                    .help("Netconf server IP")
                    .dest("ip");

            parser.addArgument("--port")
                    .type(Integer.class)
                    .setDefault(2830)
                    .type(Integer.class)
                    .help("Netconf server port")
                    .dest("port");

            parser.addArgument("--edits")
                    .type(Integer.class)
                    .setDefault(100)
                    .type(Integer.class)
                    .help("Netconf edit rpcs to be sent")
                    .dest("edit-count");

            parser.addArgument("--edit-content")
                    .type(File.class)
                    .setDefault(new File("edit.txt"))
                    .type(File.class)
                    .help("Netconf edit rpc content")
                    .dest("edit-content");

            parser.addArgument("--edit-batch-size")
                    .type(Integer.class)
                    .required(false)
                    .setDefault(-1)
                    .type(Integer.class)
                    .help("Netconf commit frequency")
                    .dest("edit-batch-size");

            parser.addArgument("--debug")
                    .type(Boolean.class)
                    .setDefault(false)
                    .help("Whether to use debug log level instead of INFO")
                    .dest("debug");

            // TODO add get-config option instead of edit + commit
            // TODO SSH option
            // TODO different edit config content
            // TODO optional message prepare
            // TODO option for synchronous/asynchronous message sending
            // TODO add thread option

            return parser;
        }

        void validate() {
            Preconditions.checkArgument(port > 0, "Port =< 0");
            Preconditions.checkArgument(editCount > 0, "Edit count =< 0");
            if (editBatchSize == -1) {
                editBatchSize = editCount;
            } else {
                Preconditions.checkArgument(editBatchSize <= editCount, "Edit count =< 0");
            }

            Preconditions.checkArgument(editContent.exists(), "Edit content file missing");
            Preconditions.checkArgument(editContent.isDirectory() == false, "Edit content file is a dir");
            Preconditions.checkArgument(editContent.canRead(), "Edit content file is unreadable");
            // TODO validate
        }

        public InetSocketAddress getInetAddress() {
            try {
                return new InetSocketAddress(InetAddress.getByName(ip), port);
            } catch (final UnknownHostException e) {
                throw new IllegalArgumentException("Unknown ip", e);
            }
        }
    }

    private static final QName editConfigQName = QName.create(EditConfigInput.QNAME, "edit-config");
    private static final org.w3c.dom.Document editBlueprint;

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
        final Params params = parseArgs(args, Params.getParser());
        params.validate();

        try {
            Thread.sleep(10000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(params.debug ? Level.DEBUG : Level.INFO);

        // Prepare all msgs up front
        final List<NetconfMessage> preparedMessages = Lists.newArrayListWithCapacity(params.editCount);
        for (int i = 0; i < params.editCount; i++) {
            final Document msg = XmlUtil.createDocumentCopy(editBlueprint);
            // TODO cannot be i as message id for multiple batches
            msg.getDocumentElement().setAttribute("message-id", Integer.toString(i));
            final NetconfMessage netconfMessage = new NetconfMessage(msg);
            preparedMessages.add(netconfMessage);
        }

        final Element editContent;
        try {
            editContent = XmlUtil.readXmlToElement(params.editContent);
            final Node config = ((Element) editBlueprint.getDocumentElement().getElementsByTagName("edit-config").item(0)).
                    getElementsByTagName("config").item(0);
            config.appendChild(editBlueprint.importNode(editContent, true));
        } catch (final IOException | SAXException e) {
            throw new IllegalArgumentException("Edit content file is unreadable", e);
        }

        // TODO make thread size configurable
        final NioEventLoopGroup eventExecutors = new NioEventLoopGroup();
        final Timer timer = new HashedWheelTimer();
        final NetconfClientDispatcherImpl netconfClientDispatcher = new NetconfClientDispatcherImpl(eventExecutors, eventExecutors, timer);

        final NetconfDeviceCommunicator sessionListener = getSessionListener();

        final NetconfClientConfiguration cfg = getNetconfClientConfiguration(params, sessionListener);

        final NetconfClientSession netconfClientSession;
        try {
            netconfClientSession = netconfClientDispatcher.createClient(cfg).get();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        } catch (final ExecutionException e) {
            throw new RuntimeException("Unable to connect", e);
        }

        final AtomicInteger responseCounter = new AtomicInteger(0);
        final List<ListenableFuture<RpcResult<NetconfMessage>>> futures = Lists.newArrayList();

        final List<Integer> editBatches = countEditBatchSizes(params);

        final Stopwatch started = Stopwatch.createStarted();
        int batchI = 0;
        for (final Integer editBatch : editBatches) {
            for (int i = 0; i < editBatch; i++) {
                final int msgId = i + (batchI * params.editBatchSize);
                final NetconfMessage msg = preparedMessages.get(msgId);
                LOG.debug("Sending message {}", msgId);
                if(LOG.isTraceEnabled()) {
                    LOG.trace("Sending message {}", XmlUtil.toString(msg.getDocument()));
                }
                final ListenableFuture<RpcResult<NetconfMessage>> netconfMessageFuture = sessionListener.sendRequest(msg, editConfigQName);
                futures.add(netconfMessageFuture);
            }
            batchI++;

            // TODO Commit
        }

        // Wait for every future
        for (final ListenableFuture<RpcResult<NetconfMessage>> future : futures) {
            try {
                final RpcResult<NetconfMessage> netconfMessageRpcResult = future.get(1L, TimeUnit.MINUTES);
                if(netconfMessageRpcResult.isSuccessful()) {
                    responseCounter.incrementAndGet();
                    LOG.debug("Received response {}", responseCounter.get());
                } else {
                    LOG.warn("Request failed {}", netconfMessageRpcResult);
                }
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            } catch (final ExecutionException | TimeoutException e) {
                throw new RuntimeException("Request not finished", e);
            }
        }

        Preconditions.checkState(responseCounter.get() == params.editCount, "Not all responses were received, only %s from %s", responseCounter.get(), params.editCount);

        started.stop();
        LOG.info("Execution time: {}", started);
        LOG.info("Requests per second: {}", (params.editCount * 1000.0 / started.elapsed(TimeUnit.MILLISECONDS)));

        // Cleanup
        netconfClientSession.close();
        timer.stop();
        try {
            eventExecutors.shutdownGracefully().get(20L, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Unable to close executor properly", e);
        }
    }

    private static List<Integer> countEditBatchSizes(final Params params) {
        final List<Integer> editBatches = Lists.newArrayList();
        if (params.editBatchSize != params.editCount) {
            final int fullBatches = params.editCount / params.editBatchSize;
            for (int i = 0; i < fullBatches; i++) {
                editBatches.add(params.editBatchSize);
            }

            if (params.editCount % params.editBatchSize != 0) {
                editBatches.add(params.editCount % params.editBatchSize);
            }
        } else {
            editBatches.add(params.editBatchSize);
        }
        return editBatches;
    }

    private static NetconfClientConfiguration getNetconfClientConfiguration(final Params params, final NetconfDeviceCommunicator sessionListener) {
        final NetconfClientConfigurationBuilder netconfClientConfigurationBuilder = NetconfClientConfigurationBuilder.create();
        netconfClientConfigurationBuilder.withSessionListener(sessionListener);
        netconfClientConfigurationBuilder.withAddress(params.getInetAddress());
        // TODO Make configurable protocol
        netconfClientConfigurationBuilder.withProtocol(NetconfClientConfiguration.NetconfClientProtocol.TCP);
        netconfClientConfigurationBuilder.withConnectionTimeoutMillis(20000L);
        netconfClientConfigurationBuilder.withReconnectStrategy(new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000));
        return netconfClientConfigurationBuilder.build();
    }

    static NetconfDeviceCommunicator getSessionListener() {
        final RemoteDevice<NetconfSessionPreferences, NetconfMessage, NetconfDeviceCommunicator> loggingRemoteDevice = new LoggingRemoteDevice();
        // TODO add inet address
        return new NetconfDeviceCommunicator(new RemoteDeviceId("secure-test"), loggingRemoteDevice);
    }

    private static Params parseArgs(final String[] args, final ArgumentParser parser) {
        final Params opt = new Params();
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
