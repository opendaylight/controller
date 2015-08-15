/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

/**
 * Message sent to a shard actor indicating one of its peers is up.
 *
 * @author Thomas Pantelis
 */
public class PeerUp {
    private final String memberName;
    private final String peerId;

    public PeerUp(String memberName, String peerId) {
        this.memberName = memberName;
        this.peerId = peerId;
    }

    public String getMemberName() {
        return memberName;
    }


    public String getPeerId() {
        return peerId;
    }

    @Override
    public String toString() {
        return "PeerUp [memberName=" + memberName + ", peerId=" + peerId + "]";
    }
}