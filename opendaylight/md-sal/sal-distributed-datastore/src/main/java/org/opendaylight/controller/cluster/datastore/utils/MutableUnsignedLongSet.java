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
import org.opendaylight.yangtools.concepts.Mutable;

@Beta
public final class MutableUnsignedLongSet extends UnsignedLongSet implements Mutable {
    MutableUnsignedLongSet(final TreeSet<Entry> ranges) {
        super(ranges);
    }

    public static @NonNull MutableUnsignedLongSet of() {
        return new MutableUnsignedLongSet(new TreeSet<>());
    }

    public static @NonNull MutableUnsignedLongSet readFrom(final DataInput in) throws IOException {
        return new MutableUnsignedLongSet(readRanges(in));
    }

    public void add(final long longBits) {
        addImpl(longBits);
    }

    @Override
    public ImmutableUnsignedLongSet immutableCopy() {
        return ImmutableUnsignedLongSet.of(copyRanges());
    }
}
