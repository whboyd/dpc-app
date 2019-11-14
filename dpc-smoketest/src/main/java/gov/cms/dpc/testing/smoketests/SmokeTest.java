package gov.cms.dpc.testing.smoketests;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.hl7.fhir.dstu3.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SmokeTest extends AbstractJavaSamplerClient {

    private static final Logger logger = LoggerFactory.getLogger(SmokeTest.class);
    private static final String KEY_ID = "smoke-test-key";

    private FhirContext ctx;

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = new Arguments();
        arguments.addArgument("host", "http://localhost:3002/v1");
        arguments.addArgument("admin-url", "http://localhost:3002/tasks");
        arguments.addArgument("attribution-url", "http://localhost:3500/v1");
        arguments.addArgument("seed-file", "src/main/resources/test_associations.csv");
        arguments.addArgument("provider-bundle", "provider_bundle.json");
        arguments.addArgument("patient-bundle", "patient_bundle.json");

        return arguments;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
    }

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        // Create things
        final String organizationID = UUID.randomUUID().toString();
        final String hostParam = javaSamplerContext.getParameter("host");
        final String adminURL = javaSamplerContext.getParameter("admin-url");
        logger.info("Running against {}", hostParam);
        logger.info("Admin URL: {}", adminURL);
        logger.info("Running with {} threads", JMeterContextService.getNumberOfThreads());

        logger.info("Creating organization {}", organizationID);
        // Disable validation against Attribution service
        this.ctx = FhirContext.forDstu3();
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        ctx.getRestfulClientFactory().setConnectTimeout(1800);

        final String goldenMacaroon;
        try {
            goldenMacaroon = APIAuthHelpers.createGoldenMacaroon(adminURL);
        } catch (Exception e) {
            throw new RuntimeException("Failed creating Macaroon", e);
        }
        // Create admin client for registering organization
        final IGenericClient adminClient = APIAuthHelpers.buildAdminClient(ctx, hostParam, goldenMacaroon, true);

        final SampleResult smokeTestResult = new SampleResult();
        smokeTestResult.sampleStart();

        final SampleResult orgRegistrationResult = new SampleResult();
        smokeTestResult.addSubResult(orgRegistrationResult);

        String token;
        orgRegistrationResult.sampleStart();
        try {
            token = FHIRHelpers.registerOrganization(adminClient, ctx.newJsonParser(), organizationID, adminURL);
            orgRegistrationResult.setSuccessful(true);
        } catch (Exception e) {
            orgRegistrationResult.setSuccessful(false);
            throw new RuntimeException("Cannot register org", e);
        } finally {
            orgRegistrationResult.sampleEnd();
        }

        // Create a new public key
        final Pair<UUID, PrivateKey> keyTuple;
        try {
            keyTuple = APIAuthHelpers.generateAndUploadKey(KEY_ID, organizationID, goldenMacaroon, hostParam);
        } catch (IOException | NoSuchAlgorithmException | URISyntaxException e) {
            throw new RuntimeException("Failed uploading public key", e);
        }

        // Create an authenticated and async client (the async part is ignored by other endpoints)
        final IGenericClient exportClient;
        try {
            exportClient = APIAuthHelpers.buildAuthenticatedClient(ctx, String.format("%s/", hostParam), token, keyTuple.getLeft(), keyTuple.getRight(), true);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Cannot create export client", e);
        }

        // Upload a batch of patients and a batch of providers
        logger.debug("Submitting practitioners");
        final SampleResult practitionerSample = new SampleResult();
        practitionerSample.sampleStart();
        final List<String> providerNPIs = ClientUtils.submitPractitioners(javaSamplerContext.getParameter("provider-bundle"), this.getClass(), ctx, exportClient);
        practitionerSample.sampleEnd();
        practitionerSample.setSuccessful(true);
        smokeTestResult.addSubResult(practitionerSample);

        logger.debug("Submitting patients");
        final SampleResult patientSample = new SampleResult();

        patientSample.sampleStart();
        final Map<String, Reference> patientReferences = ClientUtils.submitPatients(javaSamplerContext.getParameter("patient-bundle"), this.getClass(), ctx, exportClient);
        patientSample.setSuccessful(true);
        patientSample.sampleEnd();
        smokeTestResult.addSubResult(patientSample);

        // Upload the roster bundle
        logger.debug("Uploading roster");
        try {
            ClientUtils.createAndUploadRosters(javaSamplerContext.getParameter("seed-file"), exportClient, UUID.fromString(organizationID), patientReferences);
        } catch (Exception e) {
            throw new RuntimeException("Cannot upload roster", e);
        }

        // Run the job
        // We need the fully authed access_token, which we'll need to manually pull from the Macaroons Interceptor
        // Gross, but the other options are even worse
        final List<Object> interceptors = exportClient.getInterceptorService().getAllRegisteredInterceptors();

        final APIAuthHelpers.MacaroonsInterceptor mInterceptor = interceptors
                .stream()
                .filter(interceptor -> interceptor instanceof APIAuthHelpers.MacaroonsInterceptor)
                .map(APIAuthHelpers.MacaroonsInterceptor.class::cast)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot get interceptor"));
        ClientUtils.handleExportJob(exportClient, providerNPIs, mInterceptor.getMacaroon());
        smokeTestResult.setSuccessful(true);

        logger.info("Test completed");
        return smokeTestResult;
    }
}
