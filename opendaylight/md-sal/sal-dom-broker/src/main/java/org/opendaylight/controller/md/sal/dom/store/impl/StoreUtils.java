package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;

import com.google.common.base.Function;
import com.google.common.base.Strings;
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

    /*
     * Suppressing warnings here allows us to fool the compiler enough
     * such that we can reuse a single function for all applicable types
     * and present it in a type-safe manner to our users.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <V> Function<Identifiable<V>, V> identifierExtractor() {
        return (Function) EXTRACT_IDENTIFIER;
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

    public static <V> Set<V> toIdentifierSet(final Iterable<? extends Identifiable<V>> children) {
        return FluentIterable.from(children).transform(StoreUtils.<V> identifierExtractor()).toSet();
    }

    public static String toStringTree(final StoreMetadataNode metaNode) {
        StringBuilder builder = new StringBuilder();
        toStringTree(builder, metaNode, 0);
        return builder.toString();
    }

    private static void toStringTree(final StringBuilder builder, final StoreMetadataNode metaNode, final int offset) {
        String prefix = Strings.repeat(" ", offset);
        builder.append(prefix).append(toStringTree(metaNode.getIdentifier()));
        NormalizedNode<?, ?> dataNode = metaNode.getData();
        if (dataNode instanceof NormalizedNodeContainer<?, ?, ?>) {
            builder.append(" {\n");
            for (StoreMetadataNode child : metaNode.getChildren()) {
                toStringTree(builder, child, offset + 4);
            }
            builder.append(prefix).append('}');
        } else {
            builder.append(' ').append(dataNode.getValue());
        }
        builder.append('\n');
    }

    private static String toStringTree(final PathArgument identifier) {
        if (identifier instanceof NodeIdentifierWithPredicates) {
            StringBuilder builder = new StringBuilder();
            builder.append(identifier.getNodeType().getLocalName());
            builder.append(((NodeIdentifierWithPredicates) identifier).getKeyValues().values());
            return builder.toString();
        } else if (identifier instanceof AugmentationIdentifier) {
            return "augmentation";
        }
        return identifier.getNodeType().getLocalName();
    }
}
