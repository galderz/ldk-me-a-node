package org.example.ldk.node;

import org.junit.jupiter.api.Test;
import org.ldk.structs.FeeEstimator;

public class FeeEstimatorTest
{
    @Test
    void feeEstimate()
    {
        final var feeEstimator = FeeEstimator.new_impl(confirmation_target -> 253);
        System.out.println(feeEstimator.get_est_sat_per_1000_weight(null));
    }
}
