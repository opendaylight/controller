/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;


import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Base class to be used for all simple modifications that can be applied to a DOMStoreTransaction
 */
public abstract class AbstractModification implements Modification {

    private YangInstanceIdentifier path;
    private short version;

    protected AbstractModification(short version) {
        this.version = version;
    }

    protected AbstractModification(YangInstanceIdentifier path) {
        this.path = path;
    }

    protected void setPath(YangInstanceIdentifier path) {
        this.path = path;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public short getVersion() {
        return version;
    }
}
