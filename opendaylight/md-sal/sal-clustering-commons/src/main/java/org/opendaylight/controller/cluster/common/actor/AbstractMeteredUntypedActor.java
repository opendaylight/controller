package org.opendaylight.controller.cluster.common.actor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.typesafe.config.Config;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;

/**
 */
public abstract class AbstractMeteredUntypedActor extends AbstractUntypedActor {

    private final MetricRegistry METRICREGISTRY = MetricsReporter.getInstance().getMetricsRegistry();
    private final String MSG_PROCESSING_RATE = "msg-rate";

    private String actorName;
    private Timer msgProcessingTimer;

    public AbstractMeteredUntypedActor() {
        super();

        if (isMetricsCaptureEnabled()) {
            actorName = getSelf().path().toStringWithoutAddress();
            final String msgProcessingTime = MetricRegistry.name(actorName, MSG_PROCESSING_RATE);
            msgProcessingTimer = METRICREGISTRY.timer(msgProcessingTime);
        }
    }

    @Override public void onReceive(Object message) throws Exception {
        if (isMetricsCaptureEnabled())
            timedReceive(message);
        else
            super.onReceive(message);
    }

    private void timedReceive(Object message) throws Exception{
        final String messageType = message.getClass().getSimpleName();

        final String msgProcessingTimeByMsgType =
                MetricRegistry.name(actorName, MSG_PROCESSING_RATE, messageType);

        final Timer msgProcessingTimerByMsgType = METRICREGISTRY.timer(msgProcessingTimeByMsgType);

        //start timers
        final Timer.Context context = msgProcessingTimer.time();
        final Timer.Context contextByMsgType = msgProcessingTimerByMsgType.time();

        super.onReceive(message);

        //stop timers
        contextByMsgType.stop();
        context.stop();
    }
    private boolean isMetricsCaptureEnabled(){
        Config actorSystemConfig = getContext().system().settings().config();

        if (actorSystemConfig.hasPath("metric-capture-enabled"))
            return actorSystemConfig.getBoolean("metric-capture-enabled");
        else
            return false;
    }
}
