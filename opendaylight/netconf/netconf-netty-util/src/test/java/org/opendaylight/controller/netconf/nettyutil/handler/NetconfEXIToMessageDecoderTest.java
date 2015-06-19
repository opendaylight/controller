package org.opendaylight.controller.netconf.nettyutil.handler;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;

public class NetconfEXIToMessageDecoderTest {


    private static final EXIOptions DEFAULT_OPTIONS;

    static {
        final EXIOptions opts = new EXIOptions();
        try {
//            opts.setPreserveDTD(true);
            opts.setPreserveNS(true);
            opts.setPreserveLexicalValues(true);
            opts.setAlignmentType(AlignmentType.preCompress);
        } catch (EXIOptionsException e) {
            throw new ExceptionInInitializerError(e);
        }

        DEFAULT_OPTIONS = opts;
    }

    @Test
    public void testName() throws Exception {
        final NetconfEXICodec netconfEXICodec = new NetconfEXICodec(DEFAULT_OPTIONS);

        final NetconfMessageToEXIEncoder netconfMessageToEXIEncoder = NetconfMessageToEXIEncoder.create(netconfEXICodec);
        final NetconfEXIToMessageDecoder netconfEXIToMessageDecoder = NetconfEXIToMessageDecoder.create(netconfEXICodec);

        final NetconfMessage netconfMessage = new NetconfMessage(XmlUtil.readXmlToDocument(
                "<notification xmlns=\"urn:ietf:params:xml:ns:netconf:notification:1.0\">\n" +
                " <eventTime>1970-01-01T00:00:02Z</eventTime>\n" +
                " <vrf-route-notification xmlns=\"org:opendaylight:coretutorials:ncmount:example:notifications\">\n" +
                "  <vrf-prefixes>\n" +
                "  <vrf-prefix>\n" +
                "  <prefix>127.0.0.1</prefix>\n" +
                "  <prefix-length xmlns:a=\"namespace\">a:32</prefix-length>\n" +
                "  <vrf-route xmlns:b=\"namespace2\">\n" +
                "  <b:vrf-next-hops>\n" +
                "  <next-hop-address>\n" +
                "  <next-hop-address>10.0.0.1</next-hop-address>\n" +
                "  </next-hop-address>\n" +
                "  </b:vrf-next-hops>\n" +
                "  </vrf-route>\n" +
                "  </vrf-prefix>\n" +
                "  </vrf-prefixes>\n" +
                " </vrf-route-notification>\n" +
                "</notification>"));

        System.err.println(XmlUtil.toString(netconfMessage.getDocument()).getBytes().length);

        final int count = 300000;

        Stopwatch started = Stopwatch.createStarted();
        final ByteBuf buffer = Unpooled.buffer();
        long bytes = 0;

        for (int i = 0; i < count; i++) {
            netconfMessageToEXIEncoder.encode(null, netconfMessage, buffer);
            bytes += buffer.readableBytes();
            final ArrayList<Object> out = Lists.newArrayList();
            netconfEXIToMessageDecoder.decode(null, buffer, out);
            buffer.resetWriterIndex();
            buffer.resetReaderIndex();
        }

        long elapsed = started.elapsed(TimeUnit.MILLISECONDS);
        System.err.println("WARMUP----------");
//        System.err.println(elapsed);
//        System.err.println(((count * 1.0) / elapsed) * 1000);
//        System.err.println("Traffic " + bytes);
//




        started = Stopwatch.createStarted();
        bytes = 0;
        for (int i = 0; i < count; i++) {

            netconfMessageToEXIEncoder.encode(null, netconfMessage, buffer);
            bytes += buffer.readableBytes();
            final ArrayList<Object> out = Lists.newArrayList();
            netconfEXIToMessageDecoder.decode(null, buffer, out);
            buffer.resetWriterIndex();
            buffer.resetReaderIndex();
        }

        elapsed = started.elapsed(TimeUnit.MILLISECONDS);
        System.err.println("EXI------------- " + DEFAULT_OPTIONS.getAlignmentType());
        System.err.println("Messages encoded and decoded per second: " + ((count * 1.0) / elapsed) * 1000);
        System.err.println("Traffic(total bytes to transfer): " + bytes);

        final NetconfMessageToXMLEncoder netconfMessageToXMLEncoder = new NetconfMessageToXMLEncoder();
        final NetconfXMLToMessageDecoder netconfXMLToMessageDecoder = new NetconfXMLToMessageDecoder();

        started = Stopwatch.createStarted();
        bytes = 0;
        for (int i = 0; i < count; i++) {
            netconfMessageToXMLEncoder.encode(null, netconfMessage, buffer);
            bytes += buffer.readableBytes();
            final ArrayList<Object> out = Lists.newArrayList();
            netconfXMLToMessageDecoder.decode(null, buffer, out);
            buffer.resetWriterIndex();
            buffer.resetReaderIndex();
        }

        elapsed = started.elapsed(TimeUnit.MILLISECONDS);
        System.err.println("XML-------------");
        System.err.println("Messages encoded and decoded per second: " + ((count * 1.0) / elapsed) * 1000);
        System.err.println("Traffic(total bytes to transfer): " + bytes);

    }
}