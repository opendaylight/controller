/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TermInfoTest {
    @ParameterizedTest
    @MethodSource
    void toStringWorks(final String expected, final TermInfo termInfo) {
        assertEquals(expected, termInfo.toString());
    }

    private static List<Arguments> toStringWorks() {
        return List.of(
            arguments("TermInfo{term=42, votedFor=Zaphod}", new TermInfo(42, "Zaphod")),
            arguments("TermInfo{term=0, votedFor=}", new TermInfo(0, "")),
            arguments("TermInfo{term=9223372036854775807}", new TermInfo(Long.MAX_VALUE), null),
            // FIXME: should be '9223372036854775808' with unsigned
            arguments("TermInfo{term=-9223372036854775808}", new TermInfo(Long.MIN_VALUE)),
            // FIXME: should be '18446744073709551614' with unsigned
            arguments("TermInfo{term=-2}", new TermInfo(-2)),
            // FIXME: should be '18446744073709551615' with unsigned or TermInfo(none}?
            //        is votedFor nullness significant in that decision?
            arguments("TermInfo{term=-1}", new TermInfo(-1)));
    }
}
