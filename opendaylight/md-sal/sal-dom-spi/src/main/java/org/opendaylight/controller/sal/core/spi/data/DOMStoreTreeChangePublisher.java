/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Interface implemented by classes interested in obtaining a change cursor
 * whenever a subtree is undergoing a potential change.
 */
public interface DOMStoreTreeChangePublisher {
    /**
     * Registers a {@link DOMDataTreeChangeListener} to receive
     * notifications when data changes under a given path in the conceptual data
     * tree.
     * <p>
     * You are able to register for notifications  for any node or subtree
     * which can be represented using {@link YangInstanceIdentifier}.
     * <p>
     *
     * You are able to register for data change notifications for a subtree or leaf
     * even if it does not exist. You will receive notification once that node is
     * created.
     * <p>
     * If there are any preexisting data in data tree on path for which you are
     * registering, you will receive initial data change event, which will
     * contain all preexisting data, marked as created.
     *
     * <p>
     * This method returns a {@link ListenerRegistration} object. To
     * "unregister" your listener for changes call the "close" method on this
     * returned object.
     * <p>
     * You MUST call close when you no longer need to receive notifications
     * (such as during shutdown or for example if your bundle is shutting down).
     *
     * @param subtree
     *            Yang Instance Identifier identifying node or subtree which
     *            triggers change event.
     * @param listener
     *            Instance of listener which should be invoked on
     * @return Listener registration object, which may be used to unregister
     *         your listener using {@link ListenerRegistration#close()} to stop
     *         delivery of change events.
     */
    @Nonnull <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(@Nonnull YangInstanceIdentifier subtree, @Nonnull L listener);
}
