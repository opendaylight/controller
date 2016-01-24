/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.protobuff.client.messages;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;

@Deprecated
public class CompositeModificationPayload extends Payload implements
    Serializable {

    private final PersistentMessages.CompositeModification modification;

    public CompositeModificationPayload(){
        modification = null;
    }
    public CompositeModificationPayload(Object modification){
        this.modification = (PersistentMessages.CompositeModification) Preconditions.checkNotNull(modification, "modification should not be null");
    }

    public Object getModification(){
        return this.modification;
    }

    @Override
    public int size(){
        return this.modification.getSerializedSize();
    }
}
