/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.rest.connector.impl;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.controller.config.yang.rest.connector.Config;
import org.opendaylight.controller.config.yang.rest.connector.Delete;
import org.opendaylight.controller.config.yang.rest.connector.Get;
import org.opendaylight.controller.config.yang.rest.connector.Operational;
import org.opendaylight.controller.config.yang.rest.connector.Post;
import org.opendaylight.controller.config.yang.rest.connector.Put;
import org.opendaylight.controller.config.yang.rest.connector.RestConnectorRuntimeMXBean;
import org.opendaylight.controller.config.yang.rest.connector.Rpcs;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.RestConnector;
import org.opendaylight.controller.rest.connector.RestSchemaController;
import org.opendaylight.controller.rest.errors.RestconfDocumentedExceptionMapper;
import org.opendaylight.controller.rest.providers.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.rest.providers.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.rest.providers.NormalizedNodeXmlBodyWriter;
import org.opendaylight.controller.rest.providers.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.rest.schema.SchemaExportContentYangBodyWriter;
import org.opendaylight.controller.rest.schema.SchemaExportContentYinBodyWriter;
import org.opendaylight.controller.rest.services.RestconfStatisticsServiceWrapper;
import org.opendaylight.controller.rest.services.impl.RestconfStatisticsServiceWrapperImpl;
import org.opendaylight.controller.rest.streams.websockets.WebSocketServer;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class RestConnectorProvider implements Provider, AutoCloseable, RestConnector, RestConnectorRuntimeMXBean {

    private static final Logger LOG = LoggerFactory.getLogger(RestConnectorProvider.class);

    private static final String REST_CONNECTOR_BASE_PATH = "/restconf";

    private final BundleContext bundleCx;
    private final PortNumber port;

    private HttpService httpService;
    private Thread webSocketServerThread;
    private ListenerRegistration<SchemaContextListener> listenerRegistration;
    private RestconfStatisticsServiceWrapper servicesAndStatistics;

    /**
     * RestconfProviderImpl constructor
     *
     * @param bundleCx
     * @param port
     */
    public RestConnectorProvider(@Nonnull final BundleContext bundleCx, @Nonnull final PortNumber port) {
        this.bundleCx = Preconditions.checkNotNull(bundleCx);
        this.port = Preconditions.checkNotNull(port);
    }

    @Override
    public void onSessionInitiated(final ProviderSession session) {
        LOG.info("REST_CONNECTOR_BUNDLE: BUNDLE IS STARTING");
        Preconditions.checkState(( ! session.isClosed()), "Session is closed.");

        final DOMDataBroker domDataBroker = Preconditions.checkNotNull(session.getService(DOMDataBroker.class));
        final SchemaService schemaService = Preconditions.checkNotNull(session.getService(SchemaService.class));
        final DOMRpcService rpcService = Preconditions.checkNotNull(session.getService(DOMRpcService.class));
        final DOMMountPointService moutpointService = Preconditions.checkNotNull(session
                .getService(DOMMountPointService.class));

        final RestBrokerFacade broker = new RestBrokerFacadeImpl(domDataBroker);
        broker.setRpcService(rpcService);

        final RestSchemaController rsCr = new RestSchemaControllerImpl();
        rsCr.setGlobalSchema(schemaService.getGlobalContext());
        rsCr.setMountService(moutpointService);


        final ServiceReference<?> ref = bundleCx.getServiceReference(HttpService.class.getName());
        httpService = (HttpService) bundleCx.getService(ref);
        LOG.info("REST_CONNECTOR_BUNDLE: HTTP SERVICE = " + httpService.toString());

        try {
            httpService.registerServlet(REST_CONNECTOR_BASE_PATH, restconfServletInit(broker, rsCr), null, null);
        } catch (ServletException | NamespaceException e) {
            LOG.error("REST_CONNECTOR BUNDLE: unexpected error, restconf servlet was not registred", e);
            return;
        }

        listenerRegistration = schemaService.registerSchemaContextListener(rsCr);
        streamWebSocketInit();

        LOG.info("REST_CONNECTOR_BUNDLE: restconf servlet registered");
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void close() {

        if (httpService != null) {
            LOG.info("REST_CONNECTOR BUNDLE: UNREGISTERING SERVLETS");
            httpService.unregister(REST_CONNECTOR_BASE_PATH);
            LOG.info("REST_CONNECTOR BUNDLE: SERVLETS UNREGISTERED");
        }

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
        get.setReceivedRequests(servicesAndStatistics.getConfigGet());
        get.setSuccessfulResponses(servicesAndStatistics.getSuccessGetConfig());
        get.setFailedResponses(servicesAndStatistics.getFailureGetConfig());
        config.setGet(get);

        final Post post = new Post();
        post.setReceivedRequests(servicesAndStatistics.getConfigPost());
        post.setSuccessfulResponses(servicesAndStatistics.getSuccessPost());
        post.setFailedResponses(servicesAndStatistics.getFailurePost());
        config.setPost(post);

        final Put put = new Put();
        put.setReceivedRequests(servicesAndStatistics.getConfigPut());
        put.setSuccessfulResponses(servicesAndStatistics.getSuccessPut());
        put.setFailedResponses(servicesAndStatistics.getFailurePut());
        config.setPut(put);

        final Delete delete = new Delete();
        delete.setReceivedRequests(servicesAndStatistics.getConfigDelete());
        delete.setSuccessfulResponses(servicesAndStatistics.getSuccessDelete());
        delete.setFailedResponses(servicesAndStatistics.getFailureDelete());
        config.setDelete(delete);

        return config;
    }

    @Override
    public Operational getOperational() {
        final Operational operational = new Operational();
        final BigInteger opGet = servicesAndStatistics.getOperationalGet();
        final Get get = new Get();
        get.setReceivedRequests(opGet);
        get.setSuccessfulResponses(servicesAndStatistics.getSuccessGetOperational());
        get.setFailedResponses(servicesAndStatistics.getFailureGetOperational());
        operational.setGet(get);
        return operational;
    }

    @Override
    public Rpcs getRpcs() {
        final Rpcs rpcs = new Rpcs();
        final BigInteger rpcInvoke = servicesAndStatistics.getRpc();
        rpcs.setReceivedRequests(rpcInvoke);
        return rpcs;
    }

    private ServletContainer restconfServletInit(final RestBrokerFacade br, final RestSchemaController rsCr) {
        servicesAndStatistics = new RestconfStatisticsServiceWrapperImpl(br, rsCr);
        final ResourceConfig rc = new ResourceConfig();
        rc.register(new RestconfDocumentedExceptionMapper(rsCr), ExceptionMapper.class);
        rc.register(new JsonNormalizedNodeBodyReader(rsCr), MessageBodyReader.class);
        rc.register(new XmlNormalizedNodeBodyReader(rsCr), MessageBodyReader.class);
        rc.register(servicesAndStatistics, RestconfStatisticsServiceWrapper.class);
        rc.register(NormalizedNodeJsonBodyWriter.class);
        rc.register(NormalizedNodeXmlBodyWriter.class);
        rc.register(SchemaExportContentYinBodyWriter.class);
        rc.register(SchemaExportContentYangBodyWriter.class);
        rc.register(RestconfCORSFilter.class);
        EncodingFilter.enableFor(rc, GZipEncoder.class);

        return new ServletContainer(rc);
    }

    private void streamWebSocketInit() {
        webSocketServerThread = new Thread(WebSocketServer.createInstance(port.getValue().intValue()));
        webSocketServerThread.setName("Web socket server on port " + port);
        webSocketServerThread.start();
    }
}