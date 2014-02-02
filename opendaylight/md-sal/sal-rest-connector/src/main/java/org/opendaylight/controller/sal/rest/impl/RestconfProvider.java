/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class RestconfProvider implements BundleActivator, Provider, ServiceTrackerCustomizer<Broker, Broker> {

    public final static String NOT_INITALIZED_MSG = "Restconf is not initialized yet. Please try again later";

    private ListenerRegistration<SchemaServiceListener> listenerRegistration;
    private ServiceTracker<Broker, Broker> brokerServiceTrancker;
    private BundleContext bundleContext;
    private ProviderSession session;
    private Thread webSocketServerThread;

    @Override
    public void onSessionInitiated(ProviderSession session) {
        DataBrokerService dataService = session.getService(DataBrokerService.class);

        BrokerFacade.getInstance().setContext(session);
        BrokerFacade.getInstance().setDataService(dataService);

        SchemaService schemaService = session.getService(SchemaService.class);
        listenerRegistration = schemaService.registerSchemaServiceListener(ControllerContext.getInstance());
        ControllerContext.getInstance().setSchemas(schemaService.getGlobalContext());
        ControllerContext.getInstance().setMountService(session.getService(MountService.class));
    }

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        brokerServiceTrancker = new ServiceTracker<>(context, Broker.class, this);
        brokerServiceTrancker.open();
        webSocketServerThread = new Thread(new WebSocketServer());
        webSocketServerThread.setName("Web socket server");
        webSocketServerThread.start();
    }

    @Override
    public void stop(BundleContext context) {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        webSocketServerThread.interrupt();
        session.close();
        brokerServiceTrancker.close();
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public Broker addingService(ServiceReference<Broker> reference) {
        Broker broker = bundleContext.getService(reference);
        broker.registerProvider(this, bundleContext);
        return broker;
    }

    @Override
    public void modifiedService(ServiceReference<Broker> reference, Broker service) {
        // NOOP
    }

    @Override
    public void removedService(ServiceReference<Broker> reference, Broker service) {
        bundleContext.ungetService(reference);
        BrokerFacade.getInstance().setContext(null);
        BrokerFacade.getInstance().setDataService(null);
        ControllerContext.getInstance().setSchemas(null);
    }
}
