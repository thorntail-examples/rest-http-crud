package io.openshift.boosters.jdbc;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * @author Heiko Braun
 */
@ApplicationPath("/api")
public class RestApplication extends Application {

    public RestApplication() {
    }
}
