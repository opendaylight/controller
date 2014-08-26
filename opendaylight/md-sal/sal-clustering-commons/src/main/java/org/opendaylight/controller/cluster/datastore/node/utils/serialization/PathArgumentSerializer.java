/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.node.utils.NodeIdentifierFactory;
import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.PathArgumentType.AUGMENTATION_IDENTIFIER;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.PathArgumentType.NODE_IDENTIFIER_WITH_PREDICATES;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.PathArgumentType.NODE_IDENTIFIER_WITH_VALUE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.PathArgumentType.getSerializablePathArgumentType;

public class PathArgumentSerializer {
    public static NormalizedNodeMessages.PathArgument serialize(NormalizedNodeSerializationContext context, YangInstanceIdentifier.PathArgument pathArgument){
        return new Serializer(context, pathArgument).serialize();
    }

    public static YangInstanceIdentifier.PathArgument deSerialize(NormalizedNodeDeSerializationContext context, NormalizedNodeMessages.PathArgument pathArgument){
        return new DeSerializer(context, pathArgument).deSerialize();
    }

    private static class Serializer {

        private final NormalizedNodeSerializationContext context;
        private final YangInstanceIdentifier.PathArgument pathArgument;
        private static final Map<Class, PathArgumentAttributesGetter> pathArgumentAttributesGetters = new HashMap<>();

        static {
            pathArgumentAttributesGetters.put(YangInstanceIdentifier.NodeWithValue.class, new PathArgumentAttributesGetter() {
                @Override
                public Iterable<? extends NormalizedNodeMessages.PathArgumentAttribute> get(
                    Serializer instance,
                    YangInstanceIdentifier.PathArgument pathArgument) {
                    List<NormalizedNodeMessages.PathArgumentAttribute> attributes =
                        new ArrayList<>();

                    YangInstanceIdentifier.NodeWithValue identifier
                        = (YangInstanceIdentifier.NodeWithValue) pathArgument;

                    NormalizedNodeMessages.PathArgumentAttribute attribute =
                        instance.buildAttribute(null, identifier.getValue());

                    attributes.add(attribute);

                    return attributes;

                }
            });

            pathArgumentAttributesGetters.put(YangInstanceIdentifier.NodeIdentifierWithPredicates.class, new PathArgumentAttributesGetter() {
                @Override
                public Iterable<? extends NormalizedNodeMessages.PathArgumentAttribute> get(
                    Serializer instance,
                    YangInstanceIdentifier.PathArgument pathArgument) {

                    List<NormalizedNodeMessages.PathArgumentAttribute> attributes =
                        new ArrayList<>();

                    YangInstanceIdentifier.NodeIdentifierWithPredicates identifier
                        =
                        (YangInstanceIdentifier.NodeIdentifierWithPredicates) pathArgument;

                    for (QName key : identifier.getKeyValues().keySet()) {
                        Object value = identifier.getKeyValues().get(key);
                        NormalizedNodeMessages.PathArgumentAttribute attribute =
                            instance.buildAttribute(key, value);

                        attributes.add(attribute);

                    }

                    return attributes;

                }
            });

            pathArgumentAttributesGetters.put(YangInstanceIdentifier.AugmentationIdentifier.class, new PathArgumentAttributesGetter() {
                @Override
                public Iterable<? extends NormalizedNodeMessages.PathArgumentAttribute> get(
                    Serializer instance,
                    YangInstanceIdentifier.PathArgument pathArgument) {

                    List<NormalizedNodeMessages.PathArgumentAttribute> attributes =
                        new ArrayList<>();

                    YangInstanceIdentifier.AugmentationIdentifier identifier
                        =
                        (YangInstanceIdentifier.AugmentationIdentifier) pathArgument;

                    for (QName key : identifier.getPossibleChildNames()) {
                        Object value = key;
                        NormalizedNodeMessages.PathArgumentAttribute attribute =
                            instance.buildAttribute(key, value);

                        attributes.add(attribute);

                    }

                    return attributes;

                }
            });


            pathArgumentAttributesGetters.put(YangInstanceIdentifier.NodeIdentifier.class, new PathArgumentAttributesGetter() {
                @Override
                public Iterable<? extends NormalizedNodeMessages.PathArgumentAttribute> get(
                    Serializer instance,
                    YangInstanceIdentifier.PathArgument pathArgument) {
                    return Collections.EMPTY_LIST;
                }
            });
        }

        public Serializer(NormalizedNodeSerializationContext context,
            YangInstanceIdentifier.PathArgument pathArgument) {

            this.context = context;
            this.pathArgument = pathArgument;
        }

        public NormalizedNodeMessages.PathArgument serialize() {
            return serialize(this.pathArgument);
        }

        private NormalizedNodeMessages.PathArgument serialize(
            YangInstanceIdentifier.PathArgument pathArgument) {
            QName nodeType = null;
            if (!(pathArgument instanceof YangInstanceIdentifier.AugmentationIdentifier)) {
                nodeType = pathArgument.getNodeType();
            }

            NormalizedNodeMessages.PathArgument.Builder builder =
                NormalizedNodeMessages.PathArgument.newBuilder();

            NormalizedNodeMessages.PathArgument serializablePathArgument =
                builder
                    .setType(getSerializablePathArgumentType(pathArgument))
                    .setNodeType(encodeQName(nodeType))
                    .addAllAttribute(getPathArgumentAttributes(pathArgument))
                    .build();

            return serializablePathArgument;

        }


