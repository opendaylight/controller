package org.opendaylight.controller.cluster.raft.client.messages;

/**
 * Created by kramesha on 7/17/14.
 */
public class AddRaftPeer {

    private String name;
    private String address;

    public AddRaftPeer(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
