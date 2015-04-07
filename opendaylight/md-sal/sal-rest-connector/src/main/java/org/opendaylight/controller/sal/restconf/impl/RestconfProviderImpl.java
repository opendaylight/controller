/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Config;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Delete;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Failure;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Get;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Operational;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Post;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Put;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Success;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.RestConnectorRuntimeMXBean;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Rpcs;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.ResponseStat;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.api.RestConnector;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public class RestconfProviderImpl implements Provider, AutoCloseable, RestConnector, RestConnectorRuntimeMXBean {

    private final StatisticsRestconfServiceWrapper stats = StatisticsRestconfServiceWrapper.getInstance();
    private ListenerRegistration<SchemaContextListener> listenerRegistration;
    private PortNumber port;
    private Thread webSocketServerThread;

    public void setWebsocketPort(final PortNumber port) {
        this.port = port;
    }

    @Override
    public void onSessionInitiated(final ProviderSession session) {
        final DOMDataBroker domDataBroker = session.getService(DOMDataBroker.class);

        BrokerFacade.getInstance().setContext(session);
        BrokerFacade.getInstance().setDomDataBroker( domDataBroker);
        final SchemaService schemaService = session.getService(SchemaService.class);
        listenerRegistration = schemaService.registerSchemaContextListener(ControllerContext.getInstance());
        BrokerFacade.getInstance().setRpcService(session.getService(DOMRpcService.class));


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
        final BigInteger opGet = stats.getOperationalGet();
        final Operational operational = new Operational();
        final Get get = new Get();
        get.setReceivedRequests(opGet);
        operational.setGet(get);
        return operational;
    }

    @Override
    public Rpcs getRpcs() {
        final BigInteger rpcInvoke = stats.getRpc();
        final Rpcs rpcs = new Rpcs();
        rpcs.setReceivedRequests(rpcInvoke);
        return rpcs ;
    }

    @Override
    public ResponseStat getResponseStat() {
        final ResponseStat responseStat = new ResponseStat();
        final Get get = new Get();
        get.setReceivedRequests(stats.getResponseStatGet());
        responseStat.setGet(get);
        final Success success = new Success();
        final Post postSuccess = new Post();
        postSuccess.setReceivedRequests(stats.getResponseStatPostSuccess());
        success.setPost(postSuccess);
        final Put putSuccess = new Put();
        putSuccess.setReceivedRequests(stats.getResponseStatPutSuccess());
        success.setPut(putSuccess);
        final Delete deleteSuccess = new Delete();
        deleteSuccess.setReceivedRequests(stats.getResponseStatDeleteSuccess());
        success.setDelete(deleteSuccess);
        responseStat.setSuccess(success);
        final Failure failure = new Failure();
        final Post postFailure = new Post();
        postFailure.setReceivedRequests(stats.getResponseStatPostFailure());
        failure.setPost(postFailure);
        final Put putFailure = new Put();
        putFailure.setReceivedRequests(stats.getResponseStatPutFailure());
        failure.setPut(putFailure);
        final Delete deleteFailure = new Delete();
        deleteFailure.setReceivedRequests(stats.getResponseStatDeleteFailure());
        failure.setDelete(deleteFailure);
        responseStat.setFailure(failure);
        return responseStat;
    }
}