/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.DataOutput;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.rfc8528.data.api.MountPointIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for NormalizedNodeDataOutput based on {@link MagnesiumNode}, {@link MagnesiumPathArgument} and
 * {@link MagnesiumValue}.
 */
abstract class AbstractMagnesiumDataOutput extends AbstractNormalizedNodeDataOutput {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMagnesiumDataOutput.class);

    // Marker for encoding state when we have entered startLeafNode() within a startMapEntry() and that leaf corresponds
    // to a key carried within NodeIdentifierWithPredicates.
    private static final Object KEY_LEAF_STATE = new Object();
    // Marker for nodes which have simple content and do not use END_NODE marker to terminate
    private static final Object NO_ENDNODE_STATE = new Object();

    private static final TransformerFactory TF = TransformerFactory.newInstance();

    /**
     * Stack tracking encoding state. In general we track the node identifier of the currently-open element, but there
     * are a few other circumstances where we push other objects. See {@link #KEY_LEAF_STATE} and
     * {@link #NO_ENDNODE_STATE}.
     */
    private final Deque<Object> stack = new ArrayDeque<>();

    // Coding maps
    private final Map<AugmentationIdentifier, Integer> aidCodeMap = new HashMap<>();
    private final Map<QNameModule, Integer> moduleCodeMap = new HashMap<>();
    private final Map<String, Integer> stringCodeMap = new HashMap<>();
    private final Map<QName, Integer> qnameCodeMap = new HashMap<>();

    AbstractMagnesiumDataOutput(final DataOutput output) {
        super(output);
    }

    @Override
    public final void startLeafNode(final NodeIdentifier name) throws IOException {
        final Object current = stack.peek();
        if (current instanceof NodeIdentifierWithPredicates) {
            final QName qname = name.getNodeType();
            if (((NodeIdentifierWithPredicates) current).containsKey(qname)) {
                writeQNameNode(MagnesiumNode.NODE_LEAF | MagnesiumNode.PREDICATE_ONE, qname);
                stack.push(KEY_LEAF_STATE);
                return;
            }
        }

        startSimpleNode(MagnesiumNode.NODE_LEAF, name);
    }

    @Override
    public final void startLeafSet(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(MagnesiumNode.NODE_LEAFSET, name);
    }

    @Override
    public final void startOrderedLeafSet(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(MagnesiumNode.NODE_LEAFSET_ORDERED, name);
    }

    @Override
    public final void startLeafSetEntryNode(final NodeWithValue<?> name) throws IOException {
        if (matchesParentQName(name.getNodeType())) {
            output.writeByte(MagnesiumNode.NODE_LEAFSET_ENTRY);
            stack.push(NO_ENDNODE_STATE);
        } else {
            startSimpleNode(MagnesiumNode.NODE_LEAFSET_ENTRY, name);
        }
    }

    @Override
    public final void startContainerNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(MagnesiumNode.NODE_CONTAINER, name);
    }

    @Override
    public final void startUnkeyedList(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(MagnesiumNode.NODE_LIST, name);
    }

    @Override
    public final void startUnkeyedListItem(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startInheritedNode(MagnesiumNode.NODE_LIST_ENTRY, name);
    }

    @Override
    public final void startMapNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(MagnesiumNode.NODE_MAP, name);
    }

    @Override
    public final void startMapEntryNode(final NodeIdentifierWithPredicates identifier, final int childSizeHint)
            throws IOException {
        final int size = identifier.size();
        if (size == 1) {
            startInheritedNode((byte) (MagnesiumNode.NODE_MAP_ENTRY | MagnesiumNode.PREDICATE_ONE), identifier);
        } else if (size == 0) {
            startInheritedNode((byte) (MagnesiumNode.NODE_MAP_ENTRY | MagnesiumNode.PREDICATE_ZERO), identifier);
        } else if (size < 256) {
            startInheritedNode((byte) (MagnesiumNode.NODE_MAP_ENTRY | MagnesiumNode.PREDICATE_1B), identifier);
            output.writeByte(size);
        } else {
            startInheritedNode((byte) (MagnesiumNode.NODE_MAP_ENTRY | MagnesiumNode.PREDICATE_4B), identifier);
            output.writeInt(size);
        }

        writePredicates(identifier);
    }

    @Override
    public final void startOrderedMapNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(MagnesiumNode.NODE_MAP_ORDERED, name);
    }

    @Override
    public final void startChoiceNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(MagnesiumNode.NODE_CHOICE, name);
    }

    @Override
    public final void startAugmentationNode(final AugmentationIdentifier identifier) throws IOException {
        final Integer code = aidCodeMap.get(identifier);
        if (code == null) {
            aidCodeMap.put(identifier, aidCodeMap.size());
            output.writeByte(MagnesiumNode.NODE_AUGMENTATION | MagnesiumNode.ADDR_DEFINE);
            final Set<QName> qnames = identifier.getPossibleChildNames();
            output.writeInt(qnames.size());
            for (QName qname : qnames) {
                writeQNameInternal(qname);
            }
        } else {
            writeNodeType(MagnesiumNode.NODE_AUGMENTATION, code);
        }
        stack.push(identifier);
    }

    @Override
    public final void startAnyxmlNode(final NodeIdentifier name) throws IOException {
        startSimpleNode(MagnesiumNode.NODE_ANYXML, name);
    }

    @Override
    public final void domSourceValue(final DOMSource value) throws IOException {
        final StringWriter writer = new StringWriter();
        try {
            TF.newTransformer().transform(value, new StreamResult(writer));
        } catch (TransformerException e) {
            throw new IOException("Error writing anyXml", e);
        }
        writeValue(writer.toString());
    }

    @Override
    public final void startYangModeledAnyXmlNode(final NodeIdentifier name, final int childSizeHint)
            throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    @Override
    public final void endNode() throws IOException {
        if (stack.pop() instanceof PathArgument) {
            output.writeByte(MagnesiumNode.NODE_END);
        }
    }

    @Override
    public final void scalarValue(final Object value) throws IOException {
        if (KEY_LEAF_STATE.equals(stack.peek())) {
            LOG.trace("Inside a map entry key leaf, not emitting value {}", value);
        } else {
            writeObject(value);
        }
    }

    @Override
    final void writeQNameInternal(final QName qname) throws IOException {
        final Integer code = qnameCodeMap.get(qname);
        if (code == null) {
            output.writeByte(MagnesiumValue.QNAME);
            encodeQName(qname);
        } else {
            writeQNameRef(code);
        }
    }

    @Override
    final void writePathArgumentInternal(final PathArgument pathArgument) throws IOException {
        if (pathArgument instanceof NodeIdentifier) {
            writeNodeIdentifier((NodeIdentifier) pathArgument);
        } else if (pathArgument instanceof NodeIdentifierWithPredicates) {
            writeNodeIdentifierWithPredicates((NodeIdentifierWithPredicates) pathArgument);
        } else if (pathArgument instanceof AugmentationIdentifier) {
            writeAugmentationIdentifier((AugmentationIdentifier) pathArgument);
        } else if (pathArgument instanceof NodeWithValue) {
            writeNodeWithValue((NodeWithValue<?>) pathArgument);
        } else if (pathArgument instanceof MountPointIdentifier) {
            writeMountPointIdentifier((MountPointIdentifier) pathArgument);
        } else {
            throw new IOException("Unhandled PathArgument " + pathArgument);
        }
    }

    private void writeAugmentationIdentifier(final AugmentationIdentifier identifier) throws IOException {
        final Set<QName> qnames = identifier.getPossibleChildNames();
        final int size = qnames.size();
        if (size < 29) {
            output.writeByte(MagnesiumPathArgument.AUGMENTATION_IDENTIFIER
                | size << MagnesiumPathArgument.AID_COUNT_SHIFT);
        } else if (size < 256) {
            output.writeByte(MagnesiumPathArgument.AUGMENTATION_IDENTIFIER | MagnesiumPathArgument.AID_COUNT_1B);
            output.writeByte(size);
        } else if (size < 65536) {
            output.writeByte(MagnesiumPathArgument.AUGMENTATION_IDENTIFIER | MagnesiumPathArgument.AID_COUNT_2B);
            output.writeShort(size);
        } else {
            output.writeByte(MagnesiumPathArgument.AUGMENTATION_IDENTIFIER | MagnesiumPathArgument.AID_COUNT_4B);
            output.writeInt(size);
        }

        for (QName qname : qnames) {
            writeQNameInternal(qname);
        }
    }

    private void writeNodeIdentifier(final NodeIdentifier identifier) throws IOException {
        writePathArgumentQName(identifier.getNodeType(), MagnesiumPathArgument.NODE_IDENTIFIER);
    }

    private void writeMountPointIdentifier(final MountPointIdentifier identifier) throws IOException {
        writePathArgumentQName(identifier.getNodeType(), MagnesiumPathArgument.MOUNTPOINT_IDENTIFIER);
    }

    private void writeNodeIdentifierWithPredicates(final NodeIdentifierWithPredicates identifier) throws IOException {
        final int size = identifier.size();
        if (size < 5) {
            writePathArgumentQName(identifier.getNodeType(),
                (byte) (MagnesiumPathArgument.NODE_IDENTIFIER_WITH_PREDICATES
                        | size << MagnesiumPathArgument.SIZE_SHIFT));
        } else if (size < 256) {
            writePathArgumentQName(identifier.getNodeType(),
                (byte) (MagnesiumPathArgument.NODE_IDENTIFIER_WITH_PREDICATES | MagnesiumPathArgument.SIZE_1B));
            output.writeByte(size);
        } else if (size < 65536) {
            writePathArgumentQName(identifier.getNodeType(),
                (byte) (MagnesiumPathArgument.NODE_IDENTIFIER_WITH_PREDICATES | MagnesiumPathArgument.SIZE_2B));
            output.writeShort(size);
        } else {
            writePathArgumentQName(identifier.getNodeType(),
                (byte) (MagnesiumPathArgument.NODE_IDENTIFIER_WITH_PREDICATES | MagnesiumPathArgument.SIZE_4B));
            output.writeInt(size);
        }

        writePredicates(identifier);
    }

    private void writePredicates(final NodeIdentifierWithPredicates identifier) throws IOException {
        for (Entry<QName, Object> e : identifier.entrySet()) {
            writeQNameInternal(e.getKey());
            writeObject(e.getValue());
        }
    }

    private void writeNodeWithValue(final NodeWithValue<?> identifier) throws IOException {
        writePathArgumentQName(identifier.getNodeType(), MagnesiumPathArgument.NODE_WITH_VALUE);
        writeObject(identifier.getValue());
    }

    private void writePathArgumentQName(final QName qname, final byte typeHeader) throws IOException {
        final Integer code = qnameCodeMap.get(qname);
        if (code != null) {
            final int val = code;
            if (val < 256) {
                output.writeByte(typeHeader | MagnesiumPathArgument.QNAME_REF_1B);
                output.writeByte(val);
            } else if (val < 65792) {
                output.writeByte(typeHeader | MagnesiumPathArgument.QNAME_REF_2B);
                output.writeShort(val - 256);
            } else {
                output.writeByte(typeHeader | MagnesiumPathArgument.QNAME_REF_4B);
                output.writeInt(val);
            }
        } else {
            // implied '| MagnesiumPathArgument.QNAME_DEF'
            output.writeByte(typeHeader);
            encodeQName(qname);
        }
    }

    @Override
    final void writeYangInstanceIdentifierInternal(final YangInstanceIdentifier identifier) throws IOException {
        writeValue(identifier);
    }

    private void writeObject(final @NonNull Object value) throws IOException {
        if (value instanceof String) {
            writeValue((String) value);
        } else if (value instanceof Boolean) {
            writeValue((Boolean) value);
        } else if (value instanceof Byte) {
            writeValue((Byte) value);
        } else if (value instanceof Short) {
            writeValue((Short) value);
        } else if (value instanceof Integer) {
            writeValue((Integer) value);
        } else if (value instanceof Long) {
            writeValue((Long) value);
        } else if (value instanceof Uint8) {
            writeValue((Uint8) value);
        } else if (value instanceof Uint16) {
            writeValue((Uint16) value);
        } else if (value instanceof Uint32) {
            writeValue((Uint32) value);
        } else if (value instanceof Uint64) {
            writeValue((Uint64) value);
        } else if (value instanceof QName) {
            writeQNameInternal((QName) value);
        } else if (value instanceof YangInstanceIdentifier) {
            writeValue((YangInstanceIdentifier) value);
        } else if (value instanceof byte[]) {
            writeValue((byte[]) value);
        } else if (value instanceof Empty) {
            output.writeByte(MagnesiumValue.EMPTY);
        } else if (value instanceof Set) {
            writeValue((Set<?>) value);
        } else if (value instanceof BigDecimal) {
            writeValue((BigDecimal) value);
        } else if (value instanceof BigInteger) {
            writeValue((BigInteger) value);
        } else {
            throw new IOException("Unhandled value type " + value.getClass());
        }
    }

    private void writeValue(final boolean value) throws IOException {
        output.writeByte(value ? MagnesiumValue.BOOLEAN_TRUE : MagnesiumValue.BOOLEAN_FALSE);
    }

    private void writeValue(final byte value) throws IOException {
        if (value != 0) {
            output.writeByte(MagnesiumValue.INT8);
            output.writeByte(value);
        } else {
            output.writeByte(MagnesiumValue.INT8_0);
        }
    }

    private void writeValue(final short value) throws IOException {
        if (value != 0) {
            output.writeByte(MagnesiumValue.INT16);
            output.writeShort(value);
        } else {
            output.writeByte(MagnesiumValue.INT16_0);
        }
    }

    private void writeValue(final int value) throws IOException {
        if ((value & 0xFFFF0000) != 0) {
            output.writeByte(MagnesiumValue.INT32);
            output.writeInt(value);
        } else if (value != 0) {
            output.writeByte(MagnesiumValue.INT32_2B);
            output.writeShort(value);
        } else {
            output.writeByte(MagnesiumValue.INT32_0);
        }
    }

    private void writeValue(final long value) throws IOException {
        if ((value & 0xFFFFFFFF00000000L) != 0) {
            output.writeByte(MagnesiumValue.INT64);
            output.writeLong(value);
        } else if (value != 0) {
            output.writeByte(MagnesiumValue.INT64_4B);
            output.writeInt((int) value);
        } else {
            output.writeByte(MagnesiumValue.INT64_0);
        }
    }

    private void writeValue(final Uint8 value) throws IOException {
        final byte b = value.byteValue();
        if (b != 0) {
            output.writeByte(MagnesiumValue.UINT8);
            output.writeByte(b);
        } else {
            output.writeByte(MagnesiumValue.UINT8_0);
        }
    }

    private void writeValue(final Uint16 value) throws IOException {
        final short s = value.shortValue();
        if (s != 0) {
            output.writeByte(MagnesiumValue.UINT16);
            output.writeShort(s);
        } else {
            output.writeByte(MagnesiumValue.UINT16_0);
        }
    }

    private void writeValue(final Uint32 value) throws IOException {
        final int i = value.intValue();
        if ((i & 0xFFFF0000) != 0) {
            output.writeByte(MagnesiumValue.UINT32);
            output.writeInt(i);
        } else if (i != 0) {
            output.writeByte(MagnesiumValue.UINT32_2B);
            output.writeShort(i);
        } else {
            output.writeByte(MagnesiumValue.UINT32_0);
        }
    }

    private void writeValue(final Uint64 value) throws IOException {
        final long l = value.longValue();
        if ((l & 0xFFFFFFFF00000000L) != 0) {
            output.writeByte(MagnesiumValue.UINT64);
            output.writeLong(l);
        } else if (l != 0) {
            output.writeByte(MagnesiumValue.UINT64_4B);
            output.writeInt((int) l);
        } else {
            output.writeByte(MagnesiumValue.UINT64_0);
        }
    }

    private void writeValue(final BigDecimal value) throws IOException {
        output.writeByte(MagnesiumValue.BIGDECIMAL);
        output.writeUTF(value.toString());
    }

    abstract void writeValue(BigInteger value) throws IOException;

    private void writeValue(final String value) throws IOException {
        if (value.isEmpty()) {
            output.writeByte(MagnesiumValue.STRING_EMPTY);
        } else if (value.length() <= Short.MAX_VALUE / 2) {
            output.writeByte(MagnesiumValue.STRING_UTF);
            output.writeUTF(value);
        } else if (value.length() <= 1048576) {
            final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 65536) {
                output.writeByte(MagnesiumValue.STRING_2B);
                output.writeShort(bytes.length);
            } else {
                output.writeByte(MagnesiumValue.STRING_4B);
                output.writeInt(bytes.length);
            }
            output.write(bytes);
        } else {
            output.writeByte(MagnesiumValue.STRING_CHARS);
            output.writeInt(value.length());
            output.writeChars(value);
        }
    }

    private void writeValue(final byte[] value) throws IOException {
        if (value.length < 128) {
            output.writeByte(MagnesiumValue.BINARY_0 + value.length);
        } else if (value.length < 384) {
            output.writeByte(MagnesiumValue.BINARY_1B);
            output.writeByte(value.length - 128);
        } else if (value.length < 65920) {
            output.writeByte(MagnesiumValue.BINARY_2B);
            output.writeShort(value.length - 384);
        } else {
            output.writeByte(MagnesiumValue.BINARY_4B);
            output.writeInt(value.length);
        }
        output.write(value);
    }

    private void writeValue(final YangInstanceIdentifier value) throws IOException {
        final List<PathArgument> args = value.getPathArguments();
        final int size = args.size();
        if (size > 31) {
            output.writeByte(MagnesiumValue.YIID);
            output.writeInt(size);
        } else {
            output.writeByte(MagnesiumValue.YIID_0 + size);
        }
        for (PathArgument arg : args) {
            writePathArgumentInternal(arg);
        }
    }

    private void writeValue(final Set<?> value) throws IOException {
        final int size = value.size();
        if (size < 29) {
            output.writeByte(MagnesiumValue.BITS_0 + size);
        } else if (size < 285) {
            output.writeByte(MagnesiumValue.BITS_1B);
            output.writeByte(size - 29);
        } else if (size < 65821) {
            output.writeByte(MagnesiumValue.BITS_2B);
            output.writeShort(size - 285);
        } else {
            output.writeByte(MagnesiumValue.BITS_4B);
            output.writeInt(size);
        }

        for (Object bit : value) {
            checkArgument(bit instanceof String, "Expected value type to be String but was %s", bit);
            encodeString((String) bit);
        }
    }

    // Check if the proposed QName matches the parent. This is only effective if the parent is identified by
    // NodeIdentifier -- which is typically true
    private boolean matchesParentQName(final QName qname) {
        final Object current = stack.peek();
        return current instanceof NodeIdentifier && qname.equals(((NodeIdentifier) current).getNodeType());
    }

    // Start an END_NODE-terminated node, which typically has a QName matching the parent. If that is the case we emit
    // a parent reference instead of an explicit QName reference -- saving at least one byte
    private void startInheritedNode(final byte type, final PathArgument name) throws IOException {
        final QName qname = name.getNodeType();
        if (matchesParentQName(qname)) {
            output.write(type);
        } else {
            writeQNameNode(type, qname);
        }
        stack.push(name);
    }

    // Start an END_NODE-terminated node, which needs its QName encoded
    private void startQNameNode(final byte type, final PathArgument name) throws IOException {
        writeQNameNode(type, name.getNodeType());
        stack.push(name);
    }

    // Start a simple node, which is not terminated through END_NODE and encode its QName
    private void startSimpleNode(final byte type, final PathArgument name) throws IOException {
        writeQNameNode(type, name.getNodeType());
        stack.push(NO_ENDNODE_STATE);
    }

    // Encode a QName-based (i.e. NodeIdentifier*) node with a particular QName. This will either result in a QName
    // definition, or a reference, where this is encoded along with the node type.
    private void writeQNameNode(final int type, final @NonNull QName qname) throws IOException {
        final Integer code = qnameCodeMap.get(qname);
        if (code == null) {
            output.writeByte(type | MagnesiumNode.ADDR_DEFINE);
            encodeQName(qname);
        } else {
            writeNodeType(type, code);
        }
    }

    // Write a node type + lookup
    private void writeNodeType(final int type, final int code) throws IOException {
        if (code <= 255) {
            output.writeByte(type | MagnesiumNode.ADDR_LOOKUP_1B);
            output.writeByte(code);
        } else {
            output.writeByte(type | MagnesiumNode.ADDR_LOOKUP_4B);
            output.writeInt(code);
        }
    }

    // Encode a QName using lookup tables, resuling either in a reference to an existing entry, or emitting two
    // String values.
    private void encodeQName(final @NonNull QName qname) throws IOException {
        final Integer prev = qnameCodeMap.put(qname, qnameCodeMap.size());
        if (prev != null) {
            throw new IOException("Internal coding error: attempted to re-encode " + qname + "%s already encoded as "
                    + prev);
        }

        final QNameModule module = qname.getModule();
        final Integer code = moduleCodeMap.get(module);
        if (code == null) {
            moduleCodeMap.put(module, moduleCodeMap.size());
            encodeString(module.getNamespace().toString());
            final Optional<Revision> rev = module.getRevision();
            if (rev.isPresent()) {
                encodeString(rev.get().toString());
            } else {
                output.writeByte(MagnesiumValue.STRING_EMPTY);
            }
        } else {
            writeModuleRef(code);
        }
        encodeString(qname.getLocalName());
    }

    // Encode a String using lookup tables, resulting either in a reference to an existing entry, or emitting as
    // a literal value
    private void encodeString(final @NonNull String str) throws IOException {
        final Integer code = stringCodeMap.get(str);
        if (code != null) {
            writeRef(code);
        } else {
            stringCodeMap.put(str, stringCodeMap.size());
            writeValue(str);
        }
    }

    // Write a QName with a lookup table reference. This is a combination of asserting the value is a QName plus
    // the effects of writeRef()
    private void writeQNameRef(final int code) throws IOException {
        final int val = code;
        if (val < 256) {
            output.writeByte(MagnesiumValue.QNAME_REF_1B);
            output.writeByte(val);
        } else if (val < 65792) {
            output.writeByte(MagnesiumValue.QNAME_REF_2B);
            output.writeShort(val - 256);
        } else {
            output.writeByte(MagnesiumValue.QNAME_REF_4B);
            output.writeInt(val);
        }
    }

    // Write a lookup table reference, which table is being referenced is implied by the caller
    private void writeRef(final int code) throws IOException {
        final int val = code;
        if (val < 256) {
            output.writeByte(MagnesiumValue.STRING_REF_1B);
            output.writeByte(val);
        } else if (val < 65792) {
            output.writeByte(MagnesiumValue.STRING_REF_2B);
            output.writeShort(val - 256);
        } else {
            output.writeByte(MagnesiumValue.STRING_REF_4B);
            output.writeInt(val);
        }
    }

    // Write a lookup module table reference, which table is being referenced is implied by the caller
    private void writeModuleRef(final int code) throws IOException {
        final int val = code;
        if (val < 256) {
            output.writeByte(MagnesiumValue.MODREF_1B);
            output.writeByte(val);
        } else if (val < 65792) {
            output.writeByte(MagnesiumValue.MODREF_2B);
            output.writeShort(val - 256);
        } else {
            output.writeByte(MagnesiumValue.MODREF_4B);
            output.writeInt(val);
        }
    }
}
