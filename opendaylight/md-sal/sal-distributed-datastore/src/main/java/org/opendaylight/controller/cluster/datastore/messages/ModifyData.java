/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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

public abstract class ModifyData implements Externalizable {
    private static final long serialVersionUID = 1L;

    private YangInstanceIdentifier path;
    private NormalizedNode<?, ?> data;
    private short version;

    protected ModifyData() {
    }

    protected ModifyData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        this.path = path;
        this.data = data;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }

    public short getVersion() {
        return version;
    }

    protected void setVersion(short version) {
        this.version = version;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        version = in.readShort();
        SerializationUtils.deserializePathAndNode(in, this, APPLIER);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(version);
        SerializationUtils.serializePathAndNode(path, data, out);
    }

    private static final Applier<ModifyData> APPLIER = new Applier<ModifyData>() {
        @Override
        public void apply(ModifyData instance, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
            instance.path = path;
            instance.data = data;
        }
    };
}
