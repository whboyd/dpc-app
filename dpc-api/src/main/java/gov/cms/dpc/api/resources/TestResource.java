package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.annotations.Public;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hl7.fhir.dstu3.model.Patient;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
@FHIR
@Api(value = "test", hidden = true)
public class TestResource {

    @Inject
    public TestResource() {
        // Not used
    }

    @GET
    @Public
    @ApiOperation(value = "test resource")
    public Response base() {
        return Response.status(Response.Status.OK).entity("Hello there!").build();
    }

    @POST
    @Public
    @ApiOperation(value = "validation test resource")
    public Response testValidations(@Valid @Profiled(profile = "https://dpc.cms.gov/fhir/v1/StructureDefinition/dpc-profile-patient") Patient patient) {
        return Response.ok().build();
    }
}
