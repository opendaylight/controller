package org.opendaylight.controller.md.sal.dom.store.impl;

import static org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope.BASE;
import static org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope.ONE;
import static org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope.SUBTREE;
import static org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.builder;
import static org.opendaylight.controller.md.sal.dom.store.impl.StoreUtils.append;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.Builder;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree.Walker;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

public class DataChangeEventResolver {
    private static final Logger LOG = LoggerFactory.getLogger(DataChangeEventResolver.class);
    private static final DOMImmutableDataChangeEvent NO_CHANGE = builder(BASE).build();
    private final ImmutableList.Builder<ChangeListenerNotifyTask> tasks = ImmutableList.builder();
    private InstanceIdentifier rootPath;
    private ListenerTree listenerRoot;
    private NodeModification modificationRoot;
    private Optional<StoreMetadataNode> beforeRoot;
    private Optional<StoreMetadataNode> afterRoot;
    private final Multimap<ListenerTree.Node, DOMImmutableDataChangeEvent> events = HashMultimap.create();

    protected InstanceIdentifier getRootPath() {
        return rootPath;
    }

    protected DataChangeEventResolver setRootPath(final InstanceIdentifier rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    protected ListenerTree getListenerRoot() {
        return listenerRoot;
    }

    protected DataChangeEventResolver setListenerRoot(final ListenerTree listenerRoot) {
        this.listenerRoot = listenerRoot;
        return this;
    }

    protected NodeModification getModificationRoot() {
        return modificationRoot;
    }

    protected DataChangeEventResolver setModificationRoot(final NodeModification modificationRoot) {
        this.modificationRoot = modificationRoot;
        return this;
    }

    protected Optional<StoreMetadataNode> getBeforeRoot() {
        return beforeRoot;
    }

    protected DataChangeEventResolver setBeforeRoot(final Optional<StoreMetadataNode> beforeRoot) {
        this.beforeRoot = beforeRoot;
        return this;
    }

    protected Optional<StoreMetadataNode> getAfterRoot() {
        return afterRoot;
    }

    protected DataChangeEventResolver setAfterRoot(final Optional<StoreMetadataNode> afterRoot) {
        this.afterRoot = afterRoot;
        return this;
    }

    public Iterable<ChangeListenerNotifyTask> resolve() {
        LOG.trace("Resolving events for {}", modificationRoot);

        try (final Walker w = listenerRoot.getWalker()) {
            resolveAnyChangeEvent(rootPath, Collections.singleton(w.getRootNode()), modificationRoot, beforeRoot,
                    afterRoot);
            return createNotificationTasks();
        }
    }

    private Iterable<ChangeListenerNotifyTask> createNotificationTasks() {
        ImmutableList.Builder<ChangeListenerNotifyTask> taskListBuilder = ImmutableList.builder();
        for (Entry<ListenerTree.Node, Collection<DOMImmutableDataChangeEvent>> entry : events.asMap().entrySet()) {
            addNotificationTask(taskListBuilder, entry.getKey(), entry.getValue());
        }
        return taskListBuilder.build();
    }

    private static void addNotificationTask(final ImmutableList.Builder<ChangeListenerNotifyTask> taskListBuilder,
            final ListenerTree.Node listeners, final Collection<DOMImmutableDataChangeEvent> entries) {

        if (!entries.isEmpty()) {
            if (entries.size() == 1) {
                addNotificationTask(taskListBuilder, listeners, Iterables.getOnlyElement(entries));
            } else {
                addNotificationTasksAndMergeEvents(taskListBuilder, listeners, entries);
            }

        }
    }

    private static void addNotificationTask(
            final com.google.common.collect.ImmutableList.Builder<ChangeListenerNotifyTask> taskListBuilder,
            final ListenerTree.Node listeners, final DOMImmutableDataChangeEvent event) {
        DataChangeScope eventScope = event.getScope();
        for (DataChangeListenerRegistration<?> listenerReg : listeners.getListeners()) {
            DataChangeScope listenerScope = listenerReg.getScope();
            List<DataChangeListenerRegistration<?>> listenerSet = Collections
                    .<DataChangeListenerRegistration<?>> singletonList(listenerReg);
            if (eventScope == BASE) {
                taskListBuilder.add(new ChangeListenerNotifyTask(listenerSet, event));
            } else if (eventScope == ONE && listenerScope != BASE) {
                taskListBuilder.add(new ChangeListenerNotifyTask(listenerSet, event));
            } else if (eventScope == SUBTREE && listenerScope == SUBTREE) {
                taskListBuilder.add(new ChangeListenerNotifyTask(listenerSet, event));
            }

        }

    }

    private static void addNotificationTasksAndMergeEvents(
            final com.google.common.collect.ImmutableList.Builder<ChangeListenerNotifyTask> taskListBuilder,
            final ListenerTree.Node listeners, final Collection<DOMImmutableDataChangeEvent> entries) {

        final Builder baseBuilder = builder(BASE);
        final Builder oneBuilder = builder(ONE);
        final Builder subtreeBuilder = builder(SUBTREE);
        for (final DOMImmutableDataChangeEvent entry : entries) {
            switch (entry.getScope()) {
            // Absence of breaks is intentional here. Subtree contains base and
            // one,
            // one also contains base
            case BASE:
                baseBuilder.merge(entry);
            case ONE:
                oneBuilder.merge(entry);
            case SUBTREE:
                subtreeBuilder.merge(entry);
            }
        }

        addNotificationTask(taskListBuilder, listeners, baseBuilder.build());
        addNotificationTask(taskListBuilder, listeners, oneBuilder.build());
        addNotificationTask(taskListBuilder, listeners, subtreeBuilder.build());

    }

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
                return resolveReplacedEvent(path, listeners, modification, before.get(), after.get());
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
            final Collection<ListenerTree.Node> listeners, final NodeModification modification,
            final StoreMetadataNode before, final StoreMetadataNode after) {
        // FIXME Add task
        return builder(BASE).build();
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
        final NormalizedNode<?, ?> node = afterState.getData();
        Builder builder = builder(DataChangeScope.BASE).setAfter(node).addCreated(path, node);

        for (StoreMetadataNode child : afterState.getChildren()) {
            PathArgument childId = child.getIdentifier();
            Collection<ListenerTree.Node> childListeners = getChildrenWildcarded(listeners, childId);
            InstanceIdentifier childPath = StoreUtils.append(path, childId);
            builder.merge(resolveCreateEvent(childPath, childListeners, child));
        }

        return addPartialTask(listeners, builder.build());
    }

