/*
 * Copyright (C) 2020-present The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MatchedRulesCache is a cache for bean name to configured rule mapping (See
 * JmxCollector.Receiver). The cache also retains unmatched entries (a bean name not matching a rule
 * pattern) to avoid matching against the same pattern in later bean collections.
 */
class MatchedRulesCache {

    private final Map<Rule, Map<String, MatchedRule>> cachedRules;

    /**
     * Constructor
     *
     * @param rules rules
     */
    public MatchedRulesCache(Collection<Rule> rules) {
        this.cachedRules = new HashMap<>(rules.size());
        for (Rule rule : rules) {
            this.cachedRules.put(rule, new ConcurrentHashMap<>());
        }
    }

    /**
     * Method to put a matched rule in the cache
     *
     * @param rule rule
     * @param cacheKey cacheKey
     * @param matchedRule matchedRule
     */
    public void put(final Rule rule, final String cacheKey, final MatchedRule matchedRule) {
        Map<String, MatchedRule> cachedRulesForRule = cachedRules.get(rule);
        cachedRulesForRule.put(cacheKey, matchedRule);
    }

    /**
     * Method to get a MatchedRule from the cache
     *
     * @param rule rule
     * @param cacheKey cacheKey
     * @return the MatchedRule
     */
    public MatchedRule get(final Rule rule, final String cacheKey) {
        return cachedRules.get(rule).get(cacheKey);
    }

    /**
     * Method to remove stale rules (in the cache but not collected in the last run of the
     * collector)
     *
     * @param stalenessTracker stalenessTracker
     */
    public void evictStaleEntries(final StalenessTracker stalenessTracker) {
        for (Map.Entry<Rule, Map<String, MatchedRule>> entry : cachedRules.entrySet()) {
            Rule rule = entry.getKey();
            Map<String, MatchedRule> cachedRulesForRule = entry.getValue();
            cachedRulesForRule
                    .keySet()
                    .removeIf(cacheKey -> !stalenessTracker.contains(rule, cacheKey));
        }
    }
}
