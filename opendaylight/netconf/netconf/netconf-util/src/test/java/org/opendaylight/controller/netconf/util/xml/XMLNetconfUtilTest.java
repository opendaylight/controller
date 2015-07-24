package org.opendaylight.controller.netconf.util.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import org.junit.Test;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.w3c.dom.Element;

public class XMLNetconfUtilTest {

    @Test
    public void testXPath() throws Exception {
        final XPathExpression correctXPath = XMLNetconfUtil.compileXPath("/top/innerText");
        try {
            XMLNetconfUtil.compileXPath("!@(*&$!");
            fail("Incorrect xpath should fail");
        } catch (IllegalStateException e) {}
        final Object value = XmlUtil.evaluateXPath(correctXPath, XmlUtil.readXmlToDocument("<top><innerText>value</innerText></top>"), XPathConstants.NODE);
        assertEquals("value", ((Element) value).getTextContent());
    }

}