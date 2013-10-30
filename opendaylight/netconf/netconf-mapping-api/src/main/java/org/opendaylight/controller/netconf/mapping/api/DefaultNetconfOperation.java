package org.opendaylight.controller.netconf.mapping.api;

import org.opendaylight.controller.netconf.api.NetconfSession;

public interface DefaultNetconfOperation {
    void setNetconfSession(NetconfSession s);
}
