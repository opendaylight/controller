package org.opendaylight.controller.cluster.datastore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class CompositeModificationPayloadTest {


    private static final String SERIALIZE_OUT = "serialize.out";

    @After
    public void shutDown(){
        File f = new File(SERIALIZE_OUT);
        if(f.exists()){
            f.delete();
        }
    }

    @Test
    public void testBasic() throws IOException {

        List<ReplicatedLogEntry> entries = new ArrayList<>();

        entries.add(0, new ReplicatedLogEntry() {
            @Override public Payload getData() {
                WriteModification writeModification =
                    new WriteModification(TestModel.TEST_PATH, ImmutableNodes
                        .containerNode(TestModel.TEST_QNAME),
                        TestModel.createTestContext());

                MutableCompositeModification compositeModification =
                    new MutableCompositeModification();

                compositeModification.addModification(writeModification);

                return new CompositeModificationPayload(compositeModification.toSerializable());
            }

            @Override public long getTerm() {
                return 1;
            }

            @Override public long getIndex() {
                return 1;
            }

            @Override
            public int size() {
                return getData().size();
            }
        });

        AppendEntries appendEntries =
            new AppendEntries(1, "member-1", 0, 100, entries, 1, -1);

        AppendEntriesMessages.AppendEntries o = (AppendEntriesMessages.AppendEntries) appendEntries.toSerializable();

        o.writeDelimitedTo(new FileOutputStream(SERIALIZE_OUT));

        AppendEntriesMessages.AppendEntries appendEntries2 =
            AppendEntriesMessages.AppendEntries
                .parseDelimitedFrom(new FileInputStream(SERIALIZE_OUT));

        AppendEntries appendEntries1 = AppendEntries.fromSerializable(appendEntries2);

        Payload data = appendEntries1.getEntries().get(0).getData();


        Assert.assertTrue(((CompositeModificationPayload) data).getModification().toString().contains(TestModel.TEST_QNAME.getNamespace().toString()));

    }

}
