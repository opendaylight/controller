package org.opendaylight.controller.sal.streams.listeners;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

/**
 * {@link Notificator} is responsible to create, remove and find {@link ListenerAdapter} listener.
 */
public class Notificator {

	private static Map<String, ListenerAdapter> listenersByStreamName = new ConcurrentHashMap<>();
	private static Map<InstanceIdentifier, ListenerAdapter> listenersByInstanceIdentifier = new ConcurrentHashMap<>();
	private static final Lock lock = new ReentrantLock();

	private Notificator() {
	}

	/**
	 * Gets {@link ListenerAdapter} specified by stream name.
	 * 
	 * @param streamName
	 *            The name of the stream.
	 * @return {@link ListenerAdapter} specified by stream name.
	 */
	public static ListenerAdapter getListenerFor(String streamName) {
		return listenersByStreamName.get(streamName);
	}

	/**
	 * Gets {@link ListenerAdapter} listener specified by
	 * {@link InstanceIdentifier} path.
	 * 
	 * @param path
	 *            Path to data in data repository.
	 * @return ListenerAdapter
	 */
	public static ListenerAdapter getListenerFor(InstanceIdentifier path) {
		return listenersByInstanceIdentifier.get(path);
	}

	/**
	 * Checks if the listener specified by {@link InstanceIdentifier} path
	 * exist.
	 * 
	 * @param path
	 *            Path to data in data repository.
	 * @return True if the listener exist, false otherwise.
	 */
	public static boolean existListenerFor(InstanceIdentifier path) {
		return listenersByInstanceIdentifier.containsKey(path);
	}

	/**
	 * Creates new {@link ListenerAdapter} listener from
	 * {@link InstanceIdentifier} path and stream name.
	 * 
	 * @param path
	 *            Path to data in data repository.
	 * @param streamName
	 *            The name of the stream.
	 * @return New {@link ListenerAdapter} listener from
	 *         {@link InstanceIdentifier} path and stream name.
	 */
	public static ListenerAdapter createListener(InstanceIdentifier path,
			String streamName) {
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

	/**
	 * Looks for listener determined by {@link InstanceIdentifier} path and
	 * removes it.
	 * 
	 * @param path
	 *            InstanceIdentifier
	 */
	public static void removeListener(InstanceIdentifier path) {
		ListenerAdapter listener = listenersByInstanceIdentifier.get(path);
		deleteListener(listener);
	}

	/**
	 * Creates String representation of stream name from URI. Removes slash from
	 * URI in start and end position.
	 * 
	 * @param uri
	 *            URI for creation stream name.
	 * @return String representation of stream name.
	 */
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

	/**
	 * Removes all listeners.
	 */
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

	/**
	 * Checks if listener has at least one subscriber. In case it has any, delete
	 * listener.
	 * 
	 * @param listener
	 *            ListenerAdapter
	 */
	public static void removeListenerIfNoSubscriberExists(
			ListenerAdapter listener) {
		if (!listener.hasSubscribers()) {
			deleteListener(listener);
		}
	}

	/**
	 * Delete {@link ListenerAdapter} listener specified in parameter.
	 * 
	 * @param listener
	 *            ListenerAdapter
	 */
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