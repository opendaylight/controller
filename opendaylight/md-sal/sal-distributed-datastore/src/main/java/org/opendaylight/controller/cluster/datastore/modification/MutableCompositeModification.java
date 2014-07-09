/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MutableCompositeModification is just a mutable version of a
 * CompositeModification {@link org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification#addModification(Modification)}
 */
public class MutableCompositeModification
    implements CompositeModification {

    private static final long serialVersionUID = 1163377899140186790L;

    private final List<Modification> modifications = new ArrayList<>();

    @Override
    public void apply(DOMStoreWriteTransaction transaction) {
        for (Modification modification : modifications) {
            modification.apply(transaction);
        }
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

    public List<Modification> getModifications() {
        return Collections.unmodifiableList(modifications);
    }

    @Override public Object toSerializable() {
        PersistentMessages.CompositeModification.Builder builder =
            PersistentMessages.CompositeModification.newBuilder();

        for (Modification m : modifications) {
            builder.addModification(
                (PersistentMessages.Modification) m.toSerializable());
        }

        return builder.build();
    }

    public static MutableCompositeModification fromSerializable(Object serializable, SchemaContext schemaContext){
        PersistentMessages.CompositeModification o = (PersistentMessages.CompositeModification) serializable;
        MutableCompositeModification compositeModification = new MutableCompositeModification();

        for(PersistentMessages.Modification m : o.getModificationList()){
            if(m.getType().equals(DeleteModification.class.toString())){
                compositeModification.addModification(DeleteModification.fromSerializable(m));
            } else if(m.getType().equals(WriteModification.class.toString())){
                compositeModification.addModification(WriteModification.fromSerializable(m, schemaContext));
            } else if(m.getType().equals(MergeModification.class.toString())){
                compositeModification.addModification(MergeModification.fromSerializable(m, schemaContext));
            }
        }

        return compositeModification;
    }
}
