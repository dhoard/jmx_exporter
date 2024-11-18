package io.prometheus.jmx.test.support;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Class to implement ResourceLoader */
public class ResourceLoader {

    /** Constructor */
    private ResourceLoader() {
        // DO NOTHING
    }

    /**
     * Method to load a resource
     *
     * @param resource resource
     * @return the content
     */
    public static String load(String resource) {
        StringBuilder content = new StringBuilder();

        InputStream inputStream = null;
        BufferedReader bufferedReader = null;

        try {
            inputStream = ResourceLoader.class.getResourceAsStream(resource);
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

                content.append(line).append(System.lineSeparator());
            }

            if (content.length() > 0) {
                content.setLength(content.length() - System.lineSeparator().length());
            }

            return content.toString();
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
