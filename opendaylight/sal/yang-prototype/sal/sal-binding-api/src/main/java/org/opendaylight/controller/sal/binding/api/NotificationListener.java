package org.opendaylight.controller.sal.binding.api;

public interface NotificationListener<T> {

    void onNotification(T notification);
}
