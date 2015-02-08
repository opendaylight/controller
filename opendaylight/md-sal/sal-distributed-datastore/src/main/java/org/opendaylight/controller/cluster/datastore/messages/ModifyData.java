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
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils.Applier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * @deprecated Replaced by BatchedModifications.
 */
@Deprecated
public abstract class ModifyData extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private YangInstanceIdentifier path;
    private NormalizedNode<?, ?> data;

    protected ModifyData() {
    }

    protected ModifyData(YangInstanceIdentifier path, NormalizedNode<?, ?> data, short version) {
        super(version);
        this.path = path;
        this.data = data;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        SerializationUtils.deserializePathAndNode(in, this, APPLIER);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
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
