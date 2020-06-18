# frozen_string_literal: true

module Internal
  class OrganizationsController < ApplicationController
    before_action :authenticate_internal_user!

    def index
      results = BaseSearch.new(params: params, scope: params[:org_type]).results

      @organizations = org_page_params(results)
      render layout: 'table_index'
    end

    def new
      if from_user_params[:from_user].present?
        user = User.find from_user_params[:from_user]
        @organization = Organization.new name: user.requested_organization,
                                         organization_type: user.requested_organization_type,
                                         num_providers: user.requested_num_providers

        @organization.build_address street: user.address_1,
                                    street_2: user.address_2,
                                    city: user.city,
                                    state: user.state,
                                    zip: user.zip
      else
        @organization = Organization.new
        @organization.build_address
      end
    end

    def create
      @organization = Organization.new organization_params

      if @organization.save
        flash[:notice] = 'Organization created.'
        if prod_sbx?
          redirect_to new_internal_organization_registered_organization_path(organization_id: @organization.id,
                                                                             api_env: 'sandbox')
        elsif from_user_params[:from_user].present?
          redirect_to edit_internal_user_path(from_user_params[:from_user], user_organization_ids: @organization.id)
        else
          redirect_to internal_organization_path(@organization)
        end
      else
        flash[:alert] = "Organization could not be created: #{model_error_string(@organization)}"
        render :new
      end
    end

    def show
      @organization = Organization.find org_account_params
    end

    def edit
      @organization = Organization.find org_account_params
    end

    def update
      @organization = Organization.find org_account_params

      if @organization.update organization_params
        flash[:notice] = 'Organization updated.'
        redirect_to internal_organization_path(@organization)
      else
        flash[:alert] = "Organization could not be updated: #{model_error_string(@organization)}"
        render :edit
      end
    end

    def destroy
      @organization = Organization.find org_account_params
      if @organization.destroy
        flash[:notice] = 'Organization deleted.'
        redirect_to internal_organizations_path
      else
        flash[:alert] = "Organization could not be deleted: #{model_error_string(@organization)}"
        redirect_to internal_organization_path(@organization)
      end
    end

    private

    def prod_sbx?
      ENV['ENV'] == 'prod-sbx'
    end

    def keyword_param
      params.permit(:keyword)
    end

    def org_account_params
      params.require(:id)
    end

    def org_page_params(results)
      results.page params[:page]
    end

    def from_user_params
      params.permit(:from_user)
    end

    def organization_params
      params.require(:organization).permit(
        :name, :organization_type, :num_providers, :npi, :vendor,
        address_attributes: %i[id street street_2 city state zip address_use address_type]
      )
    end
  end
end
