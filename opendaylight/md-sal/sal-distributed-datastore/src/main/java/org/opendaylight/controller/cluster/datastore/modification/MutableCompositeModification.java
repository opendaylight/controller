/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.messages.VersionedExternalizableMessage;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * MutableCompositeModification is just a mutable version of a CompositeModification.
 */
public class MutableCompositeModification extends VersionedExternalizableMessage implements CompositeModification {
    private static final long serialVersionUID = 1L;

    private final List<Modification> modifications = new ArrayList<>();
    private List<Modification> immutableModifications = null;

    public MutableCompositeModification() {
        this(DataStoreVersions.CURRENT_VERSION);
    }

    public MutableCompositeModification(short version) {
        super(version);
    }

    @Override
    public void apply(DOMStoreWriteTransaction transaction) {
        for (Modification modification : modifications) {
            modification.apply(transaction);
        }
    }

    @Override
    public void apply(DataTreeModification transaction) {
        for (Modification modification : modifications) {
            modification.apply(transaction);
        }
    }

    @Override
    public byte getType() {
        return COMPOSITE;
    }

    /**
     * Add a new Modification to the list of Modifications represented by this composite.
     *
     * @param modification the modification to add.
     */
    public void addModification(Modification modification) {
        Preconditions.checkNotNull(modification);
        modifications.add(modification);
    }

    public void addModifications(Iterable<Modification> newMods) {
        for (Modification mod : newMods) {
            addModification(mod);
        }
    }

    @Override
    public List<Modification> getModifications() {
        if (immutableModifications == null) {
            immutableModifications = Collections.unmodifiableList(modifications);
        }

        return immutableModifications;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        int size = in.readInt();

        if (size > 1) {
            SerializationUtils.REUSABLE_READER_TL.set(new NormalizedNodeInputStreamReader(in));
        }

        try {
            for (int i = 0; i < size; i++) {
                byte type = in.readByte();
                switch (type) {
                    case Modification.WRITE:
                        modifications.add(WriteModification.fromStream(in, getVersion()));
                        break;

                    case Modification.MERGE:
                        modifications.add(MergeModification.fromStream(in, getVersion()));
                        break;

                    case Modification.DELETE:
                        modifications.add(DeleteModification.fromStream(in, getVersion()));
                        break;
                    default:
                        break;
                }
            }
        } finally {
            SerializationUtils.REUSABLE_READER_TL.remove();
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeInt(modifications.size());

        if (modifications.size() > 1) {
            SerializationUtils.REUSABLE_WRITER_TL.set(NormalizedNodeInputOutput.newDataOutput(out));
        }

        try {
            for (Modification mod: modifications) {
                out.writeByte(mod.getType());
                mod.writeExternal(out);
            }
        } finally {
            SerializationUtils.REUSABLE_WRITER_TL.remove();
        }
    }

    public static MutableCompositeModification fromSerializable(Object serializable) {
        Preconditions.checkArgument(serializable instanceof MutableCompositeModification);
        return (MutableCompositeModification)serializable;
    }
}
