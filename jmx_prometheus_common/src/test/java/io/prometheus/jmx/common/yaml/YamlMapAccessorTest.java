/*
 * Copyright (C) 2018-present The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.common.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings("unchecked")
public class YamlMapAccessorTest {

    @Test
    public void testValidPaths() throws IOException {
        YamlMapAccessor yamlMapAccessor = createYamlMapAccessor("/YamlMapAccessorTest.yaml");

        Optional<Object> optional = yamlMapAccessor.get("/");

        assertThat(optional).isNotNull();
        assertThat(optional).isPresent();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Map.class);

        optional = yamlMapAccessor.get("/httpServer");

        assertThat(optional).isNotNull();
        assertThat(optional).isPresent();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Map.class);

        optional = yamlMapAccessor.get("/httpServer/authentication");

        assertThat(optional).isNotNull();
        assertThat(optional).isPresent();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Map.class);

        optional = yamlMapAccessor.get("/httpServer/authentication/basic");

        assertThat(optional).isNotNull();
        assertThat(optional).isPresent();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Map.class);

        optional = yamlMapAccessor.get("/httpServer/authentication/basic/username");

        assertThat(optional).isNotNull();
        assertThat(optional).isPresent();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(String.class);
        assertThat(optional.get()).isEqualTo("Prometheus");

        optional = yamlMapAccessor.get("/httpServer/authentication/basic/password");

        assertThat(optional).isNotNull();
        assertThat(optional).isPresent();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(String.class);
        assertThat(optional.get())
                .isEqualTo("c6d52fc2733af33e62b45d4525261e35e04f7b0ec227e4feee8fd3fe1401a2a9");

        optional = yamlMapAccessor.get("/httpServer/threads");

        assertThat(optional).isNotNull();
        assertThat(optional).isPresent();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Map.class);

        optional = yamlMapAccessor.get("/httpServer/threads/minimum");

        assertThat(optional).isNotNull();
        assertThat(optional).isPresent();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Integer.class);
        assertThat(optional.get()).isEqualTo(1);

        optional = yamlMapAccessor.get("/httpServer/threads/maximum");

        assertThat(optional).isNotNull();
        assertThat(optional).isPresent();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Integer.class);
        assertThat(optional.get()).isEqualTo(10);

        optional = yamlMapAccessor.get("/httpServer/threads/keepAlive");

        assertThat(optional).isNotNull();
        assertThat(optional).isPresent();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Integer.class);
        assertThat(optional.get()).isEqualTo(120);
    }

    @Test
    public void testInvalidPaths() throws IOException {
        YamlMapAccessor yamlMapAccessor = createYamlMapAccessor("/YamlMapAccessorTest.yaml");

        assertThatThrownBy(() -> yamlMapAccessor.get(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> yamlMapAccessor.get("//"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> yamlMapAccessor.get("foo"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> yamlMapAccessor.get("/foo/"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testOtherPaths() throws IOException {
        YamlMapAccessor yamlMapAccessor = createYamlMapAccessor("/YamlMapAccessorTest.yaml");

        Optional<Object> optional = yamlMapAccessor.get("/foo");

        assertThat(optional).isNotNull();
        assertThat(optional.isPresent()).isFalse();

        optional = yamlMapAccessor.getOrCreate("/foo", LinkedHashMap::new);

        assertThat(optional).isNotNull();
        assertThat(optional.isPresent()).isTrue();

        assertThat(optional).isNotNull();
        assertThat(optional.get()).isInstanceOf(Map.class);

        Map<Object, Object> map = (Map<Object, Object>) optional.get();

        assertThat(map).isNotNull();
        assertThat(map.size()).isEqualTo(0);

        optional = yamlMapAccessor.get("/foo");

        assertThat(optional).isNotNull();
        assertThat(optional.isPresent()).isTrue();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Map.class);

        map = (Map<Object, Object>) optional.get();
        map.put("value", 1);

        optional = yamlMapAccessor.get("/foo/value");

        assertThat(optional).isNotNull();
        assertThat(optional.isPresent()).isTrue();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Integer.class);
        assertThat(optional.get()).isEqualTo(1);

        assertThatThrownBy(
                        () ->
                                yamlMapAccessor.getOrThrow(
                                        "/foo/value2",
                                        () -> new IllegalArgumentException("path doesn't exist")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("path doesn't exist");

        YamlMapAccessor emptyYamlMapAccessor = YamlMapAccessor.empty();

        optional = emptyYamlMapAccessor.getOrCreate("/foo/bar/value", () -> 1);

        assertThat(optional).isNotNull();
        assertThat(optional.isPresent()).isTrue();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Integer.class);
        assertThat(optional.get()).isEqualTo(1);
    }

    @Test
    public void testEmpty() {
        YamlMapAccessor yamlMapAccessor = YamlMapAccessor.empty();
        Optional<Object> optional = yamlMapAccessor.get("/");

        assertThat(optional).isNotNull();
        assertThat(optional.isPresent()).isTrue();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Map.class);

        Map<Object, Object> map = (Map<Object, Object>) optional.get();

        assertThat(map).isEmpty();

        optional = yamlMapAccessor.getOrCreate("/foo", LinkedHashMap::new);

        assertThat(optional).isNotNull();
        assertThat(optional.isPresent()).isTrue();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Map.class);

        map = (Map<Object, Object>) optional.get();
        map.put("value", 1);

        optional = yamlMapAccessor.get("/foo/value");

        assertThat(optional).isNotNull();
        assertThat(optional.isPresent()).isTrue();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Integer.class);
        assertThat(optional.get()).isEqualTo(1);

        optional = yamlMapAccessor.get("/foo");

        assertThat(optional).isNotNull();
        assertThat(optional.isPresent()).isTrue();
        assertThat(optional.get()).isNotNull();
        assertThat(optional.get()).isInstanceOf(Map.class);

        map = (Map<Object, Object>) optional.get();

        assertThat(map.get("value")).isInstanceOf(Integer.class);
        assertThat((int) ((Integer) map.get("value"))).isEqualTo(1);
    }

    @Test
    public void testContainsPath() throws IOException {
        YamlMapAccessor yamlMapAccessor = createYamlMapAccessor("/YamlMapAccessorTest.yaml");

        assertThat(yamlMapAccessor.containsPath("/")).isTrue();

        assertThat(yamlMapAccessor.containsPath("/key")).isTrue();
        assertThat(yamlMapAccessor.containsPath("/key/subkey")).isTrue();
        assertThat(yamlMapAccessor.get("/key/subkey").isPresent()).isFalse();

        assertThat(yamlMapAccessor.containsPath("/key2")).isTrue();
        assertThat(yamlMapAccessor.containsPath("/key2/subkey2")).isTrue();
        assertThat(yamlMapAccessor.get("/key2/subkey2").isPresent()).isTrue();

        assertThat(yamlMapAccessor.containsPath("/key/foo")).isFalse();
    }

    private static YamlMapAccessor createYamlMapAccessor(String resource) throws IOException {
        try (InputStream inputStream = YamlMapAccessorTest.class.getResourceAsStream(resource)) {
            Map<Object, Object> map = new Yaml().load(inputStream);
            return new YamlMapAccessor(map);
        }
    }
}
