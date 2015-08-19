/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.messages;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class UnregisterListListener {
    private final YangInstanceIdentifier listPath;

    public UnregisterListListener(YangInstanceIdentifier listPath) {
        this.listPath = Preconditions.checkNotNull(listPath, "listPath should not be null");
    }

    public YangInstanceIdentifier getListPath() {
        return listPath;
    }
}

