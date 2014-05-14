package org.opendaylight.controller.ping.northbound;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class PingNorthboundRSApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(PingNorthbound.class);
        return classes;
    }
}
