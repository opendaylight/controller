/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

public abstract class DataListenerState {
    private static final class Initial extends DataListenerState {
        Initial() {
            super(new ArrayList<>(1));
        }

        @Override
        public long changeCount() {
            return 0;
        }

        @Override
        public Optional<NormalizedNode<?, ?>> lastData() {
            throw new UnsupportedOperationException();
        }

        @Override
        Optional<DataListenerViolation> validate(final long sequence, final ModificationType type,
                final Optional<NormalizedNode<?, ?>> beforeOpt, final Optional<NormalizedNode<?, ?>> afterOpt) {
            return beforeOpt.transform(before -> new DataListenerViolation(sequence, null, before));
        }

    }

    private abstract static class Subsequent extends DataListenerState {
        private final long changeCount;

        Subsequent(final List<DataListenerViolation> violations, final long changeCount) {
            super(violations);
            this.changeCount = changeCount;
        }

        @Override
        public final long changeCount() {
            return changeCount;
        }
    }

    private static final class Absent extends Subsequent {
        Absent(final List<DataListenerViolation> violations, final long changeCount) {
            super(violations, changeCount);
        }

        @Override
        public Optional<NormalizedNode<?, ?>> lastData() {
            return Optional.absent();
        }

        @Override
        Optional<DataListenerViolation> validate(final long sequence, final ModificationType type,
                final Optional<NormalizedNode<?, ?>> beforeOpt, final Optional<NormalizedNode<?, ?>> afterOpt) {
            return beforeOpt.transform(before -> new DataListenerViolation(sequence, null, before));
        }
    }

    private static final class Present extends Subsequent {
        private final NormalizedNode<?, ?> data;

        Present(final List<DataListenerViolation> violations, final long changeCount, final NormalizedNode<?, ?> data) {
            super(violations, changeCount);
            this.data = checkNotNull(data);
        }

        @Override
        public Optional<NormalizedNode<?, ?>> lastData() {
            return Optional.of(data);
        }

        @Override
        Optional<DataListenerViolation> validate(final long sequence, final ModificationType type,
                final Optional<NormalizedNode<?, ?>> beforeOpt, final Optional<NormalizedNode<?, ?>> afterOpt) {
            if (!beforeOpt.isPresent()) {
                return Optional.of(new DataListenerViolation(sequence, data, null));
            }

            final NormalizedNode<?, ?> before = beforeOpt.get();
            // Identity check to keep things fast. We'll run a diff to eliminate false positives later
            if (data != before) {
                return Optional.of(new DataListenerViolation(sequence, data, before));
            }

            return Optional.absent();
        }
    }

    private final List<DataListenerViolation> violations;

    DataListenerState(final List<DataListenerViolation> violations) {
        this.violations = checkNotNull(violations);
    }

    static DataListenerState initial() {
        return new Initial();
    }

    final DataListenerState append(final DataTreeCandidate change) {
        final DataTreeCandidateNode root = change.getRootNode();
        final Optional<NormalizedNode<?, ?>> beforeOpt = root.getDataBefore();
        final Optional<NormalizedNode<?, ?>> afterOpt = root.getDataAfter();
        final long count = changeCount() + 1;

        final Optional<DataListenerViolation> opt = validate(count, root.getModificationType(), beforeOpt, afterOpt);
        if (opt.isPresent()) {
            violations.add(opt.get());
        }

        return afterOpt.isPresent() ? new Present(violations, count, afterOpt.get()) : new Absent(violations, count);
    }

    public final List<DataListenerViolation> violations() {
        return Collections.unmodifiableList(violations);
    }


    public abstract long changeCount();

    public abstract Optional<NormalizedNode<?, ?>> lastData();

    abstract Optional<DataListenerViolation> validate(final long sequence, final ModificationType type,
            final Optional<NormalizedNode<?, ?>> beforeOpt, final Optional<NormalizedNode<?, ?>> afterOpt);

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("changeCount", changeCount());
    }
}
