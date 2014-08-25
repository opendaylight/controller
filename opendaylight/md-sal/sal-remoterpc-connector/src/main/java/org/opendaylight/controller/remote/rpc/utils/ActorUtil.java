/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.controller.remote.rpc.utils;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class ActorUtil {
    public static final FiniteDuration LOCAL_ASK_DURATION = Duration.create(2, TimeUnit.SECONDS);
    public static final FiniteDuration REMOTE_ASK_DURATION = Duration.create(15, TimeUnit.SECONDS);
    public static final FiniteDuration ASK_DURATION = Duration.create(17, TimeUnit.SECONDS);
    public static final FiniteDuration GOSSIP_TICK_INTERVAL = Duration.create(500, TimeUnit.MILLISECONDS);
    public static final String MAILBOX = "bounded-mailbox";
}
