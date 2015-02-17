package org.opendaylight.controller.md.sal.binding.test;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class AbstractNotificationBrokerTest extends AbstractSchemaAwareTest{
    private DataBrokerTestCustomizer testCustomizer;
    private NotificationService notificationService;
    private NotificationPublishService notificationPublishService;


    @Override
    protected void setupWithSchema(final SchemaContext context) {
        testCustomizer = createDataBrokerTestCustomizer();
        notificationService = testCustomizer.createNotificationService();
        notificationPublishService = testCustomizer.createNotificationPublishService();
        testCustomizer.updateSchema(context);
    }

    protected DataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        return new DataBrokerTestCustomizer();
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public NotificationPublishService getNotificationPublishService() {
        return notificationPublishService;
    }
}
