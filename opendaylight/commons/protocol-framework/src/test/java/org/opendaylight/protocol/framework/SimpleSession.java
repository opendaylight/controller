/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

public final class SimpleSession extends AbstractProtocolSession<SimpleMessage> {

    public SimpleSession() {
    }

    @Override
    public void close() {
    }

    @Override
    public void handleMessage(final SimpleMessage msg) {
    }

    @Override
    public void endOfInput() {
    }

    @Override
    protected void sessionUp() {
    }
}
