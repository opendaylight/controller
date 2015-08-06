/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;


/**
 * Default implementation of ShardCreator that creates a Shrd instance.
 *
 * @author Thomas Pantelis
 */
public class DefaultShardCreator extends ShardCreator {
    private static final long serialVersionUID = 1L;

    @Override
    public Shard create() {
        validateInputs();
        return new Shard(getShardId(), getPeerAddresses(), getDatastoreContext(), getSchemaContext());
    }
}
