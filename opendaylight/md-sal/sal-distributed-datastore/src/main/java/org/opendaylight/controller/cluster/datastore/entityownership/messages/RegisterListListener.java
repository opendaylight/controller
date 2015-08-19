/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.messages;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import org.opendaylight.controller.cluster.datastore.utils.YangListChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class RegisterListListener {
    private final YangInstanceIdentifier listPath;
    private final Supplier<YangListChangeListener> listChangeListenerFactory;

    public RegisterListListener(YangInstanceIdentifier listPath,
                                 Supplier<YangListChangeListener> listChangeListenerFactory) {
        this.listPath = Preconditions.checkNotNull(listPath, "listPath should not be null");
        this.listChangeListenerFactory = Preconditions.checkNotNull(listChangeListenerFactory,
            "listChangeListenerFactory should not be null");
    }

    public YangInstanceIdentifier getListPath() {
        return listPath;
    }

    public Supplier<YangListChangeListener> getListChangeListenerFactory() {
        return listChangeListenerFactory;
    }
}