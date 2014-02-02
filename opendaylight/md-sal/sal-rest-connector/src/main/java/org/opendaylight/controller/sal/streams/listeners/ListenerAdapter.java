package org.opendaylight.controller.sal.streams.listeners;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.ConcurrentSet;

import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class ListenerAdapter implements DataChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(ListenerAdapter.class);

    private final InstanceIdentifier path;
    private ListenerRegistration<DataChangeListener> registration;
    private final String streamName;
    private Set<Channel> subscribers = new ConcurrentSet<>(); // TODO it should be thread safe

    ListenerAdapter(InstanceIdentifier path, String streamName) {
        Preconditions.checkNotNull(path);
        Preconditions.checkArgument(streamName != null && !streamName.isEmpty());
        this.path = path;
        this.streamName = streamName;
    }

    @Override
    public void onDataChanged(DataChangeEvent<InstanceIdentifier, CompositeNode> change) {
        for (Channel subscriber : subscribers) {
            if (subscriber.isActive()) {
                logger.debug("Data are sent to subscriber {}:", subscriber.remoteAddress());
                // TODO replace foo with XML as a string
                logger.debug("foo");
                // TODO replace foo with XML as a string
                subscriber.writeAndFlush(new TextWebSocketFrame("foo"));
            } else {
                logger.debug("Subscriber {} is removed - channel is not active yet.", subscriber.remoteAddress());
                subscribers.remove(subscriber);
            }
        }
    }

    public InstanceIdentifier getPath() {
        return path;
    }

    public void setRegistration(ListenerRegistration<DataChangeListener> registration) {
        this.registration = registration;
    }

    public String getStreamName() {
        return streamName;
    }

    public void close() throws Exception {
        subscribers = new ConcurrentSet<>();
        registration.close();
        registration = null;
    }

    public boolean isListening() {
        return registration == null ? false : true;
    }

    public boolean addSubscriber(Channel subscriber) {
        if (!subscriber.isActive()) {
            logger.debug("Channel is not active between websocket server and subscriber {}"
                    + subscriber.remoteAddress());
            return false;
        }
        if (subscribers.contains(subscriber)) {
            return false;
        }
        subscribers.add(subscriber);
        return true;
    }
    
    public void removeSubscriber(Channel subscriber) {
        if (subscriber.isActive()) {
            logger.debug("Subscriber {} is removed.", subscriber.remoteAddress());
            subscribers.remove(subscriber);
        }
    }
    
    public boolean hasSubscribers() {
        return !subscribers.isEmpty();
    }
    
}
