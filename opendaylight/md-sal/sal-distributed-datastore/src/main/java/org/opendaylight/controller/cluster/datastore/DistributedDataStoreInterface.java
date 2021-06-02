/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * The public interface exposed by an AbstractDataStore via the OSGi registry.
 *
 * @author Thomas Pantelis
 */
public interface DistributedDataStoreInterface extends DOMStore {

    ActorUtils getActorUtils();

    @Beta
    <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerProxyListener(
            YangInstanceIdentifier shardLookup, YangInstanceIdentifier insideShard,
            DOMDataTreeChangeListener delegate);
}
