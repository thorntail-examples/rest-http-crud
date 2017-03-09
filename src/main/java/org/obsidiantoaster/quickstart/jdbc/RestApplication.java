package org.obsidiantoaster.quickstart.jdbc;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * @author Heiko Braun
 */
@ApplicationPath("/rest/v1")
public class RestApplication extends Application {

    public RestApplication() {
    }
}