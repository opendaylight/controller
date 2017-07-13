/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public abstract class NormalizedNodeDiff {
    private static final class Missing extends NormalizedNodeDiff {
        private final NormalizedNode<?, ?> expected;

        Missing(final NormalizedNode<?, ?> expected) {
            this.expected = checkNotNull(expected);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("expected", expected).toString();
        }
    }

    private static final class Unexpected extends NormalizedNodeDiff {
        private final NormalizedNode<?, ?> actual;

        Unexpected(final NormalizedNode<?, ?> actual) {
            this.actual = checkNotNull(actual);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("actual", actual).toString();
        }
    }

    private static final class Regular extends NormalizedNodeDiff {
        private final NormalizedNode<?, ?> expected;
        private final NormalizedNode<?, ?> actual;

        Regular(final NormalizedNode<?, ?> expected, final NormalizedNode<?, ?> actual) {
            this.expected = checkNotNull(expected);
            this.actual = checkNotNull(actual);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("expected", expected).add("actual", actual).toString();
        }
    }

    public static Optional<NormalizedNodeDiff> compute(final NormalizedNode<?, ?> expected,
            final NormalizedNode<?, ?> actual) {
        if (expected == null) {
            return actual == null ? Optional.absent() : Optional.of(new Unexpected(actual));
        }
        if (actual == null) {
            return Optional.of(new Missing(expected));
        }
        if (actual.equals(expected)) {
            return Optional.absent();
        }

        // TODO: better diff
        return Optional.of(new Regular(expected, actual));
    }
}
