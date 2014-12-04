/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

public abstract class DataNormalizationOperation<T extends PathArgument> implements Identifiable<T> {

    private final T identifier;
    private final Optional<DataSchemaNode> dataSchemaNode;

    protected DataNormalizationOperation(final T identifier, final SchemaNode schema) {
        super();
        this.identifier = identifier;
        if (schema instanceof DataSchemaNode) {
            this.dataSchemaNode = Optional.of((DataSchemaNode) schema);
        } else {
            this.dataSchemaNode = Optional.absent();
        }
    }

    @Override
    public T getIdentifier() {
        return identifier;
    }

    public Optional<DataSchemaNode> getDataSchemaNode() {
        return dataSchemaNode;
    }

    public boolean isMixin() {
        return false;
    }

    public boolean isKeyedEntry() {
        return false;
    }

    protected Set<QName> getQNameIdentifiers() {
        return Collections.singleton(identifier.getNodeType());
    }

    public abstract DataNormalizationOperation<?> getChild(final PathArgument child) throws DataNormalizationException;

    public abstract DataNormalizationOperation<?> getChild(QName child) throws DataNormalizationException;

    private static abstract class CompositeNodeNormalizationOperation<T extends PathArgument> extends
            DataNormalizationOperation<T> {

        protected CompositeNodeNormalizationOperation(final T identifier, final DataSchemaNode schema) {
            super(identifier, schema);
        }
    }

    private static abstract class DataContainerNormalizationOperation<T extends PathArgument> extends
            CompositeNodeNormalizationOperation<T> {

        private final DataNodeContainer schema;
        private final Map<QName, DataNormalizationOperation<?>> byQName;
        private final Map<PathArgument, DataNormalizationOperation<?>> byArg;

        protected DataContainerNormalizationOperation(final T identifier, final DataNodeContainer schema,
                final DataSchemaNode node) {
            super(identifier, node);
            this.schema = schema;
            this.byArg = new ConcurrentHashMap<>();
            this.byQName = new ConcurrentHashMap<>();
        }

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) throws DataNormalizationException {
            DataNormalizationOperation<?> potential = byArg.get(child);
            if (potential != null) {
                return potential;
            }
            potential = fromLocalSchema(child);
            return register(potential);
        }

        private DataNormalizationOperation<?> fromLocalSchema(final PathArgument child)
                throws DataNormalizationException {
            if (child instanceof AugmentationIdentifier) {
                return fromSchemaAndQNameChecked(schema, ((AugmentationIdentifier) child).getPossibleChildNames()
                        .iterator().next());
            }
            return fromSchemaAndQNameChecked(schema, child.getNodeType());
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) throws DataNormalizationException {
            DataNormalizationOperation<?> potential = byQName.get(child);
            if (potential != null) {
                return potential;
            }
            potential = fromLocalSchemaAndQName(schema, child);
            return register(potential);
        }

        protected DataNormalizationOperation<?> fromLocalSchemaAndQName(final DataNodeContainer schema2,
                final QName child) throws DataNormalizationException {
            return fromSchemaAndQNameChecked(schema2, child);
        }

