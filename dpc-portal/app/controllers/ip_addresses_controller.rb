# frozen_string_literal: true

# Handles IP address requests
class IpAddressesController < ApplicationController
  before_action :authenticate_user!
  before_action :check_user_verification
  before_action :load_organization
  before_action :require_can_access
  before_action :tos_accepted

  def new
    render Page::IpAddress::NewAddressComponent.new(@organization)
  end

  # rubocop:disable Metrics/AbcSize
  def create
    manager = IpAddressManager.new(@organization.dpc_api_organization_id)
    new_ip_address = manager.create_ip_address(ip_address: params[:ip_address], label: params[:label])
    if new_ip_address[:response]
      log_credential_action(:ip_address, new_ip_address.dig(:message, 'id'), :add)
      flash[:notice] = 'IP address successfully created.'
      redirect_to organization_path(@organization)
    else
      @errors = new_ip_address[:errors] || {}
      flash[:alert] = @errors[:root] || 'IP address invalid'
      render Page::IpAddress::NewAddressComponent.new(@organization, errors: @errors)
    end
  end

  def destroy
    manager = IpAddressManager.new(@organization.dpc_api_organization_id)
    if manager.delete_ip_address(params)
      flash[:notice] = 'IP address successfully deleted.'
      log_credential_action(:ip_address, params[:id], :remove)
    else
      flash[:alert] = manager.errors[:root] || 'IP address could not be deleted.'
    end
    redirect_to organization_path(@organization)
  end
  # rubocop:enable Metrics/AbcSize
end
