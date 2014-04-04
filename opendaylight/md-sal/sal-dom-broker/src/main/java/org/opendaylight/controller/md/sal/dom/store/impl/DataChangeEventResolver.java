package org.opendaylight.controller.md.sal.dom.store.impl;

import static org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.builder;
import static org.opendaylight.controller.md.sal.dom.store.impl.StoreUtils.append;
import static org.opendaylight.controller.md.sal.dom.store.impl.tree.TreeNodeUtils.getChild;

import java.util.ArrayList;
import java.util.Collection;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.Builder;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerRegistrationNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class DataChangeEventResolver {


    private  static final Logger LOG = LoggerFactory.getLogger(DataChangeEventResolver.class);

    private static final DOMImmutableDataChangeEvent NO_CHANGE = builder().build();
    private InstanceIdentifier rootPath;
    private ListenerRegistrationNode listenerRoot;
    private NodeModification modificationRoot;
    private Optional<StoreMetadataNode> beforeRoot;
    private Optional<StoreMetadataNode> afterRoot;
    private final ImmutableList.Builder<ChangeListenerNotifyTask> tasks = ImmutableList.builder();

    protected InstanceIdentifier getRootPath() {
        return rootPath;
    }

    protected DataChangeEventResolver setRootPath(final InstanceIdentifier rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    protected ListenerRegistrationNode getListenerRoot() {
        return listenerRoot;
    }

    protected DataChangeEventResolver setListenerRoot(final ListenerRegistrationNode listenerRoot) {
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
        LOG.trace("Resolving events for {}" ,modificationRoot);
        resolveAnyChangeEvent(rootPath, Optional.of(listenerRoot), modificationRoot, beforeRoot, afterRoot);
        return tasks.build();
    }

    private DOMImmutableDataChangeEvent resolveAnyChangeEvent(final InstanceIdentifier path,
            final Optional<ListenerRegistrationNode> listeners, final NodeModification modification,
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
            final Optional<ListenerRegistrationNode> listeners, final StoreMetadataNode afterState) {
        final NormalizedNode<?, ?> node = afterState.getData();
        Builder builder = builder().setAfter(node).addCreated(path, node);

        for (StoreMetadataNode child : afterState.getChildren()) {
            PathArgument childId = child.getIdentifier();
            Optional<ListenerRegistrationNode> childListeners = getChild(listeners, childId);

            InstanceIdentifier childPath = StoreUtils.append(path, childId);
            builder.merge(resolveCreateEvent(childPath, childListeners, child));
        }

        DOMImmutableDataChangeEvent event = builder.build();
        if (listeners.isPresent()) {
            addNotifyTask(listeners.get().getListeners(), event);
        }
        return event;
    }

    private DOMImmutableDataChangeEvent resolveDeleteEvent(final InstanceIdentifier path,
            final Optional<ListenerRegistrationNode> listeners, final StoreMetadataNode beforeState) {
        final NormalizedNode<?, ?> node = beforeState.getData();
        Builder builder = builder().setBefore(node).addRemoved(path, node);

        for (StoreMetadataNode child : beforeState.getChildren()) {
            PathArgument childId = child.getIdentifier();
            Optional<ListenerRegistrationNode> childListeners = getChild(listeners, childId);
            InstanceIdentifier childPath = StoreUtils.append(path, childId);
            builder.merge(resolveDeleteEvent(childPath, childListeners, child));
        }
        DOMImmutableDataChangeEvent event = builder.build();
        if (listeners.isPresent()) {
            addNotifyTask(listeners.get().getListeners(), event);
        }
        return event;

    }

    private DOMImmutableDataChangeEvent resolveSubtreeChangeEvent(final InstanceIdentifier path,
            final Optional<ListenerRegistrationNode> listeners, final NodeModification modification,
            final StoreMetadataNode before, final StoreMetadataNode after) {

        Builder one = builder().setBefore(before.getData()).setAfter(after.getData());

        Builder subtree = builder();

        for (NodeModification childMod : modification.getModifications()) {
            PathArgument childId = childMod.getIdentifier();
            InstanceIdentifier childPath = append(path, childId);
            Optional<ListenerRegistrationNode> childListen = getChild(listeners, childId);

            Optional<StoreMetadataNode> childBefore = before.getChild(childId);
            Optional<StoreMetadataNode> childAfter = after.getChild(childId);

            switch (childMod.getModificationType()) {
            case WRITE:
            case DELETE:
                one.merge(resolveAnyChangeEvent(childPath, childListen, childMod, childBefore, childAfter));
                break;
            case SUBTREE_MODIFIED:
                subtree.merge(resolveSubtreeChangeEvent(childPath, childListen, childMod, childBefore.get(),
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
        if (listeners.isPresent()) {
            addNotifyTask(listeners.get(), DataChangeScope.ONE, oneChangeEvent);
            addNotifyTask(listeners.get(), DataChangeScope.SUBTREE, subtreeEvent);
        }
        return subtreeEvent;
    }

    private DOMImmutableDataChangeEvent resolveReplacedEvent(final InstanceIdentifier path,
            final Optional<ListenerRegistrationNode> listeners, final NodeModification modification,
            final StoreMetadataNode before, final StoreMetadataNode after) {
        // FIXME Add task
        return builder().build();
    }

    private void addNotifyTask(final ListenerRegistrationNode listenerRegistrationNode, final DataChangeScope scope,
            final DOMImmutableDataChangeEvent event) {
        Collection<DataChangeListenerRegistration<?>> potential = listenerRegistrationNode.getListeners();
        if(potential.isEmpty()) {
            return;
        }
        ArrayList<DataChangeListenerRegistration<?>> toNotify = new ArrayList<>(potential.size());
        for(DataChangeListenerRegistration<?> listener : potential) {
            if(scope.equals(listener.getScope())) {
                toNotify.add(listener);
            }
        }
        addNotifyTask(toNotify, event);

    }

    private void addNotifyTask(final Collection<DataChangeListenerRegistration<?>> listeners,
            final DOMImmutableDataChangeEvent event) {
        if(!listeners.isEmpty()) {
            tasks.add(new ChangeListenerNotifyTask(ImmutableSet.copyOf(listeners),event));
        }
    }

    public static DataChangeEventResolver create() {
        return new DataChangeEventResolver();
    }



}
