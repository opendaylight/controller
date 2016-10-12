/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.PathArgumentType.getSerializablePathArgumentType;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.node.utils.NodeIdentifierFactory;
import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class PathArgumentSerializer {
    private static final String REVISION_ARG = "?revision=";
    private static final Map<Class<?>, PathArgumentAttributesGetter> PATH_ARGUMENT_ATTRIBUTES_GETTERS = new HashMap<>();

    public static NormalizedNodeMessages.PathArgument serialize(QNameSerializationContext context,
            YangInstanceIdentifier.PathArgument pathArgument) {
        Preconditions.checkNotNull(context, "context should not be null");
        Preconditions.checkNotNull(pathArgument, "pathArgument should not be null");

        QName nodeType = null;
        if (!(pathArgument instanceof YangInstanceIdentifier.AugmentationIdentifier)) {
            nodeType = pathArgument.getNodeType();
        }

        NormalizedNodeMessages.PathArgument.Builder builder =
            NormalizedNodeMessages.PathArgument.newBuilder();

        NormalizedNodeMessages.PathArgument serializablePathArgument =
            builder
                .setIntType(getSerializablePathArgumentType(pathArgument))
                .setNodeType(encodeQName(context, nodeType))
                .addAllAttribute(getPathArgumentAttributes(context, pathArgument))
                .build();

        return serializablePathArgument;
    }


    public static YangInstanceIdentifier.PathArgument deSerialize(QNameDeSerializationContext context,
            NormalizedNodeMessages.PathArgument pathArgument) {
        Preconditions.checkNotNull(context, "context should not be null");
        Preconditions.checkNotNull(pathArgument, "pathArgument should not be null");

        return parsePathArgument(context, pathArgument);
    }


    private interface PathArgumentAttributesGetter {
        Iterable<? extends NormalizedNodeMessages.PathArgumentAttribute> get(
                QNameSerializationContext context, YangInstanceIdentifier.PathArgument pathArgument);
    }

    static {
        PATH_ARGUMENT_ATTRIBUTES_GETTERS.put(YangInstanceIdentifier.NodeWithValue.class, (context, pathArgument) -> {
            YangInstanceIdentifier.NodeWithValue<?> identifier = (YangInstanceIdentifier.NodeWithValue<?>) pathArgument;

            NormalizedNodeMessages.PathArgumentAttribute attribute = buildAttribute(context, null,
                    identifier.getValue());

            return Arrays.asList(attribute);
        });

        PATH_ARGUMENT_ATTRIBUTES_GETTERS.put(YangInstanceIdentifier.NodeIdentifierWithPredicates.class,
            (context, pathArgument) -> {
                YangInstanceIdentifier.NodeIdentifierWithPredicates identifier =
                        (YangInstanceIdentifier.NodeIdentifierWithPredicates) pathArgument;

                Map<QName, Object> keyValues = identifier.getKeyValues();
                List<NormalizedNodeMessages.PathArgumentAttribute> attributes = new ArrayList<>(keyValues.size());
                for (Entry<QName, Object> e : keyValues.entrySet()) {
                    NormalizedNodeMessages.PathArgumentAttribute attribute = buildAttribute(context, e.getKey(),
                            e.getValue());

                    attributes.add(attribute);
                }

                return attributes;
            });

        PATH_ARGUMENT_ATTRIBUTES_GETTERS.put(YangInstanceIdentifier.AugmentationIdentifier.class,
            (context, pathArgument) -> {
                YangInstanceIdentifier.AugmentationIdentifier identifier =
                        (YangInstanceIdentifier.AugmentationIdentifier) pathArgument;

                Set<QName> possibleChildNames = identifier.getPossibleChildNames();
                List<NormalizedNodeMessages.PathArgumentAttribute> attributes = new ArrayList<>(
                        possibleChildNames.size());
                for (QName key : possibleChildNames) {
                    Object value = key;
                    NormalizedNodeMessages.PathArgumentAttribute attribute = buildAttribute(context, key, value);

                    attributes.add(attribute);
                }

                return attributes;
            });

        PATH_ARGUMENT_ATTRIBUTES_GETTERS.put(YangInstanceIdentifier.NodeIdentifier.class,
            (context, pathArgument) -> Collections.emptyList());
    }

    private static NormalizedNodeMessages.PathArgumentAttribute buildAttribute(
            QNameSerializationContext context, QName name, Object value) {
        NormalizedNodeMessages.PathArgumentAttribute.Builder builder =
            NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

        builder.setName(encodeQName(context, name));
        ValueSerializer.serialize(builder, context, value);

        return builder.build();

    }

    private static NormalizedNodeMessages.QName.Builder encodeQName(QNameSerializationContext context, QName qname) {
        if (qname == null) {
            return NormalizedNodeMessages.QName.getDefaultInstance().toBuilder();
        }
        NormalizedNodeMessages.QName.Builder qnameBuilder = NormalizedNodeMessages.QName.newBuilder();

        qnameBuilder.setNamespace(context.addNamespace(qname.getNamespace()));

        qnameBuilder.setRevision(context.addRevision(qname.getRevision()));

        qnameBuilder.setLocalName(context.addLocalName(qname.getLocalName()));

        return qnameBuilder;
    }

    private static Iterable<? extends NormalizedNodeMessages.PathArgumentAttribute> getPathArgumentAttributes(
            QNameSerializationContext context, YangInstanceIdentifier.PathArgument pathArgument) {

        return PATH_ARGUMENT_ATTRIBUTES_GETTERS.get(pathArgument.getClass()).get(context, pathArgument);
    }


    private static String qNameToString(QNameDeSerializationContext context, NormalizedNodeMessages.QName qname) {
        // If this serializer is used qName cannot be null (see encodeQName)
        // adding null check only in case someone tried to deSerialize a protocol buffer node
        // that was not serialized using the PathArgumentSerializer
//        Preconditions.checkNotNull(qName, "qName should not be null");
//        Preconditions.checkArgument(qName.getNamespace() != -1, "qName.namespace should be valid");

        String namespace = context.getNamespace(qname.getNamespace());
        String localName = context.getLocalName(qname.getLocalName());
        StringBuilder sb;
        if (qname.getRevision() != -1) {
            String revision = context.getRevision(qname.getRevision());
            sb = new StringBuilder(namespace.length() + REVISION_ARG.length() + revision.length()
                    + localName.length() + 2);
            sb.append('(').append(namespace).append(REVISION_ARG).append(
                revision).append(')').append(localName);
        } else {
            sb = new StringBuilder(namespace.length() + localName.length() + 2);
            sb.append('(').append(namespace).append(')').append(localName);
        }

        return sb.toString();
    }

    /**
     * Parse a protocol buffer PathArgument and return an MD-SAL PathArgument.
     *
     * @param pathArgument protocol buffer PathArgument
     * @return MD-SAL PathArgument
     */
    private static YangInstanceIdentifier.PathArgument parsePathArgument(
            QNameDeSerializationContext context, NormalizedNodeMessages.PathArgument pathArgument) {

        switch (PathArgumentType.values()[pathArgument.getIntType()]) {
            case NODE_IDENTIFIER_WITH_VALUE : {
                YangInstanceIdentifier.NodeWithValue<?> nodeWithValue =
                    new YangInstanceIdentifier.NodeWithValue<>(
                        QNameFactory.create(qNameToString(context, pathArgument.getNodeType())),
                        parseAttribute(context, pathArgument.getAttribute(0)));

                return nodeWithValue;
            }

            case NODE_IDENTIFIER_WITH_PREDICATES : {
                YangInstanceIdentifier.NodeIdentifierWithPredicates
                    nodeIdentifierWithPredicates =
                    new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                        QNameFactory.create(qNameToString(context, pathArgument.getNodeType())),
                        toAttributesMap(context, pathArgument.getAttributeList()));

                return nodeIdentifierWithPredicates;
            }

            case AUGMENTATION_IDENTIFIER: {
                Set<QName> qnameSet = new HashSet<>();

                for (NormalizedNodeMessages.PathArgumentAttribute attribute : pathArgument.getAttributeList()) {
                    qnameSet.add(QNameFactory.create(qNameToString(context, attribute.getName())));
                }

                return new YangInstanceIdentifier.AugmentationIdentifier(qnameSet);

            }
            default: {
                return NodeIdentifierFactory.getArgument(qNameToString(context,
                    pathArgument.getNodeType()));
            }

        }
    }

    private static Map<QName, Object> toAttributesMap(
            QNameDeSerializationContext context,
            List<NormalizedNodeMessages.PathArgumentAttribute> attributesList) {

        Map<QName, Object> map;
        if (attributesList.size() == 1) {
            NormalizedNodeMessages.PathArgumentAttribute attribute = attributesList.get(0);
            NormalizedNodeMessages.QName name = attribute.getName();
            Object value = parseAttribute(context, attribute);
            map = Collections.singletonMap(QNameFactory.create(qNameToString(context, name)), value);
        } else {
            map = new HashMap<>();

            for (NormalizedNodeMessages.PathArgumentAttribute attribute : attributesList) {
                NormalizedNodeMessages.QName name = attribute.getName();
                Object value = parseAttribute(context, attribute);

                map.put(QNameFactory.create(qNameToString(context, name)), value);
            }
        }

        return map;
    }

    private static Object parseAttribute(QNameDeSerializationContext context,
            NormalizedNodeMessages.PathArgumentAttribute attribute) {
        return ValueSerializer.deSerialize(context, attribute);
    }
}
