/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.util;

import org.opendaylight.controller.cluster.datastore.node.utils.NodeIdentifierFactory;
import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.PathArgumentSerializer;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.QNameDeSerializationContext;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.QNameDeSerializationContextImpl;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.QNameSerializationContext;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.QNameSerializationContextImpl;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.InstanceIdentifier.Builder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class contains utility methods for converting an MD-SAL
 * YangInstanceIdentifier to and from other representations.
 * <p>
 * The representations convered for now are,
 *
 * <ul>
 *     <li>String</li>
 *     <li>Protocol Buffer</li>
 * </ul>
 */
public class InstanceIdentifierUtils {

    protected static final Logger logger = LoggerFactory
        .getLogger(InstanceIdentifierUtils.class);

    /**
     * Convert an MD-SAL YangInstanceIdentifier into a protocol buffer version of it
     *
     * @param path an MD-SAL YangInstanceIdentifier
     * @return a protocol buffer version of the MD-SAL YangInstanceIdentifier
     */
    public static NormalizedNodeMessages.InstanceIdentifier toSerializable(YangInstanceIdentifier path) {
        QNameSerializationContextImpl context = new QNameSerializationContextImpl();
        Builder builder = toSerializableBuilder(path, context);
        return builder.addAllCode(context.getCodes()).build();
    }

    public static NormalizedNodeMessages.InstanceIdentifier toSerializable(
            YangInstanceIdentifier path, QNameSerializationContext context) {
        return toSerializableBuilder(path, context).build();
    }

    private static NormalizedNodeMessages.InstanceIdentifier.Builder toSerializableBuilder(
            YangInstanceIdentifier path, QNameSerializationContext context) {
        NormalizedNodeMessages.InstanceIdentifier.Builder builder =
            NormalizedNodeMessages.InstanceIdentifier.newBuilder();

        try {
            for (PathArgument pathArgument : path.getPathArguments()) {
                NormalizedNodeMessages.PathArgument serializablePathArgument;
                if(context == null) {
                    String nodeType = "";
                    if (!(pathArgument instanceof AugmentationIdentifier)) {
                        nodeType = pathArgument.getNodeType().toString();
                    }

                    serializablePathArgument = NormalizedNodeMessages.PathArgument.newBuilder()
                            .setValue(pathArgument.toString())
                            .setType(pathArgument.getClass().getSimpleName())
                            .setNodeType(NormalizedNodeMessages.QName.newBuilder().setValue(nodeType))
                            .addAllAttributes(getPathArgumentAttributes(pathArgument)).build();
                } else {
                    serializablePathArgument = PathArgumentSerializer.serialize(context, pathArgument);
                }

                builder.addArguments(serializablePathArgument);
            }
        } catch(Exception e){
            logger.error("An exception occurred", e);
        }

        return builder;
    }


    /**
     * Convert a protocol buffer version of the MD-SAL YangInstanceIdentifier into
     * the MD-SAL version of the YangInstanceIdentifier
     *
     * @param path a protocol buffer version of the MD-SAL YangInstanceIdentifier
     * @return  an MD-SAL YangInstanceIdentifier
     */
    public static YangInstanceIdentifier fromSerializable(NormalizedNodeMessages.InstanceIdentifier path) {
        return fromSerializable(path, new QNameDeSerializationContextImpl(path.getCodeList()));
    }

    public static YangInstanceIdentifier fromSerializable(NormalizedNodeMessages.InstanceIdentifier path,
            QNameDeSerializationContext context) {

        List<PathArgument> pathArguments = new ArrayList<>();

        for(NormalizedNodeMessages.PathArgument pathArgument : path.getArgumentsList()) {
            if(context == null || pathArgument.hasType()) {
                pathArguments.add(parsePathArgument(pathArgument));
            } else {
                pathArguments.add(PathArgumentSerializer.deSerialize(context, pathArgument));
            }
        }

        return YangInstanceIdentifier.create(pathArguments);
    }

