/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 * Provides a stateful read-only view of the data tree.
 *
 * <p>
 * View of the data tree is a stable point-in-time snapshot of the current data tree state when
 * the transaction was created. It's state and underlying data tree
 * is not affected by other concurrently running transactions.
 *
 * <p>
 * <b>Implementation Note:</b> This interface is not intended to be implemented
 * by users of MD-SAL, but only to be consumed by them.
 *
 * <h2>Transaction isolation example</h2> Lest assume initial state of data tree
 * for <code>PATH</code> is <code>A</code>.
 *
 * <pre>
 * txRead = broker.newReadOnlyTransaction();   // read Transaction is snapshot of data
 * txWrite = broker.newReadWriteTransactoin(); // concurrent write transaction
 *
 * txRead.read(OPERATIONAL,PATH).get();        // will return Optional containing A
 * txWrite = broker.put(OPERATIONAL,PATH,B);   // writes B to PATH
 *
 * txRead.read(OPERATIONAL,PATH).get();        // still returns Optional containing A
 *
 * txWrite.commit().get();                     // data tree is updated, PATH contains B
 * txRead.read(OPERATIONAL,PATH).get();        // still returns Optional containing A
 *
 * txAfterCommit = broker.newReadOnlyTransaction(); // read Transaction is snapshot of new state
 * txAfterCommit.read(OPERATIONAL,PATH).get(); // returns Optional containing B;
 * </pre>
 *
 * <p>
 * <b>Note:</b> example contains blocking calls on future only to illustrate
 * that action happened after other asynchronous action. Use of blocking call
 * {@link ListenableFuture#get()} is discouraged for most uses and you should
 * use
 * {@link com.google.common.util.concurrent.Futures#addCallback(ListenableFuture, com.google.common.util.concurrent.FutureCallback)}
 * or other functions from {@link com.google.common.util.concurrent.Futures} to
 * register more specific listeners.
 *
 * @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
public interface AsyncReadTransaction<P extends Path<P>, D> extends AsyncTransaction<P, D> {

    /**
     *
     * Reads data from provided logical data store located at the provided path.
     *<p>
     * If the target is a subtree, then the whole subtree is read (and will be
     * accessible from the returned data object).
     *
     * @param store
     *            Logical data store from which read should occur.
     * @param path
     *            Path which uniquely identifies subtree which client want to
     *            read
     * @return Listenable Future which contains read result
     *         <ul>
     *         <li>If data at supplied path exists the
     *         {@link ListeblaFuture#get()} returns Optional object containing
     *         data once read is done.
     *         <li>If data at supplied path does not exists the
     *         {@link ListenbleFuture#get()} returns {@link Optional#absent()}.
     *         </ul>
     */
    ListenableFuture<Optional<D>> read(LogicalDatastoreType store, P path);

}
