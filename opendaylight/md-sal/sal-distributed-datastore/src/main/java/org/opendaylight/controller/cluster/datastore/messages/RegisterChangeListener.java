/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class RegisterChangeListener {
  private final InstanceIdentifier path;
  private final AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> listener;
  private final AsyncDataBroker.DataChangeScope scope;


  public RegisterChangeListener(InstanceIdentifier path, AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> listener, AsyncDataBroker.DataChangeScope scope) {
    this.path = path;
    this.listener = listener;
    this.scope = scope;
  }

  public InstanceIdentifier getPath() {
    return path;
  }

  public AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> getListener() {
    return listener;
  }

  public AsyncDataBroker.DataChangeScope getScope() {
    return scope;
  }
}
