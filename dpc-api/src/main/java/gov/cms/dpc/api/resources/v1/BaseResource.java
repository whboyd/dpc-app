package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.core.Capabilities;
import gov.cms.dpc.api.resources.*;
import gov.cms.dpc.common.annotations.ServiceBaseURL;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Path;


@Path("/v1")
public class BaseResource extends AbstractBaseResource {

    private final AbstractGroupResource gr;
    private final AbstractJobResource jr;
    private final AbstractDataResource dr;
    private final AbstractRosterResource rr;
    private final AbstractOrganizationResource or;

    @Inject
    public BaseResource(GroupResource gr,
                        JobResource jr,
                        DataResource dr,
                        RosterResource rr,
                        OrganizationResource or) {
        this.gr = gr;
        this.jr = jr;
        this.dr = dr;
        this.rr = rr;
        this.or = or;
    }

    @Override
    public String version() {
        return "Version 1";
    }

    @Override
    public CapabilityStatement metadata() {
        return Capabilities.buildCapabilities();
    }

    @Override
    public AbstractGroupResource groupOperations() {
        return this.gr;
    }

    @Override
    public AbstractJobResource jobOperations() {
        return this.jr;
    }

    @Override
    public AbstractDataResource dataOperations() {
        return this.dr;
    }

    @Override
    public AbstractRosterResource rosterOperations() {
        return this.rr;
    }

    @Override
    public AbstractOrganizationResource organizationOperations() {
        return this.or;
    }
}
