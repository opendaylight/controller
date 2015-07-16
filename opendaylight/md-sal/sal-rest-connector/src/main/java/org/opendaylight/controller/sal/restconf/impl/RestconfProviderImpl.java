/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.RestConnectorRuntimeMXBean;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.rest.schema.SchemaExportContentYangBodyWriter;
import org.opendaylight.controller.md.sal.rest.schema.SchemaExportContentYinBodyWriter;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.api.RestConnector;
import org.opendaylight.controller.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeXmlBodyWriter;
import org.opendaylight.controller.sal.rest.impl.RestconfApplication;
import org.opendaylight.controller.sal.rest.impl.RestconfDocumentedExceptionMapper;
import org.opendaylight.controller.sal.rest.impl.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class RestconfProviderImpl implements Provider, AutoCloseable, RestConnector, RestConnectorRuntimeMXBean {

    private static final Logger LOG = LoggerFactory.getLogger(RestconfProviderImpl.class);

    private final BundleContext bundleCx;
    private final PortNumber port;

    private HttpService httpService;
    private ServiceTracker restconfHttpServiceTracker;
    private ServletContainer restconfServletContainer;
    private Thread webSocketServerThread;
    private ListenerRegistration<SchemaContextListener> listenerRegistration;

    // private StatisticsRestconfServiceWrapper stats;

    /**
     * RestconfProviderImpl constructor
     *
     * @param bundleCx
     * @param port
     */
    public RestconfProviderImpl(@Nonnull final BundleContext bundleCx, @Nonnull final PortNumber port) {
        this.bundleCx = Preconditions.checkNotNull(bundleCx);
        this.port = Preconditions.checkNotNull(port);
    }

    @Override
    public void onSessionInitiated(final ProviderSession session) {
        Preconditions.checkState(( ! session.isClosed()), "Session is closed.");

        final DOMDataBroker domDataBroker = Preconditions.checkNotNull(session.getService(DOMDataBroker.class));
        final SchemaService schemaService = Preconditions.checkNotNull(session.getService(SchemaService.class));
        final DOMRpcService rpcService = Preconditions.checkNotNull(session.getService(DOMRpcService.class));
        final DOMMountPointService moutpointService = Preconditions.checkNotNull(session
                .getService(DOMMountPointService.class));

        // FIXME: ControllerContext is still singleton

        final BrokerFacade brokerFacade = new BrokerFacade(domDataBroker);
        brokerFacade.setRpcService(rpcService);

        listenerRegistration = schemaService.registerSchemaContextListener(ControllerContext.getInstance());

        ControllerContext.getInstance().setSchemas(schemaService.getGlobalContext());
        ControllerContext.getInstance().setMountService(moutpointService);

        // Note: ServiceTracker
        final ServiceReference<?> ref = bundleCx.getServiceReference(HttpService.class.getName());
        httpService = (HttpService) bundleCx.getService(ref);
        final ServiceTracker tracker = new ServiceTracker(this.bundleCx, HttpService.class.getName(), null) {
            @Override
            public Object addingService(final ServiceReference serviceRef) {
                httpService = (HttpService) super.addingService(serviceRef);
                registerServlets();
                return httpService;
            }

            @Override
            public void removedService(final ServiceReference ref, final Object service) {
                if (httpService == service) {
                    unregisterServlets();
                    httpService = null;
                }
                super.removedService(ref, service);
            }
        };
        tracker.open();

        // Note: Direct add HttpServices

        // final ServiceRegistration<RestconfDocumentedExceptionMapper> reg1 = bundleCx.registerService(
        // RestconfDocumentedExceptionMapper.class, new RestconfDocumentedExceptionMapper(), null);
        // final ServiceRegistration<XmlNormalizedNodeBodyReader> reg2 = bundleCx.registerService(
        // XmlNormalizedNodeBodyReader.class, new XmlNormalizedNodeBodyReader(), null);
        // final ServiceRegistration<JsonNormalizedNodeBodyReader> reg3 = bundleCx.registerService(
        // JsonNormalizedNodeBodyReader.class, new JsonNormalizedNodeBodyReader(), null);
        // final ServiceRegistration<NormalizedNodeJsonBodyWriter> reg4 = bundleCx.registerService(
        // NormalizedNodeJsonBodyWriter.class, new NormalizedNodeJsonBodyWriter(), null);
        // final ServiceRegistration<SchemaExportContentYinBodyWriter> reg5 = bundleCx.registerService(
        // SchemaExportContentYinBodyWriter.class, new SchemaExportContentYinBodyWriter(), null);
        // final ServiceRegistration<SchemaExportContentYangBodyWriter> reg6 = bundleCx.registerService(
        // SchemaExportContentYangBodyWriter.class, new SchemaExportContentYangBodyWriter(), null);
        // final ServiceRegistration<RestconfService> reg7 = bundleCx.registerService(RestconfService.class,
        // new RestconfImpl(brokerFacade, ControllerContext.getInstance()), null);

        // Note: Servlet approach

        // final ServiceReference<?> ref = bundleCx.getServiceReference(HttpService.class.getName());
        // final HttpService service = (HttpService) bundleCx.getService(ref);
        //
        // final ResourceConfig rc = new ResourceConfig();
        // rc.register(RestconfDocumentedExceptionMapper.class);
        // rc.register(XmlNormalizedNodeBodyReader.class);
        // rc.register(JsonNormalizedNodeBodyReader.class);
        // rc.register(NormalizedNodeJsonBodyWriter.class);
        // rc.register(NormalizedNodeXmlBodyWriter.class);
        // rc.register(SchemaExportContentYinBodyWriter.class);
        // rc.register(SchemaExportContentYangBodyWriter.class);
        // rc.register(RestconfImpl.class);
        //
        // final ServletContainer servlContainer = new ServletContainer(rc);
        // try {
        // service.registerServlet("/restconf", servlContainer, null, null);
        // } catch (final Exception ex) {
        // // noop
        // }


        streamWebSocketInitialization();
    }

    private void unregisterServlets() {
        if (this.httpService != null) {
            LOG.info("JERSEY BUNDLE: UNREGISTERING SERVLETS");
            httpService.unregister("/jersey-http-service");
            LOG.info("JERSEY BUNDLE: SERVLETS UNREGISTERED");
        }
    }

    private void registerServlets() {
        try {
            rawRegisterServlets();
        } catch (InterruptedException | NamespaceException | ServletException ie) {
            throw new RuntimeException(ie);
        }
    }

    private void rawRegisterServlets() throws ServletException, NamespaceException, InterruptedException {
        LOG.info("JERSEY BUNDLE: REGISTERING SERVLETS");
        LOG.info("JERSEY BUNDLE: HTTP SERVICE = " + httpService.toString());
        // TODO - temporary workaround
        // This is a workaround related to issue JERSEY-2093; grizzly (1.9.5) needs to have the correct context
        // classloader set
        final ClassLoader myClassLoader = getClass().getClassLoader();
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(myClassLoader);
            httpService.registerServlet("/jersey-http-service", new ServletContainer(), getJerseyServletParams(), null);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
        // END of workaround - after grizzly updated to the recent version, only the inner call from try block will
        // remain:
        // httpService.registerServlet("/jersey-http-service", new ServletContainer(), getJerseyServletParams(), null);
        sendAdminEvent();
        LOG.info("JERSEY BUNDLE: SERVLETS REGISTERED");
    }

    private void sendAdminEvent() {
        final ServiceReference eaRef = bundleCx.getServiceReference(EventAdmin.class.getName());
        // if (eaRef != null) {
        // final EventAdmin ea = (EventAdmin) bundleCx.getService(eaRef);
        // ea.sendEvent(new Event("restconf/", new HashMap<String, String>() {
        // {
        // put("context-path", "/");
        // }
        // }));
        // bundleCx.ungetService(eaRef);
        // }
    }

    @SuppressWarnings("UseOfObsoleteCollectionType")
    private Dictionary<String, String> getJerseyServletParams() {
        final Dictionary<String, String> jerseyServletParams = new Hashtable<>();
        // jerseyServletParams.put("javax.ws.rs.Application", JerseyApplication.class.getName());
        jerseyServletParams.put("javax.ws.rs.Application", RestconfApplication.class.getName());
        return jerseyServletParams;
    }

    class JerseyApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            final Set<Class<?>> result = new HashSet<Class<?>>();
            result.add(RestconfDocumentedExceptionMapper.class);
            result.add(XmlNormalizedNodeBodyReader.class);
            result.add(JsonNormalizedNodeBodyReader.class);
            result.add(NormalizedNodeJsonBodyWriter.class);
            result.add(NormalizedNodeXmlBodyWriter.class);
            result.add(SchemaExportContentYinBodyWriter.class);
            result.add(SchemaExportContentYangBodyWriter.class);
            // result.add(RestconfImpl.class);
            return result;
        }
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
    public org.opendaylight.controller.config.yang.md.sal.rest.connector.Config getConfig() {
        final org.opendaylight.controller.config.yang.md.sal.rest.connector.Config config = new org.opendaylight.controller.config.yang.md.sal.rest.connector.Config();

        // final Get get = new Get();
        // get.setReceivedRequests(stats.getConfigGet());
        // get.setSuccessfulResponses(stats.getSuccessGetConfig());
        // get.setFailedResponses(stats.getFailureGetConfig());
        // config.setGet(get);
        //
        // final Post post = new Post();
        // post.setReceivedRequests(stats.getConfigPost());
        // post.setSuccessfulResponses(stats.getSuccessPost());
        // post.setFailedResponses(stats.getFailurePost());
        // config.setPost(post);
        //
        // final Put put = new Put();
        // put.setReceivedRequests(stats.getConfigPut());
        // put.setSuccessfulResponses(stats.getSuccessPut());
        // put.setFailedResponses(stats.getFailurePut());
        // config.setPut(put);
        //
        // final Delete delete = new Delete();
        // delete.setReceivedRequests(stats.getConfigDelete());
        // delete.setSuccessfulResponses(stats.getSuccessDelete());
        // delete.setFailedResponses(stats.getFailureDelete());
        // config.setDelete(delete);

        return config;
    }

    @Override
    public org.opendaylight.controller.config.yang.md.sal.rest.connector.Operational getOperational() {
        final org.opendaylight.controller.config.yang.md.sal.rest.connector.Operational operational = new org.opendaylight.controller.config.yang.md.sal.rest.connector.Operational();
        // final BigInteger opGet = stats.getOperationalGet();
        // final Get get = new Get();
        // get.setReceivedRequests(opGet);
        // get.setSuccessfulResponses(stats.getSuccessGetOperational());
        // get.setFailedResponses(stats.getFailureGetOperational());
        // operational.setGet(get);
        return operational;
    }

    @Override
    public org.opendaylight.controller.config.yang.md.sal.rest.connector.Rpcs getRpcs() {
        final org.opendaylight.controller.config.yang.md.sal.rest.connector.Rpcs rpcs = new org.opendaylight.controller.config.yang.md.sal.rest.connector.Rpcs();
        // final BigInteger rpcInvoke = stats.getRpc();
        // rpcs.setReceivedRequests(rpcInvoke);
        return rpcs;
    }

    private void prepareRestconfResourceConfig() {
        // restconfHttpServiceTracker = new ServiceTracker<>(bundleCx, Http, customizer)

        final ResourceConfig rc = new ResourceConfig();
        rc.register(RestconfDocumentedException.class);
        rc.register(XmlNormalizedNodeBodyReader.class);
        rc.register(JsonNormalizedNodeBodyReader.class);
        rc.register(NormalizedNodeJsonBodyWriter.class);
        rc.register(NormalizedNodeXmlBodyWriter.class);
        rc.register(SchemaExportContentYinBodyWriter.class);
        rc.register(SchemaExportContentYangBodyWriter.class);
        rc.register(RestconfImpl.class);

        restconfServletContainer = new ServletContainer(rc);

//        restconfHttpServiceTracker = new ServiceTracker<>(bundleCx, ResourceConfig.class.getName(), rc);

        // bundleCx.registerService(Servlet.class.getName(), restconfServletContainer, null);

        // final ServiceTracker restconfHttpServiceTracker = new ServiceTracker<>(bundleCx, clazz, customizer)

        // final Builder resourceBuilder = Resource.builder();
        // resourceBuilder.path("restconf");
        // resourceBuilder.addChildResource(Resource.from(RestconfImpl.class));
        // final ResourceConfig rc = new ResourceConfig();
        // rc.register(RestconfDocumentedException.class);
        // rc.register(XmlNormalizedNodeBodyReader.class);
        // rc.register(JsonNormalizedNodeBodyReader.class);
        // rc.register(NormalizedNodeJsonBodyWriter.class);
        // rc.register(NormalizedNodeXmlBodyWriter.class);
        // rc.register(SchemaExportContentYinBodyWriter.class);
        // rc.register(SchemaExportContentYangBodyWriter.class);
        // rc.register(RestconfImpl.class);
        // final ServletContainer sc = new ServletContainer(rc);
        // bundleCx.
        // resourceBuilder.addChildResource(rc);

        // final ServletContainer sc = new ServletContainer(rc);
        // final ApplicationHandler appHandler = sc.getApplicationHandler();
        System.out.println("brm");
    }

    private void streamWebSocketInitialization() {
        webSocketServerThread = new Thread(WebSocketServer.createInstance(port.getValue().intValue()));
        webSocketServerThread.setName("Web socket server on port " + port);
        webSocketServerThread.start();
    }
}