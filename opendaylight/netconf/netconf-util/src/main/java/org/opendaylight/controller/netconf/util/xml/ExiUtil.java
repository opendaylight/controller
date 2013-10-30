package org.opendaylight.controller.netconf.util.xml;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.siemens.ct.exi.EXIFactory;
import com.siemens.ct.exi.api.dom.DOMBuilder;
import com.siemens.ct.exi.api.dom.DOMWriter;
import com.siemens.ct.exi.exceptions.EXIException;
import com.siemens.ct.exi.helpers.DefaultEXIFactory;

public class ExiUtil {

    private final static Logger logger = LoggerFactory.getLogger(ExiUtil.class);



    public static void encode(final Object msg, final ByteBuf out,
            ExiParameters parameters)
            throws EXIException, IOException, SAXException {
        final byte[] bytes = toExi(msg, parameters);
        out.writeBytes(bytes);
    }

    public static void decode(ByteBuf in, List<Object> out,
            ExiParameters parameters) throws ParserConfigurationException, EXIException
             {

        EXIFactory exiFactory = DefaultEXIFactory.newInstance();
        if (parameters.getGrammars() != null) {
            exiFactory.setGrammars(parameters.getGrammars());
        }

        if (parameters.getFidelityOptions() != null) {
            exiFactory.setFidelityOptions(parameters.getFidelityOptions());
        }

        exiFactory.setCodingMode(parameters.getCodingMode());
        ByteArrayInputStream exiIS = null;
        if (!(in instanceof EmptyByteBuf)){
            exiIS = new ByteArrayInputStream(in.readBytes(in.readableBytes()).array());
            DOMBuilder domBuilder = new DOMBuilder(exiFactory);
            NetconfMessage _nMessage = new NetconfMessage(domBuilder.parse(exiIS));
            out.add(_nMessage);
        }
    }

    private static byte[] toExi(Object msg, ExiParameters parameters) throws EXIException, IOException,
            SAXException {

        EXIFactory exiFactory = DefaultEXIFactory.newInstance();
        if (parameters.getGrammars() != null) {
            exiFactory.setGrammars(parameters.getGrammars());
        }

        if (parameters.getFidelityOptions() != null) {
            exiFactory.setFidelityOptions(parameters.getFidelityOptions());
        }

        exiFactory.setCodingMode(parameters.getCodingMode());

        ByteArrayOutputStream exiOS = new ByteArrayOutputStream();

        DOMWriter domWriter = new DOMWriter(exiFactory);
        domWriter.setOutput(exiOS);
        if (msg instanceof NetconfMessage){
            domWriter.encode( ((NetconfMessage)msg).getDocument()) ;
        } else {
            domWriter.encode( ((Document)msg));
        }
        exiOS.close();
        return exiOS.toByteArray();
    }

}
