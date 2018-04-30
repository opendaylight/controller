/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.xpath.XPathBooleanResult;
import org.opendaylight.yangtools.yang.data.api.schema.xpath.XPathNodesetResult;
import org.opendaylight.yangtools.yang.data.api.schema.xpath.XPathNumberResult;
import org.opendaylight.yangtools.yang.data.api.schema.xpath.XPathResult;
import org.opendaylight.yangtools.yang.data.api.schema.xpath.XPathStringResult;

/**
 * Reply to any of the {@link AbstractEvaluateXPath} requests.
 *
 * @author Robert Varga
 */
@Beta
@NonNullByDefault
public final class EvaluateXPathReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;
    private static final byte BOOLEAN_FALSE = 0;
    private static final byte BOOLEAN_TRUE = 1;
    private static final byte NUMBER = 2;
    private static final byte STRING = 3;
    private static final byte NODESET = 4;

    private @Nullable XPathResult<?> result;

    public EvaluateXPathReply() {
        // For Externalizable
    }

    public EvaluateXPathReply(final @Nullable XPathResult<?> result, final short version) {
        super(version);
        this.result = result;
    }

    public EvaluateXPathReply(final @Nullable XPathResult<?> result) {
        this.result = result;
    }

    public Optional<? extends XPathResult<?>> getResult() {
        return Optional.ofNullable(result);
    }

    @Override
    public void readExternal(final @Nullable ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        final byte type = in.readByte();
        switch (type) {
            case BOOLEAN_TRUE:
                result = (XPathBooleanResult) () -> Boolean.TRUE;
                break;
            case BOOLEAN_FALSE:
                result = (XPathBooleanResult) () -> Boolean.FALSE;
                break;
            case NUMBER:
                final Number number = (Number) in.readObject();
                result = (XPathNumberResult) () -> number;
                break;
            case STRING:
                final String str = (String) in.readObject();
                result = (XPathStringResult) () -> str;
                break;
            case NODESET:
                final int size = in.readInt();
                final Builder<Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> b =
                        ImmutableList.builderWithExpectedSize(size);
                for (int i = 0; i < size; ++i) {
                    final YangInstanceIdentifier path = SerializationUtils.deserializePath(in);
                    final NormalizedNode<?, ?> node = SerializationUtils.deserializeNormalizedNode(in);
                    b.add(new SimpleImmutableEntry<>(path, node));
                }

                final Collection<Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> res = b.build();
                result = (XPathNodesetResult) () -> res;
                break;
            default:
                throw new IOException("Unrecognized result type " + type);
        }
    }

    @Override
    public void writeExternal(final @Nullable ObjectOutput out) throws IOException {
        super.writeExternal(out);

        if (result instanceof XPathBooleanResult) {
            out.writeByte(Boolean.TRUE.equals(result.getValue()) ? BOOLEAN_TRUE : BOOLEAN_FALSE);
        } else if (result instanceof XPathNumberResult) {
            out.writeByte(NUMBER);
            out.writeObject(result.getValue());
        } else if (result instanceof XPathStringResult) {
            out.writeByte(STRING);
            out.writeObject(result.getValue());
        } else if (result instanceof XPathNodesetResult) {
            final Collection<Entry<YangInstanceIdentifier, NormalizedNode<?, ?>>> value =
                    ((XPathNodesetResult) result).getValue();
            out.writeByte(NODESET);
            out.writeInt(value.size());
            value.forEach(e -> SerializationUtils.serializePathAndNode(e.getKey(), e.getValue(), out));
        } else {
            throw new IllegalStateException("Unhandled result " + result);
        }
    }
}
