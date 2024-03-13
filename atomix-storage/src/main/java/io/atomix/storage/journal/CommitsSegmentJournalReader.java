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

import java.util.NoSuchElementException;

/**
 * A {@link JournalReader} traversing only committed entries.
 */
final class CommitsSegmentJournalReader<E> extends SegmentedJournalReader<E> {
    CommitsSegmentJournalReader(SegmentedJournal<E> journal, JournalSegment<E> segment) {
        super(journal, segment);
    }

    @Override
    public boolean hasNext() {
        return isNextCommited() && super.hasNext();
    }

    @Override
    public Indexed<E> next() {
        if (isNextCommited()) {
            return super.next();
        }
        throw new NoSuchElementException();
    }

    private boolean isNextCommited() {
        return getNextIndex() <= journal.getCommitIndex();
    }
}
