# hadoop-metrics-exporter

Prometheus-Exporter für Hadoop-Komponenten (HDFS, YARN, Hive), implementiert als Spring-Boot-Anwendung.

## Funktionsweise
Die Anwendung ruft bei jedem Scrape die JMX-HTTP-Endpunkte konfigurierter Hadoop-Dienste ab, wandelt die JMX-Beans anhand regelbasierter YAML-Definitionen (`src/main/resources/metrics/*.yaml`) in Prometheus-Gauges um und stellt sie unter `/metrics` bereit.

Unterstützte Dienste:
- HDFS NameNode
- HDFS DataNode
- HDFS JournalNode
- YARN ResourceManager
- YARN NodeManager
- HiveServer2

## Konfiguration
Die Ziel-JMX-Endpunkte werden pro Cluster in `application.yml` unter `exporter.jmx` eingetragen:

```yaml
exporter:
  cluster-name: hadoop_cluster
  path: /metrics
  jmx:
    - cluster: hadoop_prod
      services:
        namenode:
          - http://nn1:9870/jmx
        datanode:
          - http://dn1:9864/jmx
          - http://dn2:9864/jmx
        resourcemanager:
          - http://rm1:8088/jmx
        nodemanager:
          - http://nm1:8042/jmx
        hiveserver2:
          - http://hs2:10002/jmx
```

Port und Adresse des Servers werden über die Standard-Spring-Properties `server.port` / `server.address` gesteuert.

## Endpunkte
- `GET /metrics` – Prometheus-Metriken der konfigurierten Hadoop-Dienste
- `GET /actuator/health` – Health-Check
- `GET /actuator/info` – Anwendungsinformationen

## Build & Start

```bash
./mvnw clean package
java -jar target/hadoop-metrics-exporter-0.0.1-SNAPSHOT.jar
```
