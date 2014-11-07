/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.utils;

import akka.actor.ActorRef;
import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConditionalProbe {
    private final ActorRef actorRef;
    private final Predicate<Object> predicate;
    Logger log = LoggerFactory.getLogger(ConditionalProbe.class);

    public ConditionalProbe(ActorRef actorRef, Predicate<Object> predicate) {
        this.actorRef = actorRef;
        this.predicate = predicate;
    }

    public void tell(Object message, ActorRef sender){
        if(predicate.apply(message)) {
            log.info("sending message to probe {}", message);
            actorRef.tell(message, sender);
        }
    }
}
