/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.Builder;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.SimpleEventFactory;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerWalker;
import org.opendaylight.yangtools.util.concurrent.NotificationManager;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolve Data Change Events based on modifications and listeners
 *
 * Computes data change events for all affected registered listeners in data
 * tree.
 */
final class ResolveDataChangeEventsTask {
    private static final Logger LOG = LoggerFactory.getLogger(ResolveDataChangeEventsTask.class);

    private final DataTreeCandidate candidate;
    private final ListenerTree listenerRoot;

    private Multimap<DataChangeListenerRegistration<?>, DOMImmutableDataChangeEvent> collectedEvents;

    public ResolveDataChangeEventsTask(final DataTreeCandidate candidate, final ListenerTree listenerTree) {
        this.candidate = Preconditions.checkNotNull(candidate);
        this.listenerRoot = Preconditions.checkNotNull(listenerTree);
    }

    /**
     * Resolves and submits notification tasks to the specified manager.
     */
    public synchronized void resolve(final NotificationManager<DataChangeListenerRegistration<?>, DOMImmutableDataChangeEvent> manager) {
        try (final ListenerWalker w = listenerRoot.getWalker()) {
            // Defensive: reset internal state
            collectedEvents = ArrayListMultimap.create();

            // Run through the tree
            final ResolveDataChangeState s = ResolveDataChangeState.initial(candidate.getRootPath(), w.getRootNode());
            resolveAnyChangeEvent(s, candidate.getRootNode());

            /*
             * Convert to tasks, but be mindful of multiple values -- those indicate multiple
             * wildcard matches, which need to be merged.
             */
            for (Entry<DataChangeListenerRegistration<?>, Collection<DOMImmutableDataChangeEvent>> e : collectedEvents.asMap().entrySet()) {
                final Collection<DOMImmutableDataChangeEvent> col = e.getValue();
                final DOMImmutableDataChangeEvent event;

                if (col.size() != 1) {
                    final Builder b = DOMImmutableDataChangeEvent.builder(DataChangeScope.BASE);
                    for (DOMImmutableDataChangeEvent i : col) {
                        b.merge(i);
                    }

                    event = b.build();
                    LOG.trace("Merged events {} into event {}", col, event);
                } else {
                    event = col.iterator().next();
                }

                manager.submitNotification(e.getKey(), event);
            }
        }
    }

    /**
     * Resolves data change event for supplied node
     *
     * @param path
     *            Path to current node in tree
     * @param listeners
     *            Collection of Listener registration nodes interested in
     *            subtree
     * @param modification
     *            Modification of current node
     * @param before
     *            - Original (before) state of current node
     * @param after
     *            - After state of current node
     * @return True if the subtree changed, false otherwise
     */
    private boolean resolveAnyChangeEvent(final ResolveDataChangeState state, final DataTreeCandidateNode node) {
        if (node.getModificationType() != ModificationType.UNMODIFIED &&
                !node.getDataAfter().isPresent() && !node.getDataBefore().isPresent()) {
            LOG.debug("Modification at {} has type {}, but no before- and after-data. Assuming unchanged.",
                    state.getPath(), node.getModificationType());
            return false;
        }

        // no before and after state is present

        switch (node.getModificationType()) {
        case SUBTREE_MODIFIED:
            return resolveSubtreeChangeEvent(state, node);
        case MERGE:
        case WRITE:
            Preconditions.checkArgument(node.getDataAfter().isPresent(),
                    "Modification at {} has type {} but no after-data", state.getPath(), node.getModificationType());
            if (!node.getDataBefore().isPresent()) {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                final NormalizedNode<PathArgument, ?> afterNode = (NormalizedNode)node.getDataAfter().get();
                resolveSameEventRecursivelly(state, afterNode, DOMImmutableDataChangeEvent.getCreateEventFactory());
                return true;
            }

            return resolveReplacedEvent(state, node.getDataBefore().get(), node.getDataAfter().get());
        case DELETE:
            Preconditions.checkArgument(node.getDataBefore().isPresent(),
                    "Modification at {} has type {} but no before-data", state.getPath(), node.getModificationType());

            @SuppressWarnings({ "unchecked", "rawtypes" })
            final NormalizedNode<PathArgument, ?> beforeNode = (NormalizedNode)node.getDataBefore().get();
            resolveSameEventRecursivelly(state, beforeNode, DOMImmutableDataChangeEvent.getRemoveEventFactory());
            return true;
        case UNMODIFIED:
            return false;
        }

        throw new IllegalStateException(String.format("Unhandled node state %s at %s", node.getModificationType(), state.getPath()));
    }

    private boolean resolveReplacedEvent(final ResolveDataChangeState state,
            final NormalizedNode<?, ?> beforeData, final NormalizedNode<?, ?> afterData) {

        if (beforeData instanceof NormalizedNodeContainer<?, ?, ?>) {
            /*
             * Node is a container (contains a child) and we have interested
             * listeners registered for it, that means we need to do
             * resolution of changes on children level and can not
             * shortcut resolution.
             */
            LOG.trace("Resolving subtree replace event for {} before {}, after {}", state.getPath(), beforeData, afterData);
            @SuppressWarnings("unchecked")
            NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>> beforeCont = (NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>>) beforeData;
            @SuppressWarnings("unchecked")
            NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>> afterCont = (NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>>) afterData;
            return resolveNodeContainerReplaced(state, beforeCont, afterCont);
        }

        // Node is a Leaf type (does not contain child nodes)
        // so normal equals method is sufficient for determining change.
        if (beforeData.equals(afterData)) {
            LOG.trace("Skipping equal leaf {}", state.getPath());
            return false;
        }

        LOG.trace("Resolving leaf replace event for {} , before {}, after {}", state.getPath(), beforeData, afterData);
        DOMImmutableDataChangeEvent event = DOMImmutableDataChangeEvent.builder(DataChangeScope.BASE).addUpdated(state.getPath(), beforeData, afterData).build();
        state.addEvent(event);
        state.collectEvents(beforeData, afterData, collectedEvents);
        return true;
    }

