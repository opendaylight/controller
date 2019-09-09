/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;
import static org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter.UNKNOWN_SIZE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.io.DataInput;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.dom.DOMSource;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.MagnesiumTokens.LeafValue;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.MagnesiumTokens.NodeType;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

final class MagnesiumDataInput extends AbstractNormalizedNodeDataInput {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLithiumDataInput.class);

    // Known singleton objects
    private static final @NonNull Byte INT8_0 = 0;
    private static final @NonNull Byte INT8_1 = 1;
    private static final @NonNull Byte INT8_MIN = Byte.MIN_VALUE;
    private static final @NonNull Byte INT8_MAX = Byte.MAX_VALUE;
    private static final @NonNull Short INT16_0 = 0;
    private static final @NonNull Short INT16_1 = 1;
    private static final @NonNull Short INT16_MIN = Short.MIN_VALUE;
    private static final @NonNull Short INT16_MAX = Short.MAX_VALUE;
    private static final @NonNull Integer INT32_0 = 0;
    private static final @NonNull Integer INT32_1 = 1;
    private static final @NonNull Integer INT32_MIN = Integer.MIN_VALUE;
    private static final @NonNull Integer INT32_MAX = Integer.MAX_VALUE;
    private static final @NonNull Long INT64_0 = 0L;
    private static final @NonNull Long INT64_1 = 1L;
    private static final @NonNull Long INT64_MIN = Long.MIN_VALUE;
    private static final @NonNull Long INT64_MAX = Long.MAX_VALUE;
    private static final byte@ NonNull [] BINARY_0 = new byte[0];

    // FIXME: these should be available as constants
    private static final @NonNull Uint8 UINT8_0 = Uint8.valueOf(0);
    private static final @NonNull Uint8 UINT8_1 = Uint8.valueOf(1);
    private static final @NonNull Uint8 UINT8_MAX = Uint8.valueOf(255);
    private static final @NonNull Uint16 UINT16_0 = Uint16.valueOf(0);
    private static final @NonNull Uint16 UINT16_1 = Uint16.valueOf(1);
    private static final @NonNull Uint16 UINT16_MAX = Uint16.valueOf(65535);
    private static final @NonNull Uint32 UINT32_0 = Uint32.valueOf(0);
    private static final @NonNull Uint32 UINT32_1 = Uint32.valueOf(1);
    private static final @NonNull Uint32 UINT32_MAX = Uint32.fromIntBits(-1);
    private static final @NonNull Uint64 UINT64_0 = Uint64.valueOf(0);
    private static final @NonNull Uint64 UINT64_1 = Uint64.valueOf(1);
    private static final @NonNull Uint64 UINT64_MAX = Uint64.fromLongBits(-1);

    private final List<QNameModule> codedModules = new ArrayList<>();
    private final List<String> codedStrings = new ArrayList<>();
    private final List<QName> codedQNames = new ArrayList<>();

    MagnesiumDataInput(final DataInput input) {
        super(input);
    }

    @Override
    public NormalizedNodeStreamVersion getVersion() throws IOException {
        return NormalizedNodeStreamVersion.MAGNESIUM;
    }

    @Override
    public void streamNormalizedNode(final NormalizedNodeStreamWriter writer) throws IOException {
        streamNormalizedNode(requireNonNull(writer), null, input.readByte());
    }

    private void streamNormalizedNode(final NormalizedNodeStreamWriter writer, final PathArgument parent,
            final byte nodeHeader) throws IOException {
        switch (nodeHeader & 0x0F) {
            case NodeType.NODE_LEAF:
                streamLeaf(writer, parent, nodeHeader);
                break;
            case NodeType.NODE_CONTAINER:
                streamContainer(writer, nodeHeader);
                break;
            case NodeType.NODE_LIST:
                streamList(writer, nodeHeader);
                break;
            case NodeType.NODE_MAP:
                streamMap(writer, nodeHeader);
                break;
            case NodeType.NODE_MAP_ORDERED:
                streamMapOrdered(writer, nodeHeader);
                break;
            case NodeType.NODE_LEAFSET:
                streamLeafset(writer, nodeHeader);
                break;
            case NodeType.NODE_LEAFSET_ORDERED:
                streamLeafsetOrdered(writer, nodeHeader);
                break;
            case NodeType.NODE_CHOICE:
                streamChoice(writer, nodeHeader);
                break;
            case NodeType.NODE_AUGMENTATION:
                streamAugmentation(writer, nodeHeader);
                break;
            case NodeType.NODE_ANYXML:
                streamAnyxml(writer, nodeHeader);
                break;
            case NodeType.NODE_ANYXML_MODELED:
                streamAnyxmlModeled(writer, nodeHeader);
                break;
            case NodeType.NODE_LIST_ENTRY:
                streamListEntry(writer, nodeHeader);
                break;
            case NodeType.NODE_LEAFSET_ENTRY:
                streamLeafsetEntry(writer, nodeHeader);
                break;
            case NodeType.NODE_MAP_ENTRY:
                streamMapEntry(writer, nodeHeader);
                break;
            default:
                throw new InvalidNormalizedNodeStreamException("Unexpected node header " + nodeHeader);
        }
    }

    private void streamAnyxml(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming anyxml node {}", identifier);
        writer.startAnyxmlNode(identifier);
        writer.domSourceValue(readDOMSource());
        writer.endNode();
    }

    private void streamAnyxmlModeled(final NormalizedNodeStreamWriter writer, final byte nodeHeader)
            throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    private void streamAugmentation(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    private void streamChoice(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming choice node {}", identifier);
        writer.startChoiceNode(identifier, UNKNOWN_SIZE);
        commonStreamContainer(writer, identifier);
    }

    private void streamContainer(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming container node {}", identifier);
        writer.startContainerNode(identifier, UNKNOWN_SIZE);
        commonStreamContainer(writer, identifier);
    }

    private void streamLeaf(final NormalizedNodeStreamWriter writer, final PathArgument parent, final byte nodeHeader)
            throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming leaf node {}", identifier);
        writer.startLeafNode(identifier);

        final Object value;
        if ((nodeHeader & NodeType.PREDICATE_ONE) != 0) {
            if (!(parent instanceof NodeIdentifierWithPredicates)) {
                throw new InvalidNormalizedNodeStreamException("Invalid predicate leaf " + identifier + " in parent "
                        + parent);
            }

            value = ((NodeIdentifierWithPredicates) parent).getValue(identifier.getNodeType());
            if (value == null) {
                throw new InvalidNormalizedNodeStreamException("Failed to find predicate leaf " + identifier
                    + " in parent " + parent);
            }
        } else {
            value = readLeafValue();
        }

        writer.scalarValue(value);
        writer.endNode();
    }

    private void streamLeafset(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming leaf set node {}", identifier);
        writer.startLeafSet(identifier, UNKNOWN_SIZE);
        commonStreamLeafSet(writer, identifier);
    }

    private void streamLeafsetOrdered(final NormalizedNodeStreamWriter writer, final byte nodeHeader)
            throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming ordered leaf set node {}", identifier);
        writer.startOrderedLeafSet(identifier, UNKNOWN_SIZE);
        commonStreamLeafSet(writer, identifier);
    }

    private void commonStreamLeafSet(final NormalizedNodeStreamWriter writer, final NodeIdentifier identifier)
            throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    private void streamLeafsetEntry(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    private void streamList(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        writer.startUnkeyedList(identifier, UNKNOWN_SIZE);
        commonStreamContainer(writer, identifier);
    }

    private void streamListEntry(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming unkeyed list item node {}", identifier);
        writer.startUnkeyedListItem(identifier, UNKNOWN_SIZE);
        commonStreamContainer(writer, identifier);
    }

    private void streamMap(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming map node {}", identifier);
        writer.startMapNode(identifier, UNKNOWN_SIZE);
        commonStreamContainer(writer, identifier);
    }

    private void streamMapOrdered(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming ordered map node {}", identifier);
        writer.startOrderedMapNode(identifier, UNKNOWN_SIZE);
        commonStreamContainer(writer, identifier);
    }

    private void streamMapEntry(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    private void commonStreamContainer(final NormalizedNodeStreamWriter writer, final PathArgument parent)
            throws IOException {
        for (byte nodeType = input.readByte(); nodeType != NodeType.NODE_END; nodeType = input.readByte()) {
            streamNormalizedNode(writer, parent, nodeType);
        }
        writer.endNode();
    }

    private NodeIdentifier decodeNodeIdentifier(final byte nodeHeader) throws IOException {
        final int index;
        switch (nodeHeader & 0x30) {
            case NodeType.ADDR_DEFINE:
                // FIXME: implement this
                throw new UnsupportedOperationException();
            case NodeType.ADDR_LOOKUP_1B:
                index = Byte.toUnsignedInt(input.readByte());
                break;
            case NodeType.ADDR_LOOKUP_4B:
                index = input.readInt();
                break;
            default:
                throw new InvalidNormalizedNodeStreamException("Unexpected node identifier addressing in header "
                        + nodeHeader);
        }

        try {
            // FIXME: use a cache
            return new NodeIdentifier(codedQNames.get(index));
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidNormalizedNodeStreamException("Invalid QName reference " + index, e);
        }
    }

    @Override
    public YangInstanceIdentifier readYangInstanceIdentifier() throws IOException {
        final byte type = input.readByte();
        if (type == LeafValue.YIID) {
            return readYangInstanceIdentifier(input.readInt());
        } else if (type >= LeafValue.YIID_0 && type <= LeafValue.YIID_31) {
            return readYangInstanceIdentifier(type - LeafValue.YIID_0);
        } else {
            throw new InvalidNormalizedNodeStreamException("Unexpected YangInstanceIdentifier type " + type);
        }
    }

    private @NonNull YangInstanceIdentifier readYangInstanceIdentifier(final int size) throws IOException {
        if (size > 0) {
            final Builder<PathArgument> builder = ImmutableList.builderWithExpectedSize(size);
            for (int i = 0; i < size; ++i) {
                builder.add(readPathArgument());
            }
            return YangInstanceIdentifier.create(builder.build());
        } else if (size == 0) {
            return YangInstanceIdentifier.EMPTY;
        } else {
            throw new InvalidNormalizedNodeStreamException("Invalid YangInstanceIdentifier size " + size);
        }
    }

    @Override
    public QName readQName() throws IOException {
        final byte type = input.readByte();
        final int index;
        switch (type) {
            case LeafValue.QNAME:
                return decodeQName();
            case LeafValue.QNAME_REF_1B:
                index = Byte.toUnsignedInt(input.readByte());
                break;
            case LeafValue.QNAME_REF_2B:
                index = Short.toUnsignedInt(input.readShort());
                break;
            case LeafValue.QNAME_REF_4B:
                index = input.readInt();
                break;
            default:
                throw new InvalidNormalizedNodeStreamException("Unexpected QName type " + type);
        }

        try {
            return codedQNames.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidNormalizedNodeStreamException("Invalid QName reference " + index, e);
        }
    }

    @Override
    public PathArgument readPathArgument() throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    private @NonNull QName decodeQName() throws IOException {
        final QName qname = QName.create(decodeQNameModule(), readRefString()).intern();
        codedQNames.add(qname);
        return qname;
    }

    private @NonNull QNameModule decodeQNameModule() throws IOException {
        final byte type = input.readByte();
        final int index;
        switch (type) {
            case LeafValue.REF_1B:
                index = Byte.toUnsignedInt(input.readByte());
                break;
            case LeafValue.REF_2B:
                index = Short.toUnsignedInt(input.readShort());
                break;
            case LeafValue.REF_4B:
                index = input.readInt();
                break;
            default:
                return decodeQNameModuleDef(type);
        }

        try {
            return codedModules.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidNormalizedNodeStreamException("Invalid QNameModule reference " + index, e);
        }
    }

    // QNameModule definition, i.e. two encoded strings
    private @NonNull QNameModule decodeQNameModuleDef(final byte type) throws IOException {
        final URI namespace;
        try {
            namespace = new URI(readRefString(type));
        } catch (URISyntaxException e) {
            throw new InvalidNormalizedNodeStreamException("Illegal QNameModule namespace", e);
        }

        final String revision = readRefString();
        final QNameModule module;
        if (!revision.isEmpty()) {
            final Revision rev;
            try {
                rev = Revision.of(revision);
            } catch (DateTimeParseException e) {
                throw new InvalidNormalizedNodeStreamException("Illegal QNameModule revision", e);
            }

            module = QNameModule.create(namespace, rev);
        } else {
            module = QNameModule.create(namespace);
        }

        final QNameModule interned = module.intern();
        codedModules.add(interned);
        return interned;
    }

    private @NonNull String readRefString() throws IOException {
        return readRefString(input.readByte());
    }

    private @NonNull String readRefString(final byte type) throws IOException {
        final String str;
        switch (type) {
            case LeafValue.REF_1B:
                return lookupString(Byte.toUnsignedInt(input.readByte()));
            case LeafValue.REF_2B:
                return lookupString(Short.toUnsignedInt(input.readShort()));
            case LeafValue.REF_4B:
                return lookupString(input.readInt());
            case LeafValue.STRING_EMPTY:
                return "";
            case LeafValue.STRING_2B:
                str = readByteString(Short.toUnsignedInt(input.readShort()));
                break;
            case LeafValue.STRING_4B:
                str = readByteString(input.readInt());
                break;
            case LeafValue.STRING_CHARS:
                str = readCharsString();
                break;
            case LeafValue.STRING_UTF:
                str = input.readUTF();
                break;
            default:
                throw new InvalidNormalizedNodeStreamException("Unexpected String type " + type);
        }

        // TODO: consider interning Strings -- that would help with bits, but otherwise it's probably not worth it
        codedStrings.add(verifyNotNull(str));
        return str;
    }

    private @NonNull String readByteString(final int size) throws IOException {
        if (size > 0) {
            final byte[] bytes = new byte[size];
            input.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } else if (size == 0) {
            return "";
        } else {
            throw new InvalidNormalizedNodeStreamException("Invalid String bytes length " + size);
        }
    }

    private @NonNull String readCharsString() throws IOException {
        final int size = input.readInt();
        if (size > 0) {
            final char[] chars = new char[size];
            for (int i = 0; i < size; ++i) {
                chars[i] = input.readChar();
            }
            return String.valueOf(chars);
        } else if (size == 0) {
            return "";
        } else {
            throw new InvalidNormalizedNodeStreamException("Invalid String chars length " + size);
        }
    }

    private @NonNull String lookupString(final int index) throws InvalidNormalizedNodeStreamException {
        try {
            return codedStrings.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidNormalizedNodeStreamException("Invalid String reference " + index, e);
        }
    }

    private @NonNull DOMSource readDOMSource() throws IOException {
        // FIXME: read string from stream
        final String str = "";
        try {
            return new DOMSource(UntrustedXML.newDocumentBuilder().parse(new InputSource(new StringReader(str)))
                .getDocumentElement());
        } catch (SAXException e) {
            throw new IOException("Error parsing XML: " + str, e);
        }
    }

    private @NonNull Object readLeafValue() throws IOException {
        final byte type = input.readByte();
        switch (type) {
            case LeafValue.BOOLEAN_FALSE:
                return Boolean.FALSE;
            case LeafValue.BOOLEAN_TRUE:
                return Boolean.TRUE;
            case LeafValue.EMPTY:
                return Empty.getInstance();
            case LeafValue.INT8:
                return input.readByte();
            case LeafValue.INT8_0:
                return INT8_0;
            case LeafValue.INT8_1:
                return INT8_1;
            case LeafValue.INT8_MIN:
                return INT8_MIN;
            case LeafValue.INT8_MAX:
                return INT8_MAX;
            case LeafValue.INT16:
                return input.readShort();
            case LeafValue.INT16_0:
                return INT16_0;
            case LeafValue.INT16_1:
                return INT16_1;
            case LeafValue.INT16_MIN:
                return INT16_MIN;
            case LeafValue.INT16_MAX:
                return INT16_MAX;
            case LeafValue.INT32:
                return input.readInt();
            case LeafValue.INT32_0:
                return INT32_0;
            case LeafValue.INT32_1:
                return INT32_1;
            case LeafValue.INT32_MIN:
                return INT32_MIN;
            case LeafValue.INT32_MAX:
                return INT32_MAX;
            case LeafValue.INT64:
                return input.readLong();
            case LeafValue.INT64_0:
                return INT64_0;
            case LeafValue.INT64_1:
                return INT64_1;
            case LeafValue.INT64_MIN:
                return INT64_MIN;
            case LeafValue.INT64_MAX:
                return INT64_MAX;
            case LeafValue.UINT8:
                return Uint8.fromByteBits(input.readByte());
            case LeafValue.UINT8_0:
                return UINT8_0;
            case LeafValue.UINT8_1:
                return UINT8_1;
            case LeafValue.UINT8_MAX:
                return UINT8_MAX;
            case LeafValue.UINT16:
                return Uint16.fromShortBits(input.readShort());
            case LeafValue.UINT16_0:
                return UINT16_0;
            case LeafValue.UINT16_1:
                return UINT16_1;
            case LeafValue.UINT16_MAX:
                return UINT16_MAX;
            case LeafValue.UINT32:
                return Uint32.fromIntBits(input.readInt());
            case LeafValue.UINT32_0:
                return UINT32_0;
            case LeafValue.UINT32_1:
                return UINT32_1;
            case LeafValue.UINT32_MAX:
                return UINT32_MAX;
            case LeafValue.UINT64:
                return Uint64.fromLongBits(input.readLong());
            case LeafValue.UINT64_0:
                return UINT64_0;
            case LeafValue.UINT64_1:
                return UINT64_1;
            case LeafValue.UINT64_MAX:
                return UINT64_MAX;
            case LeafValue.DECIMAL64:
                // FIXME: use string -> BigDecimal cache
                return new BigDecimal(input.readUTF());
            case LeafValue.STRING_EMPTY:
                return "";
            case LeafValue.STRING_UTF:
                return input.readUTF();
            case LeafValue.STRING_2B:
                return readByteString(Short.toUnsignedInt(input.readShort()));
            case LeafValue.STRING_4B:
                return readByteString(input.readInt());
            case LeafValue.STRING_CHARS:
                return readCharsString();
            case LeafValue.BINARY_0:
                return BINARY_0;
            case LeafValue.BINARY_1B:
                return readBinary(128 + Byte.toUnsignedInt(input.readByte()));
            case LeafValue.BINARY_2B:
                return readBinary(384 + Short.toUnsignedInt(input.readShort()));
            case LeafValue.BINARY_4B:
                return readBinary(input.readInt());
            case LeafValue.YIID_0:
                return YangInstanceIdentifier.EMPTY;
            case LeafValue.YIID:
                return readYangInstanceIdentifier(input.readInt());

                // FIXME: implement this
                //              // Literal QName, assigns next available code
                //              static final byte QNAME          = 0x14;
                //              // Note: single-byte code (unsigned!)
                //              static final byte QNAME_REF_1B   = 0x15;
                //              // Note: two-byte code (unsigned!) + 256
                //              static final byte QNAME_REF_2B   = 0x16;
                //              // Note: four-byte code (signed!)
                //              static final byte QNAME_REF_4B   = 0x17;
                // FIXME: BITS type


            default:
                if (type > LeafValue.BINARY_0 && type <= LeafValue.BINARY_127) {
                    return readBinary(type - LeafValue.BINARY_0);
                } else if (type > LeafValue.YIID_0 && type <= LeafValue.YIID_31) {
                    return readYangInstanceIdentifier(type - LeafValue.YIID_0);
                } else {
                    throw new InvalidNormalizedNodeStreamException("Invalid value type " + type);
                }
        }
    }

    private byte @NonNull [] readBinary(final int size) throws IOException {
        if (size > 0) {
            final byte[] ret = new byte[size];
            input.readFully(ret);
            return ret;
        } else if (size == 0) {
            return BINARY_0;
        } else {
            throw new InvalidNormalizedNodeStreamException("Invalid binary length " + size);
        }
    }
}
