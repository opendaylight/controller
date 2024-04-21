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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link JournalReader} traversing only committed entries.
 */
@NonNullByDefault
final class CommitsSegmentJournalReader<E> extends SegmentedJournalReader<E> {
    CommitsSegmentJournalReader(final SegmentedJournal<E> journal, final JournalSegment segment) {
        super(journal, segment);
    }

    @Override
    public <T> T tryNext(final EntryMapper<E, T> mapper) {
        return getNextIndex() <= journal.getCommitIndex() ? super.tryNext(mapper) : null;
    }
}
