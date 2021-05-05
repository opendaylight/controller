/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.datastore.messages.OnInitialData;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

final class RootDataTreeChangeListenerActor extends DataTreeChangeListenerActor {
    private final int shardCount;

    // Initial messages, retaining order in which we have received them
    private Map<ActorRef, Object> initialMessages = new LinkedHashMap<>();
    private Deque<DataTreeChanged> otherMessages = new ArrayDeque<>();

    private RootDataTreeChangeListenerActor(final DOMDataTreeChangeListener listener, final int shardCount) {
        super(listener, YangInstanceIdentifier.empty());
        this.shardCount = shardCount;
    }

    @Override
    void onInitialData(final OnInitialData message) {
        final ActorRef sender = getSender();
        verifyNotNull(initialMessages, "Received OnInitialData from %s after initial convergence", sender);

        final Object prev = initialMessages.put(sender, message);
        verify(prev == null, "Received OnInitialData from %s after %s", sender, prev);
        checkInitialConvergence();
    }

    @Override
    void dataTreeChanged(final DataTreeChanged message) {
        if (initialMessages == null) {
            super.dataTreeChanged(message);
        } else {
            processMessage(message);
        }
    }

    private void processMessage(final DataTreeChanged message) {
        // Put the message into initial messages if we do not have a message from that actor yet. If we do, just stash
        // it to other messages for later processing.
        if (initialMessages.putIfAbsent(getSender(), message) == null) {
            checkInitialConvergence();
        } else {
            otherMessages.addLast(message);
        }
    }

    private void checkInitialConvergence() {
        if (initialMessages.size() != shardCount) {
            // We do not have initial state from all shards yet
            return;
        }

        /*
         * We need to make-pretend that the data coming into the listener is coming from a single logical entity, where
         * ordering is partially guaranteed (on shard boundaries). The data layout in shards is such that each DataTree
         * is rooted at YangInstanceIdentifier.empty(), but their contents vary:
         *
         * 1) non-default shards contain immediate children of root from one module
         * 2) default shard contains everything else
         * 3) there is no overlap between shards
         *
         * When we subscribe to each of the shards, each of them will report root as being written, which is an accurate
         * view from each shard's perspective, but it does not reflect the aggregate reality.
         *
         * Construct an overall NormalizedNode view of the entire datastore by combining first-level children from all
         * reported initial state reports, report that node as written and then report any additional deltas.
         */
        final Deque<DataTreeCandidate> initialChanges = new ArrayDeque<>();
        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> rootBuilder = Builders.containerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(SchemaContext.NAME));
        for (Object message : initialMessages.values()) {
            if (message instanceof DataTreeChanged) {
                final Collection<DataTreeCandidate> changes = ((DataTreeChanged) message).getChanges();
                final DataTreeCandidate initial;
                if (changes.size() != 1) {
                    final Iterator<DataTreeCandidate> it = changes.iterator();
                    initial = it.next();
                    // Append to changes to report as initial. This should not be happening (often?).
                    it.forEachRemaining(initialChanges::addLast);
                } else {
                    initial = Iterables.get(changes, 0);
                }

                final NormalizedNode root = initial.getRootNode().getDataAfter().orElseThrow();
                verify(root instanceof ContainerNode, "Unexpected root node %s", root);
                ((ContainerNode) root).body().forEach(rootBuilder::withChild);
            }
        }
        // We will not be intercepting any other messages, allow initial state to be reclaimed as soon as possible
        initialMessages = null;

        // Prepend combined initial changed and report initial changes and clear the map
        initialChanges.addFirst(DataTreeCandidates.newDataTreeCandidate(YangInstanceIdentifier.empty(),
            DataTreeCandidateNodes.written(rootBuilder.build())));
        super.dataTreeChanged(new DataTreeChanged(initialChanges));

        // Now go through all messages we have held back and report them. Note we are removing them from the queue
        // to allow them to be reclaimed as soon as possible.
        for (DataTreeChanged message = otherMessages.poll(); message != null; message = otherMessages.poll()) {
            super.dataTreeChanged(message);
        }
        otherMessages = null;
    }

    static Props props(final DOMDataTreeChangeListener instance, final int shardCount) {
        return Props.create(RootDataTreeChangeListenerActor.class, instance, shardCount);
    }
}
