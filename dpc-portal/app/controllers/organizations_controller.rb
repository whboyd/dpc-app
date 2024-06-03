# frozen_string_literal: true

# Shows Credential Delegates info about the organizations they manage the credentials for
class OrganizationsController < ApplicationController
  before_action :authenticate_user!
  before_action :load_organization, only: %i[show tos_form sign_tos success]
  before_action :require_can_access, only: %i[show]
  before_action :check_npi, only: %i[create]
  before_action :require_ao, only: %i[tos_form sign_tos success]
  before_action :tos_accepted, only: %i[show]

  def index
    @organizations = current_user.provider_organizations
    render(Page::Organization::OrganizationListComponent.new(organizations: @organizations))
  end

  def show
    show_cds = current_user.ao?(@organization)
    if show_cds
      @invitations = Invitation.where(provider_organization: @organization,
                                      invited_by: current_user,
                                      status: :pending)
      @cds = CdOrgLink.where(provider_organization: @organization, disabled_at: nil)
    end
    render(Page::Organization::CompoundShowComponent.new(@organization, @cds, @invitations, show_cds))
  end

  def new
    render(Page::Organization::NewOrganizationComponent.new)
  end

  def create
    @organization = ProviderOrganization.find_or_create_by(npi: params[:npi]) do |org|
      org.name = CpiApiGatewayClient.new.org_info(params[:npi]).dig('provider', 'orgName')
    end

    @ao_org_link = AoOrgLink.find_or_create_by(user: current_user, provider_organization: @organization)

    create_response
  end

  def tos_form
    render(Page::Organization::TosFormComponent.new(@organization))
  end

  def sign_tos
    @organization.terms_of_service_accepted_at = DateTime.now
    @organization.terms_of_service_accepted_by = current_user
    @organization.save!
    redirect_to organization_path(@organization)
  end

  def success
    render(Page::Organization::NewOrganizationSuccessComponent.new(@organization))
  end

  def check_npi
    @npi_error = if params[:npi].blank?
                   "can't be blank"
                 elsif params[:npi].length != 10
                   'length has to be 10'
                 end

    render(Page::Organization::NewOrganizationComponent.new(@npi_error), status: :bad_request) if @npi_error.present?
  end

  private

  def organization_id
    params[:id]
  end

  def create_response
    if @ao_org_link.errors.present?
      log_link_error
      flash[:alert] = 'System Error: unable to create link'
      redirect_to organizations_path
    elsif @organization.terms_of_service_accepted_at.present?
      redirect_to success_organization_path(@organization)
    else
      redirect_to tos_form_organization_path(@organization)
    end
  end

  def log_link_error
    errors = @ao_org_link.errors.messages.map { |k, v| "#{k}: #{v.join(',')}" }.join(' | ')
    logger.error("Unable to create AoOrgLink: #{errors}")
  end
end
