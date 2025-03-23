/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.notifications;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A local message initiated internally from the RaftActor when some state of a leader has changed.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public sealed interface LeaderStateChanged extends MemberNotication
        permits DefaultLeaderStateChanged, ForwadingLeaderStateChanged {

    @Nullable String leaderId();

    short leaderPayloadVersion();
}
