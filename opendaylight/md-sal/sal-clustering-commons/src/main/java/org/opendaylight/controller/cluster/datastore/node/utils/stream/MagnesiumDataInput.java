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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

final class MagnesiumDataInput extends AbstractNormalizedNodeDataInput {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLithiumDataInput.class);

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
        streamNormalizedNode(requireNonNull(writer), input.readByte());
    }

    private void streamNormalizedNode(final NormalizedNodeStreamWriter writer, final byte nodeHeader)
            throws IOException {
        switch (nodeHeader & 0x0F) {
            case NodeType.NODE_LEAF:
                streamLeaf(writer, nodeHeader);
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

    private void streamAnyxmlModeled(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
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
        commonStreamContainer(writer);
    }

    private void streamContainer(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming container node {}", identifier);
        writer.startContainerNode(identifier, UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamLeaf(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    private void streamLeafset(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming leaf set node {}", identifier);
        writer.startLeafSet(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
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
        commonStreamContainer(writer);
    }

    private void streamListEntry(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    private void streamMap(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming map node {}", identifier);
        writer.startMapNode(identifier, UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamMapOrdered(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        final NodeIdentifier identifier = decodeNodeIdentifier(nodeHeader);
        LOG.trace("Streaming ordered map node {}", identifier);
        writer.startOrderedMapNode(identifier, UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamMapEntry(final NormalizedNodeStreamWriter writer, final byte nodeHeader) throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    private void commonStreamContainer(final NormalizedNodeStreamWriter writer) throws IOException {
        // FIXME: implement this
        throw new UnsupportedOperationException();
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

    private YangInstanceIdentifier readYangInstanceIdentifier(final int size) throws IOException {
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

    private DOMSource readDOMSource() throws IOException {
        // FIXME: read string from stream
        final String str = "";
        try {
            return new DOMSource(UntrustedXML.newDocumentBuilder().parse(new InputSource(new StringReader(str)))
                .getDocumentElement());
        } catch (SAXException e) {
            throw new IOException("Error parsing XML: " + str, e);
        }
    }
}
