package org.example.ldk.node;

import org.junit.jupiter.api.Test;
import org.ldk.batteries.NioPeerHandler;
import org.ldk.enums.LDKNetwork;
import org.ldk.structs.BroadcasterInterface;
import org.ldk.structs.ChainMonitor;
import org.ldk.structs.ChannelManager;
import org.ldk.structs.ChannelMonitor;
import org.ldk.structs.ChannelMonitorUpdate;
import org.ldk.structs.Event;
import org.ldk.structs.FeeEstimator;
import org.ldk.structs.Filter;
import org.ldk.structs.KeysManager;
import org.ldk.structs.Logger;
import org.ldk.structs.NetGraphMsgHandler;
import org.ldk.structs.OutPoint;
import org.ldk.structs.PeerManager;
import org.ldk.structs.Persist;
import org.ldk.structs.Result_NoneChannelMonitorUpdateErrZ;
import org.ldk.structs.UserConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

        final BroadcasterInterface txBroadcaster = BroadcasterInterface.new_impl(tx ->
        {});
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

        NetGraphMsgHandler router = NetGraphMsgHandler.constructor_new(new byte[32], null, logger);

        byte[] random_bytes = new byte[32];
        // <insert code to fill in `random_data` with random bytes>

        PeerManager peerManager = PeerManager.constructor_new(
            channelManager.as_ChannelMessageHandler()
            , router.as_RoutingMessageHandler()
            , keysManager.as_KeysInterface().get_node_secret()
            , random_bytes
            , logger
        );
        assertThat(peerManager, is(notNullValue()));

        NioPeerHandler nioPeerhandler;
        try
        {
            nioPeerhandler = new NioPeerHandler(peerManager);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        assertThat(nioPeerhandler, is(notNullValue()));

        // Start `NioPeerHandler` listening for connections.
        int port = 9735;
        try
        {
            nioPeerhandler.bind_listener(new InetSocketAddress("0.0.0.0", port));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        // header is a []byte type, height is `int`, txdata is a
        // TwoTuple<Long, byte[]>[], where the 0th element is the transaction's position
        // in the block (with the coinbase transaction considered position 0) and the 1st
        // element is the transaction bytes
//        channel_manager.block_connected(header, txn, height);
//        chain_monitor.block_connected(header, txn, height);
//        channel_manager.block_disconnected(header);
//        chain_monitor.block_disconnected(header, height);

        // Handle LDK Events#
        // On startup, start this loop:
//        while(true) {
//            Event[] channel_manager_events =
//                channelManager.as_EventsProvider().get_and_clear_pending_events();
//            Event[] chain_monitor_events =
//                chainMonitor.as_EventsProvider().get_and_clear_pending_events();
//
//            List<Event> all_events = new ArrayList<>();
//            all_events.addAll(Arrays.asList(channel_manager_events));
//            all_events.addAll(Arrays.asList(chain_monitor_events));
//            for (Event e: all_events) {
//                // <insert code to handle each event>
//            }
//        }

        // Persist ChannelManager#
//        while (true) {
//            // <code from the previous step that handles `ChannelManager` and
//            // `ChainMonitor` events>
//
//            // After the `for` loop in the previous step has handled `all_events`:
//            byte[] channel_manager_bytes_to_write = channel_manager.write();
//            // <insert code that writes these bytes to disk and/or backups>
//        }

        // Background Processing#
//        while (true) {
//            // <wait 60 seconds>
//            channel_manager.timer_chan_freshness_every_min();
//            // Note: NioPeerHandler handles calling timer_tick_occurred
//        }
    }

    // TODO split out to build a light node (e.g. only interested in certain tx)

    // TODO test start a node that is not fresh (i.e. is restarted)
    //      https://lightningdevkit.org/docs/build_node#read-channelmonitor-state-from-disk
}
