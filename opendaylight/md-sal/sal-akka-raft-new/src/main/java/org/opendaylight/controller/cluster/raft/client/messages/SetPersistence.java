package org.opendaylight.controller.cluster.raft.client.messages;

import java.io.Serializable;

/**
 * Created by django on 7/1/16.
 */
public class SetPersistence implements Serializable {
    private final boolean persistence;

    public SetPersistence(boolean persistence) {
        this.persistence = persistence;
    }

    public boolean persistence() {
        return persistence;
    }
}
