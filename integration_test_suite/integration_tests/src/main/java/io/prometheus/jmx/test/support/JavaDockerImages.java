/*
 * Copyright (C) 2023-present The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test.support;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Class to implement JavaDockerImages */
public final class JavaDockerImages {

    private static final String DOCKER_IMAGES_RESOURCE = "/configuration/java-docker-images.txt";

    private static List<String> DOCKER_IMAGE_NAMES;

    /** Constructor */
    private JavaDockerImages() {
        // DO NOTHING
    }

    /**
     * Method to get List of Docker image names filtered by a Predicate
     *
     * @return the List of Docker image names
     */
    public static Collection<String> names() {
        synchronized (JavaDockerImages.class) {
            if (DOCKER_IMAGE_NAMES == null) {
                DOCKER_IMAGE_NAMES = Collections.unmodifiableList(load(DOCKER_IMAGES_RESOURCE));
            }
        }

        return DOCKER_IMAGE_NAMES;
    }

    /**
     * Method to load the list of Docker image names from a resource
     *
     * @param resource resource
     * @return the List of lines
     */
    private static List<String> load(String resource) {
        List<String> lines = new ArrayList<>();

        InputStream inputStream = null;
        BufferedReader bufferedReader = null;

        try {
            inputStream = JavaDockerImages.class.getResourceAsStream(resource);
            if (inputStream == null) {
                throw new IOException(format("Resource [%s] not found", resource));
            }

            bufferedReader =
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }

                if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                    lines.add(line.trim());
                }
            }

            return lines;
        } catch (Throwable t) {
            throw new UncheckedIOException(format("Exception reading resource [%s]", resource), t);
        } finally {
            close(bufferedReader);
            close(inputStream);
        }
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable t) {
                // DO NOTHING
            }
        }
    }
}
