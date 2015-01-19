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
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils.Applier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * An Externalizable version of MergeData used for efficient serialization in lieu of protobuff.
 *
 * @author Thomas Pantelis
 */
public class ExternalizableMergeData implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient YangInstanceIdentifier path;
    private transient NormalizedNode<?, ?> node;
    private transient short version;

    public ExternalizableMergeData() {
    }

    public ExternalizableMergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> node,
            short version) {
        this.path = path;
        this.node = node;
        this.version = version;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public NormalizedNode<?, ?> getNode() {
        return node;
    }

    public short getVersion() {
        return version;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        version = in.readShort(); // Read the version - don't need to do anything with it now
        SerializationUtils.deserializePathAndNode(in, this, APPLIER);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(version);
        SerializationUtils.serializePathAndNode(path, node, out);
    }

    private static final Applier<ExternalizableMergeData> APPLIER =
                                                           new Applier<ExternalizableMergeData>() {
        @Override
        public void apply(ExternalizableMergeData instance, YangInstanceIdentifier path,
                NormalizedNode<?, ?> node) {
            instance.path = path;
            instance.node = node;
        }
    };
}
