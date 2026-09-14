/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

abstract class AbstractTransactionSuccessTest<T extends TransactionSuccess<T>>
        extends AbstractRequestSuccessTest<T> {
    static final TransactionIdentifier TRANSACTION_IDENTIFIER = new TransactionIdentifier(HISTORY_IDENTIFIER, 0);

    AbstractTransactionSuccessTest(final T object, final int expectedSize) {
        super(object, expectedSize);
    }
}
