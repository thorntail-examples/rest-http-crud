package io.openshift.boosters.jdbc;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.wildfly.swarm.health.Health;
import org.wildfly.swarm.health.HealthStatus;

/**
 * A simplistic JDBC health check
 *
 * @author Heiko Braun
 * @since 24/03/2017
 */
@Path("/checks")
@ApplicationScoped
public class JDBCHealthCheck {

    @Inject
    FruitResource resource;

    @GET
    @Path("/jdbc")
    @Health
    public HealthStatus checkJDBCConnection() {

        HealthStatus status;

        try {
            Fruit[] fruits = resource.get();
            status = HealthStatus.named("jdbc-connection")
                    .withAttribute("table-size", fruits.length)
                    .up();
        } catch (Exception e) {
            status = HealthStatus.named("jdbc-connection").down();
        }

        return status;
    }

}
