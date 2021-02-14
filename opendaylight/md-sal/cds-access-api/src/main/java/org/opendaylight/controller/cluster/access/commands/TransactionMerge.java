/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Merge a {@link NormalizedNode} tree onto a specific path.
 *
 * @author Robert Varga
 */
@Beta
public final class TransactionMerge extends TransactionDataModification {
    public TransactionMerge(final YangInstanceIdentifier path, final NormalizedNode data) {
        super(path, data);
    }

    @Override
    byte getType() {
        return TYPE_MERGE;
    }
}
