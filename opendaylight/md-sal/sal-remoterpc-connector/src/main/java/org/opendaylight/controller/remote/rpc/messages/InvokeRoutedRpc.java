/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.io.Serializable;

public class InvokeRoutedRpc implements Serializable {

  private QName rpc;
  private InstanceIdentifier identifier;
  private CompositeNode input;

  public InvokeRoutedRpc(QName rpc, InstanceIdentifier identifier, CompositeNode input) {
    this.rpc = rpc;
    this.identifier = identifier;
    this.input = input;
  }

  public InvokeRoutedRpc(QName rpc, CompositeNode input) {
    this.rpc = rpc;
    this.input = input;
  }

  public QName getRpc() {
    return rpc;
  }

  public InstanceIdentifier getIdentifier() {
    return identifier;
  }

  public CompositeNode getInput() {
    return input;
  }
}
