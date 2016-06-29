package org.opendaylight.controller.cluster.raft.client.messages;

import java.io.Serializable;

public class SetPersistence implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean persistence;

    public SetPersistence(boolean persistence) {
        this.persistence = persistence;
    }

    public boolean persistence() {
        return persistence;
    }
}
