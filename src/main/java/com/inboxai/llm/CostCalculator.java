package com.inboxai.llm;

import com.inboxai.llm.dto.AnthropicResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Per-million-token USD prices for Anthropic models we use.
 * Keep small and explicit; if a model is missing we return zero cost and
 * log a warning at call sites — better than crashing on a billing detail.
 */
@Component
public class CostCalculator {

    private static final BigDecimal MILLION = new BigDecimal(1_000_000);
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private final Map<String, Pricing> pricingByModel = Map.of(
            "claude-haiku-4-5-20251001",
            new Pricing(bd("1.00"), bd("5.00"), bd("1.25"), bd("0.10")),
            "claude-sonnet-4-6-20251022",
            new Pricing(bd("3.00"), bd("15.00"), bd("3.75"), bd("0.30"))
    );

    public BigDecimal compute(String model, AnthropicResponse.Usage usage) {
        Pricing p = pricingByModel.get(model);
        if (p == null || usage == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal input = perMillion(usage.inputTokens(), p.inputPerMillion);
        BigDecimal output = perMillion(usage.outputTokens(), p.outputPerMillion);
        BigDecimal cacheWrite = perMillion(zeroIfNull(usage.cacheCreationInputTokens()), p.cacheWritePerMillion);
        BigDecimal cacheRead = perMillion(zeroIfNull(usage.cacheReadInputTokens()), p.cacheReadPerMillion);
        return input.add(output).add(cacheWrite).add(cacheRead).setScale(8, RoundingMode.HALF_UP);
    }

    private static BigDecimal perMillion(int tokens, BigDecimal pricePerMillion) {
        return new BigDecimal(tokens).multiply(pricePerMillion, MC).divide(MILLION, MC);
    }

    private static int zeroIfNull(Integer v) {
        return v == null ? 0 : v;
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private record Pricing(
            BigDecimal inputPerMillion,
            BigDecimal outputPerMillion,
            BigDecimal cacheWritePerMillion,
            BigDecimal cacheReadPerMillion
    ) {
    }
}
