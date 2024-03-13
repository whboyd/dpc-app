# frozen_string_literal: true

# Link between authorized official and provider organization
class AoOrgLink < ApplicationRecord
  validates :user_id,
            uniqueness: { scope: :provider_organization_id, message: 'already exists for this provider.' }

  belongs_to :user, required: true
  belongs_to :provider_organization, required: true
end
