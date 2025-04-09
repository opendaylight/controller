/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.annotations.VisibleForTesting;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Prototype for a method which applies the command contained in a {@link LogEntry}.
 */
@NonNullByDefault
@VisibleForTesting
@FunctionalInterface
public interface ApplyEntryMethod {

    void applyEntry(@Nullable Identifier identifier, LogEntry entry);
}
