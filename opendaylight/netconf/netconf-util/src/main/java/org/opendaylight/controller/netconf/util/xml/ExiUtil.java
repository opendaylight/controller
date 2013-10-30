package org.opendaylight.controller.netconf.util.xml;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

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
            ExiParameters parameters) throws ParserConfigurationException, EXIException, IOException
             {
        if (in instanceof EmptyByteBuf){
            return;
        }

        EXIFactory exiFactory = DefaultEXIFactory.newInstance();
        if (parameters.getGrammars() != null) {
            exiFactory.setGrammars(parameters.getGrammars());
        }

        if (parameters.getFidelityOptions() != null) {
            exiFactory.setFidelityOptions(parameters.getFidelityOptions());
        }

        exiFactory.setCodingMode(parameters.getCodingMode());
        try (ByteArrayInputStream exiIS = new ByteArrayInputStream(((ByteBuf)in).readBytes(((ByteBuf)in).readableBytes()).array())){
            DOMBuilder domBuilder = new DOMBuilder(exiFactory);
            ByteBuf result = Unpooled.copiedBuffer(XmlUtil.toString(domBuilder.parse(exiIS)).getBytes());
            exiIS.close();
            out.add(result);
        }
    }

    private static byte[] toExi(Object msg, ExiParameters parameters) throws EXIException, IOException,
            SAXException {

        if (!(msg instanceof ByteBuf)){
              return Unpooled.EMPTY_BUFFER.array();
        }

        EXIFactory exiFactory = DefaultEXIFactory.newInstance();
        if (parameters.getGrammars() != null) {
            exiFactory.setGrammars(parameters.getGrammars());
        }

        if (parameters.getFidelityOptions() != null) {
            exiFactory.setFidelityOptions(parameters.getFidelityOptions());
        }

        Document doc = XmlUtil.readXmlToDocument(new String( ((ByteBuf)msg).readBytes(((ByteBuf)msg).readableBytes()).array(),"UTF-8"));
        exiFactory.setCodingMode(parameters.getCodingMode());

        try (ByteArrayOutputStream exiOS = new ByteArrayOutputStream()){
            DOMWriter domWriter = new DOMWriter(exiFactory);
            domWriter.setOutput(exiOS);
            domWriter.encode(doc) ;
            exiOS.close();
            return exiOS.toByteArray();
        }
    }

}
