/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.modification;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.messages.VersionedExternalizableMessage;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.stream.ReusableStreamReceiver;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;
import org.opendaylight.yangtools.yang.data.impl.schema.ReusableImmutableNormalizedNodeStreamWriter;

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

    public MutableCompositeModification(final short version) {
        super(version);
    }

    @Override
    public void apply(final DOMStoreWriteTransaction transaction) {
        for (Modification modification : modifications) {
            modification.apply(transaction);
        }
    }

    @Override
    public void apply(final DataTreeModification transaction) {
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
    public void addModification(final Modification modification) {
        modifications.add(requireNonNull(modification));
    }

    public void addModifications(final Iterable<Modification> newMods) {
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
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        int size = in.readInt();
        if (size > 0) {
            final NormalizedNodeDataInput input = NormalizedNodeDataInput.newDataInputWithoutValidation(in);
            final ReusableStreamReceiver receiver = ReusableImmutableNormalizedNodeStreamWriter.create();

            for (int i = 0; i < size; i++) {
                byte type = in.readByte();
                switch (type) {
                    case Modification.WRITE:
                        modifications.add(WriteModification.fromStream(input, getVersion(), receiver));
                        break;

                    case Modification.MERGE:
                        modifications.add(MergeModification.fromStream(input, getVersion(), receiver));
                        break;

                    case Modification.DELETE:
                        modifications.add(DeleteModification.fromStream(input, getVersion()));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);

        final int size = modifications.size();
        out.writeInt(size);
        if (size > 0) {
            try (NormalizedNodeDataOutput stream = getStreamVersion().newDataOutput(out)) {
                for (Modification mod : modifications) {
                    out.writeByte(mod.getType());
                    mod.writeTo(stream);
                }
            }
        }
    }

    public static MutableCompositeModification fromSerializable(final Object serializable) {
        checkArgument(serializable instanceof MutableCompositeModification);
        return (MutableCompositeModification)serializable;
    }

    @Override
    public void writeTo(final NormalizedNodeDataOutput out) throws IOException {
        throw new UnsupportedOperationException();
    }
}
