/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.CandidateAdded;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.CandidateRemoved;
import org.opendaylight.controller.cluster.datastore.utils.YangListChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.entity.Candidate;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CandidateListChangeListener extends YangListChangeListener {

    static final Logger LOG = LoggerFactory.getLogger(CandidateListChangeListener.class);
    static final QName CANDIDATE_NAME = QName.create(Candidate.QNAME, "name");
    static final  QName ENTITY_QNAME = org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.
            md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.Entity.QNAME;
    static final QName ENTITY_OWNER = QName.create(ENTITY_QNAME, "owner");

    private final ActorRef shard;

    public CandidateListChangeListener(YangInstanceIdentifier listPath, ActorRef shard) {
        super(listPath);
        this.shard = Preconditions.checkNotNull(shard, "shard should not be null");
    }

    @Override
    protected void entryAdded(YangInstanceIdentifier key, NormalizedNode<?, ?> value) {
        YangInstanceIdentifier entityId = key.getParent().getParent();

        shard.tell(new CandidateAdded(entityId, new ArrayList<>(entries())), shard);
    }
    @Override
    protected void entryRemoved(YangInstanceIdentifier key) {
        YangInstanceIdentifier.NodeIdentifierWithPredicates candidate
                = (YangInstanceIdentifier.NodeIdentifierWithPredicates) key.getLastPathArgument();

        String candidateName = candidate.getKeyValues().get(CANDIDATE_NAME).toString();
        YangInstanceIdentifier entityId = key.getParent().getParent();

        shard.tell(new CandidateRemoved(entityId, candidateName, new ArrayList<>(entries())), shard);
    }


}