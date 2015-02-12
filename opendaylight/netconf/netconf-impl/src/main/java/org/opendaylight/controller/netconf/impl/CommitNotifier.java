/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import java.util.Set;
import org.w3c.dom.Element;

public interface CommitNotifier {
    void sendCommitNotification(String message, Element cfgSnapshot, Set<String> capabilities);

    public static final class NoopCommitNotifier implements CommitNotifier {

        private static final CommitNotifier INSTANCE = new NoopCommitNotifier();

        private NoopCommitNotifier() {}

        public static CommitNotifier getInstance() {
            return INSTANCE;
        }

        @Override
        public void sendCommitNotification(final String message, final Element cfgSnapshot, final Set<String> capabilities) {
            // NOOP
        }
    }
}
