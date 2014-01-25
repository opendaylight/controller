package org.opendaylight.controller.sal.restconf.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.rest.impl.RestUtil;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.IdentityValue;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.Predicate;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.codec.IdentityrefCodec;
import org.opendaylight.yangtools.yang.data.api.codec.InstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.api.codec.LeafrefCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestCodec {

    private RestCodec() {
    }

    public static final Codec<Object, Object> from(TypeDefinition<?> typeDefinition, MountInstance mountPoint) {
        return new ObjectCodec(typeDefinition, mountPoint);
    }

    @SuppressWarnings("rawtypes")
    public static final class ObjectCodec implements Codec<Object, Object> {

        private final Logger logger = LoggerFactory.getLogger(RestCodec.class);

        public static final Codec LEAFREF_DEFAULT_CODEC = new LeafrefCodecImpl();
        private final Codec instanceIdentifier;
        private final Codec identityrefCodec;

        private final TypeDefinition<?> type;

        private ObjectCodec(TypeDefinition<?> typeDefinition, MountInstance mountPoint) {
            type = RestUtil.resolveBaseTypeFrom(typeDefinition);
            if (type instanceof IdentityrefTypeDefinition) {
                identityrefCodec = new IdentityrefCodecImpl(mountPoint);
            } else {
                identityrefCodec = null;
            }
            if (type instanceof InstanceIdentifierTypeDefinition) {
                instanceIdentifier = new InstanceIdentifierCodecImpl(mountPoint);
            } else {
                instanceIdentifier = null;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object deserialize(Object input) {
            try {
                if (type instanceof IdentityrefTypeDefinition) {
                    if (input instanceof IdentityValuesDTO) {
                        return identityrefCodec.deserialize(input);
                    }
                    String stringValue = String.valueOf(input);
                    logger.info(
                            "Value is not instance of InstanceIdentifierTypeDefinition but is {}. Therefore string representation of value or NULL is used - {}",
                            input == null ? "null" : input.getClass(), stringValue);
                    return input == null ? null : stringValue;
                } else if (type instanceof LeafrefTypeDefinition) {
                    return LEAFREF_DEFAULT_CODEC.deserialize(input);
                } else if (type instanceof InstanceIdentifierTypeDefinition) {
                    if (input instanceof IdentityValuesDTO) {
                        return instanceIdentifier.deserialize(input);
                    }
                    String stringValue = String.valueOf(input);
                    logger.info(
                            "Value is not instance of InstanceIdentifierTypeDefinition but is {}. Therefore string representation of value or NULL is used  - {}",
                            input == null ? "null" : input.getClass(), stringValue);
                    return input == null ? null : stringValue;
                } else {
                    TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> typeAwarecodec = TypeDefinitionAwareCodec
                            .from(type);
                    if (typeAwarecodec != null) {
                        return typeAwarecodec.deserialize(String.valueOf(input));
                    } else {
                        logger.debug("Codec for type \"" + type.getQName().getLocalName()
                                + "\" is not implemented yet.");
                        return null;
                    }
                }
            } catch (ClassCastException e) { // TODO remove this catch when
                                             // everyone use codecs
                logger.error(
                        "ClassCastException was thrown when codec is invoked with parameter " + String.valueOf(input),
                        e);
                return input;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object serialize(Object input) {
            try {
                if (type instanceof IdentityrefTypeDefinition) {
                    return identityrefCodec.serialize(input);
                } else if (type instanceof LeafrefTypeDefinition) {
                    return LEAFREF_DEFAULT_CODEC.serialize(input);
                } else if (type instanceof InstanceIdentifierTypeDefinition) {
                    return instanceIdentifier.serialize(input);
                } else {
                    TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> typeAwarecodec = TypeDefinitionAwareCodec
                            .from(type);
                    if (typeAwarecodec != null) {
                        return typeAwarecodec.serialize(input);
                    } else {
                        logger.debug("Codec for type \"" + type.getQName().getLocalName()
                                + "\" is not implemented yet.");
                        return null;
                    }
                }
            } catch (ClassCastException e) { // TODO remove this catch when
                                             // everyone use codecs
                logger.error(
                        "ClassCastException was thrown when codec is invoked with parameter " + String.valueOf(input),
                        e);
                return input;
            }
        }

    }

    public static class IdentityrefCodecImpl implements IdentityrefCodec<IdentityValuesDTO> {

        private final Logger logger = LoggerFactory.getLogger(IdentityrefCodecImpl.class);

        private final MountInstance mountPoint;

        public IdentityrefCodecImpl(MountInstance mountPoint) {
            this.mountPoint = mountPoint;
        }

        @Override
        public IdentityValuesDTO serialize(QName data) {
            return new IdentityValuesDTO(data.getNamespace().toString(), data.getLocalName(), data.getPrefix());
        }

        @Override
        public QName deserialize(IdentityValuesDTO data) {
            IdentityValue valueWithNamespace = data.getValuesWithNamespaces().get(0);
            String namespace = valueWithNamespace.getNamespace();
            URI validNamespace;
            if (mountPoint != null) {
                validNamespace = ControllerContext.getInstance().findNamespaceByModuleName(mountPoint, namespace);
            } else {
                validNamespace = ControllerContext.getInstance().findNamespaceByModuleName(namespace);
            }
            if (validNamespace == null) {
                validNamespace = URI.create(namespace);
            }

            Module module = null;
            if (mountPoint != null) {
                module = ControllerContext.getInstance().findModuleByNamespace(mountPoint, validNamespace);
            } else {
                module = ControllerContext.getInstance().findModuleByNamespace(validNamespace);
            }
            if (module == null) {
                logger.info("Module for namespace " + validNamespace + " wasn't found.");
                return QName.create(validNamespace, null, valueWithNamespace.getValue());
            }
            
            return QName.create(validNamespace, module.getRevision(), valueWithNamespace.getValue());
        }

    }

    public static class LeafrefCodecImpl implements LeafrefCodec<String> {

        @Override
        public String serialize(Object data) {
            return String.valueOf(data);
        }

        @Override
        public Object deserialize(String data) {
            return data;
        }

    }

    public static class InstanceIdentifierCodecImpl implements InstanceIdentifierCodec<IdentityValuesDTO> {
        private final Logger logger = LoggerFactory.getLogger(InstanceIdentifierCodecImpl.class);
        private final MountInstance mountPoint;

        public InstanceIdentifierCodecImpl(MountInstance mountPoint) {
            this.mountPoint = mountPoint;
        }

        @Override
        public IdentityValuesDTO serialize(InstanceIdentifier data) {
            List<PathArgument> pathArguments = data.getPath();
            IdentityValuesDTO identityValuesDTO = new IdentityValuesDTO();
            for (PathArgument pathArgument : pathArguments) {
                IdentityValue identityValue = qNameToIdentityValue(pathArgument.getNodeType());
                if (pathArgument instanceof NodeIdentifierWithPredicates && identityValue != null) {
                    List<Predicate> predicates = keyValuesToPredicateList(((NodeIdentifierWithPredicates) pathArgument)
                            .getKeyValues());
                    identityValue.setPredicates(predicates);
                }
                identityValuesDTO.add(identityValue);
            }
            return identityValuesDTO;
        }

        @Override
        public InstanceIdentifier deserialize(IdentityValuesDTO data) {
            List<PathArgument> result = new ArrayList<PathArgument>();

            DataNodeContainer node = getModuleFromNamespaceOfFirstPathElement(data);
            if (node == null) {
                logger.info("Module for namespace of first element wasn't found");
                return new InstanceIdentifier(new ArrayList<PathArgument>());
            }

            DataNodeContainer parentContainer = null;
            for (IdentityValue identityValue : data.getValuesWithNamespaces()) {
                PathArgument pathArgument = null;
                if (node instanceof DataNodeContainer) {
                    parentContainer = (DataNodeContainer) node;
                } else {
                    logger.info("Node " + node.toString() + " isn't instance of DataNodeContainer");
                    return new InstanceIdentifier(new ArrayList<PathArgument>());
                }
                URI validNamespace = resolveValidNamespace(identityValue);
                node = ControllerContext.getInstance().findInstanceDataChildByNameAndNamespace(
                        parentContainer, identityValue.getValue(), validNamespace);
                QName qName = ((DataSchemaNode) node).getQName();
                if (identityValue.getPredicates().isEmpty()) {
                    pathArgument = new NodeIdentifier(qName);
                } else {
                    Map<QName, Object> predicatesMap = new HashMap<>();
                    if (node instanceof DataNodeContainer) {
                        parentContainer = (DataNodeContainer) node;
                    } else {
                        logger.info("Node " + node.toString() + " isn't instance of DataNodeContainer");
                        return new InstanceIdentifier(new ArrayList<PathArgument>());
                    }
                    for (Predicate predicate : identityValue.getPredicates()) {
                        validNamespace = resolveValidNamespace(predicate.getName());
                        DataSchemaNode listKey = ControllerContext.getInstance().findInstanceDataChildByNameAndNamespace(
                                parentContainer, predicate.getName().getValue(), validNamespace);
                        predicatesMap.put(listKey.getQName(), predicate.getValue());
                    }

                    pathArgument = new NodeIdentifierWithPredicates(qName, predicatesMap);

                }
                result.add(pathArgument);
            }
            return result.isEmpty() ? null : new InstanceIdentifier(result);

        }

        private DataNodeContainer getModuleFromNamespaceOfFirstPathElement(IdentityValuesDTO data) {
            IdentityValue valueWithNamespace = data.getValuesWithNamespaces().get(0);
            String namespace = valueWithNamespace.getNamespace();
            URI validNamespace;
            if (mountPoint != null) {
                validNamespace = ControllerContext.getInstance().findNamespaceByModuleName(mountPoint, namespace);
            } else {
                validNamespace = ControllerContext.getInstance().findNamespaceByModuleName(namespace);
            }
            if (validNamespace == null) {
                validNamespace = URI.create(namespace);
            }

            Module module = null;
            if (mountPoint != null) {
                module = ControllerContext.getInstance().findModuleByNamespace(mountPoint, validNamespace);
            } else {
                module = ControllerContext.getInstance().findModuleByNamespace(validNamespace);
            }
            if (module == null) {
                logger.info("Module for namespace " + validNamespace + " wasn't found.");
                return null;
            }
            return module;
        }

        private List<Predicate> keyValuesToPredicateList(Map<QName, Object> keyValues) {
            List<Predicate> result = new ArrayList<>();
            for (QName qName : keyValues.keySet()) {
                Object value = keyValues.get(qName);
                result.add(new Predicate(qNameToIdentityValue(qName), String.valueOf(value)));
            }
            return result;
        }

        private IdentityValue qNameToIdentityValue(QName qName) {
            if (qName != null) {
                return new IdentityValue(qName.getNamespace().toString(), qName.getLocalName(), qName.getPrefix());
            }
            return null;
        }

        private QName identityValueToQName(IdentityValue identityValue, DataNodeContainer container) {
            URI validNamespace = resolveValidNamespace(identityValue);
            DataSchemaNode foundInstanceDataChild = ControllerContext.getInstance().findInstanceDataChildByNameAndNamespace(
                    container, identityValue.getValue(), validNamespace);
            return foundInstanceDataChild.getQName();
        }

        private URI resolveValidNamespace(IdentityValue identityValue) {
            String namespace = identityValue.getNamespace();
            URI validNamespace = ControllerContext.getInstance().findNamespaceByModuleName(namespace);
            if (validNamespace == null) {
                validNamespace = URI.create(namespace);
            }

            return validNamespace;
        }

        // private Map<QName, Object> predicatesToMap(List<Predicate>
        // predicates) {
        // Map<QName, Object> result = new HashMap<>();
        // for (Predicate predicate : predicates) {
        // result.put(identityValueToQName(predicate.getName()),
        // predicate.getValue());
        // }
        // return result;
        // }

    }

}
