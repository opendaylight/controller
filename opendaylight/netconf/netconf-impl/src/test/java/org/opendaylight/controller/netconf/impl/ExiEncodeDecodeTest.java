package org.opendaylight.controller.netconf.impl;

import static junit.framework.Assert.assertNotNull;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.ExiParameters;
import org.opendaylight.controller.netconf.util.xml.ExiUtil;
import org.opendaylight.controller.netconf.util.xml.XmlElement;



public class ExiEncodeDecodeTest  {
    @Test
    public void encodeExi() throws Exception{

        String startExiString = XmlFileLoader.xmlFileToString("netconfMessages/startExi.xml");
        assertNotNull(startExiString);

        NetconfMessage startExiMessage = XmlFileLoader.xmlFileToNetconfMessage(("netconfMessages/startExi.xml"));
        assertNotNull(startExiMessage);

        ExiParameters exiParams = new ExiParameters();
        exiParams.setParametersFromXmlElement(XmlElement.fromDomElement(startExiMessage.getDocument().getDocumentElement()));
        assertNotNull(exiParams);

        ByteBuf encodedBuf = Unpooled.buffer();
        ByteBuf sourceBuf = Unpooled.copiedBuffer(startExiString.getBytes());
        ExiUtil.encode(sourceBuf, encodedBuf, exiParams);

        List<Object> newOut = new ArrayList<Object>();
        ExiUtil.decode(encodedBuf, newOut, exiParams);

        ByteBuf decodedBuf = (ByteBuf)newOut.get(0);
        String decodedString = new String(decodedBuf.array(),"UTF-8");
        assertNotNull(decodedString);
    }
}
