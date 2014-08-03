/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore.node;

import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.codec.BitsCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.util.IdentityrefType;
import org.opendaylight.yangtools.yang.model.util.InstanceIdentifierType;
import org.opendaylight.yangtools.yang.model.util.Leafref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeValueCodec {
    protected static final Logger logger = LoggerFactory
        .getLogger(NodeValueCodec.class);

    public static Object toTypeSafeValue(DataSchemaNode schema, TypeDefinition type, NormalizedNodeMessages.Node node){

        String value = node.getValue();

        if(schema != null && value != null){
            TypeDefinition<?> baseType = type;

            while (baseType.getBaseType() != null) {
                baseType = baseType.getBaseType();
            }

            TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> codec =
                TypeDefinitionAwareCodec.from(type);

            if(codec instanceof BitsCodec){
                if(value.contains("[]")){
                    value = "";
                } else {
                    value = value.replace("[", "");
                    value = value.replace("]", "");
                    value = value.replace(",", " ");
                }
            }

            if (codec != null) {
                return codec.deserialize(value);
            } else if(baseType instanceof Leafref) {
                return value;
            } else if(baseType instanceof IdentityrefType) {
                return QNameFactory.create(value);
            } else if(baseType instanceof InstanceIdentifierType) {
                return InstanceIdentifierUtils.fromSerializable(node.getInstanceIdentifierValue());
            } else {
                logger.error("Could not figure out how to transform value " + value +  " for schemaType " + type);
            }
        }

        return value;
    }
}
