/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static java.util.Objects.requireNonNull;

/**
 * A record of a {@code writerUuid} being effective as of some {@code sequenceNr}.
 */
record UuidEntry(String writerUuid, long sequenceNr) {
    UuidEntry {
        requireNonNull(writerUuid);
    }
}
