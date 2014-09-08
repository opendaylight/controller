/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.test.benchmark;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataTransactionFactory;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.base.rev140701.BindingV1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.base.rev140701.DataFormat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.base.rev140701.DataOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.base.rev140701.NormalizedNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.base.rev140701.TwoLevelListProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.DataBrokerPerformanceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.DataBrokerPerformanceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.DataBrokerPerformanceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.OpendaylightMdsalTestBenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class BenchmarkServiceImpl implements OpendaylightMdsalTestBenchmarkService {

    private final DataBroker bindingBroker;
    private final DOMDataBroker domBroker;

    public BenchmarkServiceImpl(DataBroker bindingBroker, DOMDataBroker domBroker) {
        this.bindingBroker = bindingBroker;
        this.domBroker = domBroker;
    }

    @Override
    public Future<RpcResult<DataBrokerPerformanceOutput>> dataBrokerPerformance(DataBrokerPerformanceInput input) {

        Class<? extends DataFormat> format = input.getDataFormat();
        Preconditions.checkArgument(format != null, "data-format must be specified.");


        cleanupStore();
        @SuppressWarnings("rawtypes")
        AsyncDataTransactionFactory factory = getTransactionFactory(format,false);
        @SuppressWarnings("rawtypes")
        final ItemWriter itemWriter = getItemWriter(format,input.getWriteOperation(),input);
        long itemPerTx = input.getOuterWritesPerTransaction();

        final TransactionStatisticsCollector collector = new TransactionStatisticsCollector();
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final TransactionTestExecution<?,?> execution = new TransactionTestExecution(factory, itemWriter, itemPerTx, collector);

        return Futures.transform(execution.execute(), new Function<Void, RpcResult<DataBrokerPerformanceOutput>>() {
            @Override
            public RpcResult<DataBrokerPerformanceOutput> apply(Void input) {
                DataBrokerPerformanceOutputBuilder output = new DataBrokerPerformanceOutputBuilder(collector.toTransactionStatistics());
                output.fieldsFrom(itemWriter);
                return RpcResultBuilder.success(output.build()).build();
            }
        });
    }

    private void cleanupStore() {
        WriteTransaction tx = bindingBroker.newWriteOnlyTransaction();
        Top data = new TopBuilder().setTopLevelList(Collections.<TopLevelList>emptyList()).build();
        tx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Top.class),data);
        try {
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            throw new IllegalStateException(e);
        }
    }

    private ItemWriter<?,?> getItemWriter(Class<? extends DataFormat> format, DataOperation writeOperation,
            TwoLevelListProperties input) {
        if(BindingV1.class.equals(format)) {
            return new BindingTwoListWriter(input, writeOperation);
        } else if(NormalizedNode.class.equals(format)) {
            return new DomTwoLevelListWriter(input, writeOperation);
        }
        throw new IllegalArgumentException();
    }

    private AsyncDataTransactionFactory getTransactionFactory(Class<? extends DataFormat> format, boolean useChaining) {
        if(BindingV1.class.equals(format)) {
            return bindingBroker;
        } else if(NormalizedNode.class.equals(format)) {
            return domBroker;
        }
        throw new IllegalArgumentException();
    }

}
