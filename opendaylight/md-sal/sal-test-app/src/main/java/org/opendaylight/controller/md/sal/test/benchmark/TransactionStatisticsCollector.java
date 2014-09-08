/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.test.benchmark;

import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.TransactionStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.transaction.statistics.FailedCommits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.transaction.statistics.FailedCommitsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.transaction.statistics.SuccessfulCommits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.transaction.statistics.SuccessfulCommitsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.transaction.statistics.TransactionConstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.transaction.statistics.TransactionConstructionBuilder;
import org.opendaylight.yangtools.util.DurationStatsTracker;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class TransactionStatisticsCollector {

    AtomicLong total = new AtomicLong();
    AtomicLong success = new AtomicLong();
    AtomicLong failed = new AtomicLong();

    private final DurationStatsTracker construction = new DurationStatsTracker();
    private final DurationStatsTracker successCommitDuration= new DurationStatsTracker();
    private final DurationStatsTracker failedCommitDuration = new DurationStatsTracker();



    public AtomicLong failedTxCount() {
        return failed;
    }

    public AtomicLong successTxCount() {
        return success;
    }

    public AtomicLong getTotal() {
        return total;
    }

    public DurationStatsTracker constructionDuration() {
        return construction;
    }

    public DurationStatsTracker failedCommitDuration() {
        return failedCommitDuration;
    }

    public DurationStatsTracker getSuccessCommitDuration() {
        return successCommitDuration;
    }

    public TransactionStatistics toTransactionStatistics() {
        return new TransactionStatistics() {

            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return TransactionStatistics.class;
            }

            @Override
            public Long getTotalTransactionCount() {
                return total.get();
            }

            @Override
            public SuccessfulCommits getSuccessfulCommits() {
                return new SuccessfulCommitsBuilder(Convertors.toDurationStatistics(successCommitDuration))
                    .setCount(success.get())
                    .build();
            }

            @Override
            public TransactionConstruction getTransactionConstruction() {
                return new TransactionConstructionBuilder(Convertors.toDurationStatistics(construction)).build();
            }

            @Override
            public FailedCommits getFailedCommits() {
                return new FailedCommitsBuilder(Convertors.toDurationStatistics(failedCommitDuration))
                    .setCount(failed.get())
                    .build();
            }
        };
    }
}
