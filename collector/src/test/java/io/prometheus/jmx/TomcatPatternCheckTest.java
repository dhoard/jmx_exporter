/*
 * Copyright (C) The Prometheus jmx_exporter Authors
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;
import org.junit.Test;

/**
 * Check tomcat path
 *
 * <pre>
 * Catalina:j2eeType=Servlet,WebModule=//localhost/host-manager,name=HTMLHostManager,J2EEApplication=none,J2EEServer=none
 * </pre>
 *
 * See <a
 * href="http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html">http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html</a>
 *
 * <p>or
 *
 * <p><a
 * href="http://stackoverflow.com/questions/163360/regular-expresion-to-match-urls-in-java">http://stackoverflow.com/questions/163360/regular-expresion-to-match-urls-in-java</a>
 */
public class TomcatPatternCheckTest {

    private static final Pattern VALID_TOMCAT_PATH =
            Pattern.compile("//([-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])");

    private static final Pattern VALID_SERVLET_NAME = Pattern.compile("([-a-zA-Z0-9+/$%~_-|!.]*)");

    private static final Pattern VALID_WEBMODULE =
            Pattern.compile(
                    "^.*j2eeType=Servlet,WebModule=//([-a-zA-Z0-9+&@#/%?=~_|!:.,;]*[-a-zA-Z0-9+&@#/%=~_|]),name=([-a-zA-Z0-9+/$%~_-|!.]*),J2EEApplication=none,J2EEServer=none.*$");

    public static boolean validateTomcatPath(String identifier) {
        return VALID_TOMCAT_PATH.matcher(identifier).matches();
    }

    public static boolean validateServletName(String identifier) {
        return VALID_SERVLET_NAME.matcher(identifier).matches();
    }

    public static boolean validateWebModule(String identifier) {
        return VALID_WEBMODULE.matcher(identifier).matches();
    }

    @Test
    public void testSerlvetName() {
        assertTrue(validateServletName("C"));
        assertTrue(validateServletName("Cc"));
        assertTrue(validateServletName("C$c"));
        assertTrue(validateServletName("C9"));
        assertTrue(validateServletName("host-manager"));
        assertTrue(validateServletName("a.C"));
        assertTrue(validateServletName(".C"));
        assertTrue(validateServletName("prom_app_metrics"));
    }

    @Test
    public void validateTomcatPath() {
        assertTrue(validateTomcatPath("//localhost/"));
        assertTrue(validateTomcatPath("//localhost/docs/"));
        assertTrue(validateTomcatPath("//www.example.com/prom-metric/"));
        assertTrue(validateTomcatPath("//www.example.com/prom_metric+tomcat/"));
        // no tomcat path, but a validate url?
        assertTrue(validateTomcatPath("//www.example.com:443;jsessionid=sajakjda/prom-metric/"));
        assertFalse("cannot include $", validateTomcatPath("//localhost/$docs/"));
        assertFalse("cannot include ()", validateTomcatPath("//localhost/docs()/"));
    }

    @Test
    public void testWebModule() {
        assertTrue(
                validateWebModule(
                        "Catalina:j2eeType=Servlet,WebModule=//localhost/host-manager,name=HTMLHostManager,J2EEApplication=none,J2EEServer=none"));
    }
}
