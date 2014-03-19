/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util;

import org.opendaylight.controller.netconf.api.NetconfMessage;

/**
 * Session capable of exi communication according to http://tools.ietf.org/html/draft-varga-netconf-exi-capability-02
 */
public interface NetconfExiSession {

    /**
     * Start exi communication with parameters included in start-exi message
     */
    void startExiCommunication(NetconfMessage startExiMessage);

    /**
     * Stop exi communication, initiated by stop-exi message
     */
    void stopExiCommunication();
}
