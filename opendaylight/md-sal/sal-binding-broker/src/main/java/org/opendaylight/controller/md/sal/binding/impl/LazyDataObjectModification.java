/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType.UNMODIFIED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.dom.adapter.BindingStructuralType;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeNode;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.Item;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lazily translated {@link DataObjectModification} based on {@link DataTreeCandidateNode}.
 *
 * {@link LazyDataObjectModification} represents Data tree change event,
 * but whole tree is not translated or resolved eagerly, but only child nodes
 * which are directly accessed by user of data object modification.
 *
 * @param <T> Type of Binding Data Object
 */
final class LazyDataObjectModification<T extends DataObject> implements DataObjectModification<T> {

    private static final Logger LOG = LoggerFactory.getLogger(LazyDataObjectModification.class);

    private final BindingCodecTreeNode<T> codec;
    private final DataTreeCandidateNode domData;
    private final PathArgument identifier;

    private volatile Collection<LazyDataObjectModification<? extends DataObject>> childNodesCache;
    private volatile ModificationType modificationType;

    private LazyDataObjectModification(final BindingCodecTreeNode<T> codec, final DataTreeCandidateNode domData) {
        this.codec = requireNonNull(codec);
        this.domData = requireNonNull(domData);
        this.identifier = codec.deserializePathArgument(domData.getIdentifier());
    }

    static <T extends DataObject> LazyDataObjectModification<T> create(final BindingCodecTreeNode<T> codec,
            final DataTreeCandidateNode domData) {
        return new LazyDataObjectModification<>(codec, domData);
    }

    private static Collection<LazyDataObjectModification<? extends DataObject>> from(
            final BindingCodecTreeNode<?> parentCodec, final Collection<DataTreeCandidateNode> domChildNodes) {
        final List<LazyDataObjectModification<? extends DataObject>> result = new ArrayList<>(domChildNodes.size());
        populateList(result, parentCodec, domChildNodes);
        return result;
    }

    private static void populateList(final List<LazyDataObjectModification<? extends DataObject>> result,
            final BindingCodecTreeNode<?> parentCodec, final Collection<DataTreeCandidateNode> domChildNodes) {
        for (final DataTreeCandidateNode domChildNode : domChildNodes) {
            if (domChildNode.getModificationType() != UNMODIFIED) {
                final BindingStructuralType type = BindingStructuralType.from(domChildNode);
                if (type != BindingStructuralType.NOT_ADDRESSABLE) {
                    /*
                     *  Even if type is UNKNOWN, from perspective of BindingStructuralType
                     *  we try to load codec for it. We will use that type to further specify
                     *  debug log.
                     */
                    try {
                        final BindingCodecTreeNode<?> childCodec =
                                parentCodec.yangPathArgumentChild(domChildNode.getIdentifier());
                        populateList(result, type, childCodec, domChildNode);
                    } catch (final IllegalArgumentException e) {
                        if (type == BindingStructuralType.UNKNOWN) {
                            LOG.debug("Unable to deserialize unknown DOM node {}", domChildNode, e);
                        } else {
                            LOG.debug("Binding representation for DOM node {} was not found", domChildNode, e);
                        }
                    }
                }
            }
        }
    }

    private static void populateList(final List<LazyDataObjectModification<? extends DataObject>> result,
            final BindingStructuralType type, final BindingCodecTreeNode<?> childCodec,
            final DataTreeCandidateNode domChildNode) {
        switch (type) {
            case INVISIBLE_LIST:
                // We use parent codec intentionally.
                populateListWithSingleCodec(result, childCodec, domChildNode.getChildNodes());
                break;
            case INVISIBLE_CONTAINER:
                populateList(result, childCodec, domChildNode.getChildNodes());
                break;
            case UNKNOWN:
            case VISIBLE_CONTAINER:
                result.add(create(childCodec, domChildNode));
                break;
            default:
                break;
        }
    }

    private static void populateListWithSingleCodec(final List<LazyDataObjectModification<? extends DataObject>> result,
            final BindingCodecTreeNode<?> codec, final Collection<DataTreeCandidateNode> childNodes) {
        for (final DataTreeCandidateNode child : childNodes) {
            if (child.getModificationType() != UNMODIFIED) {
                result.add(create(codec, child));
            }
        }
    }

    @Override
    public T getDataBefore() {
        return deserialize(domData.getDataBefore());
    }

    @Override
    public T getDataAfter() {
        return deserialize(domData.getDataAfter());
    }

    @Override
    public Class<T> getDataType() {
        return codec.getBindingClass();
    }

    @Override
    public PathArgument getIdentifier() {
        return identifier;
    }

