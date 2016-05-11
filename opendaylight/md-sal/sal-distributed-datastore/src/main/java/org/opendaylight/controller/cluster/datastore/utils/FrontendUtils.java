/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.annotations.Beta;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

@Beta
public final class FrontendUtils {
    private FrontendUtils() {
        throw new UnsupportedOperationException();
    }

    public static MemberName compatMemberName(final String memberName, final String dataStoreName) {
        return MemberName.forName(memberName + '$' + dataStoreName);
    }

    public static Entry<String, String> splitCompatMemberName(final MemberName memberName) {
        final String str = memberName.getName();
        final int i = str.lastIndexOf('$');

        return i == -1 ? new SimpleImmutableEntry<>(str, null)
                : new SimpleImmutableEntry<>(str.substring(0, i), str.substring(i + 1));
    }
}
