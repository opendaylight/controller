/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

/**
 * A remote message sent to locate the primary shard.
 *
 * @author Thomas Pantelis
 */
public class RemoteFindPrimary extends FindPrimary {
    private static final long serialVersionUID = 1L;

    public RemoteFindPrimary(String shardName, boolean waitUntilReady) {
        super(shardName, waitUntilReady);
    }
}
