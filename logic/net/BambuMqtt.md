# Bambu Lab MQTT Communication

This module provides classes for communicating with Bambu Lab 3D printers over MQTT.

## Core Components

- `BambuMqttClient`: The main entry point for MQTT communication. It handles connection, authentication, and subscription to telemetry topics.
- `BambuMqttMapper`: Responsible for parsing JSON payloads from the printer and mapping them to Java objects based on the source key (e.g., `print`).
- `BambuTelemetry`: DTO classes representing the telemetry data structure of Bambu Lab printers.
- `BambuPrinterNetConnection`: Interface defining the required connection parameters (URL, Serial, Access Code).

## Pairing Process

The pairing process with Bambu Lab printers involves:
1. Connecting to the printer's IP address on port 8883 (SSL).
2. Authenticating with username `bblp` and the access code (provided by the printer's screen) as the password.
3. Subscribing to the `device/<serial>/report` topic to receive telemetry.

## Usage Example

```java
BambuPrinterNetConnection connection = new BambuMqttPrinterNetConnection(
    new URL("https://192.168.1.100:8883"),
    "SN12345678",
    "access_code"
);

BambuMqttClient client = new BambuMqttClient(connection);
client.setTelemetryConsumer(telemetry -> {
    BambuTelemetry.BambuPrintStatus status = (BambuTelemetry.BambuPrintStatus) telemetry.get("print");
    if (status != null) {
        System.out.println("Nozzle Temperature: " + status.getNozzleTemperature());
    }
});

client.connect().join();
```

## Topic Mapping

The `BambuMqttMapper` allows registering custom parsers for different source keys in the JSON payload. By default, it supports the `print` source key.
