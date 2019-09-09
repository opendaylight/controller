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
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.MagnesiumTokens.LeafValue;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.MagnesiumTokens.NodeType;
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

final class MagnesiumDataOutput extends AbstractNormalizedNodeDataOutput {
    private static final Logger LOG = LoggerFactory.getLogger(MagnesiumDataOutput.class);

    // Marker for encoding state when we have entered startLeafNode() within a startMapEntry() and that leaf corresponds
    // to a key carried within NodeIdentifierWithPredicates.
    private static final Object KEY_LEAF_STATE = new Object();

    private static final TransformerFactory TF = TransformerFactory.newInstance();

    /**
     * Stack tracking encoding state. In general we track the node identifier of the currently-open element, but there
     * are a few other circumstances where we push other objects.
     */
    // FIXME: expand the above to cover all possibilities
    private final Deque<Object> stack = new ArrayDeque<>();

    private final Map<AugmentationIdentifier, Integer> aidCodeMap = new HashMap<>();
    private final Map<QNameModule, Integer> moduleCodeMap = new HashMap<>();
    private final Map<String, Integer> stringCodeMap = new HashMap<>();
    private final Map<QName, Integer> qnameCodeMap = new HashMap<>();

    MagnesiumDataOutput(final DataOutput output) {
        super(output);
    }

    @Override
    short streamVersion() {
        return TokenTypes.MAGNESIUM_VERSION;
    }

    @Override
    public void startLeafNode(final NodeIdentifier name) throws IOException {
        final Object current = current();
        if (current instanceof NodeIdentifierWithPredicates) {
            final QName qname = name.getNodeType();
            if (((NodeIdentifierWithPredicates) current).getValue(qname) != null) {
                writeQNameNode(NodeType.NODE_LEAF | NodeType.PREDICATE_ONE, qname);
                stack.push(KEY_LEAF_STATE);
                return;
            }
        }

        startQNameNode(NodeType.NODE_LEAF, name);
    }

    @Override
    public void startLeafSet(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(NodeType.NODE_LEAFSET, name);
    }

    @Override
    public void startOrderedLeafSet(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(NodeType.NODE_LEAFSET_ORDERED, name);
    }

    @Override
    public void startLeafSetEntryNode(final NodeWithValue<?> name) throws IOException {
        if (current() != null) {
            writeByte(NodeTypes.LEAF_SET_ENTRY_NODE);
            writeObject(name.getValue());
        } else {
            // FIXME: implement this
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void startContainerNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(NodeType.NODE_CONTAINER, name);
    }

    @Override
    public void startUnkeyedList(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(NodeType.NODE_LIST, name);
    }

    @Override
    public void startUnkeyedListItem(final NodeIdentifier name, final int childSizeHint) throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    @Override
    public void startMapNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(NodeType.NODE_MAP, name);
    }

