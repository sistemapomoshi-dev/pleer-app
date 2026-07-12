package com.hiresplayer.player;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SeekIntervalPolicyTest {
    @Test
    public void normalizeKeepsAllowedIntervals() {
        for (Long interval : SeekIntervalPolicy.INSTANCE.getAllowedIntervalsMs()) {
            assertEquals(interval.longValue(), SeekIntervalPolicy.INSTANCE.normalize(interval), 0);
        }
    }

    @Test
    public void normalizeFallsBackToDefaultForUnsupportedValues() {
        assertEquals(10_000L, SeekIntervalPolicy.INSTANCE.normalize(1_000L));
        assertEquals(10_000L, SeekIntervalPolicy.INSTANCE.normalize(60_000L));
        assertEquals(10_000L, SeekIntervalPolicy.INSTANCE.normalize(-5_000L));
    }

    @Test
    public void deltasUseNormalizedInterval() {
        assertEquals(15_000L, SeekIntervalPolicy.INSTANCE.forwardDelta(15_000L));
        assertEquals(-15_000L, SeekIntervalPolicy.INSTANCE.backwardDelta(15_000L));
        assertEquals(10_000L, SeekIntervalPolicy.INSTANCE.forwardDelta(42L));
        assertEquals(-10_000L, SeekIntervalPolicy.INSTANCE.backwardDelta(42L));
    }
}
