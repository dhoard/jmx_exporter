/*
 * Copyright (C) 2024-present The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test.common;

import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.ResourceLoader;

/** Class to implement ExpectedBuildInfo */
public class ExpectedBuildInfo {

    /** Constructor */
    private ExpectedBuildInfo() {
        // DO NOTHING
    }

    /**
     * Method to get the expected build info
     *
     * @param jmxExporterMode jmxExporterMode
     * @return the expected build info
     */
    public static String getExpectedBuildInfo(JmxExporterMode jmxExporterMode) {
        String buildInfoName =
                jmxExporterMode == JmxExporterMode.JavaAgent
                        ? "jmx_prometheus_javaagent"
                        : "jmx_prometheus_httpserver";

        String javaVersion = ResourceLoader.load("/configuration/java-version.txt");

        return buildInfoName + (javaVersion.isEmpty() ? "" : "-" + javaVersion);
    }
}
