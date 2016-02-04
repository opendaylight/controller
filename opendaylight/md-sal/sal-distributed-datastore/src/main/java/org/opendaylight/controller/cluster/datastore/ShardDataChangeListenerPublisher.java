/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Interface for a class that generates and publishes notifications for DataChangeListeners.
 *
 * @author Thomas Pantelis
 */
interface ShardDataChangeListenerPublisher extends ShardDataTreeNotificationPublisher {
    ShardDataChangeListenerPublisher newInstance();

    <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> DataChangeListenerRegistration<L>
            registerDataChangeListener(final YangInstanceIdentifier path,final L listener, final DataChangeScope scope);
}
