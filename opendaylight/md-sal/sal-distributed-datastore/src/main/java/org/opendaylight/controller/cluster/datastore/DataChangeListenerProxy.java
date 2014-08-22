/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;

import com.google.common.base.Preconditions;

import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * DataChangeListenerProxy represents a single remote DataChangeListener
 */
public class DataChangeListenerProxy implements AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>{
    private final ActorSelection dataChangeListenerActor;
    private final SchemaContext schemaContext;

    public DataChangeListenerProxy(SchemaContext schemaContext, ActorSelection dataChangeListenerActor) {
        this.dataChangeListenerActor = Preconditions.checkNotNull(dataChangeListenerActor, "dataChangeListenerActor should not be null");
        this.schemaContext = schemaContext;
    }

    @Override public void onDataChanged(
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        dataChangeListenerActor.tell(new DataChanged(schemaContext, change), null);
    }
}
