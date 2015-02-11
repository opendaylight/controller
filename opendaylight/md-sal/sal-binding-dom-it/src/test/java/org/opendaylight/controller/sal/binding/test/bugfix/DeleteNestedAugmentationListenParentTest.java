package org.opendaylight.controller.sal.binding.test.bugfix;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.List11SimpleAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.List11SimpleAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.TllComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.SettableFuture;

@SuppressWarnings("deprecation")
public class DeleteNestedAugmentationListenParentTest extends AbstractDataServiceTest {

    private static final TopLevelListKey FOO_KEY = new TopLevelListKey("foo");

    private static final List1Key LIST1_KEY = new List1Key("one");

    private static final List11Key LIST11_KEY = new List11Key(100);

    private static final InstanceIdentifier<TllComplexAugment> TLL_COMPLEX_AUGMENT_PATH = InstanceIdentifier.builder(Top.class)
            .child(TopLevelList.class,FOO_KEY)
            .augmentation(TllComplexAugment.class)
            .build();

    private static final InstanceIdentifier<List11> LIST11_PATH = TLL_COMPLEX_AUGMENT_PATH.builder()
            .child(List1.class,LIST1_KEY)
            .child(List11.class,LIST11_KEY)
            .build();


    @Test
    public void deleteChildListenParent() throws InterruptedException, ExecutionException {
        DataModificationTransaction initTx = baDataService.beginTransaction();

        initTx.putOperationalData(LIST11_PATH, createList11());
        initTx.commit().get();

        final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> event = SettableFuture.create();

        baDataService.registerDataChangeListener(LIST11_PATH, new DataChangeListener() {

            @Override
            public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
                event.set(change);
            }
        });

        DataModificationTransaction deleteTx = baDataService.beginTransaction();
        deleteTx.removeOperationalData(LIST11_PATH.augmentation(List11SimpleAugment.class));
        deleteTx.commit().get();

        DataChangeEvent<InstanceIdentifier<?>, DataObject> receivedEvent = event.get();
        assertFalse(receivedEvent.getRemovedOperationalData().contains(TLL_COMPLEX_AUGMENT_PATH));
    }

    private List11 createList11() {
        List11Builder builder = new List11Builder()
            .setKey(LIST11_KEY)
            .addAugmentation(List11SimpleAugment.class,new List11SimpleAugmentBuilder()
                    .setAttrStr2("bad").build())
            .setAttrStr("good");
        return builder.build();
    }

}