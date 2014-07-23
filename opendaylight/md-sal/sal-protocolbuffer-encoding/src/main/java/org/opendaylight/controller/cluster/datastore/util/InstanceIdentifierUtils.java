package org.opendaylight.controller.cluster.datastore.util;

import org.opendaylight.controller.cluster.datastore.node.utils.NodeIdentifierFactory;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
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
 * InstanceIdentifier to and from other representations.
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

    @Deprecated
    public static InstanceIdentifier from(String path) {
        String[] ids = path.split("/");

        List<InstanceIdentifier.PathArgument> pathArguments =
            new ArrayList<>();
        for (String nodeId : ids) {
            if (!"".equals(nodeId)) {
                pathArguments
                    .add(NodeIdentifierFactory.getArgument(nodeId));
            }
        }
        final InstanceIdentifier instanceIdentifier =
            InstanceIdentifier.create(pathArguments);
        return instanceIdentifier;
    }


    /**
     * Convert an MD-SAL InstanceIdentifier into a protocol buffer version of it
     *
     * @param path an MD-SAL InstanceIdentifier
     * @return a protocol buffer version of the MD-SAL InstanceIdentifier
     */
    public static NormalizedNodeMessages.InstanceIdentifier toSerializable(InstanceIdentifier path){
        NormalizedNodeMessages.InstanceIdentifier.Builder builder =
            NormalizedNodeMessages.InstanceIdentifier.newBuilder();

        try {

            for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument pathArgument : path
                .getPathArguments()) {

                String nodeType = "";
                if(!(pathArgument instanceof InstanceIdentifier.AugmentationIdentifier)){
                    nodeType = pathArgument.getNodeType().toString();
                }

                NormalizedNodeMessages.PathArgument serializablePathArgument =
                    NormalizedNodeMessages.PathArgument.newBuilder()
                        .setValue(pathArgument.toString())
                        .setType(pathArgument.getClass().getSimpleName())
                        .setNodeType(NormalizedNodeMessages.QName.newBuilder()
                            .setValue(nodeType))
                        .addAllAttributes(getPathArgumentAttributes(
                            pathArgument))
                        .build();

                builder.addArguments(serializablePathArgument);
            }

        } catch(Exception e){
            logger.error("An exception occurred", e);
        }
        return builder.build();
    }


    /**
     * Convert a protocol buffer version of the MD-SAL InstanceIdentifier into
     * the MD-SAL version of the InstanceIdentifier
     *
     * @param path a protocol buffer version of the MD-SAL InstanceIdentifier
     * @return  an MD-SAL InstanceIdentifier
     */
    public static InstanceIdentifier fromSerializable(NormalizedNodeMessages.InstanceIdentifier path){

        List<InstanceIdentifier.PathArgument> pathArguments =
            new ArrayList<>();

        for(NormalizedNodeMessages.PathArgument pathArgument : path.getArgumentsList()){

            pathArguments
                .add(parsePathArgument(pathArgument));

        }

        final InstanceIdentifier instanceIdentifier = InstanceIdentifier.create(pathArguments);

        return instanceIdentifier;
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
        InstanceIdentifier.PathArgument pathArgument) {
        List<NormalizedNodeMessages.Attribute> attributes = new ArrayList<>();



        if (pathArgument instanceof InstanceIdentifier.NodeWithValue) {
            InstanceIdentifier.NodeWithValue identifier
                = (InstanceIdentifier.NodeWithValue) pathArgument;

            NormalizedNodeMessages.Attribute attribute =
                NormalizedNodeMessages.Attribute.newBuilder()
                    .setName("name")
                    .setValue(identifier.getValue().toString())
                    .setType(identifier.getValue().getClass().getSimpleName())
                    .build();

            attributes.add(attribute);
        } else if (pathArgument instanceof InstanceIdentifier.NodeIdentifierWithPredicates) {
            InstanceIdentifier.NodeIdentifierWithPredicates identifier
                = (InstanceIdentifier.NodeIdentifierWithPredicates) pathArgument;

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

        } else if(pathArgument instanceof InstanceIdentifier.AugmentationIdentifier) {
            InstanceIdentifier.AugmentationIdentifier identifier
                = (InstanceIdentifier.AugmentationIdentifier) pathArgument;

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
    private static InstanceIdentifier.PathArgument parsePathArgument(NormalizedNodeMessages.PathArgument pathArgument) {
        if (InstanceIdentifier.NodeWithValue.class.getSimpleName().equals(pathArgument.getType())) {

            InstanceIdentifier.NodeWithValue nodeWithValue =
                new InstanceIdentifier.NodeWithValue(
                    QName.create(pathArgument.getNodeType().getValue()),
                    parseAttribute(pathArgument.getAttributes(0)));

            return nodeWithValue;

        } else if(InstanceIdentifier.NodeIdentifierWithPredicates.class.getSimpleName().equals(pathArgument.getType())){

            InstanceIdentifier.NodeIdentifierWithPredicates
                nodeIdentifierWithPredicates =
                new InstanceIdentifier.NodeIdentifierWithPredicates(
                    QName.create(pathArgument.getNodeType().getValue()), toAttributesMap(pathArgument.getAttributesList()));

            return nodeIdentifierWithPredicates;

        } else if(InstanceIdentifier.AugmentationIdentifier.class.getSimpleName().equals(pathArgument.getType())){

            Set<QName> qNameSet = new HashSet<>();

            for(NormalizedNodeMessages.Attribute attribute : pathArgument.getAttributesList()){
                qNameSet.add(QName.create(attribute.getValue()));
            }

            return new InstanceIdentifier.AugmentationIdentifier(qNameSet);
        }

        return NodeIdentifierFactory.getArgument(pathArgument.getValue());
    }

    private static Map<QName, Object> toAttributesMap(
        List<NormalizedNodeMessages.Attribute> attributesList) {

        Map<QName, Object> map = new HashMap<>();

        for(NormalizedNodeMessages.Attribute attribute : attributesList){
            String name = attribute.getName();
            Object value = parseAttribute(attribute);

            map.put(QName.create(name), value);
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
