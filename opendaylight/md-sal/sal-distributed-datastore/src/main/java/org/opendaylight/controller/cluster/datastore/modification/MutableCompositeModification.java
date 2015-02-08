/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputStreamWriter;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

/**
 * MutableCompositeModification is just a mutable version of a
 * CompositeModification {@link org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification#addModification(Modification)}
 */
public class MutableCompositeModification implements CompositeModification {
    private static final long serialVersionUID = 1L;

    private final List<Modification> modifications = new ArrayList<>();
    private short version;

    public MutableCompositeModification() {
        this(DataStoreVersions.CURRENT_VERSION);
    }

    public MutableCompositeModification(short version) {
        this.version = version;
    }

    @Override
    public void apply(DOMStoreWriteTransaction transaction) {
        for (Modification modification : modifications) {
            modification.apply(transaction);
        }
    }

    @Override
    public byte getType() {
        return COMPOSITE;
    }

    public short getVersion() {
        return version;
    }

    public void setVersion(short version) {
        this.version = version;
    }

    /**
     * Add a new Modification to the list of Modifications represented by this
     * composite
     *
     * @param modification
     */
    public void addModification(Modification modification) {
        modifications.add(modification);
    }

    @Override
    public List<Modification> getModifications() {
        return modifications;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        version = in.readShort();

        int size = in.readInt();

        if(size > 1) {
            SerializationUtils.REUSABLE_READER_TL.set(new NormalizedNodeInputStreamReader(in));
        }

        try {
            for(int i = 0; i < size; i++) {
                byte type = in.readByte();
                switch(type) {
                case Modification.WRITE:
                    modifications.add(WriteModification.fromStream(in, version));
                    break;

                case Modification.MERGE:
                    modifications.add(MergeModification.fromStream(in, version));
                    break;

                case Modification.DELETE:
                    modifications.add(DeleteModification.fromStream(in, version));
                    break;
                }
            }
        } finally {
            SerializationUtils.REUSABLE_READER_TL.remove();
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(version);

        out.writeInt(modifications.size());

        if(modifications.size() > 1) {
            SerializationUtils.REUSABLE_WRITER_TL.set(new NormalizedNodeOutputStreamWriter(out));
        }

        try {
            for(Modification mod: modifications) {
                out.writeByte(mod.getType());
                mod.writeExternal(out);
            }
        } finally {
            SerializationUtils.REUSABLE_WRITER_TL.remove();
        }
    }

    @Override
    @Deprecated
    public Object toSerializable() {
        PersistentMessages.CompositeModification.Builder builder =
                PersistentMessages.CompositeModification.newBuilder();

        builder.setTimeStamp(System.nanoTime());

        for (Modification m : modifications) {
            builder.addModification((PersistentMessages.Modification) m.toSerializable());
        }

        return builder.build();
    }

    public static MutableCompositeModification fromSerializable(Object serializable) {
        if(serializable instanceof MutableCompositeModification) {
            return (MutableCompositeModification)serializable;
        } else {
            return fromLegacySerializable(serializable);
        }
    }

    private static MutableCompositeModification fromLegacySerializable(Object serializable) {
        PersistentMessages.CompositeModification o = (PersistentMessages.CompositeModification) serializable;
        MutableCompositeModification compositeModification = new MutableCompositeModification();

        for(PersistentMessages.Modification m : o.getModificationList()){
            if(m.getType().equals(DeleteModification.class.toString())){
                compositeModification.addModification(DeleteModification.fromSerializable(m));
            } else if(m.getType().equals(WriteModification.class.toString())){
                compositeModification.addModification(WriteModification.fromSerializable(m));
            } else if(m.getType().equals(MergeModification.class.toString())){
                compositeModification.addModification(MergeModification.fromSerializable(m));
            }
        }

        return compositeModification;
    }
}
