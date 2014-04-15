/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.builder;
import static org.opendaylight.controller.md.sal.dom.store.impl.StoreUtils.append;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.Builder;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.SimpleEventFactory;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree.Node;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree.Walker;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

/**
 *
 * Resolve Data Change Events based on modifications and listeners
 *
 * Computes data change events for all affected registered listeners in data
 * tree.
 *
 * Prerequisites for computation is to set all parameters properly:
 * <ul>
 * <li>{@link #setRootPath(InstanceIdentifier)} - Root path of datastore
 * <li>{@link #setListenerRoot(ListenerTree)} - Root of listener registration
 * tree, which contains listeners to be notified
 * <li>{@link #setModificationRoot(NodeModification)} - Modification root, for
 * which events should be computed
 * <li>{@link #setBeforeRoot(Optional)} - State of before modification occurred
 * <li>{@link #setAfterRoot(Optional)} - State of after modification occurred
 * </ul>
 *
 */
public class ResolveDataChangeEventsTask implements Callable<Iterable<ChangeListenerNotifyTask>> {
    private static final Logger LOG = LoggerFactory.getLogger(ResolveDataChangeEventsTask.class);
    private static final DOMImmutableDataChangeEvent NO_CHANGE = builder(DataChangeScope.BASE).build();

    private InstanceIdentifier rootPath;
    private ListenerTree listenerRoot;
    private NodeModification modificationRoot;
    private Optional<StoreMetadataNode> beforeRoot;
    private Optional<StoreMetadataNode> afterRoot;
    private final Multimap<ListenerTree.Node, DOMImmutableDataChangeEvent> events = HashMultimap.create();

    protected InstanceIdentifier getRootPath() {
        return rootPath;
    }

