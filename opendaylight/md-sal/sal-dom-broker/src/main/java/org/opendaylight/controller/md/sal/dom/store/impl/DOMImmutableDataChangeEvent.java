package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public final class DOMImmutableDataChangeEvent implements
        AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> {


    private static final RemoveEventFactory REMOVE_EVENT_FACTORY = new RemoveEventFactory();
    private static final CreateEventFactory CREATE_EVENT_FACTORY = new CreateEventFactory();

    private final NormalizedNode<?, ?> original;
    private final NormalizedNode<?, ?> updated;
    private final Map<InstanceIdentifier, ? extends NormalizedNode<?, ?>> originalData;
    private final Map<InstanceIdentifier, NormalizedNode<?, ?>> createdData;
    private final Map<InstanceIdentifier, NormalizedNode<?, ?>> updatedData;
    private final Set<InstanceIdentifier> removedPaths;
    private final DataChangeScope scope;



    private DOMImmutableDataChangeEvent(final Builder change) {
        original = change.before;
        updated = change.after;
        originalData = change.original.build();
        createdData = change.created.build();
        updatedData = change.updated.build();
        removedPaths = change.removed.build();
        scope = change.scope;
    }

    public static final Builder builder(final DataChangeScope scope) {
        return new Builder(scope);
    }

    protected DataChangeScope getScope() {
        return scope;
    }

    @Override
    public NormalizedNode<?, ?> getOriginalSubtree() {
        return original;
    }

    @Override
    public NormalizedNode<?, ?> getUpdatedSubtree() {
        return updated;
    }

    @Override
    public Map<InstanceIdentifier, ? extends NormalizedNode<?, ?>> getOriginalData() {
        return originalData;
    }

    @Override
    public Map<InstanceIdentifier, NormalizedNode<?, ?>> getCreatedData() {
        return createdData;
    }

    @Override
    public Map<InstanceIdentifier, NormalizedNode<?, ?>> getUpdatedData() {
        return updatedData;
    }

    @Override
    public Set<InstanceIdentifier> getRemovedPaths() {
        return removedPaths;
    }

    @Override
    public String toString() {
        return "DOMImmutableDataChangeEvent [created=" + createdData.keySet() + ", updated=" + updatedData.keySet()
                + ", removed=" + removedPaths + "]";
    }

    /**
     * Simple event factory which creates event based on path and data
     *
     *
     */
    public interface SimpleEventFactory {
        DOMImmutableDataChangeEvent create(InstanceIdentifier path, NormalizedNode<PathArgument,?> data);
    }

    /**
     * Event factory which takes after state and creates event for it.
     *
     * Factory for events based on path and after state.
     * After state is set as {@link #getUpdatedSubtree()} and is path,
     * state mapping is also present in {@link #getUpdatedData()}.
     *
     * @return
     */
    public static final SimpleEventFactory getCreateEventFactory() {
        return CREATE_EVENT_FACTORY;
    }

    /**
     * Event factory which takes before state and creates event for it.
     *
     * Factory for events based on path and after state.
     * After state is set as {@link #getOriginalSubtree()} and is path,
     * state mapping is also present in {@link #getOriginalSubtree()}.
     *
     * Path is present in {@link #getRemovedPaths()}.
     * @return
     */
    public static final SimpleEventFactory getRemoveEventFactory() {
        return REMOVE_EVENT_FACTORY;
    }
    public static class Builder {

        public DataChangeScope scope;
        private NormalizedNode<?, ?> after;
        private NormalizedNode<?, ?> before;

        private final ImmutableMap.Builder<InstanceIdentifier, NormalizedNode<?, ?>> original = ImmutableMap.builder();
        private final ImmutableMap.Builder<InstanceIdentifier, NormalizedNode<?, ?>> created = ImmutableMap.builder();
        private final ImmutableMap.Builder<InstanceIdentifier, NormalizedNode<?, ?>> updated = ImmutableMap.builder();
        private final ImmutableSet.Builder<InstanceIdentifier> removed = ImmutableSet.builder();

        private Builder(final DataChangeScope scope) {
            Preconditions.checkNotNull(scope, "Data change scope should not be null.");
            this.scope = scope;
        }

        public Builder setAfter(final NormalizedNode<?, ?> node) {
            after = node;
            return this;
        }

        public DOMImmutableDataChangeEvent build() {

            return new DOMImmutableDataChangeEvent(this);
        }

        public void merge(final DOMImmutableDataChangeEvent nestedChanges) {

            original.putAll(nestedChanges.getOriginalData());
            created.putAll(nestedChanges.getCreatedData());
            updated.putAll(nestedChanges.getUpdatedData());
            removed.addAll(nestedChanges.getRemovedPaths());

        }

        public Builder setBefore(final NormalizedNode<?, ?> node) {
            this.before = node;
            return this;
        }

        public Builder addCreated(final InstanceIdentifier path, final NormalizedNode<?, ?> node) {
            created.put(path, node);
            return this;
        }

        public Builder addRemoved(final InstanceIdentifier path, final NormalizedNode<?, ?> node) {
            original.put(path, node);
            removed.add(path);
            return this;
        }

        public Builder addUpdated(final InstanceIdentifier path, final NormalizedNode<?, ?> before,
                final NormalizedNode<?, ?> after) {
            original.put(path, before);
            updated.put(path, after);
            return this;
        }
    }

    private static final class RemoveEventFactory implements SimpleEventFactory {

        @Override
        public DOMImmutableDataChangeEvent create(final InstanceIdentifier path, final NormalizedNode<PathArgument, ?> data) {
            return builder(DataChangeScope.BASE) //
                    .setBefore(data) //
                    .addRemoved(path, data) //
                    .build();
        }

    }

    private static final class CreateEventFactory implements SimpleEventFactory {

        @Override
        public DOMImmutableDataChangeEvent create(final InstanceIdentifier path, final NormalizedNode<PathArgument, ?> data) {
            return builder(DataChangeScope.BASE) //
                    .setAfter(data) //
                    .addCreated(path, data) //
                    .build();
        }
    }

}
