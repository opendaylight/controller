/*
 * Copyright (c) 2016 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.messages;

import java.io.Serializable;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

/**
 * Message sent by an EntityOwnershipShard to its leader on startup to remove all its candidates.
 *
 * @author Thomas Pantelis
 */
public class RemoveAllCandidates implements Serializable {
    private static final long serialVersionUID = 1L;

    private final MemberName memberName;

    public RemoveAllCandidates(final MemberName memberName) {
        this.memberName = memberName;
    }

    public MemberName getMemberName() {
        return memberName;
    }

    @Override
    public String toString() {
        return "RemoveAllCandidates [memberName=" + memberName + "]";
    }
}
