/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;


import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.io.Serializable;

/**
 * Base class to be used for all simple modifications that can be applied to a DOMStoreTransaction
 */
public abstract class AbstractModification implements Modification,
    Serializable {

    private static final long serialVersionUID = 1638042650152084457L;

    protected final InstanceIdentifier path;

    protected AbstractModification(InstanceIdentifier path) {
        this.path = path;
    }
}
