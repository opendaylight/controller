/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.access.concepts.MemberName;

/**
 * Message sent to a shard actor indicating one of its peers is down.
 *
 * @author Thomas Pantelis
 */
public class PeerDown {
    private final MemberName memberName;
    private final String peerId;

    public PeerDown(MemberName memberName, String peerId) {
        this.memberName = memberName;
        this.peerId = peerId;
    }

    public MemberName getMemberName() {
        return memberName;
    }


    public String getPeerId() {
        return peerId;
    }

    @Override
    public String toString() {
        return "PeerDown [memberName=" + memberName.getName() + ", peerId=" + peerId + "]";
    }
}
