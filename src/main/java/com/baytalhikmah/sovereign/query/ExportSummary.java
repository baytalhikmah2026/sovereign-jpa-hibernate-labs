package com.baytalhikmah.sovereign.query;

import java.util.Map;

/** What an export run saw: how many rows, how they split by channel, and the most entities held in memory at once. */
public record ExportSummary(long rows, Map<String, Long> perChannel, int mostTrackedAtOnce) {
}
