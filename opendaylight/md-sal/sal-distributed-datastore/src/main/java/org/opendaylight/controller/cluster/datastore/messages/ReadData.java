/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ReadData implements SerializableMessage {
  public static final Class<ShardTransactionMessages.ReadData> SERIALIZABLE_CLASS =
          ShardTransactionMessages.ReadData.class;
  private final YangInstanceIdentifier path;

  public ReadData(final YangInstanceIdentifier path) {
    this.path = path;
  }

  public YangInstanceIdentifier getPath() {
    return path;
  }

  public Object toSerializable(){
    return ShardTransactionMessages.ReadData.newBuilder()
        .setInstanceIdentifierPathArguments(InstanceIdentifierUtils.toSerializable(path))
        .build();
  }

  public static ReadData fromSerializable(final Object serializable){
    ShardTransactionMessages.ReadData o = (ShardTransactionMessages.ReadData) serializable;
    return new ReadData(InstanceIdentifierUtils.fromSerializable(o.getInstanceIdentifierPathArguments()));
  }
}
