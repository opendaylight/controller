/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.datastore.messages.OnInitialData;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class InitialShardDataComposer implements InitialShardDataCollector {

    private final ConcurrentLinkedQueue<DataTreeCandidate> collected;
    private final CountDownLatch expectedReplies;

    public InitialShardDataComposer(final CountDownLatch expectedReplies) {
        this.expectedReplies = expectedReplies;
        Preconditions.checkArgument(expectedReplies.getCount() > 0,
                "Expected amount of replies must be greater then 0. Was: %s", expectedReplies.getCount());
        this.collected = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void collectInitialState(String shardName, DataTreeCandidate initialData) {
        if (initialData != null) {
            collected.add(initialData);
        }
        expectedReplies.countDown();
    }

    public DataTreeCandidate composeCollectedState() {
        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> rootDataBuilder =
                ImmutableContainerNodeBuilder.create()
                    .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(SchemaContext.NAME));
        for (DataTreeCandidate candidate: collected) {
            Collection<DataTreeCandidateNode> topShardNodes = candidate.getRootNode().getChildNodes();
            for (DataTreeCandidateNode topShardNode: topShardNodes) {
                if (topShardNode.getDataAfter().isPresent()) {
                    rootDataBuilder.withChild((ContainerNode)topShardNode.getDataAfter().get());
                }
            }
        }
        return DataTreeCandidates.fromNormalizedNode(YangInstanceIdentifier.empty(), rootDataBuilder.build());
    }

    public void notifyListener(final ActorRef dataChangeListenerActor) {
        if (collected.size() > 0) {
            for (DataTreeCandidate candidate : collected) {
                dataChangeListenerActor.tell(new DataTreeChanged(Collections.singleton(candidate)),
                        ActorRef.noSender());
            }
        } else {
            dataChangeListenerActor.tell(OnInitialData.INSTANCE, ActorRef.noSender());
        }
    }
}
