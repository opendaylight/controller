/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.test.benchmark;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.DurationStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.write.statistics.DataConstructionBuilder;
import org.opendaylight.yangtools.util.DurationStatsTracker;

public class Convertors {

    public static final DurationStatistics toDurationStatistics(DurationStatsTracker construct) {
        return new DataConstructionBuilder()
            .setAverageDuration(construct.getDisplayableAverageDuration())
            .setShortestDuration(construct.getDisplayableShortestDuration())
            .setLongestDuration(construct.getDisplayableLongestDuration())
            .build();

    }
}
