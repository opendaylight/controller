package org.opendaylight.controller.cluster.raft.client.messages;

/**
 * Created by kramesha on 7/17/14.
 */
public class RemoveRaftPeer {
    private String name;

    public RemoveRaftPeer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
