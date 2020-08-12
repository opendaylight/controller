package org.opendaylight.controller.rym;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executors;

public class ConcurrentDataBrokerTestCustomizer extends AbstractDataBrokerTestCustomizer {

    private final ListeningExecutorService dataTreeChangeListenerExecutorSingleton;

    public ConcurrentDataBrokerTestCustomizer(boolean useMTDataTreeChangeListenerExecutor) {
        if (useMTDataTreeChangeListenerExecutor) {
            dataTreeChangeListenerExecutorSingleton = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        } else {
            dataTreeChangeListenerExecutorSingleton = MoreExecutors.newDirectExecutorService();
        }
    }

    @Override
    public ListeningExecutorService getCommitCoordinatorExecutor() {
        return MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    }

    @Override
    public ListeningExecutorService getDataTreeChangeListenerExecutor() {
        return dataTreeChangeListenerExecutorSingleton;
    }

}