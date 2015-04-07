/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;

/**
 *
 * Defines structural mapping of Normalized Node to Binding data
 * addressable by Instance Identifier.
 *
 * Not all binding data are addressable by instance identifier
 * and there are some differences.
 *
 * See {@link #NOT_ADDRESSABLE},{@link #INVISIBLE_CONTAINER},{@link #VISIBLE_CONTAINER}
 * for more details.
 *
 *
 */
enum BindingStructuralType {

    /**
     * DOM Item is not addressable in Binding Instance Identifier,
     * data is not lost, but are available only via parent object.
     *
     * Such types of data are leaf-lists, leafs, list without keys
     * or anyxml.
     *
     */
    NOT_ADDRESSABLE,
    /**
     * Data container is addressable in NormalizedNode format,
     * but in Binding it is not represented in Instance Identifier.
     *
     * This are choice / case nodes.
     *
     * This data is still accessible using parent object and their
     * children are addressable.
     *
     */
    INVISIBLE_CONTAINER,
    /**
     * Data container is addressable in NormalizedNode format,
     * but in Binding it is not represented in Instance Identifier.
     *
     * This are list nodes.
     *
     * This data is still accessible using parent object and their
     * children are addressable.
     *
     */
    INVISIBLE_LIST,
    /**
     * Data container is addressable in Binding Instance Identifier format
     * and also YangInstanceIdentifier format.
     *
     */
    VISIBLE_CONTAINER,
    /**
     * Mapping algorithm was unable to detect type or was not updated after introduction
     * of new NormalizedNode type.
     */
    UNKNOWN;

    static BindingStructuralType from(final DataTreeCandidateNode domChildNode) {
        final Optional<NormalizedNode<?, ?>> dataBased = domChildNode.getDataAfter().or(domChildNode.getDataBefore());
        if(dataBased.isPresent()) {
            return from(dataBased.get());
        }
        return from(domChildNode.getIdentifier());
    }

    private static BindingStructuralType from(final PathArgument identifier) {
        if(identifier instanceof NodeIdentifierWithPredicates || identifier instanceof AugmentationIdentifier) {
            return VISIBLE_CONTAINER;
        }
        if(identifier instanceof NodeWithValue) {
            return NOT_ADDRESSABLE;
        }
        return UNKNOWN;
    }

    static BindingStructuralType from(final NormalizedNode<?, ?> data) {
        if(isNotAddressable(data)) {
            return NOT_ADDRESSABLE;
        }
        if(data instanceof MapNode) {
            return INVISIBLE_LIST;
        }
        if(data instanceof ChoiceNode) {
            return INVISIBLE_CONTAINER;
        }
        if(isVisibleContainer(data)) {
            return VISIBLE_CONTAINER;
        }
        return UNKNOWN;
    }

    private static boolean isVisibleContainer(final NormalizedNode<?, ?> data) {
        return data instanceof MapEntryNode || data instanceof ContainerNode || data instanceof AugmentationNode;
    }

    private static boolean isNotAddressable(final NormalizedNode<?, ?> d) {
        return d instanceof LeafNode
                || d instanceof AnyXmlNode
                || d instanceof LeafSetNode
                || d instanceof LeafSetEntryNode;
    }

}
