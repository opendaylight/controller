/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Serializable;

/**
 * Local or remote message sent in reply to FindPrimaryShard to indicate the primary shard is remote to the caller.
 */
public class RemotePrimaryShardFound implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String primaryPath;
    private final short primaryVersion;

    public RemotePrimaryShardFound(final String primaryPath, short primaryVersion) {
        this.primaryPath = primaryPath;
        this.primaryVersion = primaryVersion;
    }

    public String getPrimaryPath() {
        return primaryPath;
    }

    public short getPrimaryVersion() {
        return primaryVersion;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RemotePrimaryShardFound [primaryPath=").append(primaryPath).append(", primaryVersion=")
                .append(primaryVersion).append("]");
        return builder.toString();
    }
}
