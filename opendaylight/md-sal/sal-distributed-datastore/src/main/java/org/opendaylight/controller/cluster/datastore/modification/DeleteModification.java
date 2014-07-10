/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import org.opendaylight.controller.cluster.datastore.utils.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * DeleteModification store all the parameters required to delete a path from the data tree
 */
public class DeleteModification extends AbstractModification {
  public DeleteModification(YangInstanceIdentifier path) {
    super(path);
  }

  @Override
  public void apply(DOMStoreWriteTransaction transaction) {
    transaction.delete(path);
  }

    @Override public Object toSerializable() {
        return PersistentMessages.Modification.newBuilder()
            .setType(this.getClass().toString())
            .setPath(InstanceIdentifierUtils.toSerializable(this.path))
            .build();
    }

    public static DeleteModification fromSerializable(Object serializable){
        PersistentMessages.Modification o = (PersistentMessages.Modification) serializable;
        return new DeleteModification(InstanceIdentifierUtils.fromSerializable(o.getPath()));
    }
}
