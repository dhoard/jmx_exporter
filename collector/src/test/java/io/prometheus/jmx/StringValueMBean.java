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
package io.prometheus.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/** Class to implement StringValueMBean */
public interface StringValueMBean {

    /**
     * Method to get the text
     *
     * @return text
     */
    String getText();
}

/** Class to implement StringValue */
class StringValue implements StringValueMBean {

    @Override
    public String getText() {
        return "value";
    }

    public static void registerBean(MBeanServer mbs) throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName("io.prometheus.jmx:type=stringValue");
        StringValueMBean mbean = new StringValue();
        mbs.registerMBean(mbean, mbeanName);
    }
}
