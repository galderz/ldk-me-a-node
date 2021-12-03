package org.example.ldk.node;

import org.junit.jupiter.api.Test;
import org.ldk.batteries.ChannelManagerConstructor;
import org.ldk.batteries.NioPeerHandler;
import org.ldk.enums.ConfirmationTarget;
import org.ldk.enums.Network;
import org.ldk.structs.BroadcasterInterface;
import org.ldk.structs.ChainMonitor;
import org.ldk.structs.ChannelManager;
import org.ldk.structs.ChannelMonitor;
import org.ldk.structs.ChannelMonitorUpdate;
import org.ldk.structs.Event;
import org.ldk.structs.FeeEstimator;
import org.ldk.structs.KeysManager;
import org.ldk.structs.Logger;
import org.ldk.structs.MonitorUpdateId;
import org.ldk.structs.Option_FilterZ;
import org.ldk.structs.OutPoint;
import org.ldk.structs.Persist;
import org.ldk.structs.Record;
import org.ldk.structs.Result_NoneChannelMonitorUpdateErrZ;
import org.ldk.structs.UserConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class NodeTest
{
    @Test
    void buildANode() throws IOException
    {
        final FeeEstimator feeEstimator = FeeEstimator.new_impl(new YourFeeEstimator());
        assertThat(feeEstimator, is(notNullValue()));

        final Logger logger = Logger.new_impl(new YourLogger());
        assertThat(logger, is(notNullValue()));

        final BroadcasterInterface txBroadcaster = BroadcasterInterface.new_impl(new YourBroadcaster());
        assertThat(txBroadcaster, is(notNullValue()));

        final Persist persister = Persist.new_impl(new YourPersist());
        assertThat(persister, is(notNullValue()));

        ChannelManagerConstructor.EventHandler channel_manager_persister = new YourChannelManagerEventHandler();

        final Option_FilterZ filter = Option_FilterZ.none();// leave this as `null` or insert the Filter object, depending on// what you did for Step 7
        final ChainMonitor chainMonitor = ChainMonitor.of(filter, txBroadcaster, logger, feeEstimator, persister);
        assertThat(chainMonitor, is(notNullValue()));

        // <insert code to fill key_seed with random bytes OR if restarting, reload the
        // seed from disk>
        byte[] key_seed = SecureRandom.getSeed(32); // Use secure random to get a decent seed

        // Notes about this `KeysManager`:
        // * the current time is part of the parameters because it is used to derive
        //   random numbers from the seed where required, to ensure all random
        //   generation is unique across restarts.
        final long now = System.currentTimeMillis();
        final KeysManager keysManager = KeysManager.of(key_seed, TimeUnit.MILLISECONDS.toSeconds(now), (int) TimeUnit.MILLISECONDS.toNanos(now));
        assertThat(keysManager, is(notNullValue()));

        int block_height = 675_000; // <insert current chain tip height>;
        byte[] best_block_hash = new byte[32]; // <insert current chain tip block hash>;
        ChannelManagerConstructor channel_manager_constructor = new ChannelManagerConstructor(
            Network.LDKNetwork_Bitcoin
            , UserConfig.with_default()
            , best_block_hash
            , block_height
            , keysManager.as_KeysInterface()
            , feeEstimator
            , chainMonitor
            , null // network graph
            , txBroadcaster
            , logger
        );

        final ChannelManager channelManager = channel_manager_constructor.channel_manager;
        assertThat(channelManager, is(notNullValue()));

        final NioPeerHandler nio_peer_handler = channel_manager_constructor.nio_peer_handler;
        assertThat(nio_peer_handler, is(notNullValue()));
        final int port = 9735;
        nio_peer_handler.bind_listener(new InetSocketAddress("0.0.0.0", port));
    }

    // TODO split out to build a light node (e.g. only interested in certain tx)

    // TODO test start a node that is not fresh (i.e. is restarted)
    //      https://lightningdevkit.org/docs/build_node#read-channelmonitor-state-from-disk

    static final class YourFeeEstimator implements FeeEstimator.FeeEstimatorInterface
    {
        @Override
        public int get_est_sat_per_1000_weight(ConfirmationTarget conf_target)
        {
            switch (conf_target)
            {
                case LDKConfirmationTarget_Background:
                    // <insert code to retrieve a background feerate>
                    return 100;
                case LDKConfirmationTarget_Normal:
                    // <insert code to retrieve a normal (i.e. within ~6 blocks) feerate>
                    return 200;
                case LDKConfirmationTarget_HighPriority:
                    // <insert code to retrieve a high-priority feerate>
                    return 300;
                default:
                    throw new IllegalStateException("Unexpected value: " + conf_target);
            }
        }
    }

    static final class YourLogger implements Logger.LoggerInterface
    {
        @Override
        public void log(Record record)
        {
            System.out.println(record);
        }
    }

    static final class YourBroadcaster implements BroadcasterInterface.BroadcasterInterfaceInterface
    {
        @Override
        public void broadcast_transaction(byte[] tx)
        {
            // <insert code to broadcast the given transaction here>
        }
    }

    static final class YourPersist implements Persist.PersistInterface
    {
        // TODO For file based persistence, can use: https://www.baeldung.com/java-chronicle-map
        // TODO For in-memory hash map (testing) what is the key? OutPoint does not implement equals/hashCode)

        @Override
        public Result_NoneChannelMonitorUpdateErrZ persist_new_channel(OutPoint channel_id, ChannelMonitor data, MonitorUpdateId update_id)
        {
            final byte[] channelMonitorBytes = data.write();
            // TODO <insert code to write these bytes to disk, keyed by `id`>
            return null;
        }

        @Override
        public Result_NoneChannelMonitorUpdateErrZ update_persisted_channel(OutPoint channel_id, ChannelMonitorUpdate update, ChannelMonitor data, MonitorUpdateId update_id)
        {
            final byte[] channelMonitorBytes = data.write();
            // TODO <insert code to update the `ChannelMonitor`'s file on disk with these new bytes, keyed by `id`>
            return null;
        }
    }

    class YourChannelManagerEventHandler implements ChannelManagerConstructor.EventHandler {
        @Override
        public void handle_event(Event e) {
            if (e instanceof Event.FundingGenerationReady) {
                // <insert code to handle this event>
            }
            else if (e instanceof Event.PaymentReceived) {
                // <insert code to handle this event>
            }
            else if (e instanceof Event.PaymentSent) {
                // <insert code to handle this event>
            }
            else if (e instanceof Event.PaymentPathFailed) {
                // <insert code to handle this event>
            }
            else if (e instanceof Event.PendingHTLCsForwardable) {
                // <insert code to handle this event>
            }
            else if (e instanceof Event.SpendableOutputs) {
                // <insert code to handle this event>
            }
            else if (e instanceof Event.PaymentForwarded) {
                // <insert code to handle this event>
            }
            else if (e instanceof Event.ChannelClosed) {
                // <insert code to handle this event>
            }
        }

        @Override
        public void persist_manager(byte[] channel_manager_bytes) {
            // <insert code to persist channel_manager_bytes to disk and/or backups>
        }
    }
}
