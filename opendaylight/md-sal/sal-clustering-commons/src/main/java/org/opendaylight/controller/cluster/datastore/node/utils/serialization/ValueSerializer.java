/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;

public class ValueSerializer {
    public static void serialize(NormalizedNodeMessages.Node.Builder builder, NormalizedNodeSerializationContext context, Object value){
        builder.setIntValueType(ValueType.getSerializableType(value));

        if(value instanceof YangInstanceIdentifier){
            builder.setInstanceIdentifierValue(InstanceIdentifierUtils.toSerializable((YangInstanceIdentifier) value));
        } else {
            builder.setValue(value.toString());
        }
    }

    public static void serialize(NormalizedNodeMessages.PathArgumentAttribute.Builder builder, NormalizedNodeSerializationContext context, Object value){
        builder.setType(ValueType.getSerializableType(value));
        builder.setValue(value.toString());
    }

    public static Object deSerialize(
        NormalizedNodeDeSerializationContext context, NormalizedNodeMessages.Node node) {
        if(node.getIntValueType() == ValueType.YANG_IDENTIFIER_TYPE){
            return InstanceIdentifierUtils.fromSerializable(
                node.getInstanceIdentifierValue());
        }
        return deSerializeBasicTypes(node.getIntValueType(), node.getValue());
    }

    private static Object deSerializeBasicTypes(int valueType, String value) {
        if(valueType == ValueType.STRING_TYPE){
            return value;
        } else if(valueType == ValueType.SHORT_TYPE){
            return Short.valueOf(value);
        } else if(valueType == ValueType.BOOL_TYPE){
            return Boolean.valueOf(value);
        } else if(valueType == ValueType.BYTE_TYPE){
            return Byte.valueOf(value);
        } else if(valueType == ValueType.BITS_TYPE){
            if(value.contains("[]")){
                value = "";
            } else {
                value = value.replace("[", "");
                value = value.replace("]", "");
                value = value.replace(",", " ");
            }

            value = value.trim();
            String[] split = value.split(" ");

            HashSet<String> strings = new HashSet<>();

            for(String s : split){
                if(s.trim().length() > 0) {
                    strings.add(s.trim());
                }
            }

            return strings;
        } else if(valueType == ValueType.INT_TYPE){
            return Integer.valueOf(value);
        } else if(valueType == ValueType.LONG_TYPE){
            return Long.valueOf(value);
        } else if(valueType == ValueType.QNAME_TYPE){
            return QNameFactory.create(value);
        } else if(valueType == ValueType.BIG_INTEGER_TYPE){
            return new BigInteger(value);
        } else if(valueType == ValueType.BIG_DECIMAL_TYPE){
            return new BigDecimal(value);
        }

        throw new IllegalArgumentException("Unknown valueType = " + valueType);
    }

    public static Object deSerialize(
        NormalizedNodeDeSerializationContext context,
        NormalizedNodeMessages.PathArgumentAttribute attribute) {
        return deSerializeBasicTypes(attribute.getType(), attribute.getValue());
    }
}
