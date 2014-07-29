package org.opendaylight.controller.md.sal.binding.impl.test;

import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class ConcurrencyDataChangeTest extends AbstractDataBrokerTest {
    Logger LOG = LoggerFactory.getLogger(ConcurrencyDataChangeTest.class);

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);

    @Test
    public void putPutEmptyEmpty1stLevelTopEmptyConcurrencyTest() throws InterruptedException, ExecutionException {
        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        writeTx1.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTop(Collections.<TopLevelList>emptyList()));

        Optional<Top> actualResultPath = readWriteTx2.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();

        writeTx1.submit().get();

        if ( ! actualResultPath.isPresent()) {

            readWriteTx2.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTop(Collections.<TopLevelList>emptyList()));

            try {
                readWriteTx2.submit().get();
                fail("Exception should have been thrown");
            } catch (Exception ex) {
                LOG.debug("Exception '{}' thrown as expected", ex.getMessage());
            }
        } else {
            readWriteTx2.cancel();
        }
        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAfterTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAfterTransaction.isPresent());
        Top topAfterTx = resultAfterTransaction.get();
        assertEquals(0, topAfterTx.getTopLevelList().size());
    }

    @Test
    public void putMergeEmptyEmpty1stLevelTopEmptyConcurrencyTest() throws InterruptedException, ExecutionException {
        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        writeTx1.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTop(Collections.<TopLevelList>emptyList()));

        Optional<Top> actualResultPath = readWriteTx2.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();

        writeTx1.submit().get();

        if ( ! actualResultPath.isPresent()) {

            readWriteTx2.merge(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTop(Collections.<TopLevelList>emptyList()));

            readWriteTx2.submit().get();
        } else {
            readWriteTx2.cancel();
        }
        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(0, topAfterTx.getTopLevelList().size());
    }

    @Test
    public void putPutFooBar1stLevelTopEmptyConcurrencyTest() throws InterruptedException, ExecutionException {
        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList1.getValue()));

        Optional<Top> actualResultPath = readWriteTx2.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();

        writeTx1.submit().get();

        if ( ! actualResultPath.isPresent()) {

            readWriteTx2.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList2.getValue()));

            try {
                readWriteTx2.submit().get();
                fail("Exception should have been thrown");
            } catch (Exception ex) {
                LOG.debug("Exception '{}' thrown as expected", ex.getMessage());
            }
        } else {
            readWriteTx2.cancel();
        }
        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(1, topAfterTx.getTopLevelList().size());
        assertEquals("Expected result is 'foo' TopLevelList", topLevelList1.getValue(), topAfterTx.getTopLevelList().get(0));
    }

    @Test
    public void putMergeFooBar1stLevelTopEmptyConcurrencyTest() throws InterruptedException, ExecutionException {
        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList1.getValue()));

        Optional<Top> actualResultPath = readWriteTx2.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();

        writeTx1.submit().get();

        if ( ! actualResultPath.isPresent()) {

            readWriteTx2.merge(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList2.getValue()));

            readWriteTx2.submit().get();
        } else {
            readWriteTx2.cancel();
        }
        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(2, topAfterTx.getTopLevelList().size());
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList1.getValue()));
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList2.getValue()));
    }

    @Test
    public void mergePutFooBar1stLevelTopEmptyConcurrencyTest() throws InterruptedException, ExecutionException {
        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.merge(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList1.getValue()));

        Optional<Top> actualResultPath = readWriteTx2.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();

        writeTx1.submit().get();

        if ( ! actualResultPath.isPresent()) {

            readWriteTx2.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList2.getValue()));

            try {
                readWriteTx2.submit().get();
                fail("Exception should have been thrown");
            } catch (Exception ex) {
                LOG.debug("Exception '{}' thrown as expected", ex.getMessage());
            }
        } else {
            readWriteTx2.cancel();
        }
        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(1, topAfterTx.getTopLevelList().size());
        assertEquals("Expected result is 'foo' TopLevelList", topLevelList1.getValue(), topAfterTx.getTopLevelList().get(0));
    }

    @Test
    public void mergeMergeFooBar1stLevelTopEmptyConcurrencyTest() throws InterruptedException, ExecutionException {
        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.merge(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList1.getValue()));

        Optional<Top> actualResultPath = readWriteTx2.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();

        writeTx1.submit().get();

        if ( ! actualResultPath.isPresent()) {

            readWriteTx2.merge(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList2.getValue()));

            readWriteTx2.submit().get();
        } else {
            readWriteTx2.cancel();
        }
        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(2, topAfterTx.getTopLevelList().size());
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList1.getValue()));
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList2.getValue()));
    }

    @Test
    public void putPutFooBar1stLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);
        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList1.getValue()));

        writeTx1.submit().get();

        readWriteTx2.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList2.getValue()));

        try {
            readWriteTx2.submit().get();
            fail("Exception should have been thrown");
        } catch (Exception ex) {
            LOG.debug("Exception '{}' thrown as expected", ex.getMessage());
        }

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(1, topAfterTx.getTopLevelList().size());
        assertEquals("Expected result is 'foo' TopLevelList", topLevelList1.getValue(), topAfterTx.getTopLevelList().get(0));
    }

    @Test
    public void putMergeFooBar1stLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);
        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newWriteOnlyTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList1.getValue()));

        writeTx1.submit().get();

        readWriteTx2.merge(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList2.getValue()));

        readWriteTx2.submit().get();

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(2, topAfterTx.getTopLevelList().size());
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList1.getValue()));
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList2.getValue()));
    }

    @Test
    public void mergePutFooBar1stLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);
        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.merge(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList1.getValue()));

        writeTx1.submit().get();

        readWriteTx2.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList2.getValue()));

        try {
            readWriteTx2.submit().get();
            fail("Exception should have been thrown");
        } catch (Exception ex) {
            LOG.debug("Exception '{}' thrown as expected", ex.getMessage());
        }

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(1, topAfterTx.getTopLevelList().size());
        assertEquals("Expected result is 'foo' TopLevelList", topLevelList1.getValue(), topAfterTx.getTopLevelList().get(0));
    }

    @Test
    public void mergeMergeFooBar1stLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);
        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.merge(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList1.getValue()));

        writeTx1.submit().get();

        readWriteTx2.merge(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList2.getValue()));

        readWriteTx2.submit().get();

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(2, topAfterTx.getTopLevelList().size());
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList1.getValue()));
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList2.getValue()));
    }

    @Test
    public void deletePutTopBar1stLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);
        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.delete(LogicalDatastoreType.OPERATIONAL, TOP_PATH);

        writeTx1.submit().get();

        readWriteTx2.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList2.getValue()));

        try {
            readWriteTx2.submit().get();
            fail("Exception should have been thrown");
        } catch (Exception ex) {
            LOG.debug("Exception '{}' thrown as expected", ex.getMessage());
        }

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertFalse("Expected result after modifications is empty data store", resultAferTransaction.isPresent());
    }

    @Test
    public void deleteMergeTopBar1stLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);
        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.delete(LogicalDatastoreType.OPERATIONAL, TOP_PATH);

        writeTx1.submit().get();

        readWriteTx2.merge(LogicalDatastoreType.OPERATIONAL, TOP_PATH, makeTopWithOneTopLevelList(topLevelList2.getValue()));

        readWriteTx2.submit().get();

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(1, topAfterTx.getTopLevelList().size());
        assertEquals(topLevelList2.getValue(), topAfterTx.getTopLevelList().get(0));
    }

    @Test
    public void putPutFooBar2ndLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);
        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.put(LogicalDatastoreType.OPERATIONAL, topLevelList1.getKey(), topLevelList1.getValue());

        writeTx1.submit().get();

        readWriteTx2.put(LogicalDatastoreType.OPERATIONAL, topLevelList2.getKey(), topLevelList2.getValue());

        readWriteTx2.submit().get();

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(2, topAfterTx.getTopLevelList().size());
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList1.getValue()));
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList2.getValue()));
    }

    @Test
    public void putMergeFooBar2ndLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);
        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.put(LogicalDatastoreType.OPERATIONAL, topLevelList1.getKey(), topLevelList1.getValue());

        writeTx1.submit().get();

        readWriteTx2.merge(LogicalDatastoreType.OPERATIONAL, topLevelList2.getKey(), topLevelList2.getValue());

        readWriteTx2.submit().get();

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(2, topAfterTx.getTopLevelList().size());
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList1.getValue()));
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList2.getValue()));
    }

    @Test
    public void mergePutFooBar2ndLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);
        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.merge(LogicalDatastoreType.OPERATIONAL, topLevelList1.getKey(), topLevelList1.getValue());

        writeTx1.submit().get();

        readWriteTx2.put(LogicalDatastoreType.OPERATIONAL, topLevelList2.getKey(), topLevelList2.getValue());

        readWriteTx2.submit().get();

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(2, topAfterTx.getTopLevelList().size());
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList1.getValue()));
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList2.getValue()));
    }

    @Test
    public void mergeMergeFooBar2ndLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);
        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList1 =
                makeTopLevelList(TOP_PATH, "foo", Collections.<NestedList> emptyList());
        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.merge(LogicalDatastoreType.OPERATIONAL, topLevelList1.getKey(), topLevelList1.getValue());

        writeTx1.submit().get();

        readWriteTx2.merge(LogicalDatastoreType.OPERATIONAL, topLevelList2.getKey(), topLevelList2.getValue());

        readWriteTx2.submit().get();

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertTrue(resultAferTransaction.isPresent());
        Top topAfterTx = resultAferTransaction.get();
        assertEquals(2, topAfterTx.getTopLevelList().size());
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList1.getValue()));
        assertTrue(topAfterTx.getTopLevelList().contains(topLevelList2.getValue()));
    }

    @Test
    public void deletePutTopBar2ndLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);

        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.delete(LogicalDatastoreType.OPERATIONAL, TOP_PATH);

        writeTx1.submit().get();

        readWriteTx2.put(LogicalDatastoreType.OPERATIONAL, topLevelList2.getKey(), topLevelList2.getValue());

        try {
            readWriteTx2.submit().get();
            fail("Optimistic Lock exception should have been thrown.");
        } catch(Exception ex){
            LOG.debug("Exception '{}' thrown as expected", ex.getMessage());
        }

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertFalse(resultAferTransaction.isPresent());
    }

    @Test
    public void deleteMergeTopBar2ndLevelTopEmptyListConcurrencyTest() throws InterruptedException, ExecutionException {
        Top top = makeTop(Collections.<TopLevelList>emptyList());
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, top);
        writeTx.submit().get();

        WriteTransaction writeTx1 = getDataBroker().newReadWriteTransaction();
        ReadWriteTransaction readWriteTx2 = getDataBroker().newReadWriteTransaction();

        Entry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList2 =
                makeTopLevelList(TOP_PATH, "bar", Collections.<NestedList> emptyList());

        writeTx1.delete(LogicalDatastoreType.OPERATIONAL, TOP_PATH);

        writeTx1.submit().get();

        readWriteTx2.merge(LogicalDatastoreType.OPERATIONAL, topLevelList2.getKey(), topLevelList2.getValue());

        try {
            readWriteTx2.submit().get();
            fail("Optimistic Lock exception should have been thrown.");
        } catch(Exception ex){
            LOG.debug("Exception '{}' was thrown as expected", ex.getMessage());
        }

        ReadOnlyTransaction readOnlyTransaction = getDataBroker().newReadOnlyTransaction();
        Optional<Top> resultAferTransaction = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).get();
        assertFalse(resultAferTransaction.isPresent());
    }

    private Top makeTop(final List<TopLevelList> topLevelList) {
        TopBuilder builder = new TopBuilder();
        builder.setTopLevelList(topLevelList);
        return builder.build();
    }

    private Top makeTopWithOneTopLevelList(final TopLevelList topLevelList) {
        List<TopLevelList> listOftopLevelList = new ArrayList<>();
        listOftopLevelList.add(topLevelList);
        TopBuilder builder = new TopBuilder();
        builder.setTopLevelList(listOftopLevelList);
        return builder.build();
    }

    private Entry<InstanceIdentifier<TopLevelList>, TopLevelList> makeTopLevelList(
            final InstanceIdentifier<Top> ident, final String key, final List<NestedList> nestedList) {
        TopLevelListKey topLevelListKey = new TopLevelListKey(key);
        InstanceIdentifier<TopLevelList> topLevelListIdent = ident.child(TopLevelList.class, topLevelListKey);

        TopLevelListBuilder builder = new TopLevelListBuilder();
        builder.setKey(topLevelListKey);
        builder.setName(key);
        builder.setNestedList(nestedList);
        return new AbstractMap.SimpleEntry<>(topLevelListIdent, builder.build());
    }
}

