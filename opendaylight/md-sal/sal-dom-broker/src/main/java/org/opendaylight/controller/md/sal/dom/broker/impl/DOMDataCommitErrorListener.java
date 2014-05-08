/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import java.util.EventListener;

import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;

/**
 *
 * Listener on transaction failure which may be passed to
 * {@link DOMDataCommitExecutor}. This listener is notified during transaction
 * processing, before result is delivered to other client code outside MD-SAL.
 * This allows implementors to update their internal state before transaction
 * failure is visible to client code.
 *
 * This is internal API for MD-SAL implementations, for consumer facing error
 * listeners see {@link org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener}.
 *
 */
interface DOMDataCommitErrorListener extends EventListener {

    /**
     *
     * Callback which is invoked on transaction failure during three phase
     * commit in {@link DOMDataCommitExecutor}.
     *
     *
     * Implementation of this callback MUST NOT do any blocking calls or any
     * calls to MD-SAL, since this callback is invoked synchronously on MD-SAL
     * Broker coordination thread.
     *
     * @param tx
     *            Transaction which failed
     * @param cause
     *            Failure reason
     */
    void onCommitFailed(DOMDataWriteTransaction tx, Throwable cause);

}
