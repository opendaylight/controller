/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.ENTITY_OWNER_QNAME;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.ENTITY_QNAME;

import java.util.Optional;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.EntityType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;

public abstract class AbstractEntityOwnerChangeListener implements DOMDataTreeChangeListener {
    private static final YangInstanceIdentifier EOS_PATH = YangInstanceIdentifier.builder(ENTITY_OWNERS_PATH)
            .node(EntityType.QNAME).node(EntityType.QNAME).node(ENTITY_QNAME).node(ENTITY_QNAME)
            .node(ENTITY_OWNER_QNAME).build();

    void init(final ShardDataTree shardDataTree) {
        shardDataTree.registerTreeChangeListener(EOS_PATH, this, Optional.empty(), noop -> { /* NOOP */ });
    }

    protected static String extractOwner(final LeafNode<?> ownerLeaf) {
        return ownerLeaf.getValue().toString();
    }

}
