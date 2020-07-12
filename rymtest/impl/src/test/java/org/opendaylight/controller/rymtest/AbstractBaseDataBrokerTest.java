package org.opendaylight.controller.rymtest;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractBaseDataBrokerTest extends AbstractSchemaAwareTest {
    private static final int ASSERT_COMMIT_DEFAULT_TIMEOUT = 5000;

    private AbstractDataBrokerTestCustomizer testCustomizer;
    private DataBroker dataBroker;
    private DOMDataBroker domBroker;

    protected abstract AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer();

    public AbstractDataBrokerTestCustomizer getDataBrokerTestCustomizer() {
        if (testCustomizer == null) {
            throw new IllegalStateException("testCustomizer not yet set by call to createDataBrokerTestCustomizer()");
        }
        return testCustomizer;
    }

    @Override
    protected void setupWithSchema(final SchemaContext context) {
        testCustomizer = createDataBrokerTestCustomizer();
        dataBroker = testCustomizer.createDataBroker();
        domBroker = testCustomizer.getDOMDataBroker();
        testCustomizer.updateSchema(context);
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public DOMDataBroker getDomBroker() {
        return domBroker;
    }

    protected static final void assertCommit(final ListenableFuture<Void> commit) {
        assertCommit(commit, ASSERT_COMMIT_DEFAULT_TIMEOUT);
    }

    protected static final void assertCommit(final ListenableFuture<Void> commit, long timeoutInMS) {
        try {
            commit.get(timeoutInMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }
}
