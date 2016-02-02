/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ReadDataReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private NormalizedNode<?, ?> normalizedNode;

    public ReadDataReply() {
    }

    public ReadDataReply(NormalizedNode<?, ?> normalizedNode, short version) {
        super(version);
        this.normalizedNode = normalizedNode;
    }

    public NormalizedNode<?, ?> getNormalizedNode() {
        return normalizedNode;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        normalizedNode = SerializationUtils.deserializeNormalizedNode(in);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        SerializationUtils.serializeNormalizedNode(normalizedNode, out);
    }

    @Override
    protected Object newLegacySerializedInstance() {
        // no legacy serialized type for this class; return self
        return this;
    }

    public static ReadDataReply fromSerializable(Object serializable) {
        return (ReadDataReply) serializable;
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof ReadDataReply;
    }
}