    @Override
    public void startMapEntryNode(final NodeIdentifierWithPredicates identifier, final int childSizeHint)
            throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    @Override
    public void startOrderedMapNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(NodeType.NODE_MAP_ORDERED, name);
    }

    @Override
    public void startChoiceNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startQNameNode(NodeType.NODE_CHOICE, name);
    }

    @Override
    public void startAugmentationNode(final AugmentationIdentifier identifier) throws IOException {
        final Integer code = aidCodeMap.get(identifier);
        if (code == null) {
            aidCodeMap.put(identifier, aidCodeMap.size());
            output.writeByte(NodeType.NODE_AUGMENTATION | NodeType.ADDR_DEFINE);
            final Set<QName> qnames = identifier.getPossibleChildNames();
            output.writeInt(qnames.size());
            for (QName qname : qnames) {
                writeQNameInternal(qname);
            }
        } else {
            writeNodeType(NodeType.NODE_AUGMENTATION, code);
        }
    }

    @Override
    public void startAnyxmlNode(final NodeIdentifier name) throws IOException {
        startQNameNode(NodeType.NODE_ANYXML, name);
    }

    @Override
    public void domSourceValue(final DOMSource value) throws IOException {
        final StringWriter writer = new StringWriter();
        try {
            TF.newTransformer().transform(value, new StreamResult(writer));
        } catch (TransformerException e) {
            throw new IOException("Error writing anyXml", e);
        }
        writeValue(writer.toString());
    }

    @Override
    public void startYangModeledAnyXmlNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    @Override
    public void endNode() throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    @Override
    public void scalarValue(final Object value) throws IOException {
        if (KEY_LEAF_STATE.equals(current())) {
            LOG.trace("Inside a map entry key leaf, not emitting value {}", value);
        } else {
            writeObject(value);
        }
    }

    @Override
    void writeQNameInternal(final QName qname) throws IOException {
        final Integer code = qnameCodeMap.get(qname);
        if (code == null) {
            qnameCodeMap.put(qname, qnameCodeMap.size());
            output.writeByte(LeafValue.QNAME);
            encodeQName(qname);
        } else {
            writeQNameRef(code);
        }
    }

    @Override
    void writePathArgumentInternal(final PathArgument pathArgument) throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    @Override
    void writeYangInstanceIdentifierInternal(final YangInstanceIdentifier identifier) throws IOException {
        writeValue(identifier);
    }

    private @Nullable Object current() {
        return stack.peek();
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
        } else if (value instanceof BigDecimal) {
            writeValue((BigDecimal) value);
        } else if (value instanceof byte[]) {
            writeValue((byte[]) value);
        } else if (value instanceof Empty) {
            output.writeByte(LeafValue.EMPTY);
        } else if (value instanceof Set) {
            writeValue((Set<?>) value);
        } else {
            throw new IOException("Unhandled value " + value);
        }
    }

    private void writeValue(final boolean value) throws IOException {
        output.writeByte(value ? LeafValue.BOOLEAN_TRUE : LeafValue.BOOLEAN_FALSE);
    }

    private void writeValue(final byte value) throws IOException {
        switch (value) {
            case 0:
                output.writeByte(LeafValue.INT8_0);
                break;
            case 1:
                output.writeByte(LeafValue.INT8_1);
                break;
            case Byte.MIN_VALUE:
                output.writeByte(LeafValue.INT8_MIN);
                break;
            case Byte.MAX_VALUE:
                output.writeByte(LeafValue.INT8_MAX);
                break;
            default:
                output.writeByte(LeafValue.INT8);
                output.writeByte(value);
        }
    }

    private void writeValue(final short value) throws IOException {
        switch (value) {
            case 0:
                output.writeByte(LeafValue.INT16_0);
                break;
            case 1:
                output.writeByte(LeafValue.INT16_1);
                break;
            case Short.MIN_VALUE:
                output.writeByte(LeafValue.INT16_MIN);
                break;
            case Short.MAX_VALUE:
                output.writeByte(LeafValue.INT16_MAX);
                break;
            default:
                output.writeByte(LeafValue.INT16);
                output.writeShort(value);
        }
    }

    private void writeValue(final int value) throws IOException {
        switch (value) {
            case 0:
                output.writeByte(LeafValue.INT32_0);
                break;
            case 1:
                output.writeByte(LeafValue.INT32_1);
                break;
            case Integer.MIN_VALUE:
                output.writeByte(LeafValue.INT32_MIN);
                break;
            case Integer.MAX_VALUE:
                output.writeByte(LeafValue.INT32_MAX);
                break;
            default:
                output.writeByte(LeafValue.INT32);
                output.writeInt(value);
        }
    }

    private void writeValue(final long value) throws IOException {
        // FIXME: hmm, this dispatch does not look so nice
        if (value == 0) {
            output.writeByte(LeafValue.INT64_0);
        } else if (value == 1) {
            output.writeByte(LeafValue.INT64_1);
        } else if (value == Long.MIN_VALUE) {
            output.writeByte(LeafValue.INT64_MIN);
        } else if (value == Long.MAX_VALUE) {
            output.writeByte(LeafValue.INT64_MAX);
        } else {
            output.writeByte(LeafValue.INT64);
            output.writeLong(value);
        }
    }

    private void writeValue(final Uint8 value) throws IOException {
        final byte b = value.byteValue();
        switch (b) {
            case 0:
                output.writeByte(LeafValue.UINT8_0);
                break;
            case 1:
                output.writeByte(LeafValue.UINT8_1);
                break;
            case -1:
                output.writeByte(LeafValue.UINT8_MAX);
                break;
            default:
                output.writeByte(LeafValue.UINT8);
                output.writeByte(b);
        }
    }

    private void writeValue(final Uint16 value) throws IOException {
        final short s = value.shortValue();
        switch (s) {
            case 0:
                output.writeByte(LeafValue.UINT16_0);
                break;
            case 1:
                output.writeByte(LeafValue.UINT16_1);
                break;
            case -1:
                output.writeByte(LeafValue.UINT16_MAX);
                break;
            default:
                output.writeByte(LeafValue.UINT16);
                output.writeShort(s);
        }
    }

    private void writeValue(final Uint32 value) throws IOException {
        final int i = value.intValue();
        switch (i) {
            case 0:
                output.writeByte(LeafValue.UINT32_0);
                break;
            case 1:
                output.writeByte(LeafValue.UINT32_1);
                break;
            case -1:
                output.writeByte(LeafValue.UINT32_MAX);
                break;
            default:
                output.writeByte(LeafValue.UINT32);
                output.writeInt(i);
        }
    }

    private void writeValue(final Uint64 value) throws IOException {
        final long l = value.longValue();
        if (l == 0) {
            output.writeByte(LeafValue.UINT64_0);
        } else if (l == 1) {
            output.writeByte(LeafValue.UINT64_1);
        } else if (l == -1) {
            output.writeByte(LeafValue.UINT64_MAX);
        } else {
            output.writeByte(LeafValue.UINT64);
            output.writeLong(l);
        }
    }

    private void writeValue(final BigDecimal value) throws IOException {
        output.writeByte(LeafValue.DECIMAL64);
        output.writeUTF(value.toString());
    }

    private void writeValue(final String value) throws IOException {
        if (value.isEmpty()) {
            output.writeByte(LeafValue.STRING_EMPTY);
        } else if (value.length() <= Short.MAX_VALUE / 2) {
            output.writeByte(LeafValue.STRING_UTF);
            output.writeUTF(value);
        } else if (value.length() <= 1048576) {
            final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 65536) {
                output.writeByte(LeafValue.STRING_2B);
                output.writeShort(bytes.length);
            } else {
                output.writeByte(LeafValue.STRING_4B);
                output.writeInt(bytes.length);
            }
            output.write(bytes);
        } else {
            output.writeByte(LeafValue.STRING_CHARS);
            output.writeInt(value.length());
            output.writeChars(value);
        }
    }

    private void writeValue(final byte[] value) throws IOException {
        if (value.length < 128) {
            output.writeByte(LeafValue.BINARY_0 + value.length);
        } else if (value.length < 384) {
            output.writeByte(LeafValue.BINARY_1B);
            output.writeByte(value.length - 128);
        } else if (value.length < 65920) {
            output.writeByte(LeafValue.BINARY_2B);
            output.writeShort(value.length - 384);
        } else {
            output.writeByte(LeafValue.BINARY_4B);
            output.writeInt(value.length);
        }
        output.write(value);
    }

    private void writeValue(final YangInstanceIdentifier value) throws IOException {
        final List<PathArgument> args = value.getPathArguments();
        final int size = args.size();
        if (size > 31) {
            output.writeByte(LeafValue.YIID);
            output.writeInt(size);
        } else {
            output.writeByte(LeafValue.YIID_0 + size);
        }
        for (PathArgument arg : args) {
            writePathArgumentInternal(arg);
        }
    }

    private void writeValue(final Set<?> value) throws IOException {
        final int size = value.size();
        if (size > 32) {
            throw new IOException("Too many bits defined in " + value);
        }
        output.writeByte(size < 32 ? LeafValue.BITS_0 + size : LeafValue.BITS_32);
        for (Object bit : value) {
            checkArgument(bit instanceof String, "Expected value type to be String but was %s", bit);
            encodeString((String) bit);
        }
    }

    private void startQNameNode(final byte type, final NodeIdentifier name) throws IOException {
        writeQNameNode(type, name.getNodeType());
        stack.push(name);
    }

    // Encode a QName-based (i.e. NodeIdentifier*) node with a particular QName. This will either result in a QName
    // definition, or a reference, where this is encoded along with the node type.
    private void writeQNameNode(final int type, final @NonNull QName qname) throws IOException {
        final Integer code = qnameCodeMap.get(qname);
        if (code == null) {
            qnameCodeMap.put(qname, qnameCodeMap.size());
            output.writeByte(type | NodeType.ADDR_DEFINE);
            encodeQName(qname);
        } else {
            writeNodeType(type, code);
        }
    }

    // Write a node type + lookup
    private void writeNodeType(final int type, final int code) throws IOException {
        if (code <= 255) {
            output.writeByte(type | NodeType.ADDR_LOOKUP_1B);
            output.writeByte(code);
        } else {
            output.writeByte(type | NodeType.ADDR_LOOKUP_4B);
            output.writeInt(code);
        }
    }

    // Encode a QName using lookup tables, resuling either in a reference to an existing entry, or emitting two
    // String values.
    private void encodeQName(final @NonNull QName qname) throws IOException {
        final QNameModule module = qname.getModule();
        final Integer code = moduleCodeMap.get(module);
        if (code == null) {
            moduleCodeMap.put(module, moduleCodeMap.size());
            encodeString(module.getNamespace().toString());
            final Optional<Revision> rev = module.getRevision();
            if (rev.isPresent()) {
                encodeString(rev.get().toString());
            } else {
                output.writeByte(LeafValue.STRING_EMPTY);
            }
            encodeString(qname.getLocalName());
        } else {
            writeRef(code);
        }
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
            output.writeByte(LeafValue.QNAME_REF_1B);
            output.writeByte(val);
        } else if (val < 65536) {
            output.writeByte(LeafValue.QNAME_REF_2B);
            output.writeShort(val);
        } else {
            output.writeByte(LeafValue.QNAME_REF_4B);
            output.writeInt(val);
        }
    }

    // Write a lookup table reference, which table is being referenced is implied by the caller
    private void writeRef(final int code) throws IOException {
        final int val = code;
        if (val < 256) {
            output.writeByte(LeafValue.REF_1B);
            output.writeByte(val);
        } else if (val < 65536) {
            output.writeByte(LeafValue.REF_2B);
            output.writeShort(val);
        } else {
            output.writeByte(LeafValue.REF_4B);
            output.writeInt(val);
        }
    }
}