    private DOMImmutableDataChangeEvent resolveDeleteEvent(final InstanceIdentifier path,
            final Collection<ListenerTree.Node> listeners, final StoreMetadataNode beforeState) {

        final NormalizedNode<?, ?> node = beforeState.getData();
        Builder builder = builder(BASE).setBefore(node).addRemoved(path, node);

        for (StoreMetadataNode child : beforeState.getChildren()) {
            PathArgument childId = child.getIdentifier();
            Collection<ListenerTree.Node> childListeners = getChildrenWildcarded(listeners, childId);

            InstanceIdentifier childPath = StoreUtils.append(path, childId);
            builder.merge(resolveDeleteEvent(childPath, childListeners, child));
        }
        return addPartialTask(listeners, builder.build());
    }

    private DOMImmutableDataChangeEvent resolveSubtreeChangeEvent(final InstanceIdentifier path,
            final Collection<ListenerTree.Node> listeners, final NodeModification modification,
            final StoreMetadataNode before, final StoreMetadataNode after) {

        Builder one = builder(ONE).setBefore(before.getData()).setAfter(after.getData());

        Builder subtree = builder(SUBTREE);

        for (NodeModification childMod : modification.getModifications()) {
            PathArgument childId = childMod.getIdentifier();
            InstanceIdentifier childPath = append(path, childId);
            Collection<ListenerTree.Node> childListeners = getChildrenWildcarded(listeners, childId);

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

    private static Collection<ListenerTree.Node> getChildrenWildcarded(
            final Collection<ListenerTree.Node> parentNodes, final PathArgument child) {
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

    public static DataChangeEventResolver create() {
        return new DataChangeEventResolver();
    }
}
