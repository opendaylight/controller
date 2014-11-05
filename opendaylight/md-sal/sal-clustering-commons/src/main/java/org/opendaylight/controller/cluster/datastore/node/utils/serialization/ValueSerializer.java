/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import com.google.protobuf.ByteString;
import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

public class ValueSerializer {
    public static void serialize(NormalizedNodeMessages.Node.Builder builder,
            QNameSerializationContext context, Object value) {
        builder.setIntValueType(ValueType.getSerializableType(value).ordinal());

        if(value instanceof YangInstanceIdentifier) {
            builder.setInstanceIdentifierValue(
                InstanceIdentifierUtils.toSerializable((YangInstanceIdentifier) value, context));
        } else if(value instanceof Set) {
            Set set = (Set) value;
            if (!set.isEmpty()) {
                for (Object o : set) {
                    if (o instanceof String) {
                        builder.addBitsValue(o.toString());
                    } else {
                        throw new IllegalArgumentException("Expected value type to be Bits but was : " +
                                value.toString());
                    }
                }
            }
        } else if(value instanceof byte[]){
            builder.setBytesValue(ByteString.copyFrom((byte[]) value));
        } else {
            builder.setValue(value.toString());
        }
    }

    public static void serialize(NormalizedNodeMessages.PathArgumentAttribute.Builder builder,
            QNameSerializationContext context, Object value){

        builder.setType(ValueType.getSerializableType(value).ordinal());
        builder.setValue(value.toString());
    }

    public static Object deSerialize(QNameDeSerializationContext context,
            NormalizedNodeMessages.Node node) {
        if(node.getIntValueType() == ValueType.YANG_IDENTIFIER_TYPE.ordinal()){
            return InstanceIdentifierUtils.fromSerializable(
                    node.getInstanceIdentifierValue(), context);
        } else if(node.getIntValueType() == ValueType.BITS_TYPE.ordinal()){
            return new HashSet(node.getBitsValueList());
        } else if(node.getIntValueType() == ValueType.BINARY_TYPE.ordinal()){
            return node.getBytesValue().toByteArray();
        }
        return deSerializeBasicTypes(node.getIntValueType(), node.getValue());
    }

    public static Object deSerialize(QNameDeSerializationContext context,
            NormalizedNodeMessages.PathArgumentAttribute attribute) {
        return deSerializeBasicTypes(attribute.getType(), attribute.getValue());
    }


    private static Object deSerializeBasicTypes(int valueType, String value) {
        switch(ValueType.values()[valueType]){
           case SHORT_TYPE: {
               return Short.valueOf(value);
           }
           case BOOL_TYPE: {
               return Boolean.valueOf(value);
           }
           case BYTE_TYPE: {
               return Byte.valueOf(value);
           }
           case INT_TYPE : {
                return Integer.valueOf(value);
           }
           case LONG_TYPE: {
               return Long.valueOf(value);
           }
           case QNAME_TYPE: {
               return QNameFactory.create(value);
           }
           case BIG_INTEGER_TYPE: {
               return new BigInteger(value);
           }
           case BIG_DECIMAL_TYPE: {
               return new BigDecimal(value);
           }
           default: {
               return value;
           }
        }
    }

}
