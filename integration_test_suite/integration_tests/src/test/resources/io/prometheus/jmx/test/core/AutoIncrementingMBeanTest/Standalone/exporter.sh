#!/bin/bash

java \
  -Xmx512M \
  -jar jmx_prometheus_standalone.jar 8888 exporter.yaml