    @Override
    public ModificationType getModificationType() {
        ModificationType localType = modificationType;
        if (localType != null) {
            return localType;
        }

        switch (domData.getModificationType()) {
            case APPEARED:
            case WRITE:
                localType = ModificationType.WRITE;
                break;
            case DISAPPEARED:
            case DELETE:
                localType = ModificationType.DELETE;
                break;
            case SUBTREE_MODIFIED:
                localType = resolveSubtreeModificationType();
                break;
            default:
                // TODO: Should we lie about modification type instead of exception?
                throw new IllegalStateException("Unsupported DOM Modification type " + domData.getModificationType());
        }

        modificationType = localType;
        return localType;
    }

    private ModificationType resolveSubtreeModificationType() {
        switch (codec.getChildAddressabilitySummary()) {
            case ADDRESSABLE:
                // All children are addressable, it is safe to report SUBTREE_MODIFIED
                return ModificationType.SUBTREE_MODIFIED;
            case UNADDRESSABLE:
                // All children are non-addressable, report WRITE
                return ModificationType.WRITE;
            case MIXED:
                // This case is not completely trivial, as we may have NOT_ADDRESSABLE nodes underneath us. If that
                // is the case, we need to turn this modification into a WRITE operation, so that the user is able
                // to observe those nodes being introduced. This is not efficient, but unfortunately unavoidable,
                // as we cannot accurately represent such changes.
                for (DataTreeCandidateNode child : domData.getChildNodes()) {
                    if (BindingStructuralType.recursiveFrom(child) == BindingStructuralType.NOT_ADDRESSABLE) {
                        // We have a non-addressable child, turn this modification into a write
                        return ModificationType.WRITE;
                    }
                }

                // No unaddressable children found, proceed in addressed mode
                return ModificationType.SUBTREE_MODIFIED;
            default:
                throw new IllegalStateException("Unsupported child addressability summary "
                        + codec.getChildAddressabilitySummary());
        }
    }

    @Override
    public Collection<LazyDataObjectModification<? extends DataObject>> getModifiedChildren() {
        Collection<LazyDataObjectModification<? extends DataObject>> local = childNodesCache;
        if (local == null) {
            childNodesCache = local = from(codec, domData.getChildNodes());
        }
        return local;
    }

    @Override
    public <H extends ChoiceIn<? super T> & DataObject, C extends ChildOf<? super H>>
            Collection<DataObjectModification<C>> getModifiedChildren(final Class<H> caseType,
                    final Class<C> childType) {
        return streamModifiedChildren(childType)
                .filter(child -> caseType.equals(child.identifier.getCaseType().orElse(null)))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <C extends DataObject> Stream<LazyDataObjectModification<C>> streamModifiedChildren(
            final Class<C> childType) {
        return getModifiedChildren().stream()
                .filter(child -> childType.isAssignableFrom(child.getDataType()))
                .map(child -> (LazyDataObjectModification<C>) child);
    }

    @Override
    public DataObjectModification<? extends DataObject> getModifiedChild(final PathArgument arg) {
        final List<YangInstanceIdentifier.PathArgument> domArgumentList = new ArrayList<>();
        final BindingCodecTreeNode<?> childCodec = codec.bindingPathArgumentChild(arg, domArgumentList);
        final Iterator<YangInstanceIdentifier.PathArgument> toEnter = domArgumentList.iterator();
        DataTreeCandidateNode current = domData;
        while (toEnter.hasNext() && current != null) {
            current = current.getModifiedChild(toEnter.next()).orElse(null);
        }
        return current != null && current.getModificationType() != UNMODIFIED ? create(childCodec, current) : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Identifiable<K> & ChildOf<? super T>, K extends Identifier<C>> DataObjectModification<C>
            getModifiedChildListItem(final Class<C> listItem, final K listKey) {
        return (DataObjectModification<C>) getModifiedChild(IdentifiableItem.of(listItem, listKey));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <H extends ChoiceIn<? super T> & DataObject, C extends Identifiable<K> & ChildOf<? super H>,
            K extends Identifier<C>> DataObjectModification<C> getModifiedChildListItem(final Class<H> caseType,
                        final Class<C> listItem, final K listKey) {
        return (DataObjectModification<C>) getModifiedChild(IdentifiableItem.of(caseType, listItem, listKey));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends ChildOf<? super T>> DataObjectModification<C> getModifiedChildContainer(final Class<C> child) {
        return (DataObjectModification<C>) getModifiedChild(Item.of(child));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <H extends ChoiceIn<? super T> & DataObject, C extends ChildOf<? super H>> DataObjectModification<C>
            getModifiedChildContainer(final Class<H> caseType, final Class<C> child) {
        return (DataObjectModification<C>) getModifiedChild(Item.of(caseType, child));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Augmentation<T> & DataObject> DataObjectModification<C> getModifiedAugmentation(
            final Class<C> augmentation) {
        return (DataObjectModification<C>) getModifiedChild(Item.of(augmentation));
    }

    private T deserialize(final Optional<NormalizedNode<?, ?>> dataAfter) {
        return dataAfter.map(codec::deserialize).orElse(null);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{identifier = " + identifier + ", domData = " + domData + "}";
    }
}
