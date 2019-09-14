package gov.cms.dpc.fhir.dropwizard.handlers;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import javax.inject.Inject;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Core error handler for differentiating between FHIR and standard HTTP errors.
 * This overrides *all* of Dropwizard's error handling, but for any non-FHIR resources, we simply delegate back to the root {@link LoggingExceptionMapper}
 */
@Provider
public class FHIRExceptionHandler extends LoggingExceptionMapper<Throwable> {

    @Context
    private ResourceInfo info;

    @Inject
    public FHIRExceptionHandler() {
        super();
    }

    @Override
    public Response toResponse(Throwable exception) {
        final Response response = super.toResponse(exception);

        // If it's a FHIR resource, create a custom operation outcome, when it's an error
        if (isFHIRResource()) {
            final OperationOutcome outcome;
            final int status;
            // If the error is a HAPI error, we can pull out the existing operation outcome and send forward that, along with the existing status code
            if (BaseServerResponseException.class.isAssignableFrom(exception.getClass())) {
                final BaseServerResponseException fhirException = (BaseServerResponseException) exception;
                status = fhirException.getStatusCode();
                outcome = (OperationOutcome) fhirException.getOperationOutcome();
            } else {
                status = response.getStatus();
                outcome = new OperationOutcome();
                outcome.addIssue()
                        .setSeverity(OperationOutcome.IssueSeverity.FATAL)
                        .setDetails(new CodeableConcept().setText(exception.getMessage()));
            }

            return Response.fromResponse(response)
                    .status(status)
                    .type(FHIRMediaTypes.FHIR_JSON)
                    .entity(outcome)
                    .build();
        }

        return response;
    }

    private boolean isFHIRResource() {
        return (this.info.getResourceClass() != null && this.info.getResourceClass().getAnnotation(FHIR.class) != null) ||
                (this.info.getResourceMethod() != null && this.info.getResourceMethod().getAnnotation(FHIR.class) != null);
    }
}
