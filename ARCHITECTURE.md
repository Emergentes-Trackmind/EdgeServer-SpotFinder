# EdgeServer - IoT Bounded Context Architecture

## Project Overview
**Group ID:** `com.constructinsight`  
**Artifact ID:** `EdgeServer`  
**Java Version:** 21  
**Spring Boot Version:** 3.5.8  
**Database:** MySQL

---

## Architecture: DDD + Hexagonal Architecture

### Bounded Context: IoT Device Management
**Package:** `com.constructinsight.edgeserver.iot`

```
com.constructinsight.edgeserver.iot
├── domain/                          # Core Business Logic (Framework-independent)
│   ├── model/
│   │   ├── IotDevice.java          # Aggregate Root Entity
│   │   ├── DeviceType.java         # Value Object (Enum)
│   │   └── DeviceStatus.java       # Value Object (Enum)
│   └── port/
│       └── IotDeviceRepository.java # Repository Port (Interface)
│
├── application/                     # Use Cases & Services
│   ├── dto/
│   │   └── DeviceKpiDto.java       # KPI Statistics DTO
│   └── service/
│       ├── DeviceOwnershipService.java  # Binding/Unbinding Logic
│       └── DeviceQueryService.java      # Read Operations with Privacy Filters
│
└── infrastructure/                  # Adapters (Framework-specific)
    ├── persistence/
    │   ├── JpaIotDeviceRepository.java        # Spring Data JPA Repository
    │   └── IotDeviceRepositoryAdapter.java    # Port Adapter Implementation
    └── web/
        ├── controller/
        │   └── IotDeviceController.java       # REST API Endpoints
        ├── dto/
        │   ├── IotDeviceRequestDto.java       # Request DTO
        │   ├── IotDeviceResponseDto.java      # Response DTO
        │   └── BindDeviceRequestDto.java      # Bind Request DTO
        └── mapper/
            └── IotDeviceMapper.java            # MapStruct Mapper
```

---

## Domain Model

### IotDevice (Aggregate Root)
**Entity:** `com.constructinsight.edgeserver.iot.domain.model.IotDevice`

#### Fields:
- `id` (Long): Primary key
- `serialNumber` (String): Unique identifier
- `model` (String): Device model
- `type` (DeviceType): SENSOR, ACTUATOR, GATEWAY, CAMERA, TRACKER
- `status` (DeviceStatus): ONLINE, OFFLINE, MAINTENANCE, ERROR
- `battery` (Integer): 0-100
- `lastCheckIn` (Instant): Last communication timestamp

#### Privacy Fields (Nullable):
- `ownerId` (String): User who owns the device
- `parkingId` (String): Associated parking location
- `parkingSpotId` (String): Specific parking spot

#### Business Methods:
```java
public void bind(String userId)      // Establishes ownership
public void unbind()                 // Removes all privacy associations
public boolean isBoundToUser(String userId)
public boolean isBound()
```

---

## Application Layer Services

### 1. DeviceOwnershipService
**Purpose:** Manages device binding/unbinding with strict privacy controls

#### Methods:
- `bindDevice(String serialNumber, String userId)`: Claims a device for a user
- `unbindDevice(String serialNumber, String userId)`: Releases device (only owner can unbind)

**Privacy Rule:** Only the device owner can unbind their device.

### 2. DeviceQueryService
**Purpose:** Read operations with mandatory privacy filtering

#### Methods:
- `findAllByUser(String userId)`: Returns ONLY devices where `ownerId == userId`
- `getKpis(String userId)`: Calculates statistics for user's devices only

**KPI Metrics:**
- Total Devices
- Online Devices
- Offline Devices
- Average Battery
- Low Battery Devices (< 20%)

---

## REST API Endpoints

### Base URL: `/api/iot/devices`

#### 1. Get User's Devices
```http
GET /api/iot/devices
Headers: X-User-Id: {userId}
OR
Query: ?userId={userId}

Response: List<IotDeviceResponseDto>
```

#### 2. Get Device KPIs
```http
GET /api/iot/devices/kpis
Headers: X-User-Id: {userId}

Response: DeviceKpiDto
```

#### 3. Bind Device to User
```http
POST /api/iot/devices/{serialNumber}/bind
Body: {
  "userId": "user123"
}

Response: IotDeviceResponseDto (200 OK)
Errors:
  - 404: Device not found
  - 409: Device already bound to another user
```

#### 4. Unbind Device from User
```http
DELETE /api/iot/devices/{serialNumber}/bind
Headers: X-User-Id: {userId}

Response: IotDeviceResponseDto (200 OK)
Errors:
  - 404: Device not found
  - 403: User is not the owner (Security)
```

