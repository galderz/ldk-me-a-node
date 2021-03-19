package org.example.ldk.node;

import org.junit.jupiter.api.Test;
import org.ldk.enums.LDKNetwork;
import org.ldk.structs.BroadcasterInterface;
import org.ldk.structs.ChainMonitor;
import org.ldk.structs.ChannelManager;
import org.ldk.structs.ChannelMonitor;
import org.ldk.structs.ChannelMonitorUpdate;
import org.ldk.structs.FeeEstimator;
import org.ldk.structs.Filter;
import org.ldk.structs.KeysManager;
import org.ldk.structs.Logger;
import org.ldk.structs.OutPoint;
import org.ldk.structs.Persist;
import org.ldk.structs.Result_NoneChannelMonitorUpdateErrZ;
import org.ldk.structs.UserConfig;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

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

        final Persist persister = Persist.new_impl(new Persist.PersistInterface()
        {
            // TODO For file based persistence, can use: https://www.baeldung.com/java-chronicle-map
            // TODO For in-memory hash map (testing) what is the key? OutPoint does not implement equals/hashCode)

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

        final ChainMonitor chainMonitor = ChainMonitor.constructor_new(null, txBroadcaster, logger, feeEstimator, persister);
        assertThat(chainMonitor, is(notNullValue()));

        // <insert code to fill key_seed with random bytes OR if restarting, reload the
        // seed from disk>
        byte[] key_seed = SecureRandom.getSeed(32); // Use secure random to get a decent seed

        // Notes about this `KeysManager`:
        // * the current time is part of the parameters because it is used to derive
        //   random numbers from the seed where required, to ensure all random
        //   generation is unique across restarts.
        final long now = System.currentTimeMillis();
        final KeysManager keysManager = KeysManager.constructor_new(key_seed, TimeUnit.MILLISECONDS.toSeconds(now), (int) TimeUnit.MILLISECONDS.toNanos(now));
        assertThat(keysManager, is(notNullValue()));

        int block_height = 675_000; // <insert current chain tip height>;
        final ChannelManager channelManager = ChannelManager.constructor_new(
            feeEstimator
            , chainMonitor.as_Watch()
            , txBroadcaster
            , logger
            , keysManager.as_KeysInterface()
            , UserConfig.constructor_default()
            , LDKNetwork.LDKNetwork_Bitcoin
            , new byte[32] // params_latest_hash_arg ??
            , block_height
        );
        assertThat(channelManager, is(notNullValue()));
    }

    // TODO split out to build a light node (e.g. only interested in certain tx)

    // TODO test start a node that is not fresh (i.e. is restarted)
    //      https://lightningdevkit.org/docs/build_node#read-channelmonitor-state-from-disk
}
