package com.hiresplayer.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BookmarkRulesTest {
    @Test
    public void normalizePositionNeverReturnsNegativeValue() {
        assertEquals(0L, BookmarkRules.INSTANCE.normalizePosition(-500L, null));
    }

    @Test
    public void normalizePositionClampsToDurationWhenDurationIsKnown() {
        assertEquals(30_000L, BookmarkRules.INSTANCE.normalizePosition(45_000L, 30_000L));
        assertEquals(12_000L, BookmarkRules.INSTANCE.normalizePosition(12_000L, 30_000L));
    }

    @Test
    public void usefulBookmarkRequiresAtLeastOneSecond() {
        assertFalse(BookmarkRules.INSTANCE.isUsefulBookmark(999L));
        assertTrue(BookmarkRules.INSTANCE.isUsefulBookmark(1_000L));
    }
}
