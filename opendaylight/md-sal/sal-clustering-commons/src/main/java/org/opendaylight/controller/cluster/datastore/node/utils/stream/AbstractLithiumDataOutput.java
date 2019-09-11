/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * "original" type mapping. Baseline is Lithium but it really was introduced in Oxygen, where {@code type empty} was
 * remapped from null.
 *
 * <p>
 * {@code uint8}, {@code uint16}, {@code uint32} use java.lang types with widening, hence their value types overlap with
 * mapping of {@code int16}, {@code int32} and {@code int64}, making that difference indiscernible without YANG schema
 * knowledge.
 */
abstract class AbstractLithiumDataOutput extends AbstractNormalizedNodeDataOutput {
    private static final ImmutableMap<Class<?>, Byte> KNOWN_TYPES = ImmutableMap.<Class<?>, Byte>builder()
            .put(String.class, ValueTypes.STRING_TYPE)
            .put(Byte.class, ValueTypes.BYTE_TYPE)
            .put(Integer.class, ValueTypes.INT_TYPE)
            .put(Long.class, ValueTypes.LONG_TYPE)
            .put(Boolean.class, ValueTypes.BOOL_TYPE)
            .put(QName.class, ValueTypes.QNAME_TYPE)
            .put(Short.class, ValueTypes.SHORT_TYPE)
            .put(BigInteger.class, ValueTypes.BIG_INTEGER_TYPE)
            .put(BigDecimal.class, ValueTypes.BIG_DECIMAL_TYPE)
            .put(byte[].class, ValueTypes.BINARY_TYPE)
            .put(Empty.class, ValueTypes.EMPTY_TYPE)
            .build();


    private final Map<String, Integer> stringCodeMap = new HashMap<>();

    AbstractLithiumDataOutput(final DataOutput output) {
        super(output);
    }

    @Override
    final void writeObject(final DataOutput rawOuput, final Object value) throws IOException {
        byte type = getSerializableType(value);
        // Write object type first
        rawOuput.writeByte(type);

        switch (type) {
            case ValueTypes.BOOL_TYPE:
                rawOuput.writeBoolean((Boolean) value);
                break;
            case ValueTypes.QNAME_TYPE:
                writeQNameInternal((QName) value);
                break;
            case ValueTypes.INT_TYPE:
                rawOuput.writeInt((Integer) value);
                break;
            case ValueTypes.BYTE_TYPE:
                rawOuput.writeByte((Byte) value);
                break;
            case ValueTypes.LONG_TYPE:
                rawOuput.writeLong((Long) value);
                break;
            case ValueTypes.SHORT_TYPE:
                rawOuput.writeShort((Short) value);
                break;
            case ValueTypes.BITS_TYPE:
                writeObjSet((Set<?>) value);
                break;
            case ValueTypes.BINARY_TYPE:
                byte[] bytes = (byte[]) value;
                rawOuput.writeInt(bytes.length);
                rawOuput.write(bytes);
                break;
            case ValueTypes.YANG_IDENTIFIER_TYPE:
                writeYangInstanceIdentifierInternal((YangInstanceIdentifier) value);
                break;
            case ValueTypes.EMPTY_TYPE:
                break;
            case ValueTypes.STRING_BYTES_TYPE:
                final byte[] valueBytes = value.toString().getBytes(StandardCharsets.UTF_8);
                rawOuput.writeInt(valueBytes.length);
                rawOuput.write(valueBytes);
                break;
            default:
                rawOuput.writeUTF(value.toString());
                break;
        }
    }

    final void defaultWriteQName(final QName qname) throws IOException {
        writeString(qname.getLocalName());
        writeModule(qname.getModule());
    }

    final void defaultWriteModule(final QNameModule module) throws IOException {
        writeString(module.getNamespace().toString());
        final Optional<Revision> revision = module.getRevision();
        if (revision.isPresent()) {
            writeString(revision.get().toString());
        } else {
            writeByte(TokenTypes.IS_NULL_VALUE);
        }
    }

    @Override
    protected final void writeString(final @NonNull String string) throws IOException {
        final Integer value = stringCodeMap.get(verifyNotNull(string));
        if (value == null) {
            stringCodeMap.put(string, stringCodeMap.size());
            writeByte(TokenTypes.IS_STRING_VALUE);
            writeUTF(string);
        } else {
            writeByte(TokenTypes.IS_CODE_VALUE);
            writeInt(value);
        }
    }

    abstract void writeModule(QNameModule module) throws IOException;

    @VisibleForTesting
    static final byte getSerializableType(final Object node) {
        final Byte type = KNOWN_TYPES.get(requireNonNull(node).getClass());
        if (type != null) {
            if (type == ValueTypes.STRING_TYPE
                    && ((String) node).length() >= ValueTypes.STRING_BYTES_LENGTH_THRESHOLD) {
                return ValueTypes.STRING_BYTES_TYPE;
            }
            return type;
        }

        if (node instanceof Set) {
            return ValueTypes.BITS_TYPE;
        }

        if (node instanceof YangInstanceIdentifier) {
            return ValueTypes.YANG_IDENTIFIER_TYPE;
        }

        throw new IllegalArgumentException("Unknown value type " + node.getClass().getSimpleName());
    }
}