    private boolean resolveNodeContainerReplaced(final ResolveDataChangeState state,
            final NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>> beforeCont,
                    final NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>> afterCont) {
        if (!state.needsProcessing()) {
            LOG.trace("Not processing replaced container {}", state.getPath());
            return true;
        }

        // We look at all children from before and compare it with after state.
        boolean childChanged = false;
        for (NormalizedNode<PathArgument, ?> beforeChild : beforeCont.getValue()) {
            final PathArgument childId = beforeChild.getIdentifier();

            if (resolveNodeContainerChildUpdated(state.child(childId), beforeChild, afterCont.getChild(childId))) {
                childChanged = true;
            }
        }

        for (NormalizedNode<PathArgument, ?> afterChild : afterCont.getValue()) {
            final PathArgument childId = afterChild.getIdentifier();

            /*
             * We have already iterated of the before-children, so have already
             * emitted modify/delete events. This means the child has been
             * created.
             */
            if (!beforeCont.getChild(childId).isPresent()) {
                resolveSameEventRecursivelly(state.child(childId), afterChild, DOMImmutableDataChangeEvent.getCreateEventFactory());
                childChanged = true;
            }
        }

        if (childChanged) {
            DOMImmutableDataChangeEvent event = DOMImmutableDataChangeEvent.builder(DataChangeScope.BASE)
                    .addUpdated(state.getPath(), beforeCont, afterCont).build();
            state.addEvent(event);
        }

        state.collectEvents(beforeCont, afterCont, collectedEvents);
        return childChanged;
    }

    private boolean resolveNodeContainerChildUpdated(final ResolveDataChangeState state,
            final NormalizedNode<PathArgument, ?> before, final Optional<NormalizedNode<PathArgument, ?>> after) {
        if (after.isPresent()) {
            // REPLACE or SUBTREE Modified
            return resolveReplacedEvent(state, before, after.get());
        }

        // AFTER state is not present - child was deleted.
        resolveSameEventRecursivelly(state, before, DOMImmutableDataChangeEvent.getRemoveEventFactory());
        return true;
    }

    private void resolveSameEventRecursivelly(final ResolveDataChangeState state,
            final NormalizedNode<PathArgument, ?> node, final SimpleEventFactory eventFactory) {
        if (!state.needsProcessing()) {
            LOG.trace("Skipping child {}", state.getPath());
            return;
        }

        // We have listeners for this node or it's children, so we will try
        // to do additional processing
        if (node instanceof NormalizedNodeContainer<?, ?, ?>) {
            LOG.trace("Resolving subtree recursive event for {}, type {}", state.getPath(), eventFactory);

            // Node has children, so we will try to resolve it's children
            // changes.
            @SuppressWarnings("unchecked")
            NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>> container = (NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>>) node;
            for (NormalizedNode<PathArgument, ?> child : container.getValue()) {
                final PathArgument childId = child.getIdentifier();

                LOG.trace("Resolving event for child {}", childId);
                resolveSameEventRecursivelly(state.child(childId), child, eventFactory);
            }
        }

        final DOMImmutableDataChangeEvent event = eventFactory.create(state.getPath(), node);
        LOG.trace("Adding event {} at path {}", event, state.getPath());
        state.addEvent(event);
        state.collectEvents(event.getOriginalSubtree(), event.getUpdatedSubtree(), collectedEvents);
    }

    private boolean resolveSubtreeChangeEvent(final ResolveDataChangeState state, final DataTreeCandidateNode modification) {
        Preconditions.checkArgument(modification.getDataBefore().isPresent(), "Subtree change with before-data not present at path %s", state.getPath());
        Preconditions.checkArgument(modification.getDataAfter().isPresent(), "Subtree change with after-data not present at path %s", state.getPath());

        if (!state.needsProcessing()) {
            LOG.trace("Not processing modified subtree {}", state.getPath());
            return true;
        }

        DataChangeScope scope = null;
        for (DataTreeCandidateNode childMod : modification.getChildNodes()) {
            final ResolveDataChangeState childState = state.child(childMod.getIdentifier());

            switch (childMod.getModificationType()) {
            case WRITE:
            case MERGE:
            case DELETE:
                if (resolveAnyChangeEvent(childState, childMod)) {
                    scope = DataChangeScope.ONE;
                }
                break;
            case SUBTREE_MODIFIED:
                if (resolveSubtreeChangeEvent(childState, childMod) && scope == null) {
                    scope = DataChangeScope.SUBTREE;
                }
                break;
            case UNMODIFIED:
                // no-op
                break;
            }
        }

        final NormalizedNode<?, ?> before = modification.getDataBefore().get();
        final NormalizedNode<?, ?> after = modification.getDataAfter().get();

        if (scope != null) {
            DOMImmutableDataChangeEvent one = DOMImmutableDataChangeEvent.builder(scope).addUpdated(state.getPath(), before, after).build();
            state.addEvent(one);
        }

        state.collectEvents(before, after, collectedEvents);
        return scope != null;
    }

    public static ResolveDataChangeEventsTask create(final DataTreeCandidate candidate, final ListenerTree listenerTree) {
        return new ResolveDataChangeEventsTask(candidate, listenerTree);
    }
}
