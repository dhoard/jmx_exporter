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

package io.prometheus.jmx.common.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class PreconditionTest {

    @Test
    public void testNotNull1() {
        Precondition.notNull(new Object());
    }

    @Test
    public void testNotNull2() {
        assertThatThrownBy(() -> Precondition.notNull(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNotNull1NotEmpty1() {
        Precondition.notNullOrEmpty("test");
    }

    @Test
    public void testNotNull1NotEmpty2() {
        Precondition.notNullOrEmpty(" test ");
    }

    @Test
    public void testNotNull1NotEmpty3() {
        assertThatThrownBy(() -> Precondition.notNullOrEmpty(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNotNull1NotEmpty4() {
        assertThatThrownBy(() -> Precondition.notNullOrEmpty("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNotNull1NotEmpty5() {
        assertThatThrownBy(() -> Precondition.notNullOrEmpty("\t\r\n"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNotNull1NotEmpty6() {
        assertThatThrownBy(() -> Precondition.notNullOrEmpty("\t"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIsGreaterThanOrEqualTo1() {
        Precondition.isGreaterThanOrEqualTo(1, 1);
    }

    @Test
    public void testIsGreaterThanOrEqualTo2() {
        Precondition.isGreaterThanOrEqualTo(2, 1);
    }

    @Test
    public void testIsGreaterThanOrEqualTo3() {
        assertThatThrownBy(() -> Precondition.isGreaterThanOrEqualTo(0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
