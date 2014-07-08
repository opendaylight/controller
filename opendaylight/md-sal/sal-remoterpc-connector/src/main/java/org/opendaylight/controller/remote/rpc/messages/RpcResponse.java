/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;


import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class RpcResponse {
  private CompositeNode result;

  public RpcResponse(CompositeNode result) {
    this.result = result;
  }

  public CompositeNode getResult() {
    return result;
  }
}
