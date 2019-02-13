package gov.cms.dpc.web;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.NonFhirResponseException;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration tests for Group resource
 */
public class GroupIntegrationTest extends AbstractApplicationTest {

    /**
     * Test that the group resource correctly accepts/rejects invalid content types.
     */
    @Test
    public void testMediaTypes() {

        // Verify that FHIR is accepted
        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        final IOperationUntypedWithInput<Parameters> execute = client
                .operation()
                .onInstanceVersion(new IdDt("Group", "1"))
                .named("$export")
                .withNoParameters(Parameters.class)
                .useHttpGet();


        assertThrows(NonFhirResponseException.class, execute::execute, "Should throw exception, but accept JSON request");

        // Try again, but use a different encoding
        assertThrows(UnclassifiedServerFailureException.class, () -> execute.encodedXml().execute(), "Should not accept XML encoding");
    }

    @Test
    public void testFHIRMarshaling() {

        final Group group = new Group();
        group.addIdentifier().setValue("Group/test");

        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        MethodOutcome execute = client
                .create()
                .resource(group)
                .encodedJson()
                .preferResponseType(Patient.class)
                .execute();

        final Patient resource = (Patient) execute.getResource();
        assertAll(() -> assertNotNull(resource, "Should have patient resource"),
                () -> assertEquals("test-id", resource.getIdentifierFirstRep().getValue(), "ID's should match"),
                () -> assertEquals("Doe", resource.getNameFirstRep().getFamily(), "Should have updated family name"),
                () -> assertEquals("John", resource.getNameFirstRep().getGivenAsSingleString(), "Should have updated given name"));

        // Try to fail it
        final Group g2 = new Group();

        g2.addIdentifier().setValue("Group/fail");

        execute = client
                .create()
                .resource(g2)
                .encodedJson()
                .preferResponseType(Patient.class)
                .execute();

        final IBaseOperationOutcome outcome = execute.getOperationOutcome();
    }
}
