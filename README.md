# Smart Campus Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey 2.x) for the University of Westminster's "Smart Campus" initiative. This service manages Rooms and Sensors across campus, including historical sensor readings.

---

## Technology Stack

- **Language:** Java 11
- **Framework:** JAX-RS via Jersey 2.39.1
- **JSON:** Jackson (via `jersey-media-json-jackson`)
- **Build tool:** Maven
- **Server:** Apache Tomcat 9.x (or any Servlet 3.1 container)
- **Data storage:** In-memory `ConcurrentHashMap` (no database)

---

## Project Structure

```
SmartCampusAPI/
├── pom.xml
└── src/main/
    ├── java/com/smartcampus/
    │   ├── SmartCampusApplication.java       # @ApplicationPath("/api/v1")
    │   ├── model/
    │   │   ├── Room.java
    │   │   ├── Sensor.java
    │   │   ├── SensorReading.java
    │   │   └── DataStore.java                # Singleton in-memory store
    │   ├── resource/
    │   │   ├── DiscoveryResource.java        # GET /api/v1
    │   │   ├── RoomResource.java             # /api/v1/rooms
    │   │   ├── SensorResource.java           # /api/v1/sensors
    │   │   └── SensorReadingResource.java    # /api/v1/sensors/{id}/readings
    │   ├── exception/
    │   │   ├── ErrorResponse.java
    │   │   ├── RoomNotEmptyException.java
    │   │   ├── RoomNotEmptyExceptionMapper.java
    │   │   ├── LinkedResourceNotFoundException.java
    │   │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   │   ├── SensorUnavailableException.java
    │   │   ├── SensorUnavailableExceptionMapper.java
    │   │   └── GlobalExceptionMapper.java
    │   └── filter/
    │       └── ApiLoggingFilter.java
    └── webapp/WEB-INF/
        └── web.xml
```

---

## How to Build and Run

### Prerequisites
- Java JDK 11 or higher
- Apache Maven 3.6+
- Apache Tomcat 9.x

### Step 1 – Clone the repository
```bash
git clone https://github.com/<your-username>/SmartCampusAPI.git
cd SmartCampusAPI
```

### Step 2 – Build the WAR file
```bash
mvn clean package
```
This produces `target/SmartCampusAPI.war`.

### Step 3 – Deploy to Tomcat
Copy the WAR to your Tomcat `webapps` folder:
```bash
cp target/SmartCampusAPI.war /path/to/tomcat/webapps/
```
Then start Tomcat:
```bash
/path/to/tomcat/bin/startup.sh   # Linux/macOS
/path/to/tomcat/bin/startup.bat  # Windows
```

### Step 4 – Verify the server is running
```bash
curl http://localhost:8080/SmartCampusAPI/api/v1
```

---

## API Endpoints Summary

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1` | Discovery – API metadata and resource links |
| GET | `/api/v1/rooms` | List all rooms |
| POST | `/api/v1/rooms` | Create a new room |
| GET | `/api/v1/rooms/{roomId}` | Get a specific room |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room (blocked if sensors exist) |
| GET | `/api/v1/sensors` | List all sensors (supports `?type=` filter) |
| POST | `/api/v1/sensors` | Register a new sensor |
| GET | `/api/v1/sensors/{sensorId}` | Get a specific sensor |
| DELETE | `/api/v1/sensors/{sensorId}` | Delete a sensor |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get all readings for a sensor |
| POST | `/api/v1/sensors/{sensorId}/readings` | Post a new reading |

---

## Sample curl Commands

### 1. Discover the API
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1 \
  -H "Accept: application/json"
```

### 2. List all rooms
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/rooms \
  -H "Accept: application/json"
```

### 3. Create a new room
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-101","name":"Main Hall","capacity":200}'
```

### 4. Register a new sensor (links to existing room LIB-301)
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"HUM-001","type":"Humidity","status":"ACTIVE","currentValue":55.0,"roomId":"LIB-301"}'
```

### 5. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/SmartCampusAPI/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```

### 6. Post a sensor reading
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.3}'
```

### 7. Get all readings for a sensor
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/sensors/TEMP-001/readings \
  -H "Accept: application/json"
```

### 8. Attempt to delete a room that still has sensors (expect 409)
```bash
curl -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/LIB-301
```

### 9. Attempt to post a reading to a MAINTENANCE sensor (expect 403)
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":10.0}'
```

### 10. Register a sensor with a non-existent roomId (expect 422)
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"ERR-001","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"GHOST-999"}'
```

---

## Report – Answers to Coursework Questions

---

