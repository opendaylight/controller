/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Type-safe encapsulation of a cluster member name.
 */
@Beta
public final class MemberName implements Comparable<MemberName>, Identifier {
    // TODO: this will we heavily serialized, consider optimal serialization (cache string UTF8 chars?)
    private static final long serialVersionUID = 1L;
    private final String name;

    private MemberName(final String name) {
        this.name = Preconditions.checkNotNull(name);
    }

    public static MemberName forString(final String str) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(str));
        // TODO: consider caching instances
        return new MemberName(str);
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        // TODO: consider caching hashCode()
        return name.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof MemberName && name.equals(((MemberName)o).name));
    }

    @Override
    public int compareTo(final MemberName o) {
        return this == o ? 0 : name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(MemberName.class).add("name", name).toString();
    }
}
