package test.mock.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.BandId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.MeterBandHeadersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.meter.band.headers.MeterBandHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.meter.band.headers.MeterBandHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.meter.band.headers.MeterBandHeaderKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MeterMockGenerator {
    private static final Random RND = new Random();
    private static final MeterBuilder METER_BUILDER = new MeterBuilder();
    private static final MeterBandHeaderBuilder METER_BAND_HEADER_BUILDER = new MeterBandHeaderBuilder();
    private static final MeterBandHeadersBuilder METER_BAND_HEADERS_BUILDER = new MeterBandHeadersBuilder();

    public static Meter getRandomMeter() {
        METER_BAND_HEADER_BUILDER.setKey(new MeterBandHeaderKey(new BandId(Util.nextLong(0, 4294967295L))));
        METER_BAND_HEADER_BUILDER.setBandBurstSize(Util.nextLong(0, 4294967295L));
        METER_BAND_HEADER_BUILDER.setBandRate(Util.nextLong(0, 4294967295L));
        List<MeterBandHeader> meterBandHeaders = new ArrayList<>();
        METER_BUILDER.setKey(new MeterKey(new MeterId(Util.nextLong(0, 4294967295L))));
        METER_BUILDER.setBarrier(RND.nextBoolean());
        METER_BUILDER.setContainerName("container." + RND.nextInt(1000));
        METER_BUILDER.setMeterName("meter." + RND.nextInt(1000));
        METER_BUILDER.setMeterBandHeaders(METER_BAND_HEADERS_BUILDER.setMeterBandHeader(meterBandHeaders).build());
        return METER_BUILDER.build();
    }
}
