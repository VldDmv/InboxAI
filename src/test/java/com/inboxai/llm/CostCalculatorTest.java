package com.inboxai.llm;

import com.inboxai.llm.dto.AnthropicResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CostCalculatorTest {

    private final CostCalculator calc = new CostCalculator();

    @Test
    void haikuCostWithCacheMissChargesCacheCreation() {
        // 2000 input + 1000 cache_create + 0 cache_read + 100 output
        AnthropicResponse.Usage u = new AnthropicResponse.Usage(2000, 1000, 0, 100);

        BigDecimal cost = calc.compute("claude-haiku-4-5-20251001", u);

        // input: 2000 * 1.00 / 1e6 = 0.002
        // cache_create: 1000 * 1.25 / 1e6 = 0.00125
        // output: 100 * 5.00 / 1e6 = 0.0005
        // total = 0.00375
        assertThat(cost).isEqualByComparingTo(new BigDecimal("0.00375000"));
    }

    @Test
    void haikuCostWithCacheHitIsMostlyOutput() {
        // 50 input + 0 cache_create + 3000 cache_read + 100 output (typical 2nd call)
        AnthropicResponse.Usage u = new AnthropicResponse.Usage(50, 0, 3000, 100);

        BigDecimal cost = calc.compute("claude-haiku-4-5-20251001", u);

        // input: 50 * 1.00 / 1e6 = 0.00005
        // cache_read: 3000 * 0.10 / 1e6 = 0.0003
        // output: 100 * 5.00 / 1e6 = 0.0005
        // total = 0.00085
        assertThat(cost).isEqualByComparingTo(new BigDecimal("0.00085000"));
    }

    @Test
    void unknownModelReturnsZero() {
        AnthropicResponse.Usage u = new AnthropicResponse.Usage(1000, 0, 0, 100);
        assertThat(calc.compute("not-a-model", u)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void nullCacheCountsCountAsZero() {
        AnthropicResponse.Usage u = new AnthropicResponse.Usage(1000, null, null, 100);
        BigDecimal cost = calc.compute("claude-haiku-4-5-20251001", u);
        // input 0.001 + output 0.0005 = 0.0015
        assertThat(cost).isEqualByComparingTo(new BigDecimal("0.00150000"));
    }
}
