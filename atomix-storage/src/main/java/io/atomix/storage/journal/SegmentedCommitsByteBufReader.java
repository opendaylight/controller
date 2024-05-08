/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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