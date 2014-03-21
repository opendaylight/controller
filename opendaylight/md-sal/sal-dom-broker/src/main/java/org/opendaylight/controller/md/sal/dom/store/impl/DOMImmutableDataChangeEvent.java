package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public final class DOMImmutableDataChangeEvent implements
        AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> {

    private final NormalizedNode<?, ?> original;
    private final NormalizedNode<?, ?> updated;
    private final Map<InstanceIdentifier, ? extends NormalizedNode<?, ?>> originalData;
    private final Map<InstanceIdentifier, NormalizedNode<?, ?>> createdData;
    private final Map<InstanceIdentifier, NormalizedNode<?, ?>> updatedData;
    private final Set<InstanceIdentifier> removedPaths;

    private DOMImmutableDataChangeEvent(final Builder change) {
        original = change.before;
        updated = change.after;
        originalData = change.original.build();
        createdData = change.created.build();
        updatedData = change.updated.build();
        removedPaths = change.removed.build();
    }

    public static final Builder builder() {
        return new Builder();
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

    public static class Builder {

        private NormalizedNode<?, ?> after;
        private NormalizedNode<?, ?> before;

        private final ImmutableMap.Builder<InstanceIdentifier, NormalizedNode<?, ?>> original = ImmutableMap.builder();
        private final ImmutableMap.Builder<InstanceIdentifier, NormalizedNode<?, ?>> created = ImmutableMap.builder();
        private final ImmutableMap.Builder<InstanceIdentifier, NormalizedNode<?, ?>> updated = ImmutableMap.builder();
        private final ImmutableSet.Builder<InstanceIdentifier> removed = ImmutableSet.builder();


        private Builder() {

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

}

