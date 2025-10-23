# Spring Boot Messaging Workshop - RabbitMQ & Kafka

Praktický workshop pro seznámení s messaging systémy **RabbitMQ** a **Apache Kafka** pomocí **Spring Boot**. Workshop obsahuje dva Maven projekty, Docker Compose konfiguraci a REST API pro testování.

## 📋 Obsah

1. [Požadavky](#požadavky)
2. [Instalace a spuštění](#instalace-a-spuštění)
3. [Struktura projektu](#struktura-projektu)
4. [Praktické cvičení](#praktické-cvičení)
5. [REST API](#rest-api)
6. [Monitoring](#monitoring)
7. [Troubleshooting](#troubleshooting)

---

## 🔧 Požadavky

Před zahájením workshopu se ujistěte, že máte nainstalováno:

- **Java JDK** (verze 17 nebo vyšší)
- **Maven** (verze 3.8 nebo vyšší)
- **Docker** (verze 20.x nebo vyšší)
- **Docker Compose** (verze 2.x nebo vyšší)

### Ověření instalace

```bash
java -version
mvn -version
docker --version
docker-compose --version
```

---

## 🚀 Instalace a spuštění

### Krok 1: Stažení projektu

```bash
# Rozbalení archivu
unzip spring_messaging_workshop.zip
cd spring_messaging_workshop
```

### Krok 2: Spuštění Docker kontejnerů

```bash
cd docker
docker-compose up -d

# Kontrola běžících kontejnerů
docker-compose ps

# Zobrazení logů
docker-compose logs -f
```

### Krok 3: Build Maven projektů

```bash
# RabbitMQ projekt
cd ../rabbitmq-demo
mvn clean package

# Kafka projekt
cd ../kafka-demo
mvn clean package
```

### Krok 4: Spuštění Spring Boot aplikací

**Terminál 1 - RabbitMQ aplikace:**
```bash
cd rabbitmq-demo
mvn spring-boot:run
```

**Terminál 2 - Kafka aplikace:**
```bash
cd kafka-demo
mvn spring-boot:run
```

### Krok 5: Ověření běhu aplikací

```bash
# RabbitMQ aplikace
curl http://localhost:8081/api/workshop/health

# Kafka aplikace
curl http://localhost:8082/api/workshop/health
```

---

## 📁 Struktura projektu

```
spring_messaging_workshop/
├── docker/
│   ├── docker-compose.yml      # Docker Compose konfigurace
│   └── prometheus.yml          # Prometheus konfigurace
├── rabbitmq-demo/
│   ├── pom.xml                 # Maven konfigurace
│   └── src/main/
│       ├── java/com/workshop/rabbitmq/
│       │   ├── RabbitMQWorkshopApplication.java
│       │   ├── config/         # RabbitMQ konfigurace
│       │   ├── model/          # Data modely
│       │   ├── producer/       # Producer service
│       │   ├── consumer/       # Consumer service
│       │   ├── metrics/        # Metrics service
│       │   └── controller/     # REST controllers
│       └── resources/
│           └── application.yml # Konfigurace aplikace
├── kafka-demo/
│   ├── pom.xml                 # Maven konfigurace
│   └── src/main/
│       ├── java/com/workshop/kafka/
│       │   ├── KafkaWorkshopApplication.java
│       │   ├── config/         # Kafka konfigurace
│       │   ├── model/          # Data modely
│       │   ├── producer/       # Producer service
│       │   ├── consumer/       # Consumer service
│       │   ├── metrics/        # Metrics service
│       │   └── controller/     # REST controllers
│       └── resources/
│           └── application.yml # Konfigurace aplikace
└── README.md                   # Tento návod
```

---

## 🎯 Praktické cvičení

### Cvičení 1: RabbitMQ Producer & Consumer

#### 1.1 Spuštění RabbitMQ aplikace

```bash
cd rabbitmq-demo
mvn spring-boot:run
```

Aplikace se spustí na portu **8081**.

#### 1.2 Odeslání zpráv pomocí REST API

```bash
# Odeslání 1000 zpráv
curl -X POST "http://localhost:8081/api/workshop/send?count=1000"

# Kontrola metrik
curl http://localhost:8081/api/workshop/metrics
```

#### 1.3 Sledování v RabbitMQ Management UI

Otevřete http://localhost:15672 (admin/admin123) a prozkoumejte:
- Queues → `workshop-queue` (hlavní fronta)
- Queues → `workshop-dlq` (Dead Letter Queue)
- Exchanges → `workshop-exchange`

**Co se děje:**
- Producer odesílá zprávy do exchange
- Exchange směruje zprávy do fronty pomocí routing key
- Consumer zpracovává zprávy s ACK/NACK
- Metriky jsou automaticky měřeny (latence, propustnost)

---

### Cvičení 2: RabbitMQ Retry & DLQ

#### 2.1 Aktivace simulace chyb

Upravte `rabbitmq-demo/src/main/resources/application.yml`:

```yaml
workshop:
  rabbitmq:
    simulate-failures: true
    failure-rate: 0.1  # 10% zpráv selže
```

#### 2.2 Restart aplikace a test

```bash
mvn spring-boot:run
```

V jiném terminálu:
```bash
curl -X POST "http://localhost:8081/api/workshop/send?count=100"
```

**Co se děje:**
- 10% zpráv náhodně selže
- Zprávy jsou automaticky retry (max 3 pokusy)
- Po vyčerpání pokusů jsou přesunuty do DLQ
- Sledujte logy pro retry pokusy

#### 2.3 Kontrola DLQ

V RabbitMQ Management UI zkontrolujte frontu `workshop-dlq` - měla by obsahovat zprávy, které selhaly.

---

### Cvičení 3: Apache Kafka Producer & Consumer

#### 3.1 Spuštění Kafka aplikace

```bash
cd kafka-demo
mvn spring-boot:run
```

Aplikace se spustí na portu **8082**.

#### 3.2 Odeslání zpráv

```bash
# Odeslání 1000 zpráv
curl -X POST "http://localhost:8082/api/workshop/send?count=1000"

# Kontrola metrik
curl http://localhost:8082/api/workshop/metrics
```

**Co se děje:**
- Producer odesílá zprávy do topicu `workshop-topic`
- Zprávy jsou rozděleny do 3 partitions podle klíče
- Consumer group čte zprávy s manuálním commit
- Měření latence a propustnosti

---

### Cvičení 4: Kafka Retry & DLQ

#### 4.1 Aktivace simulace chyb

Upravte `kafka-demo/src/main/resources/application.yml`:

```yaml
workshop:
  kafka:
    simulate-failures: true
    failure-rate: 0.1
    max-retries: 3
```

#### 4.2 Test s chybami

```bash
mvn spring-boot:run
```

V jiném terminálu:
```bash
curl -X POST "http://localhost:8082/api/workshop/send?count=100"
```

**Co se děje:**
- 10% zpráv náhodně selže
- Implementován exponential backoff (1s, 2s, 4s, 8s)
- Po 3 pokusech je zpráva přesunuta do DLQ topicu
- Offset je commitnut až po úspěšném zpracování nebo přesunu do DLQ

---

## 🌐 REST API

### RabbitMQ aplikace (port 8081)

| Metoda | Endpoint | Popis |
|--------|----------|-------|
| POST | `/api/workshop/send?count=N` | Odešle N zpráv |
| GET | `/api/workshop/metrics` | Vrátí metriky výkonu |
| POST | `/api/workshop/reset` | Resetuje metriky |
| GET | `/api/workshop/health` | Health check |
| GET | `/actuator/prometheus` | Prometheus metriky |

### Kafka aplikace (port 8082)

| Metoda | Endpoint | Popis |
|--------|----------|-------|
| POST | `/api/workshop/send?count=N` | Odešle N zpráv |
| GET | `/api/workshop/metrics` | Vrátí metriky výkonu |
| POST | `/api/workshop/reset` | Resetuje metriky |
| GET | `/api/workshop/health` | Health check |
| GET | `/actuator/prometheus` | Prometheus metriky |

### Příklady použití

```bash
# Odeslání 5000 zpráv do RabbitMQ
curl -X POST "http://localhost:8081/api/workshop/send?count=5000"

# Získání metrik z Kafka
curl http://localhost:8082/api/workshop/metrics | jq

# Reset metrik
curl -X POST http://localhost:8081/api/workshop/reset
```

---

## 📊 Benchmark a srovnání

### Manuální benchmark

```bash
# 1. Reset metrik
curl -X POST http://localhost:8081/api/workshop/reset
curl -X POST http://localhost:8082/api/workshop/reset

# 2. Odeslání zpráv do RabbitMQ
time curl -X POST "http://localhost:8081/api/workshop/send?count=1000"

# 3. Získání metrik RabbitMQ
curl http://localhost:8081/api/workshop/metrics > rabbitmq_metrics.json

# 4. Odeslání zpráv do Kafka
time curl -X POST "http://localhost:8082/api/workshop/send?count=1000"

# 5. Získání metrik Kafka
curl http://localhost:8082/api/workshop/metrics > kafka_metrics.json
```

### Očekávané výsledky

**RabbitMQ:**
- Propustnost: 2000-5000 msg/s
- Průměrná latence: 1-3 ms
- P95 latence: 3-5 ms

**Kafka:**
- Propustnost: 5000-20000 msg/s
- Průměrná latence: 2-5 ms
- P95 latence: 5-10 ms

---

## 📈 Monitoring

### Spring Boot Actuator

Obě aplikace poskytují Actuator endpoints:

```bash
# Health check
curl http://localhost:8081/actuator/health

# Metrics
curl http://localhost:8081/actuator/metrics

# Prometheus format
curl http://localhost:8081/actuator/prometheus
```

### Prometheus & Grafana

1. **Prometheus**: http://localhost:9090
   - Targets → zkontrolujte, že Spring Boot aplikace jsou UP

2. **Grafana**: http://localhost:3000 (admin/admin123)
   - Configuration → Data Sources → Add Prometheus
   - URL: `http://prometheus:9090`
   - Vytvořte dashboard s metrikami:
     - `messages_produced_total`
     - `messages_consumed_total`
     - `messages_producer_latency_seconds`

### RabbitMQ Management UI

http://localhost:15672 (admin/admin123)

- Overview - celkový přehled
- Queues - fronty a jejich stav
- Exchanges - exchanges a bindings

---

## 🔍 Praktické úkoly pro studenty

### Úkol 1: Základní komunikace
1. Spusťte RabbitMQ aplikaci
2. Odešlete 1000 zpráv pomocí REST API
3. Sledujte zprávy v RabbitMQ Management UI
4. Zkontrolujte metriky
5. **Otázka:** Jaká je průměrná latence?

### Úkol 2: Retry mechanismus
1. Aktivujte simulaci chyb v RabbitMQ
2. Odešlete 100 zpráv
3. Sledujte retry pokusy v logách
4. Zkontrolujte DLQ
5. **Otázka:** Kolik zpráv skončilo v DLQ?

### Úkol 3: Kafka partitioning
1. Spusťte Kafka aplikaci
2. Odešlete 1000 zpráv
3. Sledujte logy - všimněte si partition assignments
4. **Otázka:** Jak jsou zprávy distribuovány mezi partitions?

### Úkol 4: Srovnání výkonu
1. Proveďte benchmark pro RabbitMQ (1000, 5000, 10000 zpráv)
2. Proveďte benchmark pro Kafka (stejné počty)
3. Porovnejte výsledky
4. **Otázka:** Který systém je rychlejší a proč?

### Úkol 5: Consumer groups
1. Spusťte 2 instance Kafka aplikace současně (změňte port)
2. Odešlete zprávy
3. Sledujte, jak se zprávy distribuují
4. **Otázka:** Jak Kafka balancuje zprávy mezi consumery?

---

## 🛠 Troubleshooting

### Problém: Maven build selhává

```bash
# Vyčistit Maven cache
mvn clean install -U

# Zkontrolovat Java verzi
java -version  # Musí být 17+
```

### Problém: Aplikace se nemůže připojit k RabbitMQ/Kafka

```bash
# Zkontrolovat Docker kontejnery
cd docker
docker-compose ps

# Restart služeb
docker-compose restart rabbitmq
docker-compose restart kafka

# Zkontrolovat logy
docker-compose logs rabbitmq
docker-compose logs kafka
```

### Problém: Port již používán

```bash
# Změnit port v application.yml
server:
  port: 8083  # Místo 8081 nebo 8082
```

### Problém: OutOfMemoryError

```bash
# Zvýšit heap size
export MAVEN_OPTS="-Xmx2048m"
mvn spring-boot:run
```

---

## 🧹 Ukončení a cleanup

### Zastavení Spring Boot aplikací

Stiskněte `Ctrl+C` v terminálech kde běží aplikace.

### Zastavení Docker služeb

```bash
cd docker

# Zastavení kontejnerů
docker-compose stop

# Zastavení a odstranění
docker-compose down

# Odstranění včetně volumes
docker-compose down -v
```

---

## 📚 Další zdroje

### Dokumentace
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring AMQP](https://spring.io/projects/spring-amqp)
- [Spring for Apache Kafka](https://spring.io/projects/spring-kafka)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)

### Tutoriály
- [Spring Boot RabbitMQ Tutorial](https://spring.io/guides/gs/messaging-rabbitmq/)
- [Spring Boot Kafka Tutorial](https://spring.io/guides/gs/messaging-kafka/)

---

## 🎓 Klíčové koncepty

### Spring AMQP (RabbitMQ)
- **@RabbitListener** - anotace pro consumer
- **RabbitTemplate** - pro odesílání zpráv
- **Manual ACK** - manuální potvrzování zpráv
- **DLQ** - Dead Letter Queue konfigurace

### Spring Kafka
- **@KafkaListener** - anotace pro consumer
- **KafkaTemplate** - pro odesílání zpráv
- **Manual commit** - manuální commit offsetů
- **Consumer groups** - load balancing

### Metriky
- **Micrometer** - abstrakce pro metriky
- **Prometheus** - sběr a ukládání metrik
- **Actuator** - Spring Boot monitoring endpoints

---

## ✅ Checklist pro workshop

- [ ] Java 17+ nainstalováno
- [ ] Maven nainstalován
- [ ] Docker a Docker Compose běží
- [ ] Docker kontejnery spuštěny
- [ ] RabbitMQ Management UI dostupné
- [ ] RabbitMQ aplikace zkompilována
- [ ] Kafka aplikace zkompilována
- [ ] RabbitMQ aplikace funguje
- [ ] Kafka aplikace funguje
- [ ] REST API testováno
- [ ] Metriky fungují
- [ ] Grafana nastavena

---

## 🎉 Závěr

Gratulujeme! Dokončili jste Spring Boot Messaging Workshop. Nyní byste měli rozumět:

✓ Implementaci messaging v Spring Boot  
✓ Rozdílům mezi RabbitMQ a Kafka  
✓ Retry mechanismům a DLQ  
✓ Měření výkonu pomocí Micrometer  
✓ Monitoring pomocí Actuator a Prometheus  

**Další kroky:**
- Prozkoumejte Spring Cloud Stream
- Implementujte transactions
- Nastavte clustering
- Optimalizujte pro produkci

---

**Autor:**   
**Verze:** 1.0  
**Datum:** 2025

