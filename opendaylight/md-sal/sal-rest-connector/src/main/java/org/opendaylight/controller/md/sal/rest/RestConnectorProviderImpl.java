/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest;

import com.google.common.annotations.Beta;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import javax.ws.rs.core.Response.Status;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Config;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Get;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Operational;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Post;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Put;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.RestConnectorRuntimeMXBean;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Rpcs;
import org.opendaylight.controller.md.sal.rest.common.RestconfValidationUtils;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.api.RestConnector;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.StatisticsRestconfServiceWrapper;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest
 *
 * A class for loading RestConnector bundle and a support functionality for yet.
 * Support functionality means listener for schema context change {@link SchemaContextListener}
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 19, 2015
 */
@Beta
public class RestConnectorProviderImpl implements Provider, RestConnector, SchemaContextListener, AutoCloseable, RestConnectorRuntimeMXBean {

    private final static Logger LOG = LoggerFactory.getLogger(RestConnectorProviderImpl.class);

    private static SchemaContext globalSchema;

    private StatisticsRestconfServiceWrapper stats;
    private ListenerRegistration<SchemaContextListener> listenerRegistration;
    private Thread webSocketServerThread;
    private PortNumber port;

    public static SchemaContext getSchemaContext() {
        RestconfValidationUtils.checkDocumentedError(globalSchema != null, Status.SERVICE_UNAVAILABLE);
        return globalSchema;
    }

    public void setWebsocketPort(final PortNumber port) {
        this.port = port;
    }

    @Override
    public void onSessionInitiated(final ProviderSession session) {
        final SchemaService schemaService = session.getService(SchemaService.class);
        listenerRegistration = schemaService.registerSchemaContextListener(ControllerContext.getInstance());
        globalSchema = schemaService.getGlobalContext();

        // TODO : initialize RestContextHolder
        // TODO : initialize RestBrokerFacade
        // TODO : think about runtime initialization ResourceConfig with @ApplicationPath for web.xml remove

        webSocketServerThread = new Thread(WebSocketServer.createInstance(port.getValue().intValue()));
        webSocketServerThread.setName("Web socket server on port " + port);
        webSocketServerThread.start();

        LOG.info("Rest Connector start successfully.");
    }

    @Override
    public void close() throws Exception {
        if (listenerRegistration != null) {
            listenerRegistration.close();
            listenerRegistration = null;
        }

        WebSocketServer.destroyInstance();
        webSocketServerThread.interrupt();
        webSocketServerThread = null;
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext context) {
        globalSchema = context;
        // TODO we have to provide new context to RestContextHolder too
    }

    @Override
    public Rpcs getRpcs() {
        RestconfValidationUtils.checkDocumentedError(stats != null, Status.SERVICE_UNAVAILABLE);
        final BigInteger rpcInvoke = stats.getRpc();
        final Rpcs rpcs = new Rpcs();
        rpcs.setReceivedRequests(rpcInvoke);
        return rpcs;
    }

    @Override
    public Config getConfig() {
        RestconfValidationUtils.checkDocumentedError(stats != null, Status.SERVICE_UNAVAILABLE);
        final Config config = new Config();
        final Get get = new Get();
        get.setReceivedRequests(stats.getConfigGet());
        config.setGet(get);
        final Post post = new Post();
        post.setReceivedRequests(stats.getConfigPost());
        config.setPost(post);
        final Put put = new Put();
        put.setReceivedRequests(stats.getConfigPut());
        config.setPut(put);
        return config;
    }

    @Override
    public Operational getOperational() {
        RestconfValidationUtils.checkDocumentedError(stats != null, Status.SERVICE_UNAVAILABLE);
        final BigInteger opGet = stats.getOperationalGet();
        final Operational operational = new Operational();
        final Get get = new Get();
        get.setReceivedRequests(opGet);
        operational.setGet(get);
        return operational;
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }
}
