package org.opendaylight.controller.sal.streams.listeners;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class Notificator {

    private static Map<String, ListenerAdapter> listenersByStreamName = new ConcurrentHashMap<>();
    private static Map<InstanceIdentifier, ListenerAdapter> listenersByInstanceIdentifier = new ConcurrentHashMap<>();
    private static final Lock lock = new ReentrantLock();

    private Notificator() {
    }

    public static ListenerAdapter getListenerFor(String streamName) {
        return listenersByStreamName.get(streamName);
    }

    public static ListenerAdapter getListenerFor(InstanceIdentifier path) {
        return listenersByInstanceIdentifier.get(path);
    }

    public static boolean existListenerFor(InstanceIdentifier path) {
        return listenersByInstanceIdentifier.containsKey(path);
    }

    public static ListenerAdapter createListener(InstanceIdentifier path, String streamName) {
        ListenerAdapter listener = new ListenerAdapter(path, streamName);
        try {
            lock.lock();
            listenersByInstanceIdentifier.put(path, listener);
            listenersByStreamName.put(streamName, listener);
        } finally {
            lock.unlock();
        }
        return listener;
    }

    public static void removeListener(InstanceIdentifier path) {
        ListenerAdapter listener = listenersByInstanceIdentifier.get(path);
        deleteListener(listener);
    }

    public static String createStreamNameFromUri(String uri) {
        if (uri == null) {
            return null;
        }
        String result = uri;
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        if (result.endsWith("/")) {
            result = result.substring(0, result.length());
        }
        return result;
    }

    public static void removeAllListeners() {
        for (ListenerAdapter listener : listenersByInstanceIdentifier.values()) {
            try {
                listener.close();
            } catch (Exception e) {
            }
        }
        try {
            lock.lock();
            listenersByStreamName = new ConcurrentHashMap<>();
            listenersByInstanceIdentifier = new ConcurrentHashMap<>();
        } finally {
            lock.unlock();
        }
    }

    public static void removeListenerIfNoSubscriberExists(ListenerAdapter listener) {
        if (!listener.hasSubscribers()) {
            deleteListener(listener);
        }
    }

    private static void deleteListener(ListenerAdapter listener) {
        if (listener != null) {
            try {
                listener.close();
            } catch (Exception e) {
            }
            try {
                lock.lock();
                listenersByInstanceIdentifier.remove(listener.getPath());
                listenersByStreamName.remove(listener.getStreamName());
            } finally {
                lock.unlock();
            }
        }
    }

}