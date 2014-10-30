/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.protobuff.messages.datachange.notification.DataChangeListenerMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DataChanged implements SerializableMessage {
    public static final Class<DataChangeListenerMessages.DataChanged> SERIALIZABLE_CLASS =
        DataChangeListenerMessages.DataChanged.class;

    private final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change;

    public DataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        this.change = change;
    }

    public AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> getChange() {
        return change;
    }

    @Override
    public Object toSerializable() {
        return new ExternalizableDataChanged(change, DataStoreVersions.CURRENT_VERSION);
    }

    public static DataChanged fromSerializable(Object serializable) {
        ExternalizableDataChanged ext = (ExternalizableDataChanged)serializable;
        return new DataChanged(ext.getChange());
    }
}
