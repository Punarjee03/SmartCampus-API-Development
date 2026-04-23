package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.DataStore;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Part 3 - Sensor Operations & Linking
 * Manages /api/v1/sensors
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=CO2
     * Returns all sensors, optionally filtered by type.
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.getSensors().values();
        if (type != null && !type.isBlank()) {
            List<Sensor> filtered = all.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
            return Response.ok(filtered).build();
        }
        return Response.ok(all).build();
    }

    /**
     * POST /api/v1/sensors
     * Registers a new sensor. Validates that the roomId exists.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Sensor ID is required."))
                    .build();
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("A sensor with ID '" + sensor.getId() + "' already exists."))
                    .build();
        }
        // Validate roomId exists - throws 422 via exception mapper
        if (sensor.getRoomId() == null || !store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Room with ID '" + sensor.getRoomId() + "' does not exist. Cannot register sensor."
            );
        }
        store.getSensors().put(sensor.getId(), sensor);
        store.getReadings().put(sensor.getId(), new ArrayList<>());
        // Link sensor to room
        store.getRooms().get(sensor.getRoomId()).getSensorIds().add(sensor.getId());
        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor not found: " + sensorId))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * DELETE /api/v1/sensors/{sensorId}
     * Removes the sensor and unlinks it from its room.
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor not found: " + sensorId))
                    .build();
        }
        // Unlink from room
        if (sensor.getRoomId() != null && store.getRooms().containsKey(sensor.getRoomId())) {
            store.getRooms().get(sensor.getRoomId()).getSensorIds().remove(sensorId);
        }
        store.getSensors().remove(sensorId);
        store.getReadings().remove(sensorId);
        return Response.noContent().build();
    }

    /**
     * Part 4 - Sub-Resource Locator
     * Delegates /api/v1/sensors/{sensorId}/readings to SensorReadingResource.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            throw new WebApplicationException(
                Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor not found: " + sensorId))
                    .type(MediaType.APPLICATION_JSON)
                    .build()
            );
        }
        // Guard: sensor in MAINTENANCE cannot accept readings (checked in sub-resource for POST)
        return new SensorReadingResource(sensorId);
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return body;
    }
}
