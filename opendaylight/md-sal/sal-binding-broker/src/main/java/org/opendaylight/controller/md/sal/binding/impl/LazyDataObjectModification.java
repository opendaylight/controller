/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeNode;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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

    private final static Logger LOG = LoggerFactory.getLogger(LazyDataObjectModification.class);

    private final BindingCodecTreeNode<T> codec;
    private final DataTreeCandidateNode domData;
    private final PathArgument identifier;
    private Collection<DataObjectModification<? extends DataObject>> childNodesCache;

    private LazyDataObjectModification(final BindingCodecTreeNode<T> codec, final DataTreeCandidateNode domData) {
        this.codec = Preconditions.checkNotNull(codec);
        this.domData = Preconditions.checkNotNull(domData);
        this.identifier = codec.deserializePathArgument(domData.getIdentifier());
    }

    static <T extends DataObject> DataObjectModification<T> create(final BindingCodecTreeNode<T> codec,
            final DataTreeCandidateNode domData) {
        return new LazyDataObjectModification<>(codec,domData);
    }

    private static Collection<DataObjectModification<? extends DataObject>> from(final BindingCodecTreeNode<?> parentCodec,
            final Collection<DataTreeCandidateNode> domChildNodes) {
        final List<DataObjectModification<? extends DataObject>> result = new ArrayList<>(domChildNodes.size());
        populateList(result, parentCodec, domChildNodes);
        return result;
    }

    private static void populateList(final List<DataObjectModification<? extends DataObject>> result,
            final BindingCodecTreeNode<?> parentCodec, final Collection<DataTreeCandidateNode> domChildNodes) {
        for (final DataTreeCandidateNode domChildNode : domChildNodes) {
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
                    populateList(result,type, childCodec, domChildNode);
                } catch (final IllegalArgumentException e) {
                    if (type == BindingStructuralType.UNKNOWN) {
                        LOG.debug("Unable to deserialize unknown DOM node {}",domChildNode,e);
                    } else {
                        LOG.debug("Binding representation for DOM node {} was not found",domChildNode,e);
                    }
                }
            }
        }
    }

    private static void populateList(final List<DataObjectModification<? extends DataObject>> result,
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
            default:
                break;
        }
    }

    private static void populateListWithSingleCodec(final List<DataObjectModification<? extends DataObject>> result,
            final BindingCodecTreeNode<?> codec, final Collection<DataTreeCandidateNode> childNodes) {
        for (final DataTreeCandidateNode child : childNodes) {
            result.add(create(codec, child));
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
    public DataObjectModification.ModificationType getModificationType() {
        switch(domData.getModificationType()) {
            case WRITE:
                return DataObjectModification.ModificationType.WRITE;
            case APPEARED:
            case SUBTREE_MODIFIED:
                return DataObjectModification.ModificationType.SUBTREE_MODIFIED;
            case DISAPPEARED:
            case DELETE:
                return DataObjectModification.ModificationType.DELETE;
            default:
                // TODO: Should we lie about modification type instead of exception?
                throw new IllegalStateException("Unsupported DOM Modification type " + domData.getModificationType());
        }
    }

    @Override
    public Collection<DataObjectModification<? extends DataObject>> getModifiedChildren() {
        if (childNodesCache == null) {
            childNodesCache = from(codec, domData.getChildNodes());
        }
        return childNodesCache;
    }

    @Override
    public DataObjectModification<? extends DataObject> getModifiedChild(final PathArgument arg) {
        final List<YangInstanceIdentifier.PathArgument> domArgumentList = new ArrayList<>();
        final BindingCodecTreeNode<?> childCodec = codec.bindingPathArgumentChild(arg, domArgumentList);
        final Iterator<YangInstanceIdentifier.PathArgument> toEnter = domArgumentList.iterator();
        DataTreeCandidateNode current = domData;
        while (toEnter.hasNext() && current != null) {
            current = current.getModifiedChild(toEnter.next());
        }
        if (current != null) {
            return create(childCodec, current);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Identifiable<K> & ChildOf<? super T>, K extends Identifier<C>> DataObjectModification<C> getModifiedChildListItem(
            final Class<C> listItem, final K listKey) {
        return (DataObjectModification<C>) getModifiedChild(new InstanceIdentifier.IdentifiableItem<>(listItem, listKey));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends ChildOf<? super T>> DataObjectModification<C> getModifiedChildContainer(final Class<C> arg) {
        return (DataObjectModification<C>) getModifiedChild(new InstanceIdentifier.Item<>(arg));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Augmentation<T> & DataObject> DataObjectModification<C> getModifiedAugmentation(
            final Class<C> augmentation) {
        return (DataObjectModification<C>) getModifiedChild(new InstanceIdentifier.Item<>(augmentation));
    }

    private T deserialize(final Optional<NormalizedNode<?, ?>> dataAfter) {
        if (dataAfter.isPresent()) {
            return codec.deserialize(dataAfter.get());
        }
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{identifier = " + identifier + ", domData = " + domData + "}";
    }
}
