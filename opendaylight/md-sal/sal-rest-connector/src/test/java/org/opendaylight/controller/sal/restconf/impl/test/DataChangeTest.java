package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;

public class DataChangeTest {

    @Test
    public void test() {
        // DataChangeEvent<InstanceIdentifier, CompositeNode>
        String xmlStream = 
        "<notification xmlns=\"urn:ietf:params:xml:ns:netconf:notification:1.0\">"+
        "<eventTime>2008-07-08T00:01:00Z</eventTime>" +
        "<data-changed-notification xmlns=\"urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote\">" +
         "   <path>/simple-nodes:users</path>"+
         "   <store>operational</store>"+
         "   <operation>created</operation>"+
         "   <data>" +
         "      <users xmlns=\"urn:opendaylight:simple-nodes\">"+
         "             <user>user test"+
         "             </user>"+
         "             <group>group test"+
         "             </group>"+
         "      </users>"+
         "   </data>"+
        "</data-changed-notification>"+
    "</notification>";
         
        assertTrue(validateXmlNotificationStream(xmlStream, "operational", "created","/simple-nodes:users"));
    }

    /*
     * <notification xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">
     * <eventTime>2008-07-08T00:01:00Z</eventTime> <data-changed-notification
     * xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote">
     * <path></path> <store>operational</store> <operation>created</operation>
     * <data> </data> </data-changed-notification> </notification>
     */
    private boolean validateXmlNotificationStream(String xmlStream, String storeMode, String operationType,String path) {
        StringBuilder regex = new StringBuilder();
        regex.append("^");
        // notification tag
        regex.append(".*<notification");
        regex.append(".*xmlns=\"urn:ietf:params:xml:ns:netconf:notification:1.0\"");
        regex.append(".*>");

        // eventTime tag
        regex.append(".*<eventTime>");
        regex.append(".*");
        regex.append(".*</eventTime>");
        
        // eventTime tag
        regex.append(".*<data-changed-notification");
        regex.append(".*xmlns=\"urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote\"");
        regex.append(".*>");

        // path tag
        regex.append(".*<path>");
        regex.append(".*"+path);
        regex.append(".*</path>");

        // store tag
        regex.append(".*<store>");
        regex.append(".*" + storeMode);
        regex.append(".*</store>");

        // operation tag
        regex.append(".*<operation>");
        regex.append(".*" + operationType);
        regex.append(".*</operation>");

        // data tag
        regex.append(".*<data>");

        // content of data tag
        regex.append(".*<users");
        regex.append(".*xmlns=\"urn:opendaylight:simple-nodes\"");
        regex.append(".*>");

        regex.append(".*<user>");
        regex.append(".*user test");
        regex.append(".*</user>");

        regex.append(".*<group>");
        regex.append(".*group test");
        regex.append(".*</group>");

        regex.append(".*</users>");
        // :content of data tag

        regex.append(".*</data>");
        regex.append(".*</data-changed-notification>");
        regex.append(".*</notification>");
        regex.append(".*");
        regex.append("$");

        Pattern compiledPattern = Pattern.compile(regex.toString(), Pattern.DOTALL);
        return compiledPattern.matcher(xmlStream).find();
    }

    // simple composite node structure with data
    private CompositeNode prepareCompositeNode() {
        MutableCompositeNode users = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("users", "urn:opendaylight:simple-nodes", "2013-07-30"), null, null, null, null);
        MutableSimpleNode<?> user = NodeFactory.createMutableSimpleNode(
                TestUtils.buildQName("user", "urn:opendaylight:simple-nodes", "2013-07-30"), users, "test user", null,
                null);
        MutableSimpleNode<?> group = NodeFactory.createMutableSimpleNode(
                TestUtils.buildQName("group", "urn:opendaylight:simple-nodes", "2013-07-30"), users, "test group",
                null, null);
        users.getChildren().add(user);
        users.getChildren().add(group);
        users.init();

        return users;
    }

    // path to container users
    private InstanceIdentifier prepareInstanceIdentifier() {
        List<PathArgument> pathArguments = new ArrayList<>();
        pathArguments.add(new NodeIdentifier(TestUtils.buildQName("users", "urn:opendaylight:simple-nodes",
                "2013-07-30")));
        return new InstanceIdentifier(pathArguments);
    }

}
