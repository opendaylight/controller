/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * An Externalizable version of ReadDataReply used for efficient serialization in lieu of protobuff.
 *
 * @author Thomas Pantelis
 */
public class ExternalizableReadDataReply implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient NormalizedNode<?, ?> normalizedNode;
    private transient int serializedVersion;

    public ExternalizableReadDataReply() {
    }

    public ExternalizableReadDataReply(NormalizedNode<?, ?> normalizedNode, int serializedVersion) {
        this.normalizedNode = normalizedNode;
        this.serializedVersion = serializedVersion;
    }

    public NormalizedNode<?, ?> getNormalizedNode() {
        return normalizedNode;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        in.readShort(); // Read the version - don't need to do anything with it now
        normalizedNode = SerializationUtils.deserializeNormalizedNode(in);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(serializedVersion);
        SerializationUtils.serializeNormalizedNode(normalizedNode, out);
    }
}
