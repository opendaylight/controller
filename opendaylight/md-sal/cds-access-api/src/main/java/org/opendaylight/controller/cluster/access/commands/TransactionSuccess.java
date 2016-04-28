/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;

public abstract class TransactionSuccess extends RequestSuccess<TransactionRequestIdentifier> {
    private static final long serialVersionUID = 1L;

    TransactionSuccess(final TransactionRequestIdentifier identifier) {
        super(identifier);
    }
}
