/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.test.benchmark;

import org.opendaylight.controller.md.sal.common.api.data.AsyncWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.WriteStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.write.statistics.DataConstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.write.statistics.DataConstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.write.statistics.WriteToTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.write.statistics.WriteToTransactionBuilder;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.util.DurationStatsTracker;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;

abstract class ItemWriter<P extends Path<P>, D> implements WriteStatistics,DataObject {

    private final DurationStatsTracker construct = new DurationStatsTracker();
    private final DurationStatsTracker write = new DurationStatsTracker();

    abstract boolean writeNext(AsyncWriteTransaction<P, D> tx);

    public DurationStatsTracker getConstructionStats() {
        return construct;
    }

    public DurationStatsTracker getWriteStats() {
        return write;
    }

    @Override
    public Class<? extends DataContainer> getImplementedInterface() {
        return WriteStatistics.class;
    }

    @Override
    public DataConstruction getDataConstruction() {
        return new DataConstructionBuilder(Convertors.toDurationStatistics(construct)).build();
    }

    @Override
    public WriteToTransaction getWriteToTransaction() {
        return new WriteToTransactionBuilder(Convertors.toDurationStatistics(write)).build();
    }
}
