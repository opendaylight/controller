package org.opendaylight.tutorial.mibs;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.snmp.plugin.internal.SNMPImpl;
import org.opendaylight.yang.gen.v1.org.opendaylight.tutorial.mibs.rev140922.GetDocsIfMibInput;
import org.opendaylight.yang.gen.v1.org.opendaylight.tutorial.mibs.rev140922.GetDocsIfMibOutput;
import org.opendaylight.yang.gen.v1.org.opendaylight.tutorial.mibs.rev140922.GetDocsIfMibOutputBuilder;
import org.opendaylight.yang.gen.v1.org.opendaylight.tutorial.mibs.rev140922.MibsTutorialService;
import org.opendaylight.yang.gen.v1.org.opendaylight.tutorial.mibs.rev140922.get.docs._if.mib.output.TutorialObjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2.docs._if.mib.rev060524.DocsIfCmtsObjects;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2.docs._if.mib.rev060524.DocsIfCmtsObjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2.docs._if.mib.rev060524.docsifcmtsobjectsgroup.DocsIfCmtsCmStatusEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2.docs._if.mib.rev060524.docsifcmtsobjectsgroup.DocsIfCmtsCmStatusEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

/**
 * Created by mininet on 2/16/15.
 */
public class MIBTutorialImpl implements MibsTutorialService {
    private SnmpService snmpService;


    public MIBTutorialImpl(SnmpService snmpService) {
        this.snmpService = snmpService;
    }

    private DocsIfCmtsObjects getDocsIfCmtsObjects(Ipv4Address ipv4Address) {
        SNMPImpl snmp = (SNMPImpl) snmpService;

        DocsIfCmtsObjectsBuilder docsIfCmtsObjectsBuilder = new DocsIfCmtsObjectsBuilder();
        Collection<DocsIfCmtsCmStatusEntryBuilder> docsIfCmtsCmStatusEntryBuilders = snmp.populateMibTable(ipv4Address, DocsIfCmtsCmStatusEntryBuilder.class);

        ArrayList<DocsIfCmtsCmStatusEntry> docsIfCmtsCmStatusEntries = new ArrayList<>();
        for (DocsIfCmtsCmStatusEntryBuilder cmtsCmStatusEntryBuilder : docsIfCmtsCmStatusEntryBuilders) {
            docsIfCmtsCmStatusEntries.add(cmtsCmStatusEntryBuilder.build());
        }

        docsIfCmtsObjectsBuilder.setDocsIfCmtsCmStatusEntry(docsIfCmtsCmStatusEntries);
        DocsIfCmtsObjects docsIfCmtsObjects = docsIfCmtsObjectsBuilder.build();

        return docsIfCmtsObjects;
    }

    @Override
    public Future<RpcResult<GetDocsIfMibOutput>> getDocsIfMib(GetDocsIfMibInput input) {

        GetDocsIfMibOutputBuilder getDocsIfMibOutputBuilder = new GetDocsIfMibOutputBuilder();
        TutorialObjectsBuilder tutorialObjectsBuilder = new TutorialObjectsBuilder(getDocsIfCmtsObjects(input.getIpAddress()));
        getDocsIfMibOutputBuilder.setTutorialObjects(tutorialObjectsBuilder.build());

        RpcResultBuilder<GetDocsIfMibOutput> rpcResultBuilder = RpcResultBuilder.success(getDocsIfMibOutputBuilder.build());
        return Futures.immediateFuture(rpcResultBuilder.build());
    }
}
