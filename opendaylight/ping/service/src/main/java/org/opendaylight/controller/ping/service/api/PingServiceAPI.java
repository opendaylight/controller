package org.opendaylight.controller.ping.service.api;


public interface PingServiceAPI {

    /**
     * pingDestination
     *
     * @param address An IPv4 address to be pinged
     * @return True if address is reachable,
     * false if address is unreachable or error occurs.
     */
    boolean pingDestination(String address);
}

