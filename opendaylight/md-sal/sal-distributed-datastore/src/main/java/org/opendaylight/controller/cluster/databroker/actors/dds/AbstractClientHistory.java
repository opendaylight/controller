/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;

// TODO: this looks like a cache
abstract class AbstractClientHistory implements Identifiable<LocalHistoryIdentifier> {
    private final Map<Long, LocalHistoryIdentifier> histories = new HashMap<>();
    private final LocalHistoryIdentifier identifier;

    protected AbstractClientHistory(final LocalHistoryIdentifier identifier) {
        this.identifier = Preconditions.checkNotNull(identifier);
        Preconditions.checkArgument(identifier.getCookie() == 0);
    }

    final LocalHistoryIdentifier getHistoryForCookie(final Long cookie) {
        LocalHistoryIdentifier ret = histories.get(cookie);
        if (ret == null) {
            ret = new LocalHistoryIdentifier(identifier.getClientId(), identifier.getHistoryId(), cookie);
            histories.put(cookie, ret);
        }

        return ret;
    }

    @Override
    public final LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    final ActorRef getReplyTo() {
        // TODO Auto-generated method stub
        return null;
    }
}
