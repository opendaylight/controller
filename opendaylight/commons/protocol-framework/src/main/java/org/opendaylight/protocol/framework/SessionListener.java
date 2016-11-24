/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.util.EventListener;

/**
 * Listener that receives session state information. This interface should be
 * implemented by a protocol specific abstract class, that is extended by
 * a final class that implements the methods.
 */
@Deprecated
public interface SessionListener<M, S extends ProtocolSession<?>, T extends TerminationReason> extends EventListener {
    /**
     * Fired when the session was established successfully.
     *
     * @param session New session
     */
    void onSessionUp(S session);

    /**
     * Fired when the session went down because of an IO error. Implementation should take care of closing underlying
     * session.
     *
     * @param session that went down
     * @param e Exception that was thrown as the cause of session being down
     */
    void onSessionDown(S session, Exception e);

    /**
     * Fired when the session is terminated locally. The session has already been closed and transitioned to IDLE state.
     * Any outstanding queued messages were not sent. The user should not attempt to make any use of the session.
     *
     * @param reason the cause why the session went down
     */
    void onSessionTerminated(S session, T reason);

    /**
     * Fired when a normal protocol message is received.
     *
     * @param message Protocol message
     */
    void onMessage(S session, M message);
}
