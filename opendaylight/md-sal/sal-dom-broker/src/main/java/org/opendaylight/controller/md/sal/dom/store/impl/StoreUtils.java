package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedLong;

public final class StoreUtils {

    private final static Function<Identifiable<Object>, Object> EXTRACT_IDENTIFIER = new Function<Identifiable<Object>, Object>() {

        @Override
        public Object apply(final Identifiable<Object> input) {
            return input.getIdentifier();
        }
    };

    public static final UnsignedLong increase(final UnsignedLong original) {
        return original.plus(UnsignedLong.ONE);
    }

    public static final InstanceIdentifier append(final InstanceIdentifier parent, final PathArgument arg) {

        return new InstanceIdentifier(ImmutableList.<PathArgument> builder().addAll(parent.getPath()).add(arg).build());
    }

    public static AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> initialChangeEvent(
            final InstanceIdentifier path, final StoreMetadataNode data) {
        return new InitialDataChangeEvent(path, data.getData());
    }

    private static final class InitialDataChangeEvent implements
            AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> {

        private final ImmutableMap<InstanceIdentifier, NormalizedNode<?, ?>> payload;
        private final NormalizedNode<?, ?> data;

        public InitialDataChangeEvent(final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
            payload = ImmutableMap.<InstanceIdentifier, NormalizedNode<?, ?>> of(path, data);
            this.data = data;
        }

        @Override
        public Map<InstanceIdentifier, NormalizedNode<?, ?>> getCreatedData() {
            return payload;
        }

        @Override
        public Map<InstanceIdentifier, ? extends NormalizedNode<?, ?>> getOriginalData() {
            return Collections.emptyMap();
        }

        @Override
        public NormalizedNode<?, ?> getOriginalSubtree() {
            return null;
        }

        @Override
        public Set<InstanceIdentifier> getRemovedPaths() {
            return Collections.emptySet();
        }

        @Override
        public Map<InstanceIdentifier, NormalizedNode<?, ?>> getUpdatedData() {
            return payload;
        }

        @Override
        public NormalizedNode<?, ?> getUpdatedSubtree() {
            return data;
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <V> Function<Identifiable<V>,V> identifierExtractor() {
        return (Function) EXTRACT_IDENTIFIER;
    }

    public static <V> Set<V> toIdentifierSet(final Iterable<? extends Identifiable<V>> children) {
        return FluentIterable.from(children).transform(StoreUtils.<V>identifierExtractor()).toSet();
    }

}
