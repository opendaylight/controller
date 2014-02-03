package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.*;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DataChangeTest {
    
    String xmlValue = null;
    private class MyAnswer implements Answer<TextWebSocketFrame> {
      @Override
      public TextWebSocketFrame answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        String xmlValue = ((TextWebSocketFrame) args[0]).text();
        assertTrue(validateXmlOutterElements(xmlValue));
        int startIndex = xmlValue.indexOf("<data-change-event>");
        int endIndex = xmlValue.indexOf("</data-change-event>");
        assertTrue(validateXmlNotificationStream(xmlValue.substring(startIndex,endIndex), "operation", "created", "users"));
        return null;
      }

    };
    

    @Test
    public void test() throws FileNotFoundException {
        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/yangs");
        ControllerContext.getInstance().setGlobalSchema(schemaContext);
        
        ListenerAdapter listener = Notificator.createListener(new InstanceIdentifier(new ArrayList<PathArgument>()), "streamName");
        LocalChannel mockedChannel = mock(LocalChannel.class);
        when(mockedChannel.isActive()).thenReturn(true);
        listener.addSubscriber(mockedChannel);
        
        DataChangeEventImpl changeEvent = new DataChangeEventImpl();
        changeEvent.addCreatedOperationalData(prepareInstanceIdentifier(), prepareCompositeNode());
        when(mockedChannel.writeAndFlush(any(TextWebSocketFrame.class))).then(new MyAnswer());

        listener.onDataChanged(changeEvent);
//        verify(mockedChannel.writeAndFlush(any(TextWebSocketFrame.class)));
        
        
        
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
         
    }

    /*
     * <notification xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">
     * <eventTime>2008-07-08T00:01:00Z</eventTime> <data-changed-notification
     * xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote">
     * <path></path> <store>operational</store> <operation>created</operation>
     * <data> </data> </data-changed-notification> </notification>
     */
    
    private boolean validateXmlOutterElements(String xmlStream) {
        StringBuilder regex = new StringBuilder();
        regex.append("^");
        // notification tag
        regex.append(".*<notification");
        regex.append(".*xmlns=\"urn:ietf:params:xml:ns:netconf:notification:1.0\"");
        regex.append(".*>");

//        // eventTime tag
//        regex.append(".*<eventTime>");
//        regex.append(".*");
//        regex.append(".*</eventTime>");
        
        // eventTime tag
        regex.append(".*<data-changed-notification");
        regex.append(".*xmlns=\"urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote\"");
        regex.append(".*>");

        regex.append(".*<data-change-event>");
        regex.append(".*</data-change-event>");

        regex.append(".*</data-changed-notification>");
        regex.append(".*</notification>");
        regex.append(".*");
        regex.append("$");

        Pattern compiledPattern = Pattern.compile(regex.toString(), Pattern.DOTALL);
        return compiledPattern.matcher(xmlStream).find();
        
    }
    private boolean validateXmlNotificationStream(String xmlStream, String storeMode, String operationType,String path) {
        StringBuilder regex = new StringBuilder();
        regex.append("^");
        // notification tag
        
        // path tag
        regex.append(".*<path");
        regex.append(".*>");
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
        regex.append(".*test user");
        regex.append(".*</user>");

        regex.append(".*<group>");
        regex.append(".*test group");
        regex.append(".*</group>");

        regex.append(".*</users>");
        // :content of data tag

        regex.append(".*</data>");
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
    
    
    private class DataChangeEventImpl implements DataChangeEvent<InstanceIdentifier, CompositeNode> {

        private Map<InstanceIdentifier, CompositeNode> createdOperationalData = new HashMap<InstanceIdentifier, CompositeNode>();
        private Map<InstanceIdentifier, CompositeNode> createdConfigurationData = new HashMap<InstanceIdentifier, CompositeNode>();

        public void addCreatedOperationalData(InstanceIdentifier instanceIdentifier, CompositeNode compNode) {
            createdOperationalData.put(instanceIdentifier, compNode);
        }

        public void addCreatedConfigurationData(InstanceIdentifier instanceIdentifier, CompositeNode compNode) {
            createdConfigurationData.put(instanceIdentifier, compNode);
        }
        
        @Override
        public Map<InstanceIdentifier, CompositeNode> getCreatedOperationalData() {
            
            return createdOperationalData;
        }

        @Override
        public Map<InstanceIdentifier, CompositeNode> getCreatedConfigurationData() {
            return createdConfigurationData;
        }

        @Override
        public Map<InstanceIdentifier, CompositeNode> getUpdatedOperationalData() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<InstanceIdentifier, CompositeNode> getUpdatedConfigurationData() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<InstanceIdentifier> getRemovedConfigurationData() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<InstanceIdentifier> getRemovedOperationalData() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<InstanceIdentifier, CompositeNode> getOriginalConfigurationData() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<InstanceIdentifier, CompositeNode> getOriginalOperationalData() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public CompositeNode getOriginalConfigurationSubtree() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public CompositeNode getOriginalOperationalSubtree() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public CompositeNode getUpdatedConfigurationSubtree() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public CompositeNode getUpdatedOperationalSubtree() {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    

}
