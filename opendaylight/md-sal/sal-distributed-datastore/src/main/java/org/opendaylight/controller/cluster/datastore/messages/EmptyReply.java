/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.DataStoreVersions;

/**
 * A reply with no data.
 *
 * @author Thomas Pantelis
 */
public abstract class EmptyReply extends EmptyExternalizable {

    private final Object legacySerializedInstance;

    protected EmptyReply(Object legacySerializedInstance) {
        super();
        this.legacySerializedInstance = legacySerializedInstance;
    }

    public Object toSerializable(short toVersion) {
        return toVersion >= DataStoreVersions.LITHIUM_VERSION ? this : legacySerializedInstance;
    }
}
