/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.ChannelInboundHandler;

/**
 * Session negotiator concepts. A negotiator is responsible for message
 * handling while the exact session parameters are not known. Once the
 * session parameters are finalized, the negotiator replaces itself in
 * the channel pipeline with the session.
 *
 * @param <T> Protocol session type.
 */
@Deprecated
public interface SessionNegotiator<T extends ProtocolSession<?>> extends ChannelInboundHandler {

}