#### 5. Bulk Load Devices (Admin)
```http
POST /api/iot/devices/bulk
Body: [
  {
    "serialNumber": "SN001",
    "model": "Model-X",
    "type": "SENSOR",
    "status": "ONLINE",
    "battery": 85,
    "lastCheckIn": "2025-11-27T10:00:00Z"
  }
]

Response: List<IotDeviceResponseDto> (201 CREATED)
```
**Note:** Devices are created with `ownerId=null` (unbound state).

---

## Privacy & Security Implementation

### Data Isolation Strategy
1. **User-Level Filtering:** All queries filter by `ownerId`
2. **Ownership Verification:** Unbind operations verify user is the owner
3. **Free Device Pool:** Unbound devices (`ownerId=null`) can be claimed by anyone
4. **No Cross-User Access:** Users can only see/manage their own devices

### Workflow Example:
1. Admin bulk-loads devices (all have `ownerId=null`)
2. User A calls `POST /bind` → Device A becomes owned by User A
3. User A calls `GET /devices` → Sees only Device A
4. User B calls `GET /devices` → Sees no devices (different user)
5. User A calls `DELETE /bind` → Device A becomes free (`ownerId=null`)
6. User B can now claim Device A

---

## Database Configuration

### application.properties
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/edgeserver_db
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=root

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### Database Setup
```sql
CREATE DATABASE edgeserver_db;
```

The `iot_devices` table will be auto-created by Hibernate with indexes on:
- `serialNumber` (unique)
- `ownerId` (for fast user queries)

---

## Build Configuration

### Critical Maven Setup (Java 21 + Lombok + MapStruct)

```xml
<properties>
    <java.version>21</java.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>21</source>
                <target>21</target>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Why This Matters:** Without explicit `annotationProcessorPaths`, Java 21 may fail to generate Lombok and MapStruct code, causing "unknown symbol" errors.

---

## Testing the API

### 1. Start MySQL
```bash
mysql -u root -p
CREATE DATABASE edgeserver_db;
```

### 2. Run the Application
```bash
.\mvnw.cmd spring-boot:run
```

### 3. Access Swagger UI
Una vez que la aplicación esté ejecutándose, abre tu navegador y accede a:

**Swagger UI (Interfaz Gráfica):**
```
http://localhost:8080/swagger-ui.html
```

**OpenAPI JSON (Documentación Raw):**
```
http://localhost:8080/api-docs
```

#### ¿Qué verás en Swagger?
- **Lista de todos los endpoints** con descripción detallada
- **Botón "Try it out"** para probar cada endpoint directamente desde el navegador
- **Ejemplos de request/response** con los modelos de datos
- **Códigos de respuesta HTTP** (200, 400, 403, 404, 409)
- **Parámetros requeridos** claramente marcados

### 4. Bulk Load Devices
```bash
curl -X POST http://localhost:8080/api/iot/devices/bulk \
  -H "Content-Type: application/json" \
  -d '[
    {
      "serialNumber": "SENSOR-001",
      "model": "TempSensor-X",
      "type": "SENSOR",
      "status": "ONLINE",
      "battery": 95
    },
    {
      "serialNumber": "SENSOR-002",
      "model": "TempSensor-X",
      "type": "SENSOR",
      "status": "OFFLINE",
      "battery": 15
    }
  ]'
```

### 5. Bind a Device (User alice)
```bash
curl -X POST http://localhost:8080/api/iot/devices/SENSOR-001/bind \
  -H "Content-Type: application/json" \
  -d '{"userId": "alice"}'
```

### 6. Get alice's Devices
```bash
curl http://localhost:8080/api/iot/devices?userId=alice
```

### 7. Get alice's KPIs
```bash
curl http://localhost:8080/api/iot/devices/kpis?userId=alice
```

### 8. Unbind Device
```bash
curl -X DELETE "http://localhost:8080/api/iot/devices/SENSOR-001/bind?userId=alice"
```

---

## Key Design Decisions

### ✅ Hexagonal Architecture
- **Domain:** Pure business logic, no framework dependencies
- **Ports:** Interfaces defining contracts
- **Adapters:** Spring/JPA implementations

### ✅ Privacy-First Design
- Every query filters by `ownerId`
- Ownership verification on mutations
- Clear separation between "free" and "owned" devices

### ✅ DDD Tactical Patterns
- **Aggregate Root:** `IotDevice`
- **Value Objects:** `DeviceType`, `DeviceStatus`
- **Repository Port:** Domain-defined interface
- **Application Services:** Orchestrate use cases

### ✅ Compilation Safety
- Explicit annotation processor configuration
- Lombok + MapStruct integration
- Java 21 compatibility

---

## Build Status
✅ **Compilation Successful**  
✅ **All 15 classes compiled**  
✅ **Annotation processors working**  
✅ **No build errors**

---

## Next Steps
1. Set up MySQL database
2. Run the application: `.\mvnw.cmd spring-boot:run`
3. Test API endpoints
4. Implement MQTT integration for device telemetry
5. Add authentication/authorization (Spring Security)
6. Implement audit logging
7. Add integration tests
