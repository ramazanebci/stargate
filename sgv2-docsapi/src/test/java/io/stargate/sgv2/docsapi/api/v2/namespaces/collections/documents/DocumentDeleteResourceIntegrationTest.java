package io.stargate.sgv2.docsapi.api.v2.namespaces.collections.documents;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.stargate.sgv2.common.cql.builder.Replication;
import io.stargate.sgv2.docsapi.config.constants.Constants;
import io.stargate.sgv2.docsapi.service.schema.NamespaceManager;
import io.stargate.sgv2.docsapi.service.schema.TableManager;
import io.stargate.sgv2.docsapi.testprofiles.IntegrationTestProfile;
import java.time.Duration;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@ActivateRequestContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentDeleteResourceIntegrationTest {

  public static final String BASE_PATH =
      "/v2/namespaces/{namespace}/collections/{collection}/{document-id}";
  public static final String DEFAULT_NAMESPACE = RandomStringUtils.randomAlphanumeric(16);
  public static final String DEFAULT_COLLECTION = RandomStringUtils.randomAlphanumeric(16);
  public static final String DEFAULT_DOCUMENT_ID = RandomStringUtils.randomAlphanumeric(16);
  public static final String DEFAULT_PAYLOAD =
      "{\"test\": \"document\", \"this\": [\"is\", 1, true]}";

  @Inject NamespaceManager namespaceManager;

  @Inject TableManager tableManager;

  @BeforeAll
  public void init() {

    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    namespaceManager
        .createNamespace(DEFAULT_NAMESPACE, Replication.simpleStrategy(1))
        .await()
        .atMost(Duration.ofSeconds(10));

    tableManager
        .createCollectionTable(DEFAULT_NAMESPACE, DEFAULT_COLLECTION)
        .await()
        .atMost(Duration.ofSeconds(10));
  }

  @BeforeEach
  public void setup() {
    given()
        .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
        .header("Content-Type", "application/json")
        .body(DEFAULT_PAYLOAD)
        .when()
        .put(BASE_PATH, DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
        .then()
        .statusCode(200);
  }

  @Nested
  class DeleteDocument {
    @Test
    public void happyPath() {
      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .when()
          .delete(BASE_PATH, DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(204);

      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .param("raw", true)
          .when()
          .get(BASE_PATH, DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(404);
    }

    @Test
    public void unauthorized() {
      given()
          .when()
          .delete(BASE_PATH, DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(401);
    }

    @Test
    public void deleteNotFound() {
      // When a delete occurs on an unknown document, it still returns 204 No Content
      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .when()
          .delete(BASE_PATH, DEFAULT_NAMESPACE, DEFAULT_COLLECTION, "no-id")
          .then()
          .statusCode(204);

      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .param("raw", true)
          .when()
          .get(BASE_PATH, DEFAULT_NAMESPACE, DEFAULT_COLLECTION, "no-id")
          .then()
          .statusCode(404);
    }

    @Test
    public void keyspaceNotExists() {
      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .when()
          .delete(BASE_PATH, "notakeyspace", DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(404)
          .body("code", equalTo(404))
          .body(
              "description", equalTo("Unknown namespace notakeyspace, you must create it first."));
    }

    @Test
    public void tableNotExists() {
      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .when()
          .delete(BASE_PATH, DEFAULT_NAMESPACE, "notatable", DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(404)
          .body("code", equalTo(404))
          .body("description", equalTo("Collection 'notatable' not found."));
    }
  }

  @Nested
  class DeleteDocumentPath {
    @Test
    public void testDeletePath() {
      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .when()
          .delete(BASE_PATH + "/test", DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(204);

      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .param("raw", true)
          .when()
          .get(BASE_PATH + "/test", DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(404);

      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .param("raw", true)
          .when()
          .get(BASE_PATH, DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(200)
          .body("test", nullValue());
    }

    @Test
    public void testDeleteArrayPath() {
      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .when()
          .delete(
              BASE_PATH + "/this/[2]", DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(204);

      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .param("raw", true)
          .when()
          .get(BASE_PATH + "/this", DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(200)
          .body(jsonEquals("[\"is\",1]"));

      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .when()
          .delete(
              BASE_PATH + "/this/[0]", DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(204);

      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .param("raw", true)
          .when()
          .get(BASE_PATH + "/this", DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(200)
          .body(jsonEquals("[null,1]"));

      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .param("raw", true)
          .when()
          .get(BASE_PATH, DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(200)
          .body("this", jsonEquals("[null,1]"));
    }

    @Test
    public void testDeletePathNotFound() {
      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .when()
          .delete(BASE_PATH + "/test/a", DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(204);

      given()
          .header(Constants.AUTHENTICATION_TOKEN_HEADER_NAME, "")
          .param("raw", true)
          .when()
          .get(BASE_PATH + "/test", DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(200)
          .body(jsonEquals("\"document\""));
    }

    @Test
    public void unauthorized() {
      given()
          .when()
          .delete(BASE_PATH + "/test", DEFAULT_NAMESPACE, DEFAULT_COLLECTION, DEFAULT_DOCUMENT_ID)
          .then()
          .statusCode(401);
    }
  }
}