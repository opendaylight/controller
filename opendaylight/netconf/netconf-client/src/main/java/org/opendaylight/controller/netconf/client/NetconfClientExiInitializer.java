package org.opendaylight.controller.netconf.client;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.util.handler.NetconfEXICodec;
import org.opendaylight.controller.netconf.util.handler.NetconfEXIToMessageDecoder;
import org.opendaylight.controller.netconf.util.handler.NetconfMessageToEXIEncoder;
import org.opendaylight.controller.netconf.util.xml.EXIParameters;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.openexi.proc.common.EXIOptionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by martin on 3/21/14.
 */
public class NetconfClientExiInitializer {

    private static final String startExiMessagePath = "/startExi.xml";
    private static final String stopExiMessagePath = "/stopExi.xml";
    private static final Logger logger = LoggerFactory.getLogger(NetconfClientExiInitializer.class);

    public static void initializeExiEncodedCommunication(NetconfSession clientSession){
        try {
            addExiHandlers(clientSession,callStartExiRPC(clientSession));
        } catch (EXIOptionsException e){
            logger.debug("Initialization of exi encoded communication failed, sending stop-exi.");
            callStopExiRPC(clientSession);
        }
    }
    public static void terminateExiEncodedCommunication(NetconfSession session){
        callStopExiRPC(session);
        session.remove(NetconfEXIToMessageDecoder.class);
        session.remove(NetconfMessageToEXIEncoder.class);
    }
    private static NetconfMessage callStartExiRPC(NetconfSession session){
        NetconfMessage startExiMessage = loadExiMessageTemplate(startExiMessagePath);
        session.sendMessage(startExiMessage);
        return startExiMessage;
    }
    private static void callStopExiRPC(NetconfSession session){
        NetconfMessage stopExiMessage = loadExiMessageTemplate(stopExiMessagePath);
        session.sendMessage(stopExiMessage);
    }
    private static void addExiHandlers(NetconfSession session,NetconfMessage startExiMessage) throws EXIOptionsException {

        final EXIParameters exiParams;
            exiParams = EXIParameters.forXmlElement(XmlElement.fromDomDocument(startExiMessage.getDocument()));
            NetconfEXICodec exiCodec = new NetconfEXICodec(exiParams.getOptions());
            session.addExiDecoder("exiDecoder", new NetconfEXIToMessageDecoder(exiCodec));
            session.addExiEncoder("exiEncoder", new NetconfMessageToEXIEncoder(exiCodec));
    }
    private static NetconfMessage loadExiMessageTemplate(String exiMessagePath) {
        try (InputStream is = NetconfClientSessionNegotiatorFactory.class.getResourceAsStream(exiMessagePath)) {
            Preconditions.checkState(is != null, "Input stream from %s was null", exiMessagePath);
            return new NetconfMessage(XmlUtil.readXmlToDocument(is));
        } catch (SAXException | IOException e) {
            throw new RuntimeException(String.format("Unable to load file %s due to",exiMessagePath, e));
        }
    }

}
