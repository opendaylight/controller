/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.DataOutput;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
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
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLithiumDataOutput.class);
    private static final TransformerFactory TF = TransformerFactory.newInstance();
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

    private QName lastLeafSetQName;
    private boolean inSimple;

    AbstractLithiumDataOutput(final DataOutput output) {
        super(output);
    }

    @Override
    public final void startLeafNode(final NodeIdentifier name) throws IOException {
        LOG.trace("Starting a new leaf node");
        startNode(name, NodeTypes.LEAF_NODE);
        inSimple = true;
    }

    @Override
    public final void startLeafSet(final NodeIdentifier name, final int childSizeHint) throws IOException {
        LOG.trace("Starting a new leaf set");
        commonStartLeafSet(name, NodeTypes.LEAF_SET);
    }

    @Override
    public final void startOrderedLeafSet(final NodeIdentifier name, final int childSizeHint) throws IOException {
        LOG.trace("Starting a new ordered leaf set");
        commonStartLeafSet(name, NodeTypes.ORDERED_LEAF_SET);
    }

    private void commonStartLeafSet(final NodeIdentifier name, final byte nodeType) throws IOException {
        startNode(name, nodeType);
        lastLeafSetQName = name.getNodeType();
    }

    @Override
    public final void startLeafSetEntryNode(final NodeWithValue<?> name) throws IOException {
        LOG.trace("Starting a new leaf set entry node");

        output.writeByte(NodeTypes.LEAF_SET_ENTRY_NODE);

        // lastLeafSetQName is set if the parent LeafSetNode was previously written. Otherwise this is a
        // stand alone LeafSetEntryNode so write out it's name here.
        if (lastLeafSetQName == null) {
            writeQNameInternal(name.getNodeType());
        }
        inSimple = true;
    }

    @Override
    public final void startContainerNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        LOG.trace("Starting a new container node");
        startNode(name, NodeTypes.CONTAINER_NODE);
    }

    @Override
    public final void startYangModeledAnyXmlNode(final NodeIdentifier name, final int childSizeHint)
            throws IOException {
        LOG.trace("Starting a new yang modeled anyXml node");
        startNode(name, NodeTypes.YANG_MODELED_ANY_XML_NODE);
    }

    @Override
    public final void startUnkeyedList(final NodeIdentifier name, final int childSizeHint) throws IOException {
        LOG.trace("Starting a new unkeyed list");
        startNode(name, NodeTypes.UNKEYED_LIST);
    }

    @Override
    public final void startUnkeyedListItem(final NodeIdentifier name, final int childSizeHint) throws IOException {
        LOG.trace("Starting a new unkeyed list item");
        startNode(name, NodeTypes.UNKEYED_LIST_ITEM);
    }

    @Override
    public final void startMapNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        LOG.trace("Starting a new map node");
        startNode(name, NodeTypes.MAP_NODE);
    }

    @Override
    public final void startMapEntryNode(final NodeIdentifierWithPredicates identifier, final int childSizeHint)
            throws IOException {
        LOG.trace("Starting a new map entry node");
        startNode(identifier, NodeTypes.MAP_ENTRY_NODE);
        writeKeyValueMap(identifier.entrySet());
    }

    @Override
    public final void startOrderedMapNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        LOG.trace("Starting a new ordered map node");
        startNode(name, NodeTypes.ORDERED_MAP_NODE);
    }

    @Override
    public final void startChoiceNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        LOG.trace("Starting a new choice node");
        startNode(name, NodeTypes.CHOICE_NODE);
    }

    @Override
    public final void startAugmentationNode(final AugmentationIdentifier identifier) throws IOException {
        requireNonNull(identifier, "Node identifier should not be null");
        LOG.trace("Starting a new augmentation node");

        output.writeByte(NodeTypes.AUGMENTATION_NODE);
        writeAugmentationIdentifier(identifier);
    }

    @Override
    public final void startAnyxmlNode(final NodeIdentifier name) throws IOException {
        LOG.trace("Starting any xml node");
        startNode(name, NodeTypes.ANY_XML_NODE);
        inSimple = true;
    }

    @Override
    public final void scalarValue(final Object value) throws IOException {
        writeObject(value);
    }

    @Override
    public final void domSourceValue(final DOMSource value) throws IOException {
        final StringWriter writer = new StringWriter();
        try {
            TF.newTransformer().transform(value, new StreamResult(writer));
        } catch (TransformerException e) {
            throw new IOException("Error writing anyXml", e);
        }
        writeObject(writer.toString());
    }

    @Override
    public final void endNode() throws IOException {
        LOG.trace("Ending the node");
        if (!inSimple) {
            lastLeafSetQName = null;
            output.writeByte(NodeTypes.END_NODE);
        }
        inSimple = false;
    }

    @Override
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "The casts in the switch clauses are indirectly confirmed via the determination of 'type'.")
    final void writePathArgumentInternal(final PathArgument pathArgument) throws IOException {
        final byte type = PathArgumentTypes.getSerializablePathArgumentType(pathArgument);
        output.writeByte(type);

        switch (type) {
            case PathArgumentTypes.NODE_IDENTIFIER:
                NodeIdentifier nodeIdentifier = (NodeIdentifier) pathArgument;
                writeQNameInternal(nodeIdentifier.getNodeType());
                break;
            case PathArgumentTypes.NODE_IDENTIFIER_WITH_PREDICATES:
                NodeIdentifierWithPredicates nodeIdentifierWithPredicates =
                    (NodeIdentifierWithPredicates) pathArgument;
                writeQNameInternal(nodeIdentifierWithPredicates.getNodeType());
                writeKeyValueMap(nodeIdentifierWithPredicates.entrySet());
                break;
            case PathArgumentTypes.NODE_IDENTIFIER_WITH_VALUE:
                NodeWithValue<?> nodeWithValue = (NodeWithValue<?>) pathArgument;
                writeQNameInternal(nodeWithValue.getNodeType());
                writeObject(nodeWithValue.getValue());
                break;
            case PathArgumentTypes.AUGMENTATION_IDENTIFIER:
                // No Qname in augmentation identifier
                writeAugmentationIdentifier((AugmentationIdentifier) pathArgument);
                break;
            default:
                throw new IllegalStateException("Unknown node identifier type is found : "
                        + pathArgument.getClass().toString());
        }
    }

    @Override
    final void writeYangInstanceIdentifierInternal(final YangInstanceIdentifier identifier) throws IOException {
        List<PathArgument> pathArguments = identifier.getPathArguments();
        output.writeInt(pathArguments.size());

        for (PathArgument pathArgument : pathArguments) {
            writePathArgumentInternal(pathArgument);
        }
    }

    final void defaultWriteAugmentationIdentifier(final @NonNull AugmentationIdentifier aid) throws IOException {
        final Set<QName> qnames = aid.getPossibleChildNames();
        // Write each child's qname separately, if list is empty send count as 0
        if (!qnames.isEmpty()) {
            output.writeInt(qnames.size());
            for (QName qname : qnames) {
                writeQNameInternal(qname);
            }
        } else {
            LOG.debug("augmentation node does not have any child");
            output.writeInt(0);
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
            writeByte(LithiumTokens.IS_NULL_VALUE);
        }
    }

    abstract void writeModule(QNameModule module) throws IOException;

    abstract void writeAugmentationIdentifier(@NonNull AugmentationIdentifier aid) throws IOException;

    private void startNode(final PathArgument arg, final byte nodeType) throws IOException {
        requireNonNull(arg, "Node identifier should not be null");
        checkState(!inSimple, "Attempted to start a child in a simple node");

        // First write the type of node
        output.writeByte(nodeType);
        // Write Start Tag
        writeQNameInternal(arg.getNodeType());
    }

    private void writeObjSet(final Set<?> set) throws IOException {
        output.writeInt(set.size());
        for (Object o : set) {
            checkArgument(o instanceof String, "Expected value type to be String but was %s (%s)", o.getClass(), o);
            writeString((String) o);
        }
    }

    private void writeObject(final Object value) throws IOException {
        byte type = getSerializableType(value);
        // Write object type first
        output.writeByte(type);

        switch (type) {
            case ValueTypes.BOOL_TYPE:
                output.writeBoolean((Boolean) value);
                break;
            case ValueTypes.QNAME_TYPE:
                writeQNameInternal((QName) value);
                break;
            case ValueTypes.INT_TYPE:
                output.writeInt((Integer) value);
                break;
            case ValueTypes.BYTE_TYPE:
                output.writeByte((Byte) value);
                break;
            case ValueTypes.LONG_TYPE:
                output.writeLong((Long) value);
                break;
            case ValueTypes.SHORT_TYPE:
                output.writeShort((Short) value);
                break;
            case ValueTypes.BITS_TYPE:
                writeObjSet((Set<?>) value);
                break;
            case ValueTypes.BINARY_TYPE:
                byte[] bytes = (byte[]) value;
                output.writeInt(bytes.length);
                output.write(bytes);
                break;
            case ValueTypes.YANG_IDENTIFIER_TYPE:
                writeYangInstanceIdentifierInternal((YangInstanceIdentifier) value);
                break;
            case ValueTypes.EMPTY_TYPE:
                break;
            case ValueTypes.STRING_BYTES_TYPE:
                final byte[] valueBytes = value.toString().getBytes(StandardCharsets.UTF_8);
                output.writeInt(valueBytes.length);
                output.write(valueBytes);
                break;
            default:
                output.writeUTF(value.toString());
                break;
        }
    }

    private void writeKeyValueMap(final Set<Entry<QName, Object>> entrySet) throws IOException {
        if (!entrySet.isEmpty()) {
            output.writeInt(entrySet.size());
            for (Entry<QName, Object> entry : entrySet) {
                writeQNameInternal(entry.getKey());
                writeObject(entry.getValue());
            }
        } else {
            output.writeInt(0);
        }
    }

    private void writeString(final @NonNull String string) throws IOException {
        final Integer value = stringCodeMap.get(verifyNotNull(string));
        if (value == null) {
            stringCodeMap.put(string, stringCodeMap.size());
            writeByte(LithiumTokens.IS_STRING_VALUE);
            writeUTF(string);
        } else {
            writeByte(LithiumTokens.IS_CODE_VALUE);
            writeInt(value);
        }
    }

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
