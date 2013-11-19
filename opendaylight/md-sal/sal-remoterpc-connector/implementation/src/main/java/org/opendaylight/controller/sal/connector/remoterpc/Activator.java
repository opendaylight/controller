/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc;

import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.impl.RoutingTableImpl;
import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

public class Activator extends AbstractProvider {

  Logger _logger = LoggerFactory.getLogger(Activator.class);

  BundleContext context;
  ServiceRegistration registration;

  @Override
  public void onSessionInitiated(ProviderSession session) {
    Server.getInstance().setBrokerSession(session);

    injectRoutingTable();

    Server.getInstance().start();
  }

  @Override
  protected void stopImpl(BundleContext context) {
    _logger.debug("Stopping...");
    if (registration != null) registration.unregister();

    Client.getInstance().stop();
    Server.getInstance().stop();

  }

  protected void startImpl(BundleContext context) {
    this.context = context;
    Dictionary<String, ?> emptyProperties = new Hashtable<String, String>();

    registration = context.registerService
        (Server.class, Server.getInstance(), emptyProperties);

    _logger.debug("Registering [{}]", registration);

    ServiceRegistration routerReg =
    context.registerService((Class<Client$>) Client.getInstance().getClass(), Client.getInstance(), emptyProperties);

    Client.getInstance().start();
    _logger.debug("Registering [{}]", routerReg);
  }

  private void injectRoutingTable(){

    ServiceReference ref = context.getServiceReference(RoutingTable.class.getName());
    RoutingTable table = (RoutingTableImpl) context.getService(ref);
    Server.getInstance().setRoutingTable(table);
    _logger.debug("Injected Routing table :{}", table);
  }

}
