package org.opendaylight.controller.mdsal.it.base;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.CoreOptions.composite;

import java.util.Calendar;

import javax.inject.Inject;

import org.junit.Before;
import org.opendaylight.controller.config.it.base.AbstractConfigTestBase;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMdsalTestBase extends AbstractConfigTestBase implements BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMdsalTestBase.class);
    private static final int REGISTRATION_TIMEOUT = 70000;
    @Inject @Filter(timeout=60000)
    private BundleContext context;
    private ProviderContext session = null;

    public ProviderContext getSession() {
        return session;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("Session Initiated: {}",session);
        this.session = session;
    }

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
        Calendar start = Calendar.getInstance();
        ServiceReference<BindingAwareBroker> serviceReference = context.getServiceReference(BindingAwareBroker.class);
        if(serviceReference == null) {
            throw new RuntimeException("BindingAwareBroker not found");
        }
        BindingAwareBroker broker = context.getService(serviceReference);
        broker.registerProvider(this);
        for(int i=0;i<REGISTRATION_TIMEOUT;i++) {
            if(session !=null) {
                Calendar stop = Calendar.getInstance();
                LOG.info("Registered session {} with the MD-SAL after {} ms",
                        session,
                        stop.getTimeInMillis() - start.getTimeInMillis());
                return;
            } else {
                Thread.sleep(1);
            }
        }
        throw new RuntimeException("Session not initiated after " + REGISTRATION_TIMEOUT + " ms");
    }

    @Override
    public Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(AbstractMdsalTestBase.class),
                        LogLevel.INFO.name());
        option = composite(option, super.getLoggingOption());
        return option;
    }

}
