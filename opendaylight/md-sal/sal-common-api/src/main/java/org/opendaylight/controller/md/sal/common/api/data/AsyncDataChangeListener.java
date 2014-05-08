/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import java.util.EventListener;

import org.opendaylight.yangtools.concepts.Path;

/**
 * Listener of data change events on particular subtree.
 *
 * <p>
 * User-supplied implementations of this listener interface MUST register via
 * {@link AsyncDataBroker#registerDataChangeListener(LogicalDatastoreType, Path, AsyncDataChangeListener, AsyncDataBroker.DataChangeScope)}
 * in order to start receiving data change events, which capture state changes
 * in a subtree.
 *
 * <p>
 * <b>Implementation Note:</b> This interface is intended to be implemented
 * by users of MD-SAL.
 *
 * @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
public interface AsyncDataChangeListener<P extends Path<P>, D> extends EventListener {
    /**
     *
     * Invoked when there is data change for the particular path, which was used to
     * register this listener.
     * <p>
     * This method may be also invoked during registration of the listener if
     * there is any preexisting data in the conceptual data tree for supplied path.
     * This initial event will contain all preexisting data as created.
     *
     * <p>
     * <b>Note</b>: This method may be invoked from a shared thread pool.
     * <li>Implementations <b>SHOULD NOT</b> perform CPU-intensive operations on the calling thread.
     * <li>Implementations <b>MUST NOT block the calling thread</b> - to do so could lead to deadlock
     * scenarios.
     *
     *<br>
     *
     * @param change
     *            Data Change Event being delivered.
     */
    void onDataChanged(AsyncDataChangeEvent<P, D> change);
}
