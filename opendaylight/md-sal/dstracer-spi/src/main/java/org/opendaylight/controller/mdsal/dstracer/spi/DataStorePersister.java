/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.mdsal.dstracer.spi;

import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Service providers implementing this interface receive events
 * Thread safety note: if implementation does not support concurrent writes, it is responsible for
 * guarding its state in multi-threaded environment.
 */
@ThreadSafe
public interface DataStorePersister extends AutoCloseable, DOMDataChangeListener {

    /**
     * Receive events containing change in data store.
     * {@inheritDoc}
     */
    @Override
    void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change);

    /**
     * Close any file handles or other resources related.
     */
    @Override
    void close();
}
