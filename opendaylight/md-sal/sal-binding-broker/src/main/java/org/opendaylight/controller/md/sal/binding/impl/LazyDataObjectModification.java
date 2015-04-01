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
import java.util.LinkedList;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeNode;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;

class LazyDataObjectModification<T extends DataObject> implements DataObjectModification<T> {

    private final BindingCodecTreeNode<T> codec;
    private final DataTreeCandidateNode domData;
    private final PathArgument identifier;
    private Collection<DataObjectModification<? extends DataObject>> childNodesCache;

    LazyDataObjectModification(final BindingCodecTreeNode<T> codec, final DataTreeCandidateNode domData) {
        this.codec = Preconditions.checkNotNull(codec);
        this.domData = Preconditions.checkNotNull(domData);
        this.identifier = codec.deserializePathArgument(domData.getIdentifier());
    }

    static DataObjectModification<? extends DataObject> create(final BindingCodecTreeNode<?> codec,
            final DataTreeCandidateNode domData) {
        return new LazyDataObjectModification<>(codec,domData);
    }

    static Collection<DataObjectModification<? extends DataObject>> from(final BindingCodecTreeNode<?> parentCodec,
            final Collection<DataTreeCandidateNode> domChildNodes) {
        final ArrayList<DataObjectModification<? extends DataObject>> result = new ArrayList<>(domChildNodes.size());
        populateList(result,parentCodec,domChildNodes);
        return result;
    }

    private static void populateList(final ArrayList<DataObjectModification<? extends DataObject>> result, final BindingCodecTreeNode<?> parentCodec,
            final Collection<DataTreeCandidateNode> domChildNodes) {
        for(final DataTreeCandidateNode domChildNode : domChildNodes) {
            final BindingStructuralType type = BindingStructuralType.from(domChildNode);
            if(type != BindingStructuralType.NOT_ADDRESSABLE) {
                final BindingCodecTreeNode<?> childCodec = parentCodec.yangPathArgumentChild(domChildNode.getIdentifier());
                if(type == BindingStructuralType.INVISIBLE_CONTAINER) {
                    populateList(result,childCodec,domChildNode.getChildNodes());
                } else {
                    result.add(create(childCodec, domChildNode));
                }
            }
        }
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
            case SUBTREE_MODIFIED:
                return DataObjectModification.ModificationType.SUBTREE_MODIFIED;
            case DELETE:
                return DataObjectModification.ModificationType.DELETE;

            default:
                // TODO: Should we lie about modification type instead of exception?
                throw new IllegalStateException("Unsupported DOM Modification type " + domData.getModificationType());
        }
    }

    @Override
    public Collection<DataObjectModification<? extends DataObject>> getModifiedChildren() {
        if(childNodesCache == null) {
            childNodesCache = from(codec,domData.getChildNodes());
        }
        return childNodesCache;
    }

    public DataObjectModification<? extends DataObject> getModifiedChild(final PathArgument arg) {
        final LinkedList<YangInstanceIdentifier.PathArgument> domArgumentList = new LinkedList<>();
        final BindingCodecTreeNode<?> childCodec = codec.bindingPathArgumentChild(arg, domArgumentList);

        final Iterator<YangInstanceIdentifier.PathArgument> toEnter = domArgumentList.iterator();
        DataTreeCandidateNode current = domData;
        while(toEnter.hasNext() && current != null) {
            current = current.getModifiedChild(toEnter.next());
        }
        if(current != null) {
            return create(childCodec, current);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <C extends ChildOf<T>> DataObjectModification<C> getModifiedChild(final Class<C> arg) {
        return (DataObjectModification<C>) getModifiedChild(new InstanceIdentifier.Item<>(arg));
    }

    private T deserialize(final Optional<NormalizedNode<?, ?>> dataAfter) {
        if(dataAfter.isPresent()) {
            return codec.deserialize(dataAfter.get());
        }
        return null;
    }
}
