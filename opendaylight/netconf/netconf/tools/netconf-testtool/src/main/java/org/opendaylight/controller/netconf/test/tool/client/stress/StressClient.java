/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool.client.stress;

import ch.qos.logback.classic.Level;
import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.client.AsyncSshHandler;
import org.opendaylight.controller.netconf.test.tool.TestToolUtils;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
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
                            "        <default-operation>none</default-operation>" +
                            "        <config/>\n" +
                            "    </edit-config>\n" +
                            "</rpc>");
        } catch (SAXException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final String MSG_ID_PLACEHOLDER_REGEX = "\\{MSG_ID\\}";
    private static final String PHYS_ADDR_PLACEHOLDER = "{PHYS_ADDR}";

    private static long macStart = 0xAABBCCDD0000L;

    public static void main(final String[] args) {

        final Parameters params = parseArgs(args, Parameters.getParser());
        params.validate();

        final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(params.debug ? Level.DEBUG : Level.INFO);

        final int threadAmount = params.threadAmount;
        LOG.info("thread amount: " + threadAmount);
        final int requestsPerThread = params.editCount / params.threadAmount;
        LOG.info("requestsPerThread: " + requestsPerThread);
        final int leftoverRequests = params.editCount % params.threadAmount;
        LOG.info("leftoverRequests: " + leftoverRequests);


        LOG.info("Preparing messages");
        // Prepare all msgs up front
        final List<List<NetconfMessage>> allPreparedMessages = new ArrayList<>(threadAmount);
        for (int i = 0; i < threadAmount; i++) {
            if (i != threadAmount - 1) {
                allPreparedMessages.add(new ArrayList<NetconfMessage>(requestsPerThread));
            } else {
                allPreparedMessages.add(new ArrayList<NetconfMessage>(requestsPerThread + leftoverRequests));
            }
        }


        final String editContentString;
        try {
            editContentString = Files.toString(params.editContent, Charsets.UTF_8);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Cannot read content of " + params.editContent);
        }

        for (int i = 0; i < threadAmount; i++) {
            final List<NetconfMessage> preparedMessages = allPreparedMessages.get(i);
            int padding = 0;
            if (i == threadAmount - 1) {
                padding = leftoverRequests;
            }
            for (int j = 0; j < requestsPerThread + padding; j++) {
                LOG.debug("id: " + (i * requestsPerThread + j));
                preparedMessages.add(prepareMessage(i * requestsPerThread + j, editContentString));
            }
        }

        final NioEventLoopGroup nioGroup = new NioEventLoopGroup();
        final Timer timer = new HashedWheelTimer();

        final NetconfClientDispatcherImpl netconfClientDispatcher = configureClientDispatcher(params, nioGroup, timer);

        final List<StressClientCallable> callables = new ArrayList<>(threadAmount);
        for (final List<NetconfMessage> messages : allPreparedMessages) {
            callables.add(new StressClientCallable(params, netconfClientDispatcher, messages));
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(threadAmount);

        LOG.info("Starting stress test");
        final Stopwatch started = Stopwatch.createStarted();
        try {
            final List<Future<Boolean>> futures = executorService.invokeAll(callables);
            for (final Future<Boolean> future : futures) {
                try {
                    future.get(4L, TimeUnit.MINUTES);
                } catch (ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            }
            executorService.shutdownNow();
        } catch (final InterruptedException e) {
            throw new RuntimeException("Unable to execute requests", e);
        }
        started.stop();

        LOG.info("FINISHED. Execution time: {}", started);
        LOG.info("Requests per second: {}", (params.editCount * 1000.0 / started.elapsed(TimeUnit.MILLISECONDS)));

        // Cleanup
        timer.stop();
        try {
            nioGroup.shutdownGracefully().get(20L, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Unable to close executor properly", e);
        }
        //stop the underlying ssh thread that gets spawned if we use ssh
        if (params.ssh) {
            AsyncSshHandler.DEFAULT_CLIENT.stop();
        }
    }

    static NetconfMessage prepareMessage(final int id, final String editContentString) {
        final Document msg = XmlUtil.createDocumentCopy(editBlueprint);
        msg.getDocumentElement().setAttribute("message-id", Integer.toString(id));
        final NetconfMessage netconfMessage = new NetconfMessage(msg);

        final Element editContentElement;
        try {
            // Insert message id where needed
            String specificEditContent = editContentString.replaceAll(MSG_ID_PLACEHOLDER_REGEX, Integer.toString(id));

            final StringBuilder stringBuilder = new StringBuilder(specificEditContent);
            int idx = stringBuilder.indexOf(PHYS_ADDR_PLACEHOLDER);
            while (idx!= -1) {
                stringBuilder.replace(idx, idx + PHYS_ADDR_PLACEHOLDER.length(), TestToolUtils.getMac(macStart++));
                idx = stringBuilder.indexOf(PHYS_ADDR_PLACEHOLDER);
            }
            specificEditContent = stringBuilder.toString();

            editContentElement = XmlUtil.readXmlToElement(specificEditContent);
            final Node config = ((Element) msg.getDocumentElement().getElementsByTagName("edit-config").item(0)).
                    getElementsByTagName("config").item(0);
            config.appendChild(msg.importNode(editContentElement, true));
        } catch (final IOException | SAXException e) {
            throw new IllegalArgumentException("Edit content file is unreadable", e);
        }

        return netconfMessage;
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


    static class LoggingRemoteDevice implements RemoteDevice<NetconfSessionPreferences, NetconfMessage, NetconfDeviceCommunicator> {
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
