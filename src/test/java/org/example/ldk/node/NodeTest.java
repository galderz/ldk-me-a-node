package org.example.ldk.node;

import org.junit.jupiter.api.Test;
import org.ldk.structs.BroadcasterInterface;
import org.ldk.structs.ChannelMonitor;
import org.ldk.structs.ChannelMonitorUpdate;
import org.ldk.structs.FeeEstimator;
import org.ldk.structs.Logger;
import org.ldk.structs.OutPoint;
import org.ldk.structs.Persist;
import org.ldk.structs.Result_NoneChannelMonitorUpdateErrZ;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class NodeTest
{
    @Test
    void buildANode()
    {
        final FeeEstimator feeEstimator = FeeEstimator.new_impl(confirmation_target -> 253);
        assertThat(feeEstimator, is(notNullValue()));

        final Logger logger = Logger.new_impl(System.out::println);
        assertThat(logger, is(notNullValue()));

        final BroadcasterInterface txBroadcaster = BroadcasterInterface.new_impl(tx -> {});
        assertThat(txBroadcaster, is(notNullValue()));

        // TODO check out what's the latest in java k/v file based persistence
        final Persist persister = Persist.new_impl(new Persist.PersistInterface()
        {
            @Override
            public Result_NoneChannelMonitorUpdateErrZ persist_new_channel(OutPoint id, ChannelMonitor data)
            {
                final byte[] channelMonitorBytes = data.write();
                // TODO <insert code to write these bytes to disk, keyed by `id`>
                return null;
            }

            @Override
            public Result_NoneChannelMonitorUpdateErrZ update_persisted_channel(OutPoint id, ChannelMonitorUpdate update, ChannelMonitor data)
            {
                final byte[] channelMonitorBytes = data.write();
                // TODO <insert code to update the `ChannelMonitor`'s file on disk with these new bytes, keyed by `id`>
                return null;
            }
        });
        assertThat(persister, is(notNullValue()));
    }
}
