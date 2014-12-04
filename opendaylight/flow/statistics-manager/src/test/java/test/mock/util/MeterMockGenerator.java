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
    private static final Random rnd = new Random();
    private static final MeterBuilder meterBuilder = new MeterBuilder();
    private static final MeterBandHeaderBuilder meterBandHeaderBuilder = new MeterBandHeaderBuilder();
    private static final MeterBandHeadersBuilder meterBandHeadersBuilder = new MeterBandHeadersBuilder();

    public static Meter getRandomMeter() {
        meterBandHeaderBuilder.setKey(new MeterBandHeaderKey(new BandId(TestUtils.nextLong(0, 4294967295L))));
        meterBandHeaderBuilder.setBandBurstSize(TestUtils.nextLong(0, 4294967295L));
        meterBandHeaderBuilder.setBandRate(TestUtils.nextLong(0, 4294967295L));
        List<MeterBandHeader> meterBandHeaders = new ArrayList<>();
        meterBuilder.setKey(new MeterKey(new MeterId(TestUtils.nextLong(0, 4294967295L))));
        meterBuilder.setBarrier(rnd.nextBoolean());
        meterBuilder.setContainerName("container." + rnd.nextInt(1000));
        meterBuilder.setMeterName("meter." + rnd.nextInt(1000));
        meterBuilder.setMeterBandHeaders(meterBandHeadersBuilder.setMeterBandHeader(meterBandHeaders).build());
        return meterBuilder.build();
    }
}
