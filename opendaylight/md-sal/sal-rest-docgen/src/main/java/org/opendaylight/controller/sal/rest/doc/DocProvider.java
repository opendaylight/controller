/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc;

import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.doc.impl.ApiDocGenerator;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;


public class DocProvider implements BundleActivator,
                                    ServiceTrackerCustomizer<Broker, Broker>,
                                    Provider,
                                    AutoCloseable {

  private Logger _logger = LoggerFactory.getLogger(DocProvider.class);

  private ServiceTracker<Broker, Broker> brokerServiceTracker;
  private BundleContext bundleContext;
  private Broker.ProviderSession session;

  @Override
  public void close() throws Exception {
    stop(bundleContext);
  }

  @Override
  public void onSessionInitiated(Broker.ProviderSession providerSession) {
    SchemaService schemaService = providerSession.getService(SchemaService.class);
    ApiDocGenerator.getInstance().setSchemaService(schemaService);

    _logger.debug("Restconf API Explorer started");

  }

  @Override
  public Collection<ProviderFunctionality> getProviderFunctionality() {
    return Collections.emptySet();
  }

  @Override
  public void start(BundleContext context) throws Exception {
    bundleContext = context;
    brokerServiceTracker = new ServiceTracker(context, Broker.class, this);
    brokerServiceTracker.open();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    if (brokerServiceTracker != null)
      brokerServiceTracker.close();

    if (session != null)
      session.close();
  }

  @Override
  public Broker addingService(ServiceReference<Broker> reference) {
    Broker broker = bundleContext.getService(reference);
    session = broker.registerProvider(this, bundleContext);
    return broker;
  }

  @Override
  public void modifiedService(ServiceReference<Broker> reference, Broker service) {
    if (session != null)
      session.close();

    Broker broker = bundleContext.getService(reference);
    session = broker.registerProvider(this, bundleContext);
  }

  @Override
  public void removedService(ServiceReference<Broker> reference, Broker service) {
    bundleContext.ungetService(reference);
  }
}
