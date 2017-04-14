/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import java.util.function.Consumer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Interface for a class that generates and publishes notifications for DataTreeChangeListeners.
 *
 * @author Thomas Pantelis
 */
interface ShardDataTreeChangeListenerPublisher extends ShardDataTreeNotificationPublisher {
    void registerTreeChangeListener(YangInstanceIdentifier treeId, DOMDataTreeChangeListener listener,
            Optional<DataTreeCandidate> initialState,
            Consumer<ListenerRegistration<DOMDataTreeChangeListener>> onRegistration);
}
