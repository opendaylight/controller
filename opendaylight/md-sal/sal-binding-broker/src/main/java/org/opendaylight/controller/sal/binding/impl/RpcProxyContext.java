/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.binding.impl;

import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.ServiceRegistration;

@SuppressWarnings("all")
public class RpcProxyContext {
  public RpcProxyContext(final Class<? extends RpcService> proxyClass) {
    this.proxyClass = proxyClass;
  }

  protected final Class<? extends RpcService> proxyClass;

  protected RpcService _proxy;

  public RpcService getProxy() {
    return this._proxy;
  }

  public void setProxy(final RpcService proxy) {
    this._proxy = proxy;
  }

  protected ServiceRegistration<? extends RpcService> _registration;

  public ServiceRegistration<? extends RpcService> getRegistration() {
    return this._registration;
  }

  public void setRegistration(final ServiceRegistration<? extends RpcService> registration) {
    this._registration = registration;
  }
}