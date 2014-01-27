/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import java.net.URI;
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
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.codec.IdentityrefCodec;
import org.opendaylight.yangtools.yang.data.api.codec.InstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.api.codec.LeafrefCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestCodec {
    
    private static final Logger logger = LoggerFactory.getLogger(RestCodec.class);

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
                    logger.info(
                            "Value is not instance of IdentityrefTypeDefinition but is {}. Therefore NULL is used as translation of  - {}",
                            input == null ? "null" : input.getClass(), String.valueOf(input));
                    return null;
                } else if (type instanceof LeafrefTypeDefinition) {
                    return LEAFREF_DEFAULT_CODEC.deserialize(input);
                } else if (type instanceof InstanceIdentifierTypeDefinition) {
                    if (input instanceof IdentityValuesDTO) {
                        return instanceIdentifier.deserialize(input);
                    }
                    logger.info(
                            "Value is not instance of InstanceIdentifierTypeDefinition but is {}. Therefore NULL is used as translation of  - {}",
                            input == null ? "null" : input.getClass(), String.valueOf(input));
                    return null;
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
                return null;
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
            Module module = getModuleByNamespace(valueWithNamespace.getNamespace(), mountPoint);
            if (module == null) {
                logger.info("Module was not found for namespace {}", valueWithNamespace.getNamespace());
                logger.info("Idenetityref will be translated as NULL for data - {}", String.valueOf(valueWithNamespace));
                return null;
            }
            
            return QName.create(module.getNamespace(), module.getRevision(), valueWithNamespace.getValue());
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
                } else if (pathArgument instanceof NodeWithValue && identityValue != null) {
                    List<Predicate> predicates = new ArrayList<>();
                    String value = String.valueOf(((NodeWithValue) pathArgument).getValue());
                    predicates.add(new Predicate(null, value));
                    identityValue.setPredicates(predicates);
                }
                identityValuesDTO.add(identityValue);
            }
            return identityValuesDTO;
        }

        @Override
        public InstanceIdentifier deserialize(IdentityValuesDTO data) {
            List<PathArgument> result = new ArrayList<PathArgument>();
            IdentityValue valueWithNamespace = data.getValuesWithNamespaces().get(0);
            Module module = getModuleByNamespace(valueWithNamespace.getNamespace(), mountPoint);
            if (module == null) {
                logger.info("Module by namespace '{}' of first node in instance-identiefier was not found.", valueWithNamespace.getNamespace());
                logger.info("Instance-identifier will be translated as NULL for data - {}", String.valueOf(valueWithNamespace.getValue()));
                return null;
            }

            DataNodeContainer parentContainer = module;
            List<IdentityValue> identities = data.getValuesWithNamespaces();
            for (int i = 0; i < identities.size(); i++) {
                IdentityValue identityValue = identities.get(i);
                URI validNamespace = resolveValidNamespace(identityValue.getNamespace(), mountPoint);
                DataSchemaNode node = ControllerContext.getInstance().findInstanceDataChildByNameAndNamespace(
                        parentContainer, identityValue.getValue(), validNamespace);
                if (node == null) {
                    logger.info("'{}' node was not found in {}", identityValue, parentContainer.getChildNodes());
                    logger.info("Instance-identifier will be translated as NULL for data - {}", String.valueOf(identityValue.getValue()));
                    return null;
                }
                QName qName = node.getQName();
                PathArgument pathArgument = null;
                if (identityValue.getPredicates().isEmpty()) {
                    pathArgument = new NodeIdentifier(qName);
                } else {
                    if (node instanceof LeafListSchemaNode) { // predicate is value of leaf-list entry
                        Predicate leafListPredicate = identityValue.getPredicates().get(0);
                        if (!leafListPredicate.isLeafList()) {
                            logger.info("Predicate's data is not type of leaf-list. It should be in format \".='value'\"");
                            logger.info("Instance-identifier will be translated as NULL for data - {}", String.valueOf(identityValue.getValue()));
                            return null;
                        }
                        pathArgument = new NodeWithValue(qName, leafListPredicate.getValue());
                    } else if (node instanceof ListSchemaNode) { // predicates are keys of list
                        DataNodeContainer listNode = (DataNodeContainer) node;
                        Map<QName, Object> predicatesMap = new HashMap<>();
                        for (Predicate predicate : identityValue.getPredicates()) {
                            validNamespace = resolveValidNamespace(predicate.getName().getNamespace(), mountPoint);
                            DataSchemaNode listKey = ControllerContext.getInstance().findInstanceDataChildByNameAndNamespace(
                                    listNode, predicate.getName().getValue(), validNamespace);
                            predicatesMap.put(listKey.getQName(), predicate.getValue());
                        }
                        pathArgument = new NodeIdentifierWithPredicates(qName, predicatesMap);
                    } else {
                        logger.info("Node {} is not List or Leaf-list.", node);
                        logger.info("Instance-identifier will be translated as NULL for data - {}", String.valueOf(identityValue.getValue()));
                        return null;
                    }
                }
                result.add(pathArgument);
                if (i < identities.size() - 1) { // last element in instance-identifier can be other than DataNodeContainer
                    if (node instanceof DataNodeContainer) {
                        parentContainer = (DataNodeContainer) node;
                    } else {
                        logger.info("Node {} isn't instance of DataNodeContainer", node);
                        logger.info("Instance-identifier will be translated as NULL for data - {}", String.valueOf(identityValue.getValue()));
                        return null;
                    }
                }
            }
            
            return result.isEmpty() ? null : new InstanceIdentifier(result);
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
    }
    
    private static Module getModuleByNamespace(String namespace, MountInstance mountPoint) {
        URI validNamespace = resolveValidNamespace(namespace, mountPoint);

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
    
    private static URI resolveValidNamespace(String namespace, MountInstance mountPoint) {
        URI validNamespace;
        if (mountPoint != null) {
            validNamespace = ControllerContext.getInstance().findNamespaceByModuleName(mountPoint, namespace);
        } else {
            validNamespace = ControllerContext.getInstance().findNamespaceByModuleName(namespace);
        }
        if (validNamespace == null) {
            validNamespace = URI.create(namespace);
        }

        return validNamespace;
    }

}
