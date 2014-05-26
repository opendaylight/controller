/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;

/**
 *
 * Listener on transaction failures.
 *
 *
 */
public interface DOMDataCommitErrorListener {


    /**
     *
     * Implementation of this callback MUST NOT do any blocking calls
     * or any calls to MD-SAL, since this callback is invoked synchronously
     * on MD-SAL Broker coordination thread.
     *
     * @param tx Transaction which failed
     * @param cause Failure reason
     */
    public void onCommitFailed(DOMDataWriteTransaction tx, Throwable cause);

}
