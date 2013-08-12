/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core;

import java.net.SocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFStatisticsRequest;

/**
 * This interface defines an abstraction of an Open Flow Switch.
 *
 */
public interface IEnhancedSwitch extends ISwitch {
    public void startHandler();

    public void shutDownHandler();

    public void handleMessage(OFMessage ofMessage);

    public void flushBufferedMessages();

    public SocketAddress getRemoteAddress();

    public SocketAddress getLocalAddress();
}
