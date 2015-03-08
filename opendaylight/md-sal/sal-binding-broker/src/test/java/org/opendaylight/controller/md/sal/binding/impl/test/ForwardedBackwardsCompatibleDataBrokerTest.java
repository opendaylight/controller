package org.opendaylight.controller.md.sal.binding.impl.test;

import static junit.framework.TestCase.assertNotNull;

import org.opendaylight.controller.md.sal.binding.compat.HydrogenDataBrokerAdapter;

import com.google.common.collect.ImmutableSet;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

@Deprecated
public class ForwardedBackwardsCompatibleDataBrokerTest extends
    AbstractDataBrokerTest {

    private DataBrokerTestCustomizer testCustomizer;
    private HydrogenDataBrokerAdapter dataBroker;
    private DOMDataBroker domBroker;

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);
    private static final TopLevelListKey TOP_LIST_KEY = new TopLevelListKey("foo");
    private static final InstanceIdentifier<TopLevelList> NODE_PATH = TOP_PATH.child(TopLevelList.class, TOP_LIST_KEY);
    private static final TopLevelList NODE = new TopLevelListBuilder().setKey(TOP_LIST_KEY).build();

    @Override
    protected Iterable<YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableSet.of(BindingReflections.getModuleInfo(TopLevelList.class));
    }

    @Override
    protected DataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        return new DataBrokerTestCustomizer();
    }

    @Override
    protected void setupWithDataBroker(final DataBroker dataBroker) {
        super.setupWithDataBroker(dataBroker);
        this.dataBroker = new HydrogenDataBrokerAdapter(dataBroker);
    }




    /**
     * The purpose of this test is to exercise the backwards compatible broker
     * <p>
     * This test tries to execute the code which ensures that the parents
     * for a given node get automatically created.
     *
     * @see org.opendaylight.controller.md.sal.binding.impl.AbstractReadWriteTransaction#ensureParentsByMerge(org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType, org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, org.opendaylight.yangtools.yang.binding.InstanceIdentifier)
     */
    @Test
    public void testEnsureParentsByMerge() throws InterruptedException, ExecutionException {
        final DataModificationTransaction writeTx =
            dataBroker.beginTransaction();

        writeTx.putOperationalData(NODE_PATH, NODE);

        writeTx.commit().get();

        // TOP_PATH should exist as it is the parent of NODE_PATH
        final DataObject object = dataBroker.readOperationalData(TOP_PATH);

        assertNotNull(object);

    }


}
