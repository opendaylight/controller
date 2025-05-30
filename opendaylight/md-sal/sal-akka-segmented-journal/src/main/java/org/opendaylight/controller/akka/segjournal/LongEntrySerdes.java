/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import io.atomix.storage.journal.JournalSerdes.EntryInput;
import io.atomix.storage.journal.JournalSerdes.EntryOutput;
import io.atomix.storage.journal.JournalSerdes.EntrySerdes;
import java.io.IOException;

@Deprecated(since = "11.0.0", forRemoval = true)
enum LongEntrySerdes implements EntrySerdes<Long> {
    LONG_ENTRY_SERDES {
        @Override
        public Long read(final EntryInput input) throws IOException {
            return input.readLong();
        }

        @Override
        public void write(final EntryOutput output, final Long entry) throws IOException {
            output.writeLong(entry);
        }
    }
}
