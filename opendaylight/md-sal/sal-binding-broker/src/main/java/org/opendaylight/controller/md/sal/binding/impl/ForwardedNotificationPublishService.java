package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class ForwardedNotificationPublishService implements NotificationPublishService {
    private final BindingNormalizedNodeSerializer codecRegistry;
    private final DOMNotificationPublishService domPublishService;

    public ForwardedNotificationPublishService(BindingNormalizedNodeSerializer codecRegistry, DOMNotificationPublishService domPublishService) {
        this.codecRegistry = codecRegistry;
        this.domPublishService = domPublishService;
    }

    @Override
    public void putNotification(Notification notification) throws InterruptedException {
        domPublishService.putNotification(toDomNotification(notification));
    }

    @Override
    public boolean offerNotification(Notification notification) {
        final ListenableFuture<?> listenableFuture = domPublishService.offerNotification(toDomNotification(notification));
        return !DOMNotificationPublishService.REJECTED.equals(listenableFuture);
    }

    @Override
    public boolean offerNotification(Notification notification, int timeout, TimeUnit unit) throws InterruptedException {
        final ListenableFuture<?> listenableFuture =
                domPublishService.offerNotification(toDomNotification(notification), timeout, unit);
        return !DOMNotificationPublishService.REJECTED.equals(listenableFuture);
    }

    private DOMNotification toDomNotification(Notification notification) {
        final ContainerNode domNotification = codecRegistry.toNormalizedNodeNotification(notification);
        return new DOMNotificationImpl(domNotification);
    }

    private static class DOMNotificationImpl implements DOMNotification {

        private final SchemaPath type;
        private final ContainerNode body;

        public DOMNotificationImpl(ContainerNode body) {
            this.type = SchemaPath.create(true, body.getIdentifier().getNodeType());
            this.body = body;
        }

        @Nonnull
        @Override
        public SchemaPath getType() {
            return this.type;
        }

        @Nonnull
        @Override
        public ContainerNode getBody() {
            return this.body;
        }
    }
}