    /**
     * Take the various attributes of a PathArgument and package them up as
     * protocol buffer attributes.
     * <p>
     *
     * PathArguments have 4 subtypes and each of the various subtypes have
     * different attributes
     * <ul>
     *     <li>
     *         NodeIdentifier is the most basic PathArgument. It is used for
     *         ContainerNode, LeafNode etc and has no attributes
     *     </li>
     *     <li>
     *         NodeWithValue has only a single attribute. It is used for
     *         LeafListEntryNodes and the attribute it contains is the value
     *         of the entry
     *     </li>
     *     <li>
     *         NodeIdentifierWithPredicates has a map of attributes.
     *         It is used to represent a ListItemNode. Each entry
     *         in the map of attributes represents the key and value of the
     *         keys in that entry.
     *     </li>
     *     <li>
     *         AugmentationIdentifier has a list of unnamed attributes. Each
     *         attribute represents the possible children that can go within
     *         an augmentation entry.
     *     </li>
     * </ul>
     * @param pathArgument
     * @return
     */
    private static Iterable<? extends NormalizedNodeMessages.Attribute> getPathArgumentAttributes(
        PathArgument pathArgument) {
        List<NormalizedNodeMessages.Attribute> attributes = new ArrayList<>();

        if (pathArgument instanceof NodeWithValue) {
            NodeWithValue<?> identifier = (NodeWithValue<?>) pathArgument;

            NormalizedNodeMessages.Attribute attribute =
                NormalizedNodeMessages.Attribute.newBuilder()
                    .setName("name")
                    .setValue(identifier.getValue().toString())
                    .setType(identifier.getValue().getClass().getSimpleName())
                    .build();

            attributes.add(attribute);
        } else if (pathArgument instanceof NodeIdentifierWithPredicates) {
            NodeIdentifierWithPredicates identifier = (NodeIdentifierWithPredicates) pathArgument;

            for (QName key : identifier.getKeyValues().keySet()) {
                Object value = identifier.getKeyValues().get(key);
                NormalizedNodeMessages.Attribute attribute =
                    NormalizedNodeMessages.Attribute.newBuilder()
                        .setName(key.toString())
                        .setValue(value.toString())
                        .setType(value.getClass().getSimpleName())
                        .build();

                attributes.add(attribute);

            }

        } else if(pathArgument instanceof AugmentationIdentifier) {
            AugmentationIdentifier identifier = (AugmentationIdentifier) pathArgument;

            for (QName key : identifier.getPossibleChildNames()) {
                Object value = key;
                NormalizedNodeMessages.Attribute attribute =
                    NormalizedNodeMessages.Attribute.newBuilder()
                        .setName(key.toString())
                        .setValue(value.toString())
                        .setType(value.getClass().getSimpleName())
                        .build();

                attributes.add(attribute);

            }
        }

        return attributes;
    }


    /**
     * Parse a protocol buffer PathArgument and return an MD-SAL PathArgument
     *
     * @param pathArgument protocol buffer PathArgument
     * @return MD-SAL PathArgument
     */
    private static PathArgument parsePathArgument(
            NormalizedNodeMessages.PathArgument pathArgument) {
        if (NodeWithValue.class.getSimpleName().equals(pathArgument.getType())) {

            NodeWithValue<?> nodeWithValue = new NodeWithValue<>(
                    QNameFactory.create(pathArgument.getNodeType().getValue()),
                    parseAttribute(pathArgument.getAttributes(0)));

            return nodeWithValue;

        } else if(NodeIdentifierWithPredicates.class.getSimpleName().equals(pathArgument.getType())){

            NodeIdentifierWithPredicates nodeIdentifierWithPredicates =
                new NodeIdentifierWithPredicates(
                    QNameFactory.create(pathArgument.getNodeType().getValue()), toAttributesMap(pathArgument.getAttributesList()));

            return nodeIdentifierWithPredicates;

        } else if(AugmentationIdentifier.class.getSimpleName().equals(pathArgument.getType())){

            Set<QName> qNameSet = new HashSet<>();

            for(NormalizedNodeMessages.Attribute attribute : pathArgument.getAttributesList()){
                qNameSet.add(QNameFactory.create(attribute.getValue()));
            }

            return new AugmentationIdentifier(qNameSet);
        }

        return NodeIdentifierFactory.getArgument(pathArgument.getValue());
    }

    private static Map<QName, Object> toAttributesMap(
        List<NormalizedNodeMessages.Attribute> attributesList) {

        Map<QName, Object> map = new HashMap<>();

        for(NormalizedNodeMessages.Attribute attribute : attributesList){
            String name = attribute.getName();
            Object value = parseAttribute(attribute);

            map.put(QNameFactory.create(name), value);
        }

        return map;
    }

    /**
     * FIXME: This method only covers a subset of values that may go in an InstanceIdentifier
     *
     * @param attribute
     * @return
     */
    private static Object parseAttribute(NormalizedNodeMessages.Attribute attribute){
        if(Short.class.getSimpleName().equals(attribute.getType())) {
            return Short.parseShort(attribute.getValue());
        } else if(Long.class.getSimpleName().equals(attribute.getType())){
            return Long.parseLong(attribute.getValue());
        } else if(Boolean.class.getSimpleName().equals(attribute.getType())){
            return Boolean.parseBoolean(attribute.getValue());
        } else if(Integer.class.getSimpleName().equals(attribute.getType())){
            return Integer.parseInt(attribute.getValue());
        }

        return attribute.getValue();
    }
}
