/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.thorntail.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.thorntail.openshift.test.AdditionalResources;
import io.thorntail.openshift.test.OpenShiftTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@OpenShiftTest
@AdditionalResources("classpath:database.yml")
public class OpenshiftIT {
    @BeforeAll
    public static void setUp() {
        RestAssured.basePath = "/api/fruits";
    }

    @BeforeEach
    public void setup() {
        String jsonData =
                when()
                        .get()
                .then()
                        .extract().asString();

        JsonArray array = Json.createReader(new StringReader(jsonData)).readArray();
        array.forEach(val -> {
            when()
                    .delete("/" + ((JsonObject) val).getInt("id"))
            .then()
                    .statusCode(204);
        });
    }

    @Test
    public void retrieveNoFruit() {
        when()
                .get()
        .then()
                .statusCode(200)
                .body(is("[]"));
    }

    @Test
    public void oneFruit() throws Exception {
        createFruit("Peach");

        String payload =
                when()
                        .get()
                .then()
                        .statusCode(200)
                        .extract().asString();

        JsonArray array = Json.createReader(new StringReader(payload)).readArray();

        assertThat(array).hasSize(1);
        assertThat(array.get(0).getValueType()).isEqualTo(JsonValue.ValueType.OBJECT);

        JsonObject obj = (JsonObject) array.get(0);
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);

        given()
                .pathParam("fruitId", obj.getInt("id"))
        .when()
                .get("/{fruitId}")
        .then()
                .statusCode(200)
                .body(containsString("Peach"));
    }

    @Test
    public void createFruit() {
        String payload =
                given()
                        .contentType(ContentType.JSON)
                        .body(convert(Json.createObjectBuilder().add("name", "Raspberry").build()))
                .when()
                        .post()
                .then()
                        .statusCode(201)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);
        assertThat(obj.getString("name")).isNotNull().isEqualTo("Raspberry");
    }

    @Test
    public void createInvalidPayload() {
        given()
                .contentType(ContentType.TEXT)
                .body("")
        .when()
                .post()
        .then()
                .statusCode(415);
    }

    @Test
    public void createIllegalPayload() {
        Fruit badFruit = new Fruit("Carrot");
        badFruit.setId(2);

        String payload =
                given()
                        .contentType(ContentType.JSON)
                        .body(badFruit)
                .when()
                        .post()
                .then()
                        .statusCode(422)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getString("error")).isNotNull();
        assertThat(obj.getInt("code")).isNotNull().isEqualTo(422);
    }

    @Test
    public void update() throws Exception {
        Fruit pear = createFruit("Pear");

        String response =
                given()
                        .pathParam("fruitId", pear.getId())
                .when()
                        .get("/{fruitId}")
                .then()
                        .statusCode(200)
                        .extract().asString();

        pear = new ObjectMapper().readValue(response, Fruit.class);

        pear.setName("Not Pear");

        response =
                given()
                        .pathParam("fruitId", pear.getId())
                        .contentType(ContentType.JSON)
                        .body(new ObjectMapper().writeValueAsString(pear))
                .when()
                        .put("/{fruitId}")
                .then()
                        .statusCode(200)
                        .extract().asString();

        Fruit updatedPear = new ObjectMapper().readValue(response, Fruit.class);

        assertThat(pear.getId()).isEqualTo(updatedPear.getId());
        assertThat(updatedPear.getName()).isEqualTo("Not Pear");
    }

    @Test
    public void updateWithUnknownId() throws Exception {
        Fruit bad = new Fruit("bad");
        bad.setId(12345678);

        given()
                .pathParam("fruitId", bad.getId())
                .contentType(ContentType.JSON)
                .body(new ObjectMapper().writeValueAsString(bad))
        .when()
                .put("/{fruitId}")
        .then()
                .statusCode(404)
                .extract().asString();
    }

    @Test
    public void updateInvalidPayload() {
        given()
                .contentType(ContentType.TEXT)
                .body("")
        .when()
                .post()
        .then()
                .statusCode(415);
    }

    @Test
    public void updateIllegalPayload() throws Exception {
        Fruit carrot = createFruit("Carrot");
        carrot.setName(null);

        String payload =
                given()
                        .pathParam("fruitId", carrot.getId())
                        .contentType(ContentType.JSON)
                        .body(new ObjectMapper().writeValueAsString(carrot))
                .when()
                        .put("/{fruitId}")
                .then()
                        .statusCode(422)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getString("error")).isNotNull();
        assertThat(obj.getInt("code")).isNotNull().isEqualTo(422);
    }

    @Test
    public void testDelete() throws Exception {
        Fruit orange = createFruit("Orange");

        when()
                .delete("/" + orange.getId())
        .then()
                .statusCode(204);

        when()
                .get()
        .then()
                .statusCode(200)
                .body(is("[]"));
    }

    @Test
    public void deleteWithUnknownId() {
        when()
                .delete("/unknown")
        .then()
                .statusCode(404);

        when()
                .get()
        .then()
                .statusCode(200)
                .body(is("[]"));
    }

    private Fruit createFruit(String name) throws Exception {
        String payload =
                given()
                        .contentType(ContentType.JSON)
                        .body(convert(Json.createObjectBuilder().add("name", name).build()))
                .when()
                        .post()
                .then()
                        .statusCode(201)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);

        return new ObjectMapper().readValue(payload, Fruit.class);
    }

    private String convert(JsonObject object) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(stringWriter);
        jsonWriter.writeObject(object);
        jsonWriter.close();
        return stringWriter.toString();
    }
}
