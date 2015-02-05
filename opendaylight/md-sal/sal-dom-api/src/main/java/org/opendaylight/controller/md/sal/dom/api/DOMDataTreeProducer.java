/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

/**
 * A data producer context. It allows transactions to be submitted to the subtrees
 * specified at instantiation time. At any given time there may be a single transaction
 * open. It needs to be either submitted or cancelled before another one can be open.
 * Once a transaction is submitted, it will proceed to be committed asynchronously.
 *
 * FIXME: each producer has an upper bound on the number of transactions which are
 *        in-flight. Once that capacity is exceeded, we need a blocking call to throttle
 *        the user who is attempting to allocate them.
 */
public interface DOMDataTreeProducer {
    DOMDataWriteTransaction newTransaction();
}
