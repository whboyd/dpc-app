# frozen_string_literal: true

# Base user class
class User < ApplicationRecord
  # Include default devise modules. Others available are:
  # :confirmable, :lockable, and :trackable
  devise :database_authenticatable, :registerable,
         :recoverable, :rememberable, :validatable,
         :timeoutable, :omniauthable, omniauth_providers: [:openid_connect]

  validates :verification_reason, allow_nil: true, allow_blank: true,
                                  inclusion: { in: :verification_reason }
  validates :verification_status, allow_nil: true,
                                  inclusion: { in: :verification_status }

  enum :verification_reason, %i[user_med_sanction_waived user_med_sanction]
  enum :verification_status, %i[approved rejected]

  # Autogenerated by Devise
  # Serves as template when we are ready to use it.
  def self.create_from_provider_data(provider_data)
    where(provider: provider_data.provider, uid: provider_data.uid).first_or_create do |user|
      user.email = provider_data.info.email
      user.password = Devise.friendly_token[0, 20]
    end
  end

  def provider_organizations
    ProviderOrganization.joins(:ao_org_links).where('ao_org_links.user_id = ?', id) +
      ProviderOrganization.joins(:cd_org_links).where('cd_org_links.user_id = ? AND cd_org_links.disabled_at IS NULL',
                                                      id)
  end

  def can_access?(organization)
    cd?(organization) || ao?(organization)
  end

  def ao?(organization)
    AoOrgLink.where(user: self, provider_organization: organization).exists?
  end

  def cd?(organization)
    CdOrgLink.where(user: self, provider_organization: organization, disabled_at: nil).exists?
  end
end
