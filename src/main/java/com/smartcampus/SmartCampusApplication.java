package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application entry point.
 * Sets the base URI for all API endpoints to /api/v1
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Jersey auto-discovers resources via classpath scanning.
    // No need to override getClasses() unless you want explicit registration.
}
