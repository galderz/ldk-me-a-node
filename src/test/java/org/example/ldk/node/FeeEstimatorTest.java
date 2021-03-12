package org.example.ldk.node;

import org.junit.jupiter.api.Test;
import org.ldk.structs.FeeEstimator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FeeEstimatorTest
{
    @Test
    void feeEstimate()
    {
        final var feeEstimator = FeeEstimator.new_impl(confirmation_target -> 253);
        assertThat(feeEstimator, is(notNullValue()));
    }
}
