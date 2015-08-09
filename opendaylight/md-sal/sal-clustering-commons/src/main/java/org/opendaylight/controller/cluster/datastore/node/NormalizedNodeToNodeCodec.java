/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node;

import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializer;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializer.DeSerializer;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializer.Serializer;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Container;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NormalizedNodeToNodeCodec {
    public interface Encoded {
        NormalizedNodeMessages.Container getEncodedNode();

        NormalizedNodeMessages.InstanceIdentifier getEncodedPath();
    }

    public interface Decoded {
        NormalizedNode<?,?> getDecodedNode();

        YangInstanceIdentifier getDecodedPath();
    }

    private final SchemaContext ctx;

    public NormalizedNodeToNodeCodec(final SchemaContext ctx){
        this.ctx = ctx;
    }

    public NormalizedNodeMessages.Container encode(NormalizedNode<?,?> node){
        return encode(null, node).getEncodedNode();
    }

    public Encoded encode(YangInstanceIdentifier path, NormalizedNode<?,?> node) {

        NormalizedNodeMessages.InstanceIdentifier serializedPath = null;

        NormalizedNodeMessages.Container.Builder builder = NormalizedNodeMessages.Container.newBuilder();

        // Note: parent path is no longer used
        builder.setParentPath("");

        if(node != null) {
            if(path == null) {
                builder.setNormalizedNode(NormalizedNodeSerializer.serialize(node));
            } else {
                Serializer serializer = NormalizedNodeSerializer.newSerializer(node);
                builder.setNormalizedNode(serializer.serialize(path));
                serializedPath = serializer.getSerializedPath();
            }
        }

        return new EncodedImpl(builder.build(), serializedPath);
    }


    public NormalizedNode<?,?> decode(NormalizedNodeMessages.Node node){
        return decode(null, node).getDecodedNode();
    }

    public Decoded decode(NormalizedNodeMessages.InstanceIdentifier path,
            NormalizedNodeMessages.Node node) {
        if(node.getIntType() < 0 || node.getSerializedSize() == 0){
            return new DecodedImpl(null, null);
        }

        DeSerializer deSerializer = NormalizedNodeSerializer.newDeSerializer(path, node);
        NormalizedNode<?,?> decodedNode = deSerializer.deSerialize();
        return new DecodedImpl(decodedNode, deSerializer.getDeserializedPath());
    }

    private static class DecodedImpl implements Decoded {

        private final NormalizedNode<?, ?> decodedNode;
        private final YangInstanceIdentifier decodedPath;

        public DecodedImpl(NormalizedNode<?, ?> decodedNode, YangInstanceIdentifier decodedPath) {
            this.decodedNode = decodedNode;
            this.decodedPath = decodedPath;
        }

        @Override
        public NormalizedNode<?, ?> getDecodedNode() {
            return decodedNode;
        }

        @Override
        public YangInstanceIdentifier getDecodedPath() {
            return decodedPath;
        }
    }

    private static class EncodedImpl implements Encoded {

        private final Container encodedNode;
        private final InstanceIdentifier encodedPath;

        EncodedImpl(Container encodedNode, InstanceIdentifier encodedPath) {
            this.encodedNode = encodedNode;
            this.encodedPath = encodedPath;
        }

        @Override
        public Container getEncodedNode() {
            return encodedNode;
        }

        @Override
        public InstanceIdentifier getEncodedPath() {
            return encodedPath;
        }
    }
}
