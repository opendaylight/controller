package org.opendaylight.controller.md.sal.binding.impl.test;

import org.opendaylight.controller.md.sal.binding.compat.hydrogen.ForwardedBackwardsCompatibleDataBroker;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.AbstractSchemaAwareTest;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import java.util.concurrent.ExecutionException;
import static junit.framework.TestCase.assertNotNull;

public class ForwardedBackwardsCompatibleDataBrokerTest extends
    AbstractSchemaAwareTest {

    private DataBrokerTestCustomizer testCustomizer;
    private ForwardedBackwardsCompatibleDataBroker dataBroker;
    private DOMDataBroker domBroker;

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);
    private static final TopLevelListKey TOP_LIST_KEY = new TopLevelListKey("foo");
    private static final InstanceIdentifier<TopLevelList> NODE_PATH = TOP_PATH.child(TopLevelList.class, TOP_LIST_KEY);
    private static final TopLevelList NODE = new TopLevelListBuilder().setKey(TOP_LIST_KEY).build();

    protected DataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        return new DataBrokerTestCustomizer();
    }

    @Override
    protected void setupWithSchema(final SchemaContext context) {
        testCustomizer = createDataBrokerTestCustomizer();

        domBroker = testCustomizer.createDOMDataBroker();
        dataBroker = createBackwardsCompatibleDataBroker();
        testCustomizer.updateSchema(context);
    }

    public ForwardedBackwardsCompatibleDataBroker createBackwardsCompatibleDataBroker() {
        return new ForwardedBackwardsCompatibleDataBroker(domBroker, testCustomizer.getBindingToNormalized(), testCustomizer.getSchemaService(), MoreExecutors
            .sameThreadExecutor());
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
        DataModificationTransaction writeTx =
            dataBroker.beginTransaction();

        writeTx.putOperationalData(NODE_PATH, NODE);

        writeTx.commit();

        // TOP_PATH should exist as it is the parent of NODE_PATH
        DataObject object = dataBroker.readOperationalData(TOP_PATH);

        assertNotNull(object);

    }


}
