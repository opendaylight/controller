/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

@Beta
public abstract class TransactionDataModification extends TransactionModification {
    private static final long serialVersionUID = 1L;
    private final NormalizedNode<?, ?> data;

    TransactionDataModification(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        super(path);
        this.data = Preconditions.checkNotNull(data);
    }

    public final NormalizedNode<?, ?> getData() {
        return data;
    }
}
