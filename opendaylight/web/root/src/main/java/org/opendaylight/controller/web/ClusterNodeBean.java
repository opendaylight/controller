package org.opendaylight.controller.web;

import java.net.InetAddress;

/**
 * Information about a clustered controller to send to the UI frontend
 * @author andrekim
 */
public class ClusterNodeBean {
    private final byte[] address;
    private final String name;
    private final Boolean me;
    private final Boolean coordinator;
    private final Integer numConnectedNodes;

    public static class Builder {
        // required params
        private final byte[] address;
        private final String name;

        // optional params
        private Boolean me = null;
        private Boolean coordinator = null;
        private Integer numConnectedNodes = null;

        public Builder(InetAddress address) {
            this.address = address.getAddress();
            this.name = address.getHostAddress();
        }

        public Builder highlightMe() {
            this.me = true;
            return this;
        }

        public Builder iAmCoordinator() {
            this.coordinator = true;
            return this;
        }

        public Builder nodesConnected(int numNodes) {
            this.numConnectedNodes = numNodes;
            return this;
        }

        public ClusterNodeBean build() {
            return new ClusterNodeBean(this);
        }
    }

    private ClusterNodeBean(Builder builder) {
        this.address = builder.address;
        this.name = builder.name;
        this.me = builder.me;
        this.coordinator = builder.coordinator;
        this.numConnectedNodes = builder.numConnectedNodes;
    }
}