### Part 1 – Setup & Discovery

**Q: Explain the default lifecycle of a JAX-RS resource class. How does this impact in-memory data management?**

By default, JAX-RS creates a **new instance** of each resource class for every incoming HTTP request (per-request scope). This is the specification default and means any instance variables declared directly on the resource class would be re-initialised with every call — making them unsuitable for storing shared state.

To manage in-memory data safely across requests, this project uses a **Singleton `DataStore` class** (the GoF Singleton pattern) backed by `ConcurrentHashMap`. Because `ConcurrentHashMap` is thread-safe, multiple simultaneous requests can read and write sensor/room data without causing race conditions or data loss. The resource classes never hold state themselves — they simply call `DataStore.getInstance()` to access the shared maps. This cleanly separates the stateless request-handling lifecycle of JAX-RS from the stateful data layer.

---

**Q: Why is HATEOAS considered a hallmark of advanced RESTful design, and how does it benefit client developers?**

HATEOAS (Hypermedia as the Engine of Application State) means that API responses include links to related or available actions, rather than requiring clients to construct URLs manually. This has several advantages:

1. **Discoverability** – A client starting at `GET /api/v1` can find all resource collections from the response alone, without reading external documentation.
2. **Decoupling** – If the server changes a URL path, clients following links do not break, because they navigate dynamically rather than relying on hardcoded paths.
3. **Self-documenting** – New developers can explore the API by following links, reducing onboarding time.
4. **Reduced client logic** – Clients do not need to know the state machine of the API; the server communicates what actions are available at each step.

In contrast, static documentation quickly becomes outdated and forces clients to embed server internals (URL patterns) into their own code.

---

### Part 2 – Room Management

**Q: What are the implications of returning only IDs vs full room objects in a list response?**

Returning **full room objects** provides all data in one request, reducing the number of round-trips a client needs to make. This is efficient when the client will use most of the fields (e.g., displaying a room management dashboard).

Returning **only IDs** reduces payload size considerably when rooms have many fields or nested objects. It follows a "lazy loading" pattern — the client fetches detail only for the rooms it cares about. However, this increases the number of HTTP requests (one per room), adding latency.

The superior approach depends on the use case: for listing and browsing, full objects are preferred. For large-scale pagination with thousands of records, ID-only responses (with pagination links) are more scalable. This API returns full objects for simplicity and to minimise client-side complexity.

---

**Q: Is DELETE idempotent in your implementation? Justify with what happens on repeated calls.**

Yes, **DELETE is idempotent** in this implementation in the HTTP sense: repeated calls produce the same server state. After the first successful `DELETE /api/v1/rooms/HALL-101`, the room no longer exists. Any subsequent identical DELETE request will receive `404 Not Found`, because the resource is already gone — but crucially, the server state does not change further. The resource remains absent regardless of how many times the request is repeated.

Idempotency does **not** mean the response is identical — it means the **side effects** are the same. RFC 7231 defines idempotency as "multiple identical requests having the same intended effect on the server as a single request," which is satisfied here. The one case where idempotency is intentionally broken in this API is the safety check: if sensors still exist in the room, every DELETE attempt returns 409 — this is correct behaviour because the constraint (sensors present) is a genuine business rule, not a transient state.

---

### Part 3 – Sensor Operations & Linking

**Q: What happens if a client sends data in a format other than `application/json` to a `@Consumes(APPLICATION_JSON)` endpoint?**

When a client sends a request body with a `Content-Type` header that does not match `application/json` (e.g., `text/plain` or `application/xml`), JAX-RS automatically returns **HTTP 415 Unsupported Media Type** before the resource method is even invoked. The runtime inspects the `Content-Type` header of the incoming request and compares it against the media types declared in `@Consumes`. If there is no match, the request is rejected at the framework level with no developer code required. This protects the resource method from receiving incorrectly formatted data and enforces strict contract adherence.

---

**Q: Why is `@QueryParam` superior to a path-based design (`/sensors/type/CO2`) for filtering?**

Using `@QueryParam` (e.g., `GET /api/v1/sensors?type=CO2`) is the conventional and recommended approach for filtering/searching collections for several reasons:

1. **Semantic clarity** – Path segments identify a specific resource (`/sensors/TEMP-001`). Query parameters modify a collection view. Embedding filter criteria in the path blurs this boundary.
2. **Optional by nature** – Query parameters are inherently optional. `GET /api/v1/sensors` and `GET /api/v1/sensors?type=CO2` both map to the same method, cleanly handling both cases. A path segment like `/sensors/type/CO2` implies `type` is always required.
3. **Composability** – Multiple filters can be combined easily: `?type=CO2&status=ACTIVE`. Achieving the same with path segments becomes cumbersome and creates URL explosion.
4. **Caching and bookmarking** – Filtered query URLs are easier to cache and share.
5. **RESTful convention** – REST best practices (and frameworks like OpenAPI) treat path parameters as resource identifiers and query parameters as modifiers.

