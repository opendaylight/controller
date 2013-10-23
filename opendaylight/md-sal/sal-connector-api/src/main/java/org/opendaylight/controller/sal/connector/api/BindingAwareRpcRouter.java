package org.opendaylight.controller.sal.connector.api;

import java.util.concurrent.Future;

import org.opendaylight.yangtools.concepts.Immutable;

public interface BindingAwareRpcRouter extends RpcRouter<String, String, String, byte[]> {

    @Override
    public Future<org.opendaylight.controller.sal.connector.api.RpcRouter.RpcReply<byte[]>> sendRpc(
            RpcRequest<String, String, String, byte[]> input);

    class BindingAwareRequest implements RpcRequest<String, String, String, byte[]>, Immutable {

        private final BindingAwareRouteIdentifier routingInformation;
        private final byte[] payload;

        public BindingAwareRequest(BindingAwareRouteIdentifier routingInformation, byte[] payload) {
            super();
            this.routingInformation = routingInformation;
            this.payload = payload;
        }

        public BindingAwareRouteIdentifier getRoutingInformation() {
            return this.routingInformation;
        }

        @Override
        public byte[] getPayload() {
            return payload;
        }
    }

    class BindingAwareRouteIdentifier implements RouteIdentifier<String, String, String>, Immutable {

        private final String type;
        private final String route;
        private final String content;

        public BindingAwareRouteIdentifier(String type, String route, String content) {
            super();
            this.type = type;
            this.route = route;
            this.content = content;
        }

        /**
         * Java class name of Rpc Context
         * 
         * 
         */
        @Override
        public String getContext() {
            return this.content;
        }

        /**
         * String representation of route e.g. node-id
         * 
         */
        @Override
        public String getRoute() {
            return this.route;
        }

        /**
         * Java class name of Rpc Type e.g org.opendaylight.AddFlowInput
         * 
         */
        @Override
        public String getType() {
            return this.type;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((content == null) ? 0 : content.hashCode());
            result = prime * result + ((route == null) ? 0 : route.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            BindingAwareRouteIdentifier other = (BindingAwareRouteIdentifier) obj;
            if (content == null) {
                if (other.content != null)
                    return false;
            } else if (!content.equals(other.content))
                return false;
            if (route == null) {
                if (other.route != null)
                    return false;
            } else if (!route.equals(other.route))
                return false;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            return true;
        }

    }

}