    protected ResolveDataChangeEventsTask setRootPath(final InstanceIdentifier rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    protected ListenerTree getListenerRoot() {
        return listenerRoot;
    }

    protected ResolveDataChangeEventsTask setListenerRoot(final ListenerTree listenerRoot) {
        this.listenerRoot = listenerRoot;
        return this;
    }

    protected NodeModification getModificationRoot() {
        return modificationRoot;
    }

    protected ResolveDataChangeEventsTask setModificationRoot(final NodeModification modificationRoot) {
        this.modificationRoot = modificationRoot;
        return this;
    }

    protected Optional<StoreMetadataNode> getBeforeRoot() {
        return beforeRoot;
    }

    protected ResolveDataChangeEventsTask setBeforeRoot(final Optional<StoreMetadataNode> beforeRoot) {
        this.beforeRoot = beforeRoot;
        return this;
    }

    protected Optional<StoreMetadataNode> getAfterRoot() {
        return afterRoot;
    }

    protected ResolveDataChangeEventsTask setAfterRoot(final Optional<StoreMetadataNode> afterRoot) {
        this.afterRoot = afterRoot;
        return this;
    }

    /**
     * Resolves and creates Notification Tasks
     *
     * Implementation of done as Map-Reduce with two steps: 1. resolving events
     * and their mapping to listeners 2. merging events affecting same listener
     *
     * @return Iterable of Notification Tasks which needs to be executed in
     *         order to delivery data change events.
     */
    @Override
    public Iterable<ChangeListenerNotifyTask> call() {
        LOG.trace("Resolving events for {}", modificationRoot);

        try (final Walker w = listenerRoot.getWalker()) {
            resolveAnyChangeEvent(rootPath, Collections.singleton(w.getRootNode()), modificationRoot, beforeRoot,
                    afterRoot);
            return createNotificationTasks();
        }
    }

    /**
     *
     * Walks map of listeners to data change events, creates notification
     * delivery tasks.
     *
     * Walks map of registered and affected listeners and creates notification
     * tasks from set of listeners and events to be delivered.
     *
     * If set of listeners has more then one event (applicable to wildcarded
     * listeners), merges all data change events into one, final which contains
     * all separate updates.
     *
     * Dispatch between merge variant and reuse variant of notification task is
     * done in
     * {@link #addNotificationTask(com.google.common.collect.ImmutableList.Builder, Node, Collection)}
     *
     * @return Collection of notification tasks.
     */
    private Collection<ChangeListenerNotifyTask> createNotificationTasks() {
        ImmutableList.Builder<ChangeListenerNotifyTask> taskListBuilder = ImmutableList.builder();
        for (Entry<ListenerTree.Node, Collection<DOMImmutableDataChangeEvent>> entry : events.asMap().entrySet()) {
            addNotificationTask(taskListBuilder, entry.getKey(), entry.getValue());
        }
        return taskListBuilder.build();
    }

    /**
     * Adds notification task to task list.
     *
     * If entry collection contains one event, this event is reused and added to
     * notification tasks for listeners (see
     * {@link #addNotificationTaskByScope(com.google.common.collect.ImmutableList.Builder, Node, DOMImmutableDataChangeEvent)}
     * . Otherwise events are merged by scope and distributed between listeners
     * to particular scope. See
     * {@link #addNotificationTasksAndMergeEvents(com.google.common.collect.ImmutableList.Builder, Node, Collection)}
     * .
     *
     * @param taskListBuilder
     * @param listeners
     * @param entries
     */
    private static void addNotificationTask(final ImmutableList.Builder<ChangeListenerNotifyTask> taskListBuilder,
            final ListenerTree.Node listeners, final Collection<DOMImmutableDataChangeEvent> entries) {

        if (!entries.isEmpty()) {
            if (entries.size() == 1) {
                addNotificationTaskByScope(taskListBuilder, listeners, Iterables.getOnlyElement(entries));
            } else {
                addNotificationTasksAndMergeEvents(taskListBuilder, listeners, entries);
            }
        }
    }

    /**
     *
     * Add notification deliveries task to the listener.
     *
     *
     * @param taskListBuilder
     * @param listeners
     * @param event
     */
    private static void addNotificationTaskByScope(
            final ImmutableList.Builder<ChangeListenerNotifyTask> taskListBuilder, final ListenerTree.Node listeners,
            final DOMImmutableDataChangeEvent event) {
        DataChangeScope eventScope = event.getScope();
        for (DataChangeListenerRegistration<?> listenerReg : listeners.getListeners()) {
            DataChangeScope listenerScope = listenerReg.getScope();
            List<DataChangeListenerRegistration<?>> listenerSet = Collections
                    .<DataChangeListenerRegistration<?>> singletonList(listenerReg);
            if (eventScope == DataChangeScope.BASE) {
                taskListBuilder.add(new ChangeListenerNotifyTask(listenerSet, event));
            } else if (eventScope == DataChangeScope.ONE && listenerScope != DataChangeScope.BASE) {
                taskListBuilder.add(new ChangeListenerNotifyTask(listenerSet, event));
            } else if (eventScope == DataChangeScope.SUBTREE && listenerScope == DataChangeScope.SUBTREE) {
                taskListBuilder.add(new ChangeListenerNotifyTask(listenerSet, event));
            }
        }
    }

    /**
     *
     * Add notification tasks with merged event
     *
     * Separate Events by scope and creates merged notification tasks for each
     * and every scope which is present.
     *
     * Adds merged events to task list based on scope requested by client.
     *
     * @param taskListBuilder
     * @param listeners
     * @param entries
     */
    private static void addNotificationTasksAndMergeEvents(
            final ImmutableList.Builder<ChangeListenerNotifyTask> taskListBuilder, final ListenerTree.Node listeners,
            final Collection<DOMImmutableDataChangeEvent> entries) {

        final Builder baseBuilder = builder(DataChangeScope.BASE);
        final Builder oneBuilder = builder(DataChangeScope.ONE);
        final Builder subtreeBuilder = builder(DataChangeScope.SUBTREE);

        boolean baseModified = false;
        boolean oneModified = false;
        boolean subtreeModified = false;
        for (final DOMImmutableDataChangeEvent entry : entries) {
            switch (entry.getScope()) {
            // Absence of breaks is intentional here. Subtree contains base and
            // one, one also contains base
            case BASE:
                baseBuilder.merge(entry);
                baseModified = true;
            case ONE:
                oneBuilder.merge(entry);
                oneModified = true;
            case SUBTREE:
                subtreeBuilder.merge(entry);
                subtreeModified = true;
            }
        }

        if (baseModified) {
            addNotificationTaskExclusively(taskListBuilder, listeners, baseBuilder.build());
        }
        if (oneModified) {
            addNotificationTaskExclusively(taskListBuilder, listeners, oneBuilder.build());
        }
        if (subtreeModified) {
            addNotificationTaskExclusively(taskListBuilder, listeners, subtreeBuilder.build());
        }
    }

    private static void addNotificationTaskExclusively(
            final ImmutableList.Builder<ChangeListenerNotifyTask> taskListBuilder, final Node listeners,
            final DOMImmutableDataChangeEvent event) {
        for (DataChangeListenerRegistration<?> listener : listeners.getListeners()) {
            if (listener.getScope() == event.getScope()) {
                Set<DataChangeListenerRegistration<?>> listenerSet = Collections
                        .<DataChangeListenerRegistration<?>> singleton(listener);
                taskListBuilder.add(new ChangeListenerNotifyTask(listenerSet, event));
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
     * @return Data Change Event of this node and all it's children
     */
    private DOMImmutableDataChangeEvent resolveAnyChangeEvent(final InstanceIdentifier path,
            final Collection<ListenerTree.Node> listeners, final NodeModification modification,
            final Optional<StoreMetadataNode> before, final Optional<StoreMetadataNode> after) {
        // No listeners are present in listener registration subtree
        // no before and after state is present
        if (!before.isPresent() && !after.isPresent()) {
            return NO_CHANGE;
        }
        switch (modification.getModificationType()) {
        case SUBTREE_MODIFIED:
            return resolveSubtreeChangeEvent(path, listeners, modification, before.get(), after.get());
        case WRITE:
            if (before.isPresent()) {
                return resolveReplacedEvent(path, listeners, before.get().getData(), after.get().getData());
            } else {
                return resolveCreateEvent(path, listeners, after.get());
            }
        case DELETE:
            return resolveDeleteEvent(path, listeners, before.get());
        default:
            return NO_CHANGE;
        }

    }

    private DOMImmutableDataChangeEvent resolveReplacedEvent(final InstanceIdentifier path,
            final Collection<Node> listeners, final NormalizedNode<?, ?> beforeData,
            final NormalizedNode<?, ?> afterData) {

        if (beforeData instanceof NormalizedNodeContainer<?, ?, ?> && !listeners.isEmpty()) {
            // Node is container (contains child) and we have interested
            // listeners registered for it, that means we need to do
            // resolution of changes on children level and can not
            // shortcut resolution.

            @SuppressWarnings("unchecked")
            NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>> beforeCont = (NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>>) beforeData;
            @SuppressWarnings("unchecked")
            NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>> afterCont = (NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>>) afterData;
            return resolveNodeContainerReplaced(path, listeners, beforeCont, afterCont);
        } else if (!beforeData.equals(afterData)) {
            // Node is either of Leaf type (does not contain child nodes)
            // or we do not have listeners, so normal equals method is
            // sufficient for determining change.

            DOMImmutableDataChangeEvent event = builder(DataChangeScope.BASE).setBefore(beforeData).setAfter(afterData)
                    .addUpdated(path, beforeData, afterData).build();
            addPartialTask(listeners, event);
            return event;
        } else {
            return NO_CHANGE;
        }
    }

    private DOMImmutableDataChangeEvent resolveNodeContainerReplaced(final InstanceIdentifier path,
            final Collection<Node> listeners,
            final NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>> beforeCont,
            final NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>> afterCont) {
        final Set<PathArgument> alreadyProcessed = new HashSet<>();
        final List<DOMImmutableDataChangeEvent> childChanges = new LinkedList<>();

        DataChangeScope potentialScope = DataChangeScope.BASE;
        // We look at all children from before and compare it with after state.
        for (NormalizedNode<PathArgument, ?> beforeChild : beforeCont.getValue()) {
            PathArgument childId = beforeChild.getIdentifier();
            alreadyProcessed.add(childId);
            InstanceIdentifier childPath = append(path, childId);
            Collection<ListenerTree.Node> childListeners = getListenerChildrenWildcarded(listeners, childId);
            Optional<NormalizedNode<PathArgument, ?>> afterChild = afterCont.getChild(childId);
            DOMImmutableDataChangeEvent childChange = resolveNodeContainerChildUpdated(childPath, childListeners,
                    beforeChild, afterChild);
            // If change is empty (equals to NO_CHANGE)
            if (childChange != NO_CHANGE) {
                childChanges.add(childChange);
            }

        }

        for (NormalizedNode<PathArgument, ?> afterChild : afterCont.getValue()) {
            PathArgument childId = afterChild.getIdentifier();
            if (!alreadyProcessed.contains(childId)) {
                // We did not processed that child already
                // and it was not present in previous loop, that means it is
                // created.
                Collection<ListenerTree.Node> childListeners = getListenerChildrenWildcarded(listeners, childId);
                InstanceIdentifier childPath = append(path,childId);
                childChanges.add(resolveSameEventRecursivelly(childPath , childListeners, afterChild,
                        DOMImmutableDataChangeEvent.getCreateEventFactory()));
            }
        }
        if (childChanges.isEmpty()) {
            return NO_CHANGE;
        }

        Builder eventBuilder = builder(potentialScope) //
                .setBefore(beforeCont) //
                .setAfter(afterCont);
        for (DOMImmutableDataChangeEvent childChange : childChanges) {
            eventBuilder.merge(childChange);
        }

        DOMImmutableDataChangeEvent replaceEvent = eventBuilder.build();
        addPartialTask(listeners, replaceEvent);
        return replaceEvent;
    }

    private DOMImmutableDataChangeEvent resolveNodeContainerChildUpdated(final InstanceIdentifier path,
            final Collection<Node> listeners, final NormalizedNode<PathArgument, ?> before,
            final Optional<NormalizedNode<PathArgument, ?>> after) {

        if (after.isPresent()) {
            // REPLACE or SUBTREE Modified
            return resolveReplacedEvent(path, listeners, before, after.get());

        } else {
            // AFTER state is not present - child was deleted.
            return resolveSameEventRecursivelly(path, listeners, before,
                    DOMImmutableDataChangeEvent.getRemoveEventFactory());
        }
    }

    /**
     * Resolves create events deep down the interest listener tree.
     *
     *
     * @param path
     * @param listeners
     * @param afterState
     * @return
     */
    private DOMImmutableDataChangeEvent resolveCreateEvent(final InstanceIdentifier path,
            final Collection<ListenerTree.Node> listeners, final StoreMetadataNode afterState) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final NormalizedNode<PathArgument, ?> node = (NormalizedNode) afterState.getData();
        return resolveSameEventRecursivelly(path, listeners, node, DOMImmutableDataChangeEvent.getCreateEventFactory());
    }

    private DOMImmutableDataChangeEvent resolveDeleteEvent(final InstanceIdentifier path,
            final Collection<ListenerTree.Node> listeners, final StoreMetadataNode beforeState) {

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final NormalizedNode<PathArgument, ?> node = (NormalizedNode) beforeState.getData();
        return resolveSameEventRecursivelly(path, listeners, node, DOMImmutableDataChangeEvent.getRemoveEventFactory());
    }

    private DOMImmutableDataChangeEvent resolveSameEventRecursivelly(final InstanceIdentifier path,
            final Collection<Node> listeners, final NormalizedNode<PathArgument, ?> node,
            final SimpleEventFactory eventFactory) {

        DOMImmutableDataChangeEvent event = eventFactory.create(path, node);

        if (!listeners.isEmpty()) {
            // We have listeners for this node or it's children, so we will try
            // to do additional processing
            if (node instanceof NormalizedNodeContainer<?, ?, ?>) {
                // Node has children, so we will try to resolve it's children
                // changes.
                @SuppressWarnings("unchecked")
                NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>> container = (NormalizedNodeContainer<?, PathArgument, NormalizedNode<PathArgument, ?>>) node;
                for (NormalizedNode<PathArgument, ?> child : container.getValue()) {
                    PathArgument childId = child.getIdentifier();
                    Collection<Node> childListeners = getListenerChildrenWildcarded(listeners, childId);
                    if (!childListeners.isEmpty()) {
                        resolveSameEventRecursivelly(append(path, childId), childListeners, child, eventFactory);
                    }
                }
            }
            addPartialTask(listeners, event);
        }
        return event;
    }

    private DOMImmutableDataChangeEvent resolveSubtreeChangeEvent(final InstanceIdentifier path,
            final Collection<ListenerTree.Node> listeners, final NodeModification modification,
            final StoreMetadataNode before, final StoreMetadataNode after) {

        Builder one = builder(DataChangeScope.ONE).setBefore(before.getData()).setAfter(after.getData());

        Builder subtree = builder(DataChangeScope.SUBTREE);

        for (NodeModification childMod : modification.getModifications()) {
            PathArgument childId = childMod.getIdentifier();
            InstanceIdentifier childPath = append(path, childId);
            Collection<ListenerTree.Node> childListeners = getListenerChildrenWildcarded(listeners, childId);

            Optional<StoreMetadataNode> childBefore = before.getChild(childId);
            Optional<StoreMetadataNode> childAfter = after.getChild(childId);

            switch (childMod.getModificationType()) {
            case WRITE:
            case DELETE:
                one.merge(resolveAnyChangeEvent(childPath, childListeners, childMod, childBefore, childAfter));
                break;
            case SUBTREE_MODIFIED:
                subtree.merge(resolveSubtreeChangeEvent(childPath, childListeners, childMod, childBefore.get(),
                        childAfter.get()));
                break;
            case UNMODIFIED:
                // no-op
                break;
            }
        }
        DOMImmutableDataChangeEvent oneChangeEvent = one.build();
        subtree.merge(oneChangeEvent);
        DOMImmutableDataChangeEvent subtreeEvent = subtree.build();
        if (!listeners.isEmpty()) {
            addPartialTask(listeners, oneChangeEvent);
            addPartialTask(listeners, subtreeEvent);
        }
        return subtreeEvent;
    }

    private DOMImmutableDataChangeEvent addPartialTask(final Collection<ListenerTree.Node> listeners,
            final DOMImmutableDataChangeEvent event) {

        for (ListenerTree.Node listenerNode : listeners) {
            if (!listenerNode.getListeners().isEmpty()) {
                events.put(listenerNode, event);
            }
        }
        return event;
    }

    private static Collection<ListenerTree.Node> getListenerChildrenWildcarded(final Collection<ListenerTree.Node> parentNodes,
            final PathArgument child) {
        if (parentNodes.isEmpty()) {
            return Collections.emptyList();
        }
        com.google.common.collect.ImmutableList.Builder<ListenerTree.Node> result = ImmutableList.builder();
        if (child instanceof NodeWithValue || child instanceof NodeIdentifierWithPredicates) {
            NodeIdentifier wildcardedIdentifier = new NodeIdentifier(child.getNodeType());
            addChildrenNodesToBuilder(result, parentNodes, wildcardedIdentifier);
        }
        addChildrenNodesToBuilder(result, parentNodes, child);
        return result.build();
    }

    private static void addChildrenNodesToBuilder(final ImmutableList.Builder<ListenerTree.Node> result,
            final Collection<ListenerTree.Node> parentNodes, final PathArgument childIdentifier) {
        for (ListenerTree.Node node : parentNodes) {
            Optional<ListenerTree.Node> child = node.getChild(childIdentifier);
            if (child.isPresent()) {
                result.add(child.get());
            }
        }
    }

    public static ResolveDataChangeEventsTask create() {
        return new ResolveDataChangeEventsTask();
    }
}
