/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import com.google.protobuf.ByteString;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ValueSerializer {
    private static final String NULL_VALUE = "";

    public static void serialize(NormalizedNodeMessages.Node.Builder builder,
            QNameSerializationContext context, Object value) {
        builder.setIntValueType(ValueType.getSerializableType(value).ordinal());

        if(value instanceof YangInstanceIdentifier) {
            builder.setInstanceIdentifierValue(
                InstanceIdentifierUtils.toSerializable((YangInstanceIdentifier) value, context));
        } else if(value instanceof Set) {
            Set<?> set = (Set<?>) value;
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
        } else if(value instanceof byte[]) {
            builder.setBytesValue(ByteString.copyFrom((byte[]) value));
        } else if(value == null){
            builder.setValue(NULL_VALUE);
        } else {
            builder.setValue(value.toString());
        }
    }

    public static void serialize(NormalizedNodeMessages.PathArgumentAttribute.Builder builder,
            QNameSerializationContext context, Object value){

        builder.setType(ValueType.getSerializableType(value).ordinal());

        if(value instanceof YangInstanceIdentifier) {
            builder.setInstanceIdentifierValue(
                    InstanceIdentifierUtils.toSerializable((YangInstanceIdentifier) value, context));
        } else if(value instanceof Set) {
            Set<?> set = (Set<?>) value;
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
        } else if(value == null){
            builder.setValue(NULL_VALUE);
        } else {
            builder.setValue(value.toString());
        }
    }

    public static Object deSerialize(QNameDeSerializationContext context,
            NormalizedNodeMessages.Node node) {
        if(node.getIntValueType() == ValueType.YANG_IDENTIFIER_TYPE.ordinal()){
            return InstanceIdentifierUtils.fromSerializable(
                    node.getInstanceIdentifierValue(), context);
        } else if(node.getIntValueType() == ValueType.BITS_TYPE.ordinal()){
            return new HashSet<>(node.getBitsValueList());
        } else if(node.getIntValueType() == ValueType.BINARY_TYPE.ordinal()){
            return node.getBytesValue().toByteArray();
        }
        return deSerializeBasicTypes(node.getIntValueType(), node.getValue());
    }

    public static Object deSerialize(QNameDeSerializationContext context,
            NormalizedNodeMessages.PathArgumentAttribute attribute) {

        if(attribute.getType() == ValueType.YANG_IDENTIFIER_TYPE.ordinal()){
            return InstanceIdentifierUtils.fromSerializable(
                    attribute.getInstanceIdentifierValue(), context);
        } else if(attribute.getType() == ValueType.BITS_TYPE.ordinal()){
            return new HashSet<>(attribute.getBitsValueList());
        } else if(attribute.getType() == ValueType.BINARY_TYPE.ordinal()){
            return attribute.getBytesValue().toByteArray();
        }
        return deSerializeBasicTypes(attribute.getType(), attribute.getValue());
    }


    private static Object deSerializeBasicTypes(int valueType, String value) {
        return ValueType.values()[valueType].deserialize(value);
    }

}
