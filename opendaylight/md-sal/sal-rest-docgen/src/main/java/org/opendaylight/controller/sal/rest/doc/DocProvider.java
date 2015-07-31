/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.controller.sal.rest.doc.impl.ApiDocGenerator;
import org.opendaylight.controller.sal.rest.doc.impl.ApiDocServiceImpl;
import org.opendaylight.controller.sal.rest.doc.jaxrs.JaxbContextResolver;
import org.opendaylight.controller.sal.rest.doc.mountpoints.MountPointSwagger;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocProvider implements BundleActivator, ServiceTrackerCustomizer<Broker, Broker>, Provider, AutoCloseable {

    private final Logger LOG = LoggerFactory.getLogger(DocProvider.class);
    private static final String APIDOC_BASE_PATH = "/apidoc";

    private ServiceTracker<Broker, Broker> brokerServiceTracker;
    private BundleContext bundleContext;
    private Broker.ProviderSession session;
    private HttpService httpService;

    private final List<AutoCloseable> toClose = new LinkedList<>();

    @Override
    public void close() throws Exception {
        stop(bundleContext);
    }

    @Override
    public void onSessionInitiated(final Broker.ProviderSession providerSession) {
        Preconditions.checkState((!providerSession.isClosed()), "Session is closed.");
        final SchemaService schemaService = providerSession.getService(SchemaService.class);
        ApiDocGenerator.getInstance().setSchemaService(schemaService);

        final DOMMountPointService mountService = providerSession.getService(DOMMountPointService.class);
        final ListenerRegistration<MountProvisionListener> registration = mountService
                .registerProvisionListener(MountPointSwagger.getInstance());
        MountPointSwagger.getInstance().setGlobalSchema(schemaService);
        synchronized (toClose) {
            toClose.add(registration);
        }
        MountPointSwagger.getInstance().setMountService(mountService);

        LOG.debug("Restconf API Explorer started");
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        bundleContext = context;
        brokerServiceTracker = new ServiceTracker<>(context, Broker.class, this);
        brokerServiceTracker.open();

        final ServiceReference<?> ref = bundleContext.getServiceReference(HttpService.class.getName());
        httpService = (HttpService) bundleContext.getService(ref);
        final ResourceConfig rc = new ResourceConfig();
        rc.setApplicationName("JAXRSApiDoc");
        rc.register(JaxbContextResolver.class);
        rc.register(ApiDocServiceImpl.class);
        try {
            httpService.registerServlet(APIDOC_BASE_PATH, new ServletContainer(rc), null, null);
        } catch (ServletException | NamespaceException e) {
            LOG.error("REST_CONNECTOR BUNDLE: unexpected error, restconf servlet was not registred", e);
            return;
        }

        LOG.info("REST_CONNECTOR BUNDLE: restconf servlet registered");
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (brokerServiceTracker != null) {
            brokerServiceTracker.close();
        }

        if (session != null) {
            session.close();
        }

        synchronized (toClose) {
            for (final AutoCloseable close : toClose) {
                close.close();
            }
        }

        httpService.unregister(APIDOC_BASE_PATH);
    }

    @Override
    public Broker addingService(final ServiceReference<Broker> reference) {
        final Broker broker = bundleContext.getService(reference);
        session = broker.registerProvider(this, bundleContext);
        return broker;
    }

    @Override
    public void modifiedService(final ServiceReference<Broker> reference, final Broker service) {
        if (session != null) {
            session.close();
        }

        final Broker broker = bundleContext.getService(reference);
        session = broker.registerProvider(this, bundleContext);
    }

    @Override
    public void removedService(final ServiceReference<Broker> reference, final Broker service) {
        bundleContext.ungetService(reference);
    }
}
