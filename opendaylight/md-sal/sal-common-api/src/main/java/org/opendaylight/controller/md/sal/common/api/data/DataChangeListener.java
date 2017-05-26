/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import java.util.EventListener;

import org.opendaylight.yangtools.concepts.Path;
/**
 *
 *
 * @deprecated Replaced by {@link AsyncDataChangeEvent}
 */
@Deprecated
public interface DataChangeListener<P extends Path<P>, D> extends EventListener {
    /**
     * Note that this method may be invoked from a shared thread pool, so
     * implementations SHOULD NOT perform CPU-intensive operations and they
     * definitely MUST NOT invoke any potentially blocking operations.
     *
     * @param change Data Change Event being delivered.
     **/
    void onDataChanged(DataChangeEvent<P, D> change);
}