        private DataNormalizationOperation<?> register(final DataNormalizationOperation<?> potential) {
            if (potential != null) {
                byArg.put(potential.getIdentifier(), potential);
                for (QName qName : potential.getQNameIdentifiers()) {
                    byQName.put(qName, potential);
                }
            }
            return potential;
        }

    }

    private static final class ListItemNormalization extends
            DataContainerNormalizationOperation<NodeIdentifierWithPredicates> {

        protected ListItemNormalization(final NodeIdentifierWithPredicates identifier, final ListSchemaNode schema) {
            super(identifier, schema, schema);
        }

        @Override
        public boolean isKeyedEntry() {
            return true;
        }
    }

    private static final class UnkeyedListItemNormalization extends DataContainerNormalizationOperation<NodeIdentifier> {

        protected UnkeyedListItemNormalization(final ListSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()), schema, schema);
        }

    }

    private static final class ContainerNormalization extends DataContainerNormalizationOperation<NodeIdentifier> {

        protected ContainerNormalization(final ContainerSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()), schema, schema);
        }

    }

    private static abstract class MixinNormalizationOp<T extends PathArgument> extends
            CompositeNodeNormalizationOperation<T> {

        protected MixinNormalizationOp(final T identifier, final DataSchemaNode schema) {
            super(identifier, schema);
        }

        @Override
        public final boolean isMixin() {
            return true;
        }

    }

    private static final class AugmentationNormalization extends
            DataContainerNormalizationOperation<AugmentationIdentifier> {

        public AugmentationNormalization(final AugmentationSchema augmentation, final DataNodeContainer schema) {
            super(augmentationIdentifierFrom(augmentation), augmentationProxy(augmentation, schema), null);
        }

        @Override
        public boolean isMixin() {
            return true;
        }

        @Override
        protected DataNormalizationOperation<?> fromLocalSchemaAndQName(final DataNodeContainer schema,
                final QName child) throws DataNormalizationException {
            Optional<DataSchemaNode> potential = findChildSchemaNode(schema, child);
            if (!potential.isPresent()) {
                return null;
            }

            DataSchemaNode result = potential.get();
            // We try to look up if this node was added by augmentation
            if ((schema instanceof DataSchemaNode) && result.isAugmenting()) {
                return fromAugmentation(schema, (AugmentationTarget) schema, result);
            }
            return fromDataSchemaNode(result);
        }

        @Override
        protected Set<QName> getQNameIdentifiers() {
            return getIdentifier().getPossibleChildNames();
        }

    }

    private static class UnorderedMapMixinNormalization extends MixinNormalizationOp<NodeIdentifier> {

        private final ListItemNormalization innerNode;

        public UnorderedMapMixinNormalization(final ListSchemaNode list) {
            super(new NodeIdentifier(list.getQName()), list);
            this.innerNode = new ListItemNormalization(new NodeIdentifierWithPredicates(list.getQName(),
                    Collections.<QName, Object> emptyMap()), list);
        }

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) {
            if (child.getNodeType().equals(getIdentifier().getNodeType())) {
                return innerNode;
            }
            return null;
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) {
            if (getIdentifier().getNodeType().equals(child)) {
                return innerNode;
            }
            return null;
        }
    }

    private static class UnkeyedListMixinNormalization extends MixinNormalizationOp<NodeIdentifier> {

        private final UnkeyedListItemNormalization innerNode;

        public UnkeyedListMixinNormalization(final ListSchemaNode list) {
            super(new NodeIdentifier(list.getQName()), list);
            this.innerNode = new UnkeyedListItemNormalization(list);
        }

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) {
            if (child.getNodeType().equals(getIdentifier().getNodeType())) {
                return innerNode;
            }
            return null;
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) {
            if (getIdentifier().getNodeType().equals(child)) {
                return innerNode;
            }
            return null;
        }

    }

    private static final class OrderedMapMixinNormalization extends UnorderedMapMixinNormalization {

        public OrderedMapMixinNormalization(final ListSchemaNode list) {
            super(list);
        }

    }

    private static class ChoiceNodeNormalization extends MixinNormalizationOp<NodeIdentifier> {

        private final ImmutableMap<QName, DataNormalizationOperation<?>> byQName;
        private final ImmutableMap<PathArgument, DataNormalizationOperation<?>> byArg;
        private final List<ChoiceNodeNormalization> choiceChildren;

        protected ChoiceNodeNormalization(final org.opendaylight.yangtools.yang.model.api.ChoiceNode schema) {
            super(new NodeIdentifier(schema.getQName()), schema);
            ImmutableMap.Builder<QName, DataNormalizationOperation<?>> byQNameBuilder = ImmutableMap.builder();
            ImmutableMap.Builder<PathArgument, DataNormalizationOperation<?>> byArgBuilder = ImmutableMap.builder();

            choiceChildren = new ArrayList<>();
            for (ChoiceCaseNode caze : schema.getCases()) {
                for (DataSchemaNode cazeChild : caze.getChildNodes()) {
                    DataNormalizationOperation<?> childOp = fromDataSchemaNode(cazeChild);
                    if (childOp != null) {
                        if (childOp instanceof ChoiceNodeNormalization) {
                            choiceChildren.add((ChoiceNodeNormalization) childOp);
                        }
                        byArgBuilder.put(childOp.getIdentifier(), childOp);
                        for (QName qname : childOp.getQNameIdentifiers()) {
                            byQNameBuilder.put(qname, childOp);
                        }
                    }
                }
            }
            byQName = byQNameBuilder.build();
            byArg = byArgBuilder.build();
        }

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) {
            final DataNormalizationOperation<?> dataNormalizationOperation = byArg.get(child);
            return dataNormalizationOperation == null ? searchForNormalizationOperationInChoices(child.getNodeType())
                    : dataNormalizationOperation;
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) {
            final DataNormalizationOperation<?> dataNormalizationOperation = byQName.get(child);
            return dataNormalizationOperation == null ? searchForNormalizationOperationInChoices(child)
                    : dataNormalizationOperation;
        }

        private DataNormalizationOperation<?> searchForNormalizationOperationInChoices(final QName qName) {
            for (ChoiceNodeNormalization choiceNodeNormalization : choiceChildren) {
                DataNormalizationOperation<?> child = choiceNodeNormalization.getChild(qName);
                if (child != null) {
                    return choiceNodeNormalization;
                }
            }
            return null;
        }
    }

    private static class AnyXmlNormalization extends DataNormalizationOperation<NodeIdentifier> {

        protected AnyXmlNormalization(final AnyXmlSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()), schema);
        }

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) throws DataNormalizationException {
            return null;
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) throws DataNormalizationException {
            return null;
        }

    }

    private static final Optional<DataSchemaNode> findChildSchemaNode(final DataNodeContainer parent, final QName child) {
        DataSchemaNode potential = parent.getDataChildByName(child);
        if (potential == null) {
            Iterable<org.opendaylight.yangtools.yang.model.api.ChoiceNode> choices = FluentIterable.from(
                    parent.getChildNodes()).filter(org.opendaylight.yangtools.yang.model.api.ChoiceNode.class);
            potential = findChoice(choices, child);
        }
        return Optional.fromNullable(potential);
    }

    private static DataNormalizationOperation<?> fromSchemaAndQNameChecked(final DataNodeContainer schema,
            final QName child) throws DataNormalizationException {

        Optional<DataSchemaNode> potential = findChildSchemaNode(schema, child);
        if (!potential.isPresent()) {
            throw new DataNormalizationException(String.format(
                    "Supplied QName %s is not valid according to schema %s, potential children nodes: %s", child,
                    schema, schema.getChildNodes()));
        }

        DataSchemaNode result = potential.get();
        // We try to look up if this node was added by augmentation
        if ((schema instanceof DataSchemaNode) && result.isAugmenting()) {
            return fromAugmentation(schema, (AugmentationTarget) schema, result);
        }
        return fromDataSchemaNode(result);
    }

    private static org.opendaylight.yangtools.yang.model.api.ChoiceNode findChoice(
            final Iterable<org.opendaylight.yangtools.yang.model.api.ChoiceNode> choices, final QName child) {
        org.opendaylight.yangtools.yang.model.api.ChoiceNode foundChoice = null;
        choiceLoop: for (org.opendaylight.yangtools.yang.model.api.ChoiceNode choice : choices) {
            for (ChoiceCaseNode caze : choice.getCases()) {
                if (findChildSchemaNode(caze, child).isPresent()) {
                    foundChoice = choice;
                    break choiceLoop;
                }
            }
        }
        return foundChoice;
    }

    public static AugmentationIdentifier augmentationIdentifierFrom(final AugmentationSchema augmentation) {
        ImmutableSet.Builder<QName> potentialChildren = ImmutableSet.builder();
        for (DataSchemaNode child : augmentation.getChildNodes()) {
            potentialChildren.add(child.getQName());
        }
        return new AugmentationIdentifier(potentialChildren.build());
    }

    private static DataNodeContainer augmentationProxy(final AugmentationSchema augmentation,
            final DataNodeContainer schema) {
        Set<DataSchemaNode> children = new HashSet<>();
        for (DataSchemaNode augNode : augmentation.getChildNodes()) {
            children.add(schema.getDataChildByName(augNode.getQName()));
        }
        return new DataSchemaContainerProxy(children);
    }

    /**
     * Returns a DataNormalizationOperation for provided child node
     *
     * If supplied child is added by Augmentation this operation returns a DataNormalizationOperation for augmentation,
     * otherwise returns a DataNormalizationOperation for child as call for {@link #fromDataSchemaNode(DataSchemaNode)}.
     *
     *
     * @param parent
     * @param parentAug
     * @param child
     * @return
     */
    private static DataNormalizationOperation<?> fromAugmentation(final DataNodeContainer parent,
            final AugmentationTarget parentAug, final DataSchemaNode child) {
        AugmentationSchema augmentation = null;
        for (AugmentationSchema aug : parentAug.getAvailableAugmentations()) {
            DataSchemaNode potential = aug.getDataChildByName(child.getQName());
            if (potential != null) {
                augmentation = aug;
                break;
            }

        }
        if (augmentation != null) {
            return new AugmentationNormalization(augmentation, parent);
        } else {
            return fromDataSchemaNode(child);
        }
    }

    public static DataNormalizationOperation<?> fromDataSchemaNode(final DataSchemaNode potential) {
        if (potential instanceof ContainerSchemaNode) {
            return new ContainerNormalization((ContainerSchemaNode) potential);
        } else if (potential instanceof ListSchemaNode) {
            return fromListSchemaNode((ListSchemaNode) potential);
        } else if (potential instanceof org.opendaylight.yangtools.yang.model.api.ChoiceNode) {
            return new ChoiceNodeNormalization((org.opendaylight.yangtools.yang.model.api.ChoiceNode) potential);
        } else if (potential instanceof AnyXmlSchemaNode) {
            return new AnyXmlNormalization((AnyXmlSchemaNode) potential);
        }
        return null;
    }

    private static DataNormalizationOperation<?> fromListSchemaNode(final ListSchemaNode potential) {
        List<QName> keyDefinition = potential.getKeyDefinition();
        if (keyDefinition == null || keyDefinition.isEmpty()) {
            return new UnkeyedListMixinNormalization(potential);
        }
        if (potential.isUserOrdered()) {
            return new OrderedMapMixinNormalization(potential);
        }
        return new UnorderedMapMixinNormalization(potential);
    }

    public static DataNormalizationOperation<?> from(final SchemaContext ctx) {
        return new ContainerNormalization(ctx);
    }

}
