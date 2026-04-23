package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 - API Observability: Request & Response Logging Filter.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter so a
 * single @Provider class handles the entire request/response lifecycle.
 *
 * Every incoming request logs: HTTP method + URI
 * Every outgoing response logs: HTTP status code
 *
 * Using a cross-cutting filter here (rather than Logger.info() in every
 * resource method) means logging logic is defined once and applied uniformly
 * to all endpoints — a core principle of Aspect-Oriented design.
 */
@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(ApiLoggingFilter.class.getName());

    /**
     * Invoked before the resource method is called.
     * Logs the HTTP method and full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("[REQUEST]  %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    /**
     * Invoked after the resource method has returned.
     * Logs the HTTP status code of the response.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format("[RESPONSE] %s %s -> HTTP %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()));
    }
}
