/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.controller.config.yang.md.sal.rest.connector.Config;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Get;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Operational;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Post;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Put;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.RestConnectorRuntimeMXBean;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Rpcs;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.api.RestConnector;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;

public class RestconfProviderImpl implements Provider, AutoCloseable, RestConnector, RestConnectorRuntimeMXBean {

    private final StatisticsRestconfServiceWrapper stats = StatisticsRestconfServiceWrapper.getInstance();
    private ListenerRegistration<SchemaContextListener> listenerRegistration;
    private PortNumber port;
    private Thread webSocketServerThread;

    public void setWebsocketPort(PortNumber port) {
        this.port = port;
    }

    @Override
    public void onSessionInitiated(ProviderSession session) {
        final DOMDataBroker domDataBroker = session.getService(DOMDataBroker.class);

        BrokerFacade.getInstance().setContext(session);
        BrokerFacade.getInstance().setDomDataBroker( domDataBroker);
        domDataBroker.newReadWriteTransaction()

        SchemaService schemaService = session.getService(SchemaService.class);
        listenerRegistration = schemaService.registerSchemaContextListener(ControllerContext.getInstance());
        ControllerContext.getInstance().setSchemas(schemaService.getGlobalContext());
        ControllerContext.getInstance().setMountService(session.getService(DOMMountPointService.class));

        webSocketServerThread = new Thread(WebSocketServer.createInstance(port.getValue().intValue()));
        webSocketServerThread.setName("Web socket server on port " + port);
        webSocketServerThread.start();
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void close() {

        if (listenerRegistration != null) {
            listenerRegistration.close();
        }

        WebSocketServer.destroyInstance();
        webSocketServerThread.interrupt();
    }

    @Override
    public Config getConfig() {
        Config config = new Config();
        Get get = new Get();
        get.setReceivedRequests(stats.getConfigGet());
        config.setGet(get);
        Post post = new Post();
        post.setReceivedRequests(stats.getConfigPost());
        config.setPost(post);
        Put put = new Put();
        put.setReceivedRequests(stats.getConfigPut());
        config.setPut(put);
        return config;
    }

    @Override
    public Operational getOperational() {
        BigInteger opGet = stats.getOperationalGet();
        Operational operational = new Operational();
        Get get = new Get();
        get.setReceivedRequests(opGet);
        operational.setGet(get);
        return operational;
    }

    @Override
    public Rpcs getRpcs() {
        BigInteger rpcInvoke = stats.getRpc();
        Rpcs rpcs = new Rpcs();
        rpcs.setReceivedRequests(rpcInvoke);
        return rpcs ;
    }
}
