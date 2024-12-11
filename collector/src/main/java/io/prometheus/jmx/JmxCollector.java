/*
 * Copyright (C) 2015-present The Prometheus jmx_exporter Authors
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

import static java.lang.String.format;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

import io.prometheus.jmx.logger.Logger;
import io.prometheus.jmx.logger.LoggerFactory;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.Unit;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.yaml.snakeyaml.Yaml;

/** Class to implement JmxCollector */
@SuppressWarnings("unchecked")
public class JmxCollector implements MultiCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmxCollector.class);

    /** Enum to implement Mode */
    public enum Mode {
        /** Agent mode */
        AGENT,
        /** Standalone mode */
        STANDALONE
    }

    private File file;
    private Configuration configuration;
    private final long createTimeNanoSecs = System.nanoTime();
    private final Mode mode;
    private final JmxMBeanPropertyCache jmxMBeanPropertyCache = new JmxMBeanPropertyCache();

    private Counter configReloadSuccess;
    private Counter configReloadFailure;
    private Gauge jmxScrapeDurationSeconds;
    private Gauge jmxScrapeCachedBeans;
    private Gauge jmxScrapeError;

    /**
     * Constructor
     *
     * @param mode mode
     * @param file file
     * @throws IOException IOException
     * @throws MalformedObjectNameException MalformedObjectNameException
     */
    public JmxCollector(Mode mode, File file) throws IOException, MalformedObjectNameException {
        this.file = file;
        this.mode = mode;
        this.configuration = loadConfig(new Yaml().load(new FileReader(file)));
        this.configuration.lastUpdate = this.file.lastModified();

        exitOnConfigError();
    }

    /**
     * Constructor
     *
     * @param mode mode
     * @param filename filename
     * @throws MalformedObjectNameException MalformedObjectNameException
     */
    public JmxCollector(Mode mode, String filename) throws MalformedObjectNameException {
        this.mode = mode;
        this.configuration = loadConfig(new Yaml().load(filename));

        exitOnConfigError();
    }

    /**
     * Constructor
     *
     * @param mode mode
     * @param inputStream inputStream
     * @throws MalformedObjectNameException MalformedObjectNameException
     */
    public JmxCollector(Mode mode, InputStream inputStream) throws MalformedObjectNameException {
        this.mode = mode;

        this.configuration =
                loadConfig(
                        new Yaml()
                                .load(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
    }

    /**
     * Constructor
     *
     * @param mode mode
     * @param reader reader
     * @throws MalformedObjectNameException MalformedObjectNameException
     */
    public JmxCollector(Mode mode, Reader reader) throws MalformedObjectNameException {
        this.mode = mode;
        this.configuration = loadConfig(new Yaml().load(reader));
    }

    /**
     * Method to register the JmxCollector using the default PrometheusRegistry
     *
     * @return the JmxCollector
     */
    public JmxCollector register() {
        return register(PrometheusRegistry.defaultRegistry);
    }

    /**
     * Method to register the JmxCollector to a specific PrometheusRegistry
     *
     * @param prometheusRegistry prometheusRegistry
     * @return the JmxCollector
     */
    public JmxCollector register(PrometheusRegistry prometheusRegistry) {
        configReloadSuccess =
                Counter.builder()
                        .name("jmx_config_reload_success_total")
                        .help("Number of times configuration have successfully been reloaded.")
                        .register(prometheusRegistry);

        configReloadFailure =
                Counter.builder()
                        .name("jmx_config_reload_failure_total")
                        .help("Number of times configuration have failed to be reloaded.")
                        .register(prometheusRegistry);

        jmxScrapeDurationSeconds =
                Gauge.builder()
                        .name("jmx_scrape_duration_seconds")
                        .help("Time this JMX scrape took, in seconds.")
                        .unit(Unit.SECONDS)
                        .register(prometheusRegistry);

        jmxScrapeError =
                Gauge.builder()
                        .name("jmx_scrape_error")
                        .help("Non-zero if this scrape failed.")
                        .register(prometheusRegistry);

        jmxScrapeCachedBeans =
                Gauge.builder()
                        .name("jmx_scrape_cached_beans")
                        .help("Number of beans with their matching rule cached")
                        .register(prometheusRegistry);

        prometheusRegistry.register(this);

        return this;
    }

    private void exitOnConfigError() {
        if (mode == Mode.AGENT && !configuration.jmxUrl.isEmpty()) {
            LOGGER.log(
                    SEVERE,
                    "Configuration error: When running jmx_exporter as a Java agent, you must not"
                        + " configure 'jmxUrl' or 'hostPort' because you don't want to monitor a"
                        + " remote JVM.");
            System.exit(-1);
        }

        if (mode == Mode.STANDALONE && configuration.jmxUrl.isEmpty()) {
            LOGGER.log(
                    SEVERE,
                    "Configuration error: When running jmx_exporter in standalone mode (using"
                            + " jmx_prometheus_standalone-*.jar) you must configure 'jmxUrl' or"
                            + " 'hostPort'.");
            System.exit(-1);
        }
    }

    private void reloadConfig() {
        try (FileReader fileReader = new FileReader(file)) {
            Map<String, Object> newYamlConfiguration = new Yaml().load(fileReader);
            configuration = loadConfig(newYamlConfiguration);
            configuration.lastUpdate = file.lastModified();
            configReloadSuccess.inc();
        } catch (Exception e) {
            LOGGER.log(SEVERE, "Configuration reload failed: %s: ", e);
            configReloadFailure.inc();
        }
    }

    private synchronized Configuration getLatestConfig() {
        if (file != null && file.lastModified() > configuration.lastUpdate) {
            LOGGER.log(FINE, "Configuration file changed, reloading...");
            reloadConfig();
        }
        exitOnConfigError();
        return configuration;
    }

    private Configuration loadConfig(Map<String, Object> yamlConfiguration)
            throws MalformedObjectNameException {
        if (yamlConfiguration == null) {
            yamlConfiguration = new HashMap<>();
        }

        Configuration configuration = new Configuration();

        if (yamlConfiguration.containsKey("startDelaySeconds")) {
            try {
                configuration.startDelaySeconds =
                        (Integer) yamlConfiguration.get("startDelaySeconds");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid number provided for startDelaySeconds", e);
            }
        }
        if (yamlConfiguration.containsKey("hostPort")) {
            if (yamlConfiguration.containsKey("jmxUrl")) {
                throw new IllegalArgumentException(
                        "At most one of hostPort and jmxUrl must be provided");
            }
            configuration.jmxUrl =
                    "service:jmx:rmi:///jndi/rmi://"
                            + yamlConfiguration.get("hostPort")
                            + "/jmxrmi";
        } else if (yamlConfiguration.containsKey("jmxUrl")) {
            configuration.jmxUrl = (String) yamlConfiguration.get("jmxUrl");
        }

        if (yamlConfiguration.containsKey("username")) {
            configuration.username = (String) yamlConfiguration.get("username");
        }

        if (yamlConfiguration.containsKey("password")) {
            configuration.password = (String) yamlConfiguration.get("password");
        }

        if (yamlConfiguration.containsKey("ssl")) {
            configuration.ssl = (Boolean) yamlConfiguration.get("ssl");
        }

        if (yamlConfiguration.containsKey("lowercaseOutputName")) {
            configuration.lowercaseOutputName =
                    (Boolean) yamlConfiguration.get("lowercaseOutputName");
        }

        if (yamlConfiguration.containsKey("lowercaseOutputLabelNames")) {
            configuration.lowercaseOutputLabelNames =
                    (Boolean) yamlConfiguration.get("lowercaseOutputLabelNames");
        }

        // Default to includeObjectNames, but fall back to whitelistObjectNames for backward
        // compatibility
        if (yamlConfiguration.containsKey("includeObjectNames")) {
            List<Object> names = (List<Object>) yamlConfiguration.get("includeObjectNames");
            for (Object name : names) {
                configuration.includeObjectNames.add(new ObjectName((String) name));
            }
        } else if (yamlConfiguration.containsKey("whitelistObjectNames")) {
            List<Object> names = (List<Object>) yamlConfiguration.get("whitelistObjectNames");
            for (Object name : names) {
                configuration.includeObjectNames.add(new ObjectName((String) name));
            }
        } else {
            configuration.includeObjectNames.add(null);
        }

        // Default to excludeObjectNames, but fall back to blacklistObjectNames for backward
        // compatibility
        if (yamlConfiguration.containsKey("excludeObjectNames")) {
            List<Object> names = (List<Object>) yamlConfiguration.get("excludeObjectNames");
            for (Object name : names) {
                configuration.excludeObjectNames.add(new ObjectName((String) name));
            }
        } else if (yamlConfiguration.containsKey("blacklistObjectNames")) {
            List<Object> names = (List<Object>) yamlConfiguration.get("blacklistObjectNames");
            for (Object name : names) {
                configuration.excludeObjectNames.add(new ObjectName((String) name));
            }
        }

        if (yamlConfiguration.containsKey("rules")) {
            List<Map<String, Object>> configRules =
                    (List<Map<String, Object>>) yamlConfiguration.get("rules");
            for (Map<String, Object> yamlRule : configRules) {
                Rule rule = new Rule();
                configuration.rules.add(rule);
                if (yamlRule.containsKey("pattern")) {
                    rule.pattern = Pattern.compile("^.*(?:" + yamlRule.get("pattern") + ").*$");
                }
                if (yamlRule.containsKey("name")) {
                    rule.name = (String) yamlRule.get("name");
                }
                if (yamlRule.containsKey("value")) {
                    rule.value = String.valueOf(yamlRule.get("value"));
                }
                if (yamlRule.containsKey("valueFactor")) {
                    String valueFactor = String.valueOf(yamlRule.get("valueFactor"));
                    try {
                        rule.valueFactor = Double.valueOf(valueFactor);
                    } catch (NumberFormatException e) {
                        // use default value
                    }
                }
                if (yamlRule.containsKey("attrNameSnakeCase")) {
                    rule.attrNameSnakeCase = (Boolean) yamlRule.get("attrNameSnakeCase");
                }
                if (yamlRule.containsKey("cache")) {
                    rule.cache = (Boolean) yamlRule.get("cache");
                }
                if (yamlRule.containsKey("type")) {
                    String t = (String) yamlRule.get("type");
                    // Gracefully handle switch to OM data model.
                    if ("UNTYPED".equals(t)) {
                        t = "UNKNOWN";
                    }
                    rule.type = t;
                }
                if (yamlRule.containsKey("help")) {
                    rule.help = (String) yamlRule.get("help");
                }
                if (yamlRule.containsKey("labels")) {
                    TreeMap<String, Object> labels =
                            new TreeMap<>((Map<String, Object>) yamlRule.get("labels"));
                    rule.labelNames = new ArrayList<>();
                    rule.labelValues = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : labels.entrySet()) {
                        rule.labelNames.add(entry.getKey());
                        rule.labelValues.add((String) entry.getValue());
                    }
                }

                // Validation.
                if ((rule.labelNames != null || rule.help != null) && rule.name == null) {
                    throw new IllegalArgumentException(
                            "Must provide name, if help or labels are given: " + yamlRule);
                }
                if (rule.name != null && rule.pattern == null) {
                    throw new IllegalArgumentException(
                            "Must provide pattern, if name is given: " + yamlRule);
                }
            }
        } else {
            // Default to a single default rule.
            configuration.rules.add(new Rule());
        }

        configuration.rulesCache = new MatchedRulesCache(configuration.rules);
        configuration.objectNameAttributeFilter =
                ObjectNameAttributeFilter.create(yamlConfiguration);

        return configuration;
    }

    static String toSnakeAndLowerCase(String attrName) {
        if (attrName == null || attrName.isEmpty()) {
            return attrName;
        }
        char firstChar = attrName.subSequence(0, 1).charAt(0);
        boolean prevCharIsUpperCaseOrUnderscore =
                Character.isUpperCase(firstChar) || firstChar == '_';
        StringBuilder resultBuilder =
                new StringBuilder(attrName.length()).append(Character.toLowerCase(firstChar));
        for (char attrChar : attrName.substring(1).toCharArray()) {
            boolean charIsUpperCase = Character.isUpperCase(attrChar);
            if (!prevCharIsUpperCaseOrUnderscore && charIsUpperCase) {
                resultBuilder.append("_");
            }
            resultBuilder.append(Character.toLowerCase(attrChar));
            prevCharIsUpperCaseOrUnderscore = charIsUpperCase || attrChar == '_';
        }
        return resultBuilder.toString();
    }

    /**
     * Change invalid chars to underscore, and merge underscores.
     *
     * @param name Input string
     * @return the safe string
     */
    static String safeName(String name) {
        if (name == null) {
            return null;
        }
        boolean prevCharIsUnderscore = false;
        StringBuilder safeNameBuilder = new StringBuilder(name.length());
        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            // prevent a numeric prefix.
            safeNameBuilder.append("_");
        }
        for (char nameChar : name.toCharArray()) {
            boolean isUnsafeChar = !JmxCollector.isLegalCharacter(nameChar);
            if ((isUnsafeChar || nameChar == '_')) {
                if (prevCharIsUnderscore) {
                    // INTENTIONALLY BLANK
                } else {
                    safeNameBuilder.append("_");
                    prevCharIsUnderscore = true;
                }
            } else {
                safeNameBuilder.append(nameChar);
                prevCharIsUnderscore = false;
            }
        }

        return safeNameBuilder.toString();
    }

    private static boolean isLegalCharacter(char input) {
        return ((input == ':')
                || (input == '_')
                || (input >= 'a' && input <= 'z')
                || (input >= 'A' && input <= 'Z')
                || (input >= '0' && input <= '9'));
    }

    private static class Receiver implements JmxScraperListener {

        final List<MatchedRule> matchedRules = new ArrayList<>();

        final Configuration configuration;
        final StalenessTracker stalenessTracker;

        private static final char SEP = '_';

        Receiver(Configuration configuration, StalenessTracker stalenessTracker) {
            this.configuration = configuration;
            this.stalenessTracker = stalenessTracker;
        }

        // [] and () are special in regexes, so swtich to <>.
        private String angleBrackets(String s) {
            return "<" + s.substring(1, s.length() - 1) + ">";
        }

        // Add the matched rule to the cached rules and tag it as not stale
        // if the rule is configured to be cached
        private void addToCache(
                final Rule rule, final String cacheKey, final MatchedRule matchedRule) {
            if (rule.cache) {
                configuration.rulesCache.put(rule, cacheKey, matchedRule);
                stalenessTracker.add(rule, cacheKey);
            }
        }

        private MatchedRule defaultExport(
                String matchName,
                String domain,
                LinkedHashMap<String, String> beanProperties,
                LinkedList<String> attrKeys,
                String attrName,
                String help,
                Double value,
                double valueFactor,
                String type) {
            StringBuilder name = new StringBuilder();
            name.append(domain);
            if (!beanProperties.isEmpty()) {
                name.append(SEP);
                name.append(beanProperties.values().iterator().next());
            }
            for (String k : attrKeys) {
                name.append(SEP);
                name.append(k);
            }
            name.append(SEP);
            name.append(attrName);
            String fullname = safeName(name.toString());

            if (configuration.lowercaseOutputName) {
                fullname = fullname.toLowerCase();
            }

            List<String> labelNames = new ArrayList<>();
            List<String> labelValues = new ArrayList<>();
            if (beanProperties.size() > 1) {
                Iterator<Map.Entry<String, String>> iter = beanProperties.entrySet().iterator();
                // Skip the first one, it's been used in the name.
                iter.next();
                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = iter.next();
                    String labelName = safeName(entry.getKey());
                    if (configuration.lowercaseOutputLabelNames) {
                        labelName = labelName.toLowerCase();
                    }
                    labelNames.add(labelName);
                    labelValues.add(entry.getValue());
                }
            }

            return new MatchedRule(
                    fullname, matchName, type, help, labelNames, labelValues, value, valueFactor);
        }

        public void recordMBean(
                String domain,
                LinkedHashMap<String, String> beanProperties,
                LinkedList<String> attrKeys,
                String attrName,
                String attrType,
                String attrDescription,
                Object beanValue) {

            String beanName =
                    domain
                            + angleBrackets(beanProperties.toString())
                            + angleBrackets(attrKeys.toString());

            // Build the HELP string from the bean metadata.
            String help =
                    domain
                            + ":name="
                            + beanProperties.get("name")
                            + ",type="
                            + beanProperties.get("type")
                            + ",attribute="
                            + attrName;
            // Add the attrDescription to the HELP if it exists and is useful.
            if (attrDescription != null && !attrDescription.equals(attrName)) {
                help = attrDescription + " " + help;
            }

            MatchedRule matchedRule = MatchedRule.unmatched();

            for (Rule rule : configuration.rules) {
                // Rules with bean values cannot be properly cached (only the value from the first
                // scrape will be cached).
                // If caching for the rule is enabled, replace the value with a dummy <cache> to
                // avoid caching different values at different times.
                Object matchBeanValue = rule.cache ? "<cache>" : beanValue;

                String attributeName;
                if (rule.attrNameSnakeCase) {
                    attributeName = toSnakeAndLowerCase(attrName);
                } else {
                    attributeName = attrName;
                }

                String matchName = beanName + attributeName + ": " + matchBeanValue;

                if (rule.cache) {
                    MatchedRule cachedRule = configuration.rulesCache.get(rule, matchName);
                    if (cachedRule != null) {
                        stalenessTracker.add(rule, matchName);
                        if (cachedRule.isMatched()) {
                            matchedRule = cachedRule;
                            break;
                        }

                        // The bean was cached earlier, but did not match the current rule.
                        // Skip it to avoid matching against the same pattern again
                        continue;
                    }
                }

                Matcher matcher = null;
                if (rule.pattern != null) {
                    matcher = rule.pattern.matcher(matchName);
                    if (!matcher.matches()) {
                        addToCache(rule, matchName, MatchedRule.unmatched());
                        continue;
                    }
                }

                Double value = null;
                if (rule.value != null && !rule.value.isEmpty()) {
                    String val = matcher.replaceAll(rule.value);
                    try {
                        value = Double.valueOf(val);
                    } catch (NumberFormatException e) {
                        LOGGER.log(
                                FINE,
                                "Unable to parse configured value '%s' to number for bean: %s%s:"
                                        + " %s",
                                val,
                                beanName,
                                attrName,
                                beanValue);
                        return;
                    }
                }

                // If there's no name provided, use default export format.
                if (rule.name == null) {
                    matchedRule =
                            defaultExport(
                                    matchName,
                                    domain,
                                    beanProperties,
                                    attrKeys,
                                    attributeName,
                                    help,
                                    value,
                                    rule.valueFactor,
                                    rule.type);
                    addToCache(rule, matchName, matchedRule);
                    break;
                }

                // Matcher is set below here due to validation in the constructor.
                String name = safeName(matcher.replaceAll(rule.name));
                if (name.isEmpty()) {
                    return;
                }
                if (configuration.lowercaseOutputName) {
                    name = name.toLowerCase();
                }

                // Set the help.
                if (rule.help != null) {
                    help = matcher.replaceAll(rule.help);
                }

                // Set the labels.
                ArrayList<String> labelNames = new ArrayList<>();
                ArrayList<String> labelValues = new ArrayList<>();
                if (rule.labelNames != null) {
                    for (int i = 0; i < rule.labelNames.size(); i++) {
                        final String unsafeLabelName = rule.labelNames.get(i);
                        final String labelValReplacement = rule.labelValues.get(i);
                        try {
                            String labelName = safeName(matcher.replaceAll(unsafeLabelName));
                            String labelValue = matcher.replaceAll(labelValReplacement);
                            if (configuration.lowercaseOutputLabelNames) {
                                labelName = labelName.toLowerCase();
                            }
                            if (!labelName.isEmpty() && !labelValue.isEmpty()) {
                                labelNames.add(labelName);
                                labelValues.add(labelValue);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(
                                    format(
                                            "Matcher '%s' unable to use: '%s' value: '%s'",
                                            matcher, unsafeLabelName, labelValReplacement),
                                    e);
                        }
                    }
                }

                matchedRule =
                        new MatchedRule(
                                name,
                                matchName,
                                rule.type,
                                help,
                                labelNames,
                                labelValues,
                                value,
                                rule.valueFactor);
                addToCache(rule, matchName, matchedRule);
                break;
            }

            if (matchedRule.isUnmatched()) {
                return;
            }

            Number value;
            if (matchedRule.value != null) {
                beanValue = matchedRule.value;
            }

            if (beanValue instanceof Number) {
                value = ((Number) beanValue).doubleValue() * matchedRule.valueFactor;
            } else if (beanValue instanceof Boolean) {
                value = (Boolean) beanValue ? 1 : 0;
            } else {
                LOGGER.log(
                        FINE,
                        "Ignoring unsupported bean: %s%s: %s ",
                        beanName,
                        attrName,
                        beanValue);
                return;
            }

            // Add to samples.
            LOGGER.log(
                    FINE,
                    "add metric sample: %s %s %s %s",
                    matchedRule.name,
                    matchedRule.labelNames,
                    matchedRule.labelValues,
                    value.doubleValue());

            matchedRules.add(matchedRule.withValue(value.doubleValue()));
        }
    }

    @Override
    public MetricSnapshots collect() {
        // Take a reference to the current config and collect with this one
        // (to avoid race conditions in case another thread reloads the config in the meantime)
        Configuration configuration = getLatestConfig();

        StalenessTracker stalenessTracker = new StalenessTracker();

        Receiver receiver = new Receiver(configuration, stalenessTracker);

        JmxScraper scraper =
                new JmxScraper(
                        configuration.jmxUrl,
                        configuration.username,
                        configuration.password,
                        configuration.ssl,
                        configuration.includeObjectNames,
                        configuration.excludeObjectNames,
                        configuration.objectNameAttributeFilter,
                        jmxMBeanPropertyCache,
                        receiver);

        long start = System.nanoTime();
        double error = 0;

        if ((configuration.startDelaySeconds > 0)
                && ((start - createTimeNanoSecs) / 1000000000L < configuration.startDelaySeconds)) {
            throw new IllegalStateException("JMXCollector waiting for startDelaySeconds");
        }
        try {
            scraper.scrape();
        } catch (Exception e) {
            error = 1;
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            LOGGER.log(SEVERE, "JMX scrape failed: %s", sw);
        }

        configuration.rulesCache.evictStaleEntries(stalenessTracker);

        jmxScrapeDurationSeconds.set((System.nanoTime() - start) / 1.0E9);
        jmxScrapeError.set(error);
        jmxScrapeCachedBeans.set(stalenessTracker.cachedCount());

        return MatchedRuleToMetricSnapshotsConverter.convert(receiver.matchedRules);
    }
}
