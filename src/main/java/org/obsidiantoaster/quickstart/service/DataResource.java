package org.obsidiantoaster.quickstart.service;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author Heiko Braun
 * @since 17/01/2017
 */
@Path("/")
public class DataResource {

    @GET
    @Produces("text/plain")
    public String get() throws NamingException, SQLException {
        Context ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("jboss/datasources/MyDS");
        Connection conn = ds.getConnection();
        try {
            return "Using datasource driver: " + conn.getMetaData().getDriverName();
        } finally {
            conn.close();
        }
    }
}
