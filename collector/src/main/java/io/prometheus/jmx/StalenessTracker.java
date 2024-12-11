package io.prometheus.jmx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Class to implement StalenessTracker */
class StalenessTracker {

    private final Map<Rule, Set<String>> lastCachedEntries = new HashMap<>();

    /** Constructor */
    public StalenessTracker() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to add a Rule
     *
     * @param rule rule
     * @param cacheKey cacheKey
     */
    public void add(final Rule rule, final String cacheKey) {
        Set<String> lastCachedEntriesForRule =
                lastCachedEntries.computeIfAbsent(rule, k -> new HashSet<>());
        lastCachedEntriesForRule.add(cacheKey);
    }

    /**
     * Method to return if a Rule is stale
     *
     * @param rule rule
     * @param cacheKey cacheKey
     * @return true if the stale, else false
     */
    public boolean contains(final Rule rule, final String cacheKey) {
        Set<String> lastCachedEntriesForRule = lastCachedEntries.get(rule);
        return (lastCachedEntriesForRule != null) && lastCachedEntriesForRule.contains(cacheKey);
    }

    /**
     * Method to get the count of stale rules
     *
     * @return the count of stale rules
     */
    public long cachedCount() {
        long count = 0;
        for (Set<String> cacheKeys : lastCachedEntries.values()) {
            count += cacheKeys.size();
        }
        return count;
    }
}
