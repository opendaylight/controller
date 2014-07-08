/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.yang.common.QName;

public class RpcListener implements RpcRegistrationListener{
  @Override
  public void onRpcImplementationAdded(QName name) {
    // TODO : add entry into routing registry
  }

  @Override
  public void onRpcImplementationRemoved(QName name) {
    // TODO : Remove entry from routing registry
  }
}
