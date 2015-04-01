package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_BAR_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.USES_ONE_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.complexUsesAugment;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.top;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TwoLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

public class DataTreeChangeListenerTest extends AbstractDataBrokerTest {

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);
    private static final InstanceIdentifier<TopLevelList> FOO_PATH = path(TOP_FOO_KEY);
    private static final PathArgument FOO_ARGUMENT = Iterables.getLast(FOO_PATH.getPathArguments());
    private static final TopLevelList FOO_DATA = topLevelList(TOP_FOO_KEY, complexUsesAugment(USES_ONE_KEY));
    private static final InstanceIdentifier<TopLevelList> BAR_PATH = path(TOP_BAR_KEY);
    private static final PathArgument BAR_ARGUMENT = Iterables.getLast(BAR_PATH.getPathArguments());
    private static final TopLevelList BAR_DATA = topLevelList(TOP_BAR_KEY);
    private static final DataTreeIdentifier TOP_IDENTIFIER = new DataTreeIdentifier(LogicalDatastoreType.OPERATIONAL,
            TOP_PATH);

    private static final Top TOP_INITIAL_DATA = top(FOO_DATA);

    private BindingDOMDataBrokerAdapter dataBrokerImpl;

    private static final class EventCapturingListener implements DataTreeChangeListener {

        private SettableFuture<Collection<DataTreeModification>> changes = SettableFuture.create();

        @Override
        public void onDataTreeChanged(final Collection<DataTreeModification> changes) {
            this.changes.set(changes);

        }

        Collection<DataTreeModification> nextEvent() throws Exception {
            final Collection<DataTreeModification> result = changes.get(200,TimeUnit.MILLISECONDS);
            changes = SettableFuture.create();
            return result;
        }

    }

    @Override
    protected Iterable<YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableSet.of(
                BindingReflections.getModuleInfo(TwoLevelList.class),
                BindingReflections.getModuleInfo(TreeComplexUsesAugment.class)
                );
    }

    @Override
    protected void setupWithDataBroker(final DataBroker dataBroker) {
        dataBrokerImpl = (BindingDOMDataBrokerAdapter) dataBroker;
    }

    @Test
    public void testTopLevelListener() throws Exception {
        final EventCapturingListener listener = new EventCapturingListener();
        dataBrokerImpl.registerDataTreeChangeListener(TOP_IDENTIFIER, listener);

        createAndVerifyTop(listener);

        putTx(BAR_PATH, BAR_DATA).submit().checkedGet();
        final DataTreeModification afterBarPutEvent = Iterables.getOnlyElement(listener.nextEvent());
        final DataObjectModification<? extends DataObject> barPutMod =
                getOnlyChildModification(afterBarPutEvent.getRootNode(), ModificationType.SUBTREE_MODIFIED);
        verifyModification(barPutMod, BAR_ARGUMENT, ModificationType.WRITE);

        deleteTx(BAR_PATH).submit().checkedGet();
        final DataTreeModification afterBarDeleteEvent = Iterables.getOnlyElement(listener.nextEvent());
        final DataObjectModification<? extends DataObject> barDeleteMod =
                getOnlyChildModification(afterBarDeleteEvent.getRootNode(), ModificationType.SUBTREE_MODIFIED);
        verifyModification(barDeleteMod, BAR_ARGUMENT, ModificationType.DELETE);
    }

    @Test
    public void testWildcardedListListener() throws Exception {
        final EventCapturingListener listener = new EventCapturingListener();
        final DataTreeIdentifier wildcard = new DataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, TOP_PATH.child(TopLevelList.class));
        dataBrokerImpl.registerDataTreeChangeListener(wildcard, listener);

        putTx(TOP_PATH, TOP_INITIAL_DATA).submit().checkedGet();

        final DataTreeModification fooWriteEvent = Iterables.getOnlyElement(listener.nextEvent());
        assertEquals(FOO_PATH, fooWriteEvent.getRootPath().getRootIdentifier());
        verifyModification(fooWriteEvent.getRootNode(), FOO_ARGUMENT, ModificationType.WRITE);

        putTx(BAR_PATH, BAR_DATA).submit().checkedGet();
        final DataTreeModification barWriteEvent = Iterables.getOnlyElement(listener.nextEvent());
        assertEquals(BAR_PATH, barWriteEvent.getRootPath().getRootIdentifier());
        verifyModification(barWriteEvent.getRootNode(), BAR_ARGUMENT, ModificationType.WRITE);

        deleteTx(BAR_PATH).submit().checkedGet();
        final DataTreeModification barDeleteEvent = Iterables.getOnlyElement(listener.nextEvent());
        assertEquals(BAR_PATH, barDeleteEvent.getRootPath().getRootIdentifier());
        verifyModification(barDeleteEvent.getRootNode(), BAR_ARGUMENT, ModificationType.DELETE);
    }



    private void createAndVerifyTop(final EventCapturingListener listener) throws Exception {
        putTx(TOP_PATH,TOP_INITIAL_DATA).submit().checkedGet();
        final Collection<DataTreeModification> events = listener.nextEvent();

        assertFalse("Non empty collection should be received.",events.isEmpty());
        final DataTreeModification initialWrite = Iterables.getOnlyElement(events);
        final DataObjectModification<? extends DataObject> initialNode = initialWrite.getRootNode();
        verifyModification(initialNode,TOP_PATH.getPathArguments().iterator().next(),ModificationType.WRITE);
        assertEquals(TOP_INITIAL_DATA, initialNode.getDataAfter());
    }

    private DataObjectModification<? extends DataObject> getOnlyChildModification(final DataObjectModification<? extends DataObject> parentMod,final DataObjectModification.ModificationType eventType) throws Exception {
        assertEquals(eventType,parentMod.getModificationType());
        final Collection<DataObjectModification<? extends DataObject>> childMod = parentMod.getModifiedChildren();
        assertFalse("Non empty children modification",childMod.isEmpty());
        return Iterables.getOnlyElement(childMod);
    }

    private void verifyModification(final DataObjectModification<? extends DataObject> barWrite, final PathArgument pathArg,
            final ModificationType eventType) {
        assertEquals(pathArg.getType(), barWrite.getDataType());
        assertEquals(eventType,barWrite.getModificationType());
        assertEquals(pathArg, barWrite.getIdentifier());
    }

    private <T extends DataObject> WriteTransaction putTx(final InstanceIdentifier<T> path,final T data) {
        final WriteTransaction tx = dataBrokerImpl.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, path, data);
        return tx;
    }

    private WriteTransaction deleteTx(final InstanceIdentifier<?> path) {
        final WriteTransaction tx = dataBrokerImpl.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, path);
        return tx;
    }
}