        private Iterable<? extends NormalizedNodeMessages.PathArgumentAttribute> getPathArgumentAttributes(
            YangInstanceIdentifier.PathArgument pathArgument) {

            return pathArgumentAttributesGetters.get(pathArgument.getClass()).get(this, pathArgument);

        }

        NormalizedNodeMessages.QName.Builder encodeQName(QName qName){
            if(qName == null){
                return NormalizedNodeMessages.QName.getDefaultInstance().toBuilder();
            }
            NormalizedNodeMessages.QName.Builder qNameBuilder =
                NormalizedNodeMessages.QName.newBuilder();
            URI namespace = qName.getNamespace();
            int namespaceInt = context.getNamespace(namespace);

            if (namespaceInt == -1) {
                namespaceInt = context.addNamespace(namespace);
            }

            qNameBuilder.setNamespace(namespaceInt);

            Date revision = qName.getRevision();
            int revisionInt = context.getRevision(revision);

            if (revisionInt == -1) {
                revisionInt = context.addRevision(revision);
            }

            qNameBuilder.setRevision(revisionInt);
            qNameBuilder.setLocalName(qName.getLocalName());

            return qNameBuilder;
        }

        private NormalizedNodeMessages.PathArgumentAttribute buildAttribute(QName name, Object value){
            NormalizedNodeMessages.PathArgumentAttribute.Builder builder =
                NormalizedNodeMessages.PathArgumentAttribute.newBuilder();

            builder.setName(encodeQName(name));
            ValueSerializer.serialize(builder, context, value);

            return builder.build();

        }

        private static interface PathArgumentAttributesGetter {
            Iterable<? extends NormalizedNodeMessages.PathArgumentAttribute> get(Serializer instance,
                YangInstanceIdentifier.PathArgument pathArgument);
        }

    }


    private static class DeSerializer {

        private final NormalizedNodeDeSerializationContext context;
        private final NormalizedNodeMessages.PathArgument pathArgument;

        public DeSerializer(NormalizedNodeDeSerializationContext context, NormalizedNodeMessages.PathArgument pathArgument){
            this.context = context;
            this.pathArgument = pathArgument;
        }


        public YangInstanceIdentifier.PathArgument deSerialize(){
            return parsePathArgument(pathArgument);
        }

        private String toString(NormalizedNodeMessages.QName qName){
            Preconditions.checkArgument(!"".equals(qName.getLocalName()),
                "qName.localName cannot be empty qName = " + qName.toString());
            Preconditions.checkArgument(qName.getNamespace() != -1, "qName.namespace should be valid");

                StringBuilder sb = new StringBuilder();
            String namespace = context.getNamespace(qName.getNamespace());
            String revision = "";
            if(qName.getRevision() != -1){
                    revision = context.getRevision(qName.getRevision());
                }


                    sb.append("(").append(namespace).append("?revision=").append(
                            revision).append(")").append(
                            qName.getLocalName());
            return sb.toString();

        }

        /**
         * Parse a protocol buffer PathArgument and return an MD-SAL PathArgument
         *
         * @param pathArgument protocol buffer PathArgument
         * @return MD-SAL PathArgument
         */
        private YangInstanceIdentifier.PathArgument parsePathArgument(NormalizedNodeMessages.PathArgument pathArgument) {


            if (pathArgument.getType().equals(NODE_IDENTIFIER_WITH_VALUE)) {

                YangInstanceIdentifier.NodeWithValue nodeWithValue =
                    new YangInstanceIdentifier.NodeWithValue(
                        QNameFactory
                            .create(toString(pathArgument.getNodeType())),
                        parseAttribute(pathArgument.getAttribute(0)));

                return nodeWithValue;

            } else if(pathArgument.getType().equals(NODE_IDENTIFIER_WITH_PREDICATES)){

                YangInstanceIdentifier.NodeIdentifierWithPredicates
                    nodeIdentifierWithPredicates =
                    new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                        QNameFactory.create(toString(pathArgument.getNodeType())), toAttributesMap(pathArgument.getAttributeList()));

                return nodeIdentifierWithPredicates;

            } else if(pathArgument.getType().equals(AUGMENTATION_IDENTIFIER)){

                Set<QName> qNameSet = new HashSet<>();

                for(NormalizedNodeMessages.PathArgumentAttribute attribute : pathArgument.getAttributeList()){
                    qNameSet.add(QNameFactory.create(toString(attribute.getName())));
                }

                return new YangInstanceIdentifier.AugmentationIdentifier(qNameSet);
            }

            return NodeIdentifierFactory.getArgument(toString(pathArgument.getNodeType()));
        }

        private Map<QName, Object> toAttributesMap(
            List<NormalizedNodeMessages.PathArgumentAttribute> attributesList) {

            Map<QName, Object> map = new HashMap<>();

            for(NormalizedNodeMessages.PathArgumentAttribute attribute : attributesList){
                NormalizedNodeMessages.QName name = attribute.getName();
                Object value = parseAttribute(attribute);

                map.put(QNameFactory.create(toString(name)), value);
            }

            return map;
        }

        private Object parseAttribute(NormalizedNodeMessages.PathArgumentAttribute attribute){
            return ValueSerializer.deSerialize(context, attribute);
        }

    }
}
