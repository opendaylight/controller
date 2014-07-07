/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

/**
 * Read-only transaction, which provides stable view of data
 * and is {@link AutoCloseable} resource.
 *
 *
 * <p>
 * <b>Note:</b>
 * Concretization of this interface MUST provides read method, which will
 * take DATASTORE and PATH arguments and  which will convey
 * format specific read operations (eg. relationship between <code>path</code>
 * and returned <code>dataObject</code>). Read methods must be asynchronous
 * and return ListenableFuture.
 *
 * @see AsyncReadTransaction
 *
 *
* @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
public interface AsyncReadOnlyTransaction<P extends Path<P>, D> extends AsyncReadTransaction<P, D>, AutoCloseable {

    /**
     * Closes this transaction and releases all resources associated with it.
     *
     */
    @Override
    public void close();
}
