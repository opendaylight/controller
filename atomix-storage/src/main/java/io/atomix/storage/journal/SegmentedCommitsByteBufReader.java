/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import io.netty.buffer.ByteBuf;

/**
 * A {@link ByteBufReader} traversing only committed entries.
 */
final class SegmentedCommitsByteBufReader extends SegmentedByteBufReader {
    SegmentedCommitsByteBufReader(final SegmentedByteBufJournal journal, final JournalSegment segment) {
        super(journal, segment);
    }

    @Override
    ByteBuf tryAdvance(final long index) {
        return index <= journal.getCommitIndex() ? super.tryAdvance(index) : null;
    }
}