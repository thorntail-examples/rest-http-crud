package org.obsidiantoaster.quickstart.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Heiko Braun
 * @since 17/01/2017
 */
@Path("/")
@ApplicationScoped
public class FruitResource {

    @PersistenceContext(unitName = "MyPU")
    private EntityManager em;

    @GET
    @Path("connection")
    @Produces("text/plain")
    public String getConnection() throws NamingException, SQLException {
        Context ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("jboss/datasources/MyDS");
        Connection conn = ds.getConnection();
        try {
            return "Using datasource driver: " + conn.getMetaData().getDriverName();
        } finally {
            conn.close();
        }
    }

    @GET
    @Produces("application/json")
    @Transactional
    public Fruit[] get() {
        return em
                .createNamedQuery("Fruits.findAll", Fruit.class)
                .getResultList()
                .toArray(new Fruit[0]);
    }

    @GET
    @Path("/{id}")
    @Produces("application/json")
    @Transactional
    public Fruit getSingle(@PathParam("id") Integer id) {
        return em.find(Fruit.class, id);
    }

    @GET
    @Path("/search/{token}")
    @Produces("application/json")
    @Transactional
    public List search(@PathParam("token") String token) {

        Query query = em.createQuery("Select f from Fruit f where f.name like ?1");
        query.setParameter(1, token);
        return query.getResultList();
    }

    @PUT
    @Consumes("text/plain")
    @Transactional
    public Response create(String name) {
        try {
            em.persist(new Fruit(name));
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).build();
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    @Consumes("text/plain")
    @Transactional
    public Response delete(@PathParam("id") Integer id) {
        try {

            Query query = em.createQuery("Select f from Fruit f where f.id=?1");
            query.setParameter(1, id);
            Object match = query.getSingleResult();
            em.remove(match);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).build();
        }
        return Response.ok().build();
    }
}
