/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;

/**
 * Message used to batch write, merge, delete modification operations to the  ShardTransaction actor.
 *
 * @author Thomas Pantelis
 */
public class BatchedModifications extends MutableCompositeModification implements SerializableMessage {
    private static final long serialVersionUID = 1L;

    public BatchedModifications() {
    }

    public BatchedModifications(short version) {
        super(version);
    }

    @Override
    public Object toSerializable() {
        return this;
    }
}
