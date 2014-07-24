package org.opendaylight.controller.cluster.raft.protobuff;

import com.google.protobuf.GeneratedMessage;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.protobuff.messages.AppendEntriesMessages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Encoder Decoder for all Raft related Protocol Buffer work.
 */
public class ProtoBuffEncoderDecoderUtil {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ProtoBuffEncoderDecoderUtil.class);

    public static <T extends Object> Object encode(Object message) {
        if (message instanceof AppendEntries) {
            return encodeAppendEntries((AppendEntries) message);
        }

        return message;
    }

    private static <T extends Object> AppendEntriesMessages.AppendEntries encodeAppendEntries(AppendEntries appendEntries) {

        AppendEntriesMessages.AppendEntries.Builder aBuilder = AppendEntriesMessages.AppendEntries.newBuilder();
        aBuilder.setTerm(appendEntries.getTerm())
            .setLeaderId(appendEntries.getLeaderId())
            .setPrevLogTerm(appendEntries.getPrevLogTerm())
            .setPrevLogIndex(appendEntries.getPrevLogIndex())
            .setLeaderCommit(appendEntries.getLeaderCommit());

        for (ReplicatedLogEntry logEntry : appendEntries.getEntries()) {

            AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Builder arBuilder =
                AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.newBuilder();

            AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload.Builder arpBuilder =
                AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload.newBuilder();

            //get the client specific payload extensions and add them to the payload builder
            Map<GeneratedMessage.GeneratedExtension, T> map = logEntry.getData().getProtoBuffExtensions();
            Iterator<Map.Entry<GeneratedMessage.GeneratedExtension, T>> iter = map.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry<GeneratedMessage.GeneratedExtension, T> entry = iter.next();
                arpBuilder.setExtension(entry.getKey(), entry.getValue());
            }

            arpBuilder.setClientPayloadClassName(logEntry.getData().getClientPayloadClassName());

            arBuilder.setData(arpBuilder).setIndex(logEntry.getIndex()).setTerm(logEntry.getTerm());
            aBuilder.addLogEntries(arBuilder);
        }

        return aBuilder.build();
    }



    public static Object decode(Object message) {
        if (message instanceof AppendEntriesMessages.AppendEntries) {
            return decodeAppendEntries((AppendEntriesMessages.AppendEntries) message);
        }
        return message;
    }

    private static AppendEntries decodeAppendEntries(AppendEntriesMessages.AppendEntries aeProtoBuff) {
        List<ReplicatedLogEntry> logEntryList = new ArrayList<>();
        for (AppendEntriesMessages.AppendEntries.ReplicatedLogEntry leProtoBuff : aeProtoBuff.getLogEntriesList()) {

            Payload payload = null ;
            try {
                if(leProtoBuff.getData() != null && leProtoBuff.getData().getClientPayloadClassName() != null) {
                    String clientPayloadClassName = leProtoBuff.getData().getClientPayloadClassName();
                    payload = (Payload)Class.forName(clientPayloadClassName).newInstance();
                    payload = payload.constructClientPayload(leProtoBuff.getData());
                    payload.setClientPayloadClassName(clientPayloadClassName);
                } else {
                    LOG.error("ERROR!! payload is null or payload does not have client payload class name");
                }

            } catch (InstantiationException e) {
               LOG.error("InstantiationException when instantiating "+leProtoBuff.getData().getClientPayloadClassName(), e);
            } catch (IllegalAccessException e) {
                LOG.error("IllegalAccessException when accessing "+leProtoBuff.getData().getClientPayloadClassName(), e);
            } catch (ClassNotFoundException e) {
                LOG.error("ClassNotFoundException when loading "+leProtoBuff.getData().getClientPayloadClassName(), e);
            }
            ReplicatedLogEntry logEntry = new RaftActor.ReplicatedLogImplEntry(
                leProtoBuff.getIndex(), leProtoBuff.getTerm(), payload);
            logEntryList.add(logEntry);
        }

        AppendEntries ae = new AppendEntries(aeProtoBuff.getTerm(),
            aeProtoBuff.getLeaderId(),
            aeProtoBuff.getPrevLogIndex(),
            aeProtoBuff.getPrevLogTerm(),
            logEntryList,
            aeProtoBuff.getLeaderCommit());

        return ae;

    }

}