---

### Part 4 – Deep Nesting with Sub-Resources

**Q: What are the architectural benefits of the Sub-Resource Locator pattern?**

The Sub-Resource Locator pattern allows a resource class to delegate handling of a sub-path to a separate, dedicated class. In this API, `SensorResource` delegates `/{sensorId}/readings` to `SensorReadingResource` by returning an instance of it rather than handling the path directly.

Benefits include:

1. **Separation of concerns** – `SensorResource` handles sensor lifecycle (CRUD). `SensorReadingResource` handles reading history. Each class has a single, well-defined responsibility.
2. **Maintainability** – In large APIs with deep nesting, a single "mega-controller" becomes unmanageable. Separate classes keep files small and focused.
3. **Reusability** – A sub-resource class can potentially be reused or composed from multiple parent resources.
4. **Testability** – Each class can be unit tested in isolation by constructing it with a specific `sensorId`, without needing to exercise the full resource chain.
5. **Scalability** – Adding new operations to readings (e.g., aggregation, pagination) does not touch the sensor class at all.

---

### Part 5 – Error Handling, Exception Mapping & Logging

**Q: Why is HTTP 422 more semantically accurate than 404 when a linked resource is missing from a request payload?**

`404 Not Found` conventionally means "the resource you requested by URI does not exist." If a client does `GET /api/v1/rooms/GHOST-999`, a 404 is correct — the URI target does not exist.

However, when a client POSTs a new sensor with `"roomId": "GHOST-999"` embedded in the body, the **requested URI** (`/api/v1/sensors`) is perfectly valid. The server understands the request. The problem is that the **payload contains a reference to a non-existent entity** — the room ID in the JSON body cannot be resolved. This is a semantic error in the data, not a missing endpoint.

`422 Unprocessable Entity` was designed for exactly this case: "the server understands the content type and syntax of the request, but was unable to process the contained instructions." Returning 404 here would mislead the client into thinking the sensors endpoint itself does not exist, rather than pointing to the real problem in the request body.

---

**Q: From a cybersecurity standpoint, what are the risks of exposing Java stack traces to external clients?**

Exposing raw stack traces to API consumers creates several serious security vulnerabilities:

1. **Technology fingerprinting** – Stack traces reveal the exact framework (`org.glassfish.jersey`), Java version, and library versions in use. Attackers can cross-reference these against known CVE databases to find unpatched exploits.
2. **Internal path disclosure** – Fully-qualified class names and file paths reveal the internal package structure of the application, helping attackers understand its architecture.
3. **Business logic exposure** – Method names and call chains in a trace reveal how the application processes data, helping attackers craft targeted exploits (e.g., identifying deserialization or injection points).
4. **Data leakage** – Some exceptions include variable values in their messages (e.g., `NullPointerException: Cannot read field "id" of null`), which may inadvertently expose database field names or sensitive configuration.
5. **Facilitates targeted attacks** – Combined, this information dramatically lowers the effort required for a successful attack. The `GlobalExceptionMapper` in this project ensures the server logs the full trace internally while the client receives only a generic `500 Internal Server Error` message.

---

**Q: Why is it better to use JAX-RS filters for cross-cutting concerns like logging instead of manual `Logger.info()` calls in every resource method?**

Inserting `Logger.info()` calls manually into every resource method violates the **DRY (Don't Repeat Yourself)** principle and creates several problems:

1. **Code duplication** – Every method needs the same boilerplate. With dozens of endpoints, this becomes hundreds of identical lines.
2. **Inconsistency** – A developer adding a new endpoint might forget to add logging. Filters guarantee uniform coverage with zero developer effort.
3. **Maintenance burden** – If the log format needs to change (e.g., adding a correlation ID), a filter requires one change. Manual logging requires hunting down every resource method.
4. **Separation of concerns** – Resource methods should focus on business logic. Logging, authentication, and CORS are **cross-cutting concerns** — the filter/interceptor pattern (rooted in Aspect-Oriented Programming) is the correct abstraction for them.
5. **Pre/post visibility** – Filters have access to both the request context and the response context, enabling complete request/response cycle logging in a single class, which is impossible with inline logging inside the resource method body alone.
