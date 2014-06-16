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
 * Transaction enabling client to have combined transaction, which provides read
 * and write capabilities.
 *
 * Initial state of write transaction is stable snapshot of current data tree
 * state captured when transaction was created and it's state and underlying
 * data tree are not affected by other concurrently running transactions.
 *
 * Write transaction is isolated from other concurrent write transactions, all
 * writes are local to the transaction and represents only proposal of state
 * change for data tree and it is not visible to any other concurrently running
 * transactions.
 *
 * Application (publishing) of changes proposed in this transaction is done by
 * commiting transaction via {@link #commit()} message, which seals transaction
 * (prevents any further writes using this transaction) and submits it to be
 * processed and applied to global conceptual data tree.
 *
 * Transaction commit may fail due to concurrent transaction modified data in
 * incompatible way and was commited earlier. See {@link #commit()} for more
 * concrete examples.
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
 * <code>
 * txWrite = broker.newReadWriteTransactoin(); // concurrent write transaction
 *
 * txWrite.read(OPERATIONAL,PATH).get() // will return Optional containing A
 * txWrite.put(OPERATIONAL,PATH,B); // writes B to PATH
 * txWrite.read(OPERATIONAL,PATH).get() // will return Optional Containing B
 *
 * txWrite.commit().get(); // data tree is updated, PATH contains B
 *
 * txAfterCommit = broker.newReadOnlyTransaction(); // read Transaction is snapshot of new state
 * txAfterCommit.read(OPERATIONAL,PATH).get(); // returns Optional containing B
 * </code>
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
 * <code>
 * txRead = broker.newReadOnlyTransaction(); // read Transaction is snapshot of data
 * txWrite = broker.newReadWriteTransactoin(); // concurrent write transaction
 * txRead.read(OPERATIONAL,PATH).get(); // will return Optional containing A
 *
 * txWrite.read(OPERATIONAL,PATH).get() // will return Optional containing A
 * txWrite.put(OPERATIONAL,PATH,B); // writes B to PATH
 * txWrite.read(OPERATIONAL,PATH).get() // will return Optional Containing B
 *
 * txRead.read(OPERATIONAL,PATH).get(); // concurrent read transaction still returns Optional containing A"
 *
 * txWrite.commit().get(); // data tree is updated, PATH contains B
 * txRead.read(OPERATIONAL,PATH).get(); // still returns Optional containing A
 *
 * txAfterCommit = broker.newReadOnlyTransaction(); // read Transaction is snapshot of new state
 * txAfterCommit.read(OPERATIONAL,PATH).get(); // returns Optional containing B
 * </code>
 * </pre>
 *
 * <h3>Transaction isolation (2 concurrent read-write transactions)</h3> Let
 * assume initial state of data tree for <code>PATH</code> is <code>A</code>.
 *
 * <pre>
 * <code>
 * txA = broker.newReadWriteTransaction(); // read Transaction is snapshot of data
 * txB = broker.newReadWriteTransactoin(); // concurrent write transaction
 * txA.read(OPERATIONAL,PATH).get(); // will return Optional containing A
 *
 * txB.read(OPERATIONAL,PATH).get() // will return Optional containing A
 * txB.put(OPERATIONAL,PATH,B); // writes B to PATH
 * txB.read(OPERATIONAL,PATH).get() // will return Optional Containing B
 *
 * txA.read(OPERATIONAL,PATH).get(); // txA read-write transaction still sees Optional containing A
 *                                   // since is isolated from B
 * txA.put(OPERATIONAL,PATH,C); // writes B to PATH
 * txA.read(OPERATIONAL,PATH).get() // will return Optional Containing C
 *
 * txB.read(OPERATIONAL,PATH).get() // txB read-write transaction still sees Optional containing A
 *                                  // since is isolated from B
 *
 * txB.commit().get(); // data tree is updated, PATH contains B
 * txA.read(OPERATIONAL,PATH).get(); // still returns Optional containing C since is isolated from B
 *
 * txAfterCommit = broker.newReadOnlyTransaction(); // read Transaction is snapshot of new state
 * txAfterCommit.read(OPERATIONAL,PATH).get(); // returns Optional containing B
 *
 * txA.commit() // Will fail with OptimisticLockFailedException
 *              // which means concurrent transaction changed same PATH
 *
 * </code>
 * </pre>
 *
 * <b>Note:</b> examples contains blocking calls on future only to illustrate
 * that action happened after other asynchronous action. Use of blocking call
 * {@link com.google.common.util.concurrent.ListenableFuture#get()} is discouraged for most uses and you should
 * use
 * {@link com.google.common.util.concurrent.ListenableFuture#addListener(Runnable, java.util.concurrent.Executor)}
 * or helper functions from {@link com.google.common.uti.concurrent.Futures} to
 * register more specific listeners.
 *
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
