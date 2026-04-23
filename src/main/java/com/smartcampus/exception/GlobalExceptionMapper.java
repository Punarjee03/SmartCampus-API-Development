package com.smartcampus.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.4 - Global "catch-all" safety net.
 *
 * Intercepts ANY unhandled Throwable (NullPointerException,
 * IndexOutOfBoundsException, etc.) and returns a clean HTTP 500 response.
 *
 * IMPORTANT: We intentionally pass WebApplicationException back through so
 * that legitimate 404/405/415 responses from JAX-RS itself are not swallowed.
 *
 * Security note: The real stack trace is logged server-side only.
 * The client receives a generic message so no internal details are leaked.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        // Let JAX-RS handle its own HTTP exceptions (404, 405, 415, etc.) normally
        if (e instanceof WebApplicationException) {
            return ((WebApplicationException) e).getResponse();
        }

        // Log full stack trace on the server side for debugging
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by GlobalExceptionMapper: " + e.getMessage(), e);

        // Return a safe, generic response to the client - no stack trace exposed
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of(
                        500,
                        "Internal Server Error",
                        "An unexpected error occurred. Please contact the system administrator."
                ))
                .build();
    }
}
