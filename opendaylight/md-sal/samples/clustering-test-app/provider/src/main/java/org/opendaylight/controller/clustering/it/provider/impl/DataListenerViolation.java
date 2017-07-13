/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import org.opendaylight.controller.clustering.it.provider.NormalizedNodeDiff;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public final class DataListenerViolation {
    private final NormalizedNode<?, ?> expected;
    private final NormalizedNode<?, ?> actual;
    private final long sequence;

    DataListenerViolation(final long sequence, final NormalizedNode<?, ?> expected, final NormalizedNode<?, ?> actual) {
        this.sequence = sequence;
        this.expected = expected;
        this.actual = actual;
    }

    public long getSequence() {
        return sequence;
    }

    public Optional<NormalizedNodeDiff> toDiff() {
        return NormalizedNodeDiff.compute(expected, actual);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("sequence", sequence).toString();
    }

}
