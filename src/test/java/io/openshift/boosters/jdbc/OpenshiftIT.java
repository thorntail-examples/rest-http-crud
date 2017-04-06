package io.openshift.boosters.jdbc;

import java.io.File;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Heiko Braun
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OpenshiftIT {

    private static final String APPLICATION_NAME = System.getProperty("app.name");

    private static final OpenshiftTestAssistant openshift = new OpenshiftTestAssistant(APPLICATION_NAME);

    @BeforeClass
    public static void setup() throws Exception {

        Assert.assertNotNull(APPLICATION_NAME);

        // Deploy the database and wait until it's ready.
        openshift.deploy("database", new File("src/test/resources/database.yml"));
        openshift.awaitPodReadinessOrFail(
                pod -> "my-database".equals(pod.getMetadata().getLabels().get("app"))
        );

        System.out.println("Database ready");


        // the application itself
        openshift.deployApplication();

        // wait until the pods & routes become available
        openshift.awaitApplicationReadinessOrFail();
    }

    @AfterClass
    public static void teardown() throws Exception {
       openshift.cleanup();
    }

    @Test
    public void testServiceInvocation() {

        given().
            log().all().
        expect().
            statusCode(200).
            body(containsString("Cherry")).
        when().
            get(openshift.getBaseUrl()+"/api/fruits/1");
    }
}

