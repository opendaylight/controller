package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class ListenerRegistrationNode implements StoreTreeNode<ListenerRegistrationNode>, Identifiable<PathArgument> {

    private static final Logger LOG = LoggerFactory.getLogger(ListenerRegistrationNode.class);

    private final ListenerRegistrationNode parent;
    private final Map<PathArgument, ListenerRegistrationNode> children;
    private final PathArgument identifier;
    private final HashSet<DataChangeListenerRegistration<?>> listeners;

    private ListenerRegistrationNode(final PathArgument identifier) {
        this(null, identifier);
    }

    private ListenerRegistrationNode(final ListenerRegistrationNode parent, final PathArgument identifier) {
        this.parent = parent;
        this.identifier = identifier;
        children = new HashMap<>();
        listeners = new HashSet<>();
    }

    public final static ListenerRegistrationNode createRoot() {
        return new ListenerRegistrationNode(null);
    }

    @Override
    public PathArgument getIdentifier() {
        return identifier;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Collection<org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration<?>> getListeners() {
        // FIXME: this is not thread-safe and races with listener unregistration!
        return (Collection) listeners;
    }

    @Override
    public synchronized Optional<ListenerRegistrationNode> getChild(final PathArgument child) {
        return Optional.fromNullable(children.get(child));
    }

    public synchronized ListenerRegistrationNode ensureChild(final PathArgument child) {
        ListenerRegistrationNode potential = (children.get(child));
        if (potential == null) {
            potential = new ListenerRegistrationNode(this, child);
            children.put(child, potential);
        }
        return potential;
    }

    /**
     * Registers listener on this node.
     *
     * @param path Full path on which listener is registered.
     * @param listener Listener
     * @param scope Scope of triggering event.
     * @return
     */
    public synchronized <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> DataChangeListenerRegistration<L> registerDataChangeListener(final InstanceIdentifier path,
            final L listener, final DataChangeScope scope) {

        DataChangeListenerRegistration<L> listenerReg = new DataChangeListenerRegistration<L>(path,listener, scope, this);
        listeners.add(listenerReg);
        return listenerReg;
    }

    private synchronized void removeListener(final DataChangeListenerRegistration<?> listener) {
        listeners.remove(listener);
        removeThisIfUnused();
    }

    private void removeThisIfUnused() {
        if (parent != null && listeners.isEmpty() && children.isEmpty()) {
            parent.removeChildIfUnused(this);
        }
    }

    public boolean isUnused() {
        return (listeners.isEmpty() && children.isEmpty()) || areChildrenUnused();
    }

    private boolean areChildrenUnused() {
        for (ListenerRegistrationNode child : children.values()) {
            if (!child.isUnused()) {
                return false;
            }
        }
        return true;
    }

    private void removeChildIfUnused(final ListenerRegistrationNode listenerRegistrationNode) {
        // FIXME Remove unnecessary
    }

    public static class DataChangeListenerRegistration<T extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>>
            extends AbstractObjectRegistration<T> implements
            org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration<T> {

        private final DataChangeScope scope;
        private ListenerRegistrationNode node;
        private final InstanceIdentifier path;

        public DataChangeListenerRegistration(final InstanceIdentifier path,final T listener, final DataChangeScope scope,
                final ListenerRegistrationNode node) {
            super(listener);
            this.path = path;
            this.scope = scope;
            this.node = node;
        }

        @Override
        public DataChangeScope getScope() {
            return scope;
        }

        @Override
        protected void removeRegistration() {
            node.removeListener(this);
            node = null;
        }

        @Override
        public InstanceIdentifier getPath() {
            return path;
        }
    }
}
