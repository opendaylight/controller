<<<<<<< HEAD
/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.controller.sal.rest.doc.impl.ApiDocGenerator;
import org.opendaylight.controller.sal.rest.doc.mountpoints.MountPointSwagger;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocProvider implements BundleActivator, ServiceTrackerCustomizer<Broker, Broker>,
        Provider, AutoCloseable {

    private final Logger _logger = LoggerFactory.getLogger(DocProvider.class);

    private ServiceTracker<Broker, Broker> brokerServiceTracker;
    private BundleContext bundleContext;
    private Broker.ProviderSession session;

    private final List<AutoCloseable> toClose = new LinkedList<>();

    @Override
    public void close() throws Exception {
        stop(bundleContext);
    }

    @Override
    public void onSessionInitiated(Broker.ProviderSession providerSession) {
        SchemaService schemaService = providerSession.getService(SchemaService.class);
        ApiDocGenerator.getInstance().setSchemaService(schemaService);

        DOMMountPointService mountService = providerSession
                .getService(DOMMountPointService.class);
        ListenerRegistration<MountProvisionListener> registration = mountService
                .registerProvisionListener(MountPointSwagger.getInstance());
        MountPointSwagger.getInstance().setGlobalSchema(schemaService);
        synchronized (toClose) {
            toClose.add(registration);
        }
        MountPointSwagger.getInstance().setMountService(mountService);

        _logger.debug("Restconf API Explorer started");
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        brokerServiceTracker = new ServiceTracker<>(context, Broker.class, this);
        brokerServiceTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (brokerServiceTracker != null) {
            brokerServiceTracker.close();
        }

        if (session != null) {
            session.close();
        }

        synchronized (toClose) {
            for (AutoCloseable close : toClose) {
                close.close();
            }
        }
    }

    @Override
    public Broker addingService(ServiceReference<Broker> reference) {
        Broker broker = bundleContext.getService(reference);
        session = broker.registerProvider(this, bundleContext);
        return broker;
    }

    @Override
    public void modifiedService(ServiceReference<Broker> reference, Broker service) {
        if (session != null) {
            session.close();
        }

        Broker broker = bundleContext.getService(reference);
        session = broker.registerProvider(this, bundleContext);
    }

    @Override
    public void removedService(ServiceReference<Broker> reference, Broker service) {
        bundleContext.ungetService(reference);
    }
}
=======
///*
// * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
// *
// * This program and the accompanying materials are made available under the
// * terms of the Eclipse Public License v1.0 which accompanies this distribution,
// * and is available at http://www.eclipse.org/legal/epl-v10.html
// */
//package org.opendaylight.controller.sal.rest.doc;
//
//import com.google.common.base.Preconditions;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.LinkedList;
//import java.util.List;
//import javax.servlet.ServletException;
//import org.glassfish.jersey.server.ResourceConfig;
//import org.glassfish.jersey.servlet.ServletContainer;
//import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
//import org.opendaylight.controller.sal.core.api.Broker;
//import org.opendaylight.controller.sal.core.api.Provider;
//import org.opendaylight.controller.sal.core.api.model.SchemaService;
//import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
//import org.opendaylight.controller.sal.rest.doc.impl.ApiDocGenerator;
//import org.opendaylight.controller.sal.rest.doc.impl.ApiDocServiceImpl;
//import org.opendaylight.controller.sal.rest.doc.jaxrs.JaxbContextResolver;
//import org.opendaylight.controller.sal.rest.doc.mountpoints.MountPointSwagger;
//import org.opendaylight.yangtools.concepts.ListenerRegistration;
//import org.osgi.framework.BundleActivator;
//import org.osgi.framework.BundleContext;
//import org.osgi.framework.ServiceReference;
//import org.osgi.service.http.HttpService;
//import org.osgi.service.http.NamespaceException;
//import org.osgi.util.tracker.ServiceTracker;
//import org.osgi.util.tracker.ServiceTrackerCustomizer;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class DocProvider implements BundleActivator, ServiceTrackerCustomizer<Broker, Broker>,
//        Provider, AutoCloseable {
//
//    private final Logger _logger = LoggerFactory.getLogger(DocProvider.class);
//    private static final String APIDOC_BASE_PATH = "/apidoc";
//
//    private ServiceTracker<Broker, Broker> brokerServiceTracker;
//    private BundleContext bundleContext;
//    private Broker.ProviderSession session;
//    private HttpService httpService;
//
//
//    private final List<AutoCloseable> toClose = new LinkedList<>();
//
//    @Override
//    public void close() throws Exception {
//        stop(bundleContext);
//    }
//
//    @Override
//    public void onSessionInitiated(Broker.ProviderSession providerSession) {
//        SchemaService schemaService = providerSession.getService(SchemaService.class);
//        ApiDocGenerator.getInstance().setSchemaService(schemaService);
//
//        DOMMountPointService mountService = providerSession
//                .getService(DOMMountPointService.class);
//        ListenerRegistration<MountProvisionListener> registration = mountService
//                .registerProvisionListener(MountPointSwagger.getInstance());
//        MountPointSwagger.getInstance().setGlobalSchema(schemaService);
//        synchronized (toClose) {
//            toClose.add(registration);
//        }
//        MountPointSwagger.getInstance().setMountService(mountService);
//
//        _logger.debug("Restconf API Explorer started");
//    }
//
//    @Override
//    public Collection<ProviderFunctionality> getProviderFunctionality() {
//        return Collections.emptySet();
//    }
//
//    @Override
//    public void start(BundleContext context) throws Exception {
//        bundleContext = context;
//        brokerServiceTracker = new ServiceTracker<>(context, Broker.class, this);
//        brokerServiceTracker.open();
////        Preconditions.checkState((!session.isClosed()), "Session is closed.");
//        final ServiceReference<?> ref = bundleContext.getServiceReference(HttpService.class.getName());
//        httpService = (HttpService) bundleContext.getService(ref);
//        ResourceConfig rc = new ResourceConfig();
//        rc.setApplicationName("JAXRSApiDoc");
//        rc.register(JaxbContextResolver.class);
//        rc.register(ApiDocServiceImpl.class);
//        try {
//            httpService.registerServlet(APIDOC_BASE_PATH, new ServletContainer(rc), null, null);
//        } catch (ServletException | NamespaceException e) {
//            _logger.error("REST_CONNECTOR BUNDLE: unexpected error, apidoc servlet was not registred", e);
//            return;
//        }
//
//        _logger.info("REST_CONNECTOR BUNDLE: restconf servlet registered");
//    }
//
//    @Override
//    public void stop(BundleContext context) throws Exception {
//        if (brokerServiceTracker != null) {
//            brokerServiceTracker.close();
//        }
//
//        if (session != null) {
//            session.close();
//        }
//
//        synchronized (toClose) {
//            for (AutoCloseable close : toClose) {
//                close.close();
//            }
//        }
//
//        httpService.unregister(APIDOC_BASE_PATH);
//    }
//
//    @Override
//    public Broker addingService(ServiceReference<Broker> reference) {
//        Broker broker = bundleContext.getService(reference);
//        session = broker.registerProvider(this, bundleContext);
//        return broker;
//    }
//
//    @Override
//    public void modifiedService(ServiceReference<Broker> reference, Broker service) {
//        if (session != null) {
//            session.close();
//        }
//
//        Broker broker = bundleContext.getService(reference);
//        session = broker.registerProvider(this, bundleContext);
//    }
//
//    @Override
//    public void removedService(ServiceReference<Broker> reference, Broker service) {
//        bundleContext.ungetService(reference);
//    }
//}
>>>>>>> c218f31... * migration of rest-docgen to config sunbsystem
