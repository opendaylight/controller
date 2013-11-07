/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc.router.zeromq;

import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractProvider {

  ZeroMqRpcRouter router;

  @Override
  public void onSessionInitiated(ProviderSession session) {
    router = ZeroMqRpcRouter.getInstance();
    router.setBrokerSession(session);
    router.start();
  }

  @Override
  protected void stopImpl(BundleContext context) {
    router.stop();
  }

}
