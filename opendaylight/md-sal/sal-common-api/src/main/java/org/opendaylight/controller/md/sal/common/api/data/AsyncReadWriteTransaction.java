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
 * Transaction enabling a client to have a combined read/write capabilities.
 *
 * <p>
 * The initial state of the write transaction is stable snapshot of current data tree
 * state captured when transaction was created and it's state and underlying
 * data tree are not affected by other concurrently running transactions.
 *
 * <p>
 * Write transactions are isolated from other concurrent write transactions. All
 * writes are local to the transaction and represents only a proposal of state
 * change for data tree and it is not visible to any other concurrently running
 * transactions.
 *
 * <p>
 * Applications publish the changes proposed in the transaction by calling {@link #commit}
 * on the transaction. This seals the transaction
 * (preventing any further writes using this transaction) and submits it to be
 * processed and applied to global conceptual data tree.
 *
 * <p>
 * The transaction commit may fail due to a concurrent transaction modifying and committing data in
 * an incompatible way. See {@link #commit()} for more concrete commit failure examples.
 *
 * <b>Implementation Note:</b> This interface is not intended to be implemented
 * by users of MD-SAL, but only to be consumed by them.
 *
 * <h2>Examples</h2>
 *
 * <h3>Transaction local state</h3>
 *
 * Let assume initial state of data tree for <code>PATH</code> is <code>A</code>
 * .
 *
 * <pre>
 * txWrite = broker.newReadWriteTransaction(); // concurrent write transaction
 *
 * txWrite.read(OPERATIONAL,PATH).get()        // will return Optional containing A
 * txWrite.put(OPERATIONAL,PATH,B);            // writes B to PATH
 * txWrite.read(OPERATIONAL,PATH).get()        // will return Optional Containing B
 *
 * txWrite.commit().get();                     // data tree is updated, PATH contains B
 *
 * tx1afterCommit = broker.newReadOnlyTransaction(); // read Transaction is snapshot of new state
 * tx1afterCommit.read(OPERATIONAL,PATH).get(); // returns Optional containing B
 * </pre>
 *
 * As you could see read-write transaction provides capabilities as
 * {@link AsyncWriteTransaction} but also allows for reading proposed changes as
 * if they already happened.
 *
 * <h3>Transaction isolation (read transaction, read-write transaction)</h3> Let
 * assume initial state of data tree for <code>PATH</code> is <code>A</code>.
 *
 * <pre>
 * txRead = broker.newReadOnlyTransaction();   // read Transaction is snapshot of data
 * txWrite = broker.newReadWriteTransaction(); // concurrent write transaction
 *
 * txRead.read(OPERATIONAL,PATH).get();        // will return Optional containing A
 * txWrite.read(OPERATIONAL,PATH).get()        // will return Optional containing A
 *
 * txWrite.put(OPERATIONAL,PATH,B);            // writes B to PATH
 * txWrite.read(OPERATIONAL,PATH).get()        // will return Optional Containing B
 *
 * txRead.read(OPERATIONAL,PATH).get();        // concurrent read transaction still returns
 *                                             // Optional containing A
 *
 * txWrite.commit().get();                     // data tree is updated, PATH contains B
 * txRead.read(OPERATIONAL,PATH).get();        // still returns Optional containing A
 *
 * tx1afterCommit = broker.newReadOnlyTransaction(); // read Transaction is snapshot of new state
 * tx1afterCommit.read(OPERATIONAL,PATH).get(); // returns Optional containing B
 * </pre>
 *
 * <h3>Transaction isolation (2 concurrent read-write transactions)</h3> Let
 * assume initial state of data tree for <code>PATH</code> is <code>A</code>.
 *
 * <pre>
 * tx1 = broker.newReadWriteTransaction(); // read Transaction is snapshot of data
 * tx2 = broker.newReadWriteTransaction(); // concurrent write transaction
 *
 * tx1.read(OPERATIONAL,PATH).get();       // will return Optional containing A
 * tx2.read(OPERATIONAL,PATH).get()        // will return Optional containing A
 *
 * tx2.put(OPERATIONAL,PATH,B);            // writes B to PATH
 * tx2.read(OPERATIONAL,PATH).get()        // will return Optional Containing B
 *
 * tx1.read(OPERATIONAL,PATH).get();       // tx1 read-write transaction still sees Optional
 *                                         // containing A since is isolated from tx2
 * tx1.put(OPERATIONAL,PATH,C);            // writes C to PATH
 * tx1.read(OPERATIONAL,PATH).get()        // will return Optional Containing C
 *
 * tx2.read(OPERATIONAL,PATH).get()        // tx2 read-write transaction still sees Optional
 *                                         // containing B since is isolated from tx1
 *
 * tx2.commit().get();                     // data tree is updated, PATH contains B
 * tx1.read(OPERATIONAL,PATH).get();       // still returns Optional containing C since is isolated from tx2
 *
 * tx1afterCommit = broker.newReadOnlyTransaction(); // read Transaction is snapshot of new state
 * tx1afterCommit.read(OPERATIONAL,PATH).get(); // returns Optional containing B
 *
 * tx1.commit()                            // Will fail with OptimisticLockFailedException
 *                                         // which means concurrent transaction changed the same PATH
 *
 * </pre>
 *
 * <p>
 * <b>Note:</b> examples contains blocking calls on future only to illustrate
 * that action happened after other asynchronous action. Use of blocking call
 * {@link com.google.common.util.concurrent.ListenableFuture#get()} is discouraged for most uses and you should
 * use
 * {@link com.google.common.util.concurrent.Futures#addCallback(com.google.common.util.concurrent.ListenableFuture, com.google.common.util.concurrent.FutureCallback)}
 * or other functions from {@link com.google.common.util.concurrent.Futures} to
 * register more specific listeners.
 *
 * @see AsyncReadTransaction
 * @see AsyncWriteTransaction
 *
 * @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
public interface AsyncReadWriteTransaction<P extends Path<P>, D> extends AsyncReadTransaction<P, D>,
        AsyncWriteTransaction<P, D> {

}
