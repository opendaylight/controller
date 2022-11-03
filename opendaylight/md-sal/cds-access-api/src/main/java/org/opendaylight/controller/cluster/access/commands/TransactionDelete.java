/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Delete a particular path.
 */
public final class TransactionDelete extends TransactionModification {
    public TransactionDelete(final YangInstanceIdentifier path) {
        super(path);
    }

    @Override
    byte getType() {
        return TYPE_DELETE;
    }
}
