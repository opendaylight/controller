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
import java.io.Serializable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

@Beta
public abstract class TransactionModification implements Serializable {
    private static final long serialVersionUID = 1L;
    private final YangInstanceIdentifier path;

    TransactionModification(final YangInstanceIdentifier path) {
        this.path = Preconditions.checkNotNull(path);
    }

    public final YangInstanceIdentifier getPath() {
        return path;
    }
}
