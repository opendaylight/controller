/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.annotations.Beta;
import java.io.DataInput;
import java.io.IOException;
import java.util.TreeSet;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;

@Beta
public final class ImmutableUnsignedLongSet extends UnsignedLongSet implements Immutable {
    private static final @NonNull ImmutableUnsignedLongSet EMPTY = new ImmutableUnsignedLongSet(new TreeSet<>());

    private ImmutableUnsignedLongSet(final TreeSet<Entry> ranges) {
        super(ranges);
    }

    static @NonNull ImmutableUnsignedLongSet of(final TreeSet<Entry> ranges) {
        return ranges.isEmpty() ? EMPTY : new ImmutableUnsignedLongSet(ranges);
    }

    public static @NonNull ImmutableUnsignedLongSet of() {
        return EMPTY;
    }

    public static @NonNull ImmutableUnsignedLongSet readFrom(final DataInput in) throws IOException {
        return of(readRanges(in));
    }

    @Override
    public ImmutableUnsignedLongSet immutableCopy() {
        return this;
    }
}
