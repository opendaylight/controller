/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dsbenchmark;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.dsbenchmark.listener.DsbenchmarkListenerProvider;
import org.opendaylight.dsbenchmark.simpletx.SimpletxBaDelete;
import org.opendaylight.dsbenchmark.simpletx.SimpletxBaRead;
import org.opendaylight.dsbenchmark.simpletx.SimpletxBaWrite;
import org.opendaylight.dsbenchmark.simpletx.SimpletxDomDelete;
import org.opendaylight.dsbenchmark.simpletx.SimpletxDomRead;
import org.opendaylight.dsbenchmark.simpletx.SimpletxDomWrite;
import org.opendaylight.dsbenchmark.txchain.TxchainBaDelete;
import org.opendaylight.dsbenchmark.txchain.TxchainBaRead;
import org.opendaylight.dsbenchmark.txchain.TxchainBaWrite;
import org.opendaylight.dsbenchmark.txchain.TxchainDomDelete;
import org.opendaylight.dsbenchmark.txchain.TxchainDomRead;
import org.opendaylight.dsbenchmark.txchain.TxchainDomWrite;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.CleanupStoreInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.CleanupStoreOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.CleanupStoreOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.DsbenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestStatus.ExecStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestStatusBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DsbenchmarkProvider implements DsbenchmarkService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DsbenchmarkProvider.class);
    private static final InstanceIdentifier<TestExec> TEST_EXEC_IID =
            InstanceIdentifier.builder(TestExec.class).build();
    private static final InstanceIdentifier<TestStatus> TEST_STATUS_IID =
            InstanceIdentifier.builder(TestStatus.class).build();

    private final AtomicReference<ExecStatus> execStatus = new AtomicReference<>(ExecStatus.Idle);
    private final DsbenchmarkListenerProvider listenerProvider = new DsbenchmarkListenerProvider();
    private final DOMDataBroker domDataBroker;  // Async DOM Broker for use with all DOM operations
    private final DataBroker dataBroker; // Async Binding-Aware Broker for use in tx chains

    private long testsCompleted = 0;

    public DsbenchmarkProvider(final DOMDataBroker domDataBroker, final DataBroker dataBroker) {
        this.domDataBroker = domDataBroker;
        this.dataBroker = dataBroker;
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    public void init() {
        listenerProvider.setDataBroker(dataBroker);

        try {
            // We want to set the initial operation status so users can detect we are ready to start test.
            setTestOperData(this.execStatus.get(), testsCompleted);
        } catch (final Exception e) {
            // TODO: Use a singleton service to make sure the initial write is performed only once.
            LOG.warn("Working around Bugs 8829 and 6793 by ignoring exception from setTestOperData", e);
        }

        LOG.info("DsbenchmarkProvider initiated");
    }

    @Override
    public void close() {
        LOG.info("DsbenchmarkProvider closed");
    }

    @Override
    public ListenableFuture<RpcResult<CleanupStoreOutput>> cleanupStore(final CleanupStoreInput input) {
        cleanupTestStore();
        LOG.debug("Data Store cleaned up");
        return Futures.immediateFuture(RpcResultBuilder.success(new CleanupStoreOutputBuilder().build()).build());
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    public ListenableFuture<RpcResult<StartTestOutput>> startTest(final StartTestInput input) {
        LOG.info("Starting the data store benchmark test, input: {}", input);

        // Check if there is a test in progress
        if (execStatus.compareAndSet(ExecStatus.Idle, ExecStatus.Executing) == false) {
            LOG.info("Test in progress");
            return RpcResultBuilder.success(new StartTestOutputBuilder()
                    .setStatus(StartTestOutput.Status.TESTINPROGRESS)
                    .build()).buildFuture();
        }

        // Cleanup data that may be left over from a previous test run
        cleanupTestStore();

        // Get the appropriate writer based on operation type and data format
        DatastoreAbstractWriter dsWriter = getDatastoreWriter(input);

        // Create listeners on OPERATIONAL and CONFIG test data subtrees
        listenerProvider.createAndRegisterListeners(input.getListeners().intValue());


        long startTime = System.nanoTime();
        dsWriter.createList();
        long endTime = System.nanoTime();
        final long listCreateTime = (endTime - startTime) / 1000;

        // Run the test and measure the execution time
        long execTime;
        try {
            startTime = System.nanoTime();
            dsWriter.executeList();
            endTime = System.nanoTime();
            execTime = (endTime - startTime) / 1000;

            this.testsCompleted++;

        } catch (final Exception e) {
            LOG.error("Test error: {}", e.toString());
            execStatus.set(ExecStatus.Idle);
            return RpcResultBuilder.success(new StartTestOutputBuilder()
                    .setStatus(StartTestOutput.Status.FAILED)
                    .build()).buildFuture();
        }

        LOG.info("Test finished");
        setTestOperData(ExecStatus.Idle, testsCompleted);
        execStatus.set(ExecStatus.Idle);

        // Get the number of data change events and cleanup the data change listeners
        long numDataChanges = listenerProvider.getDataChangeCount();
        long numEvents = listenerProvider.getEventCountAndDestroyListeners();

        StartTestOutput output = new StartTestOutputBuilder()
                .setStatus(StartTestOutput.Status.OK)
                .setListBuildTime(listCreateTime)
                .setExecTime(execTime)
                .setTxOk(Uint32.valueOf(dsWriter.getTxOk()))
                .setNtfOk(Uint32.valueOf(numEvents))
                .setDataChangeEventsOk(Uint32.valueOf(numDataChanges))
                .setTxError(Uint32.valueOf(dsWriter.getTxError()))
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    private void setTestOperData(final ExecStatus sts, final long tstCompl) {
        TestStatus status = new TestStatusBuilder()
                .setExecStatus(sts)
                .setTestsCompleted(Uint32.valueOf(tstCompl))
                .build();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, TEST_STATUS_IID, status);

        try {
            tx.commit().get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }

        LOG.debug("DataStore test oper status populated: {}", status);
    }

    private void cleanupTestStore() {
        TestExec data = new TestExecBuilder().setOuterList(Collections.emptyMap()).build();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, TEST_EXEC_IID, data);
        try {
            tx.commit().get();
            LOG.debug("DataStore config test data cleaned up");
        } catch (final InterruptedException | ExecutionException e) {
            LOG.info("Failed to cleanup DataStore configtest data");
            throw new IllegalStateException(e);
        }

        tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, TEST_EXEC_IID, data);
        try {
            tx.commit().get();
            LOG.debug("DataStore operational test data cleaned up");
        } catch (final InterruptedException | ExecutionException e) {
            LOG.info("Failed to cleanup DataStore operational test data");
            throw new IllegalStateException(e);
        }

    }

    private DatastoreAbstractWriter getDatastoreWriter(final StartTestInput input) {

        final DatastoreAbstractWriter retVal;

        StartTestInput.TransactionType txType = input.getTransactionType();
        StartTestInput.Operation oper = input.getOperation();
        StartTestInput.DataFormat dataFormat = input.getDataFormat();
        StartTestInput.DataStore dataStore = input.getDataStore();
        int outerListElem = input.getOuterElements().intValue();
        int innerListElem = input.getInnerElements().intValue();
        int writesPerTx = input.getPutsPerTx().intValue();

        try {
            if (txType == StartTestInput.TransactionType.SIMPLETX) {
                if (dataFormat == StartTestInput.DataFormat.BINDINGAWARE) {
                    if (StartTestInput.Operation.DELETE == oper) {
                        retVal = new SimpletxBaDelete(this.dataBroker, outerListElem,
                                innerListElem,writesPerTx, dataStore);
                    } else if (StartTestInput.Operation.READ == oper) {
                        retVal = new SimpletxBaRead(this.dataBroker, outerListElem,
                                innerListElem, writesPerTx, dataStore);
                    } else {
                        retVal = new SimpletxBaWrite(this.dataBroker, oper, outerListElem,
                                innerListElem, writesPerTx, dataStore);
                    }
                } else {
                    if (StartTestInput.Operation.DELETE == oper) {
                        retVal = new SimpletxDomDelete(this.domDataBroker, outerListElem,
                                innerListElem, writesPerTx, dataStore);
                    } else if (StartTestInput.Operation.READ == oper) {
                        retVal = new SimpletxDomRead(this.domDataBroker, outerListElem,
                                innerListElem, writesPerTx, dataStore);
                    } else {
                        retVal = new SimpletxDomWrite(this.domDataBroker, oper, outerListElem,
                                innerListElem, writesPerTx, dataStore);
                    }
                }
            } else {
                if (dataFormat == StartTestInput.DataFormat.BINDINGAWARE) {
                    if (StartTestInput.Operation.DELETE == oper) {
                        retVal = new TxchainBaDelete(this.dataBroker, outerListElem,
                                innerListElem, writesPerTx, dataStore);
                    } else if (StartTestInput.Operation.READ == oper) {
                        retVal = new TxchainBaRead(this.dataBroker, outerListElem,
                                innerListElem,writesPerTx, dataStore);
                    } else {
                        retVal = new TxchainBaWrite(this.dataBroker, oper, outerListElem,
                                innerListElem, writesPerTx, dataStore);
                    }
                } else {
                    if (StartTestInput.Operation.DELETE == oper) {
                        retVal = new TxchainDomDelete(this.domDataBroker, outerListElem,
                                innerListElem, writesPerTx, dataStore);
                    } else if (StartTestInput.Operation.READ == oper) {
                        retVal = new TxchainDomRead(this.domDataBroker, outerListElem,
                                innerListElem, writesPerTx, dataStore);

                    } else {
                        retVal = new TxchainDomWrite(this.domDataBroker, oper, outerListElem,
                                innerListElem,writesPerTx, dataStore);
                    }
                }
            }
        } finally {
            execStatus.set(ExecStatus.Idle);
        }
        return retVal;
    }
}
