package org.opendaylight.controller.md.sal.common.impl.util.compat;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;

import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class DataNormalizer {

    private final SchemaContext schemaContext;

    private final DataNormalizationOperation<?> operation;

    public DataNormalizer(final SchemaContext ctx) {
        schemaContext = ctx;
        operation = DataNormalizationOperation.from(ctx);
    }

    public InstanceIdentifier toNormalized(final InstanceIdentifier legacy) {
        ImmutableList.Builder<PathArgument> normalizedArgs = ImmutableList.builder();

        DataNormalizationOperation<?> currentOp = operation;
        for (PathArgument legacyArg : legacy.getPath()) {
            currentOp = currentOp.getChild(legacyArg);
            checkArgument(currentOp != null, "Legacy Instance Identifier %s is not correct. Normalized Instance Identifier so far %s",legacy,normalizedArgs.build());
            while (currentOp.isMixin()) {
                normalizedArgs.add(currentOp.getIdentifier());
                currentOp = currentOp.getChild(legacyArg.getNodeType());
            }
            normalizedArgs.add(legacyArg);
        }
        return new InstanceIdentifier(normalizedArgs.build());
    }

    public Map.Entry<InstanceIdentifier,NormalizedNode<?, ?>> toNormalized(final Map.Entry<InstanceIdentifier,CompositeNode> legacy) {
        return toNormalized(legacy.getKey(), legacy.getValue());
    }

    public Map.Entry<InstanceIdentifier,NormalizedNode<?, ?>> toNormalized(final InstanceIdentifier legacyPath, final CompositeNode legacyData) {

        InstanceIdentifier normalizedPath = toNormalized(legacyPath);

        DataNormalizationOperation<?> currentOp = operation;
        for (PathArgument arg : normalizedPath.getPath()) {
            currentOp = currentOp.getChild(arg);
        }
        // Write Augmentaiton data resolution
        if (legacyData.getChildren().size() == 1) {
            DataNormalizationOperation<?> potentialOp = currentOp.getChild(legacyData.getChildren().get(0)
                    .getNodeType());
            if(potentialOp.getIdentifier() instanceof AugmentationIdentifier) {
                currentOp = potentialOp;
                ArrayList<PathArgument> reworkedArgs = new ArrayList<>(normalizedPath.getPath());
                reworkedArgs.add(potentialOp.getIdentifier());
                normalizedPath = new InstanceIdentifier(reworkedArgs);
            }
        }

        Preconditions.checkArgument(currentOp != null,
                "Instance Identifier %s does not reference correct schema Node.", normalizedPath);
        return new AbstractMap.SimpleEntry<InstanceIdentifier,NormalizedNode<?, ?>>(normalizedPath,currentOp.normalize(legacyData));
    }

    public InstanceIdentifier toLegacy(final InstanceIdentifier normalized) {
        ImmutableList.Builder<PathArgument> legacyArgs = ImmutableList.builder();
        PathArgument previous = null;
        for (PathArgument normalizedArg : normalized.getPath()) {
            if (normalizedArg instanceof NodeIdentifier) {
                if (previous != null) {
                    legacyArgs.add(previous);
                }
                previous = normalizedArg;
            } else if (normalizedArg instanceof NodeIdentifierWithPredicates) {
                // We skip previous node, which was mixin.
                previous = normalizedArg;
            } else if (normalizedArg instanceof AugmentationIdentifier) {
                // We ignore argument
            }
            // FIXME : Add option for reading choice
        }
        if (previous != null) {
            legacyArgs.add(previous);
        }
        return new InstanceIdentifier(legacyArgs.build());
    }

    public CompositeNode toLegacy(final InstanceIdentifier normalizedPath, final NormalizedNode<?, ?> normalizedData) {
        // Preconditions.checkArgument(normalizedData instanceof
        // DataContainerNode<?>,"Node object %s, %s should be of type DataContainerNode",normalizedPath,normalizedData);
        if (normalizedData instanceof DataContainerNode<?>) {
            return toLegacyFromDataContainer((DataContainerNode<?>) normalizedData);
        }
        return null;
    }

    public static Node<?> toLegacy(final NormalizedNode<?, ?> node) {
        if (node instanceof MixinNode) {
            /**
             * Direct reading of MixinNodes is not supported,
             * since it is not possible in legacy APIs create pointer
             * to Mixin Nodes.
             *
             */
            return null;
        }

        if (node instanceof DataContainerNode<?>) {
            return toLegacyFromDataContainer((DataContainerNode<?>) node);
        }
        return toLegacySimple(node);

    }

    private static SimpleNode<?> toLegacySimple(final NormalizedNode<?, ?> node) {
        return new SimpleNodeTOImpl<Object>(node.getNodeType(), null, node.getValue());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static CompositeNode toLegacyFromDataContainer(final DataContainerNode<?> node) {
        CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
        builder.setQName(node.getNodeType());
        for (NormalizedNode<?, ?> child : node.getValue()) {
            if (child instanceof MixinNode && child instanceof NormalizedNodeContainer<?, ?, ?>) {
                builder.addAll(toLegacyNodesFromMixin((NormalizedNodeContainer) child));
            } else {
                addToBuilder(builder, toLegacy(child));
            }
        }
        return builder.toInstance();
    }

    private static void addToBuilder(final CompositeNodeBuilder<ImmutableCompositeNode> builder, final Node<?> legacy) {
        if (legacy != null) {
            builder.add(legacy);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Iterable<Node<?>> toLegacyNodesFromMixin(
            final NormalizedNodeContainer<?, ?, NormalizedNode<?, ?>> mixin) {
        ArrayList<Node<?>> ret = new ArrayList<>();
        for (NormalizedNode<?, ?> child : mixin.getValue()) {
            if(child instanceof MixinNode && child instanceof NormalizedNodeContainer<?, ?, ?>) {
                Iterables.addAll(ret,toLegacyNodesFromMixin((NormalizedNodeContainer) child));
            } else {
                ret.add(toLegacy(child));
            }
        }
        return FluentIterable.from(ret).filter(new Predicate<Node<?>>() {

            @Override
            public boolean apply(final Node<?> input) {
                return input != null;
            }
        });
    }

    public DataNormalizationOperation<?> getRootOperation() {
        return operation;
    }

}
