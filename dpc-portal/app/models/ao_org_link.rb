# frozen_string_literal: true

# Link between authorized official and provider organization
class AoOrgLink < ApplicationRecord
  validates :user_id,
            uniqueness: { scope: :provider_organization_id, message: 'already exists for this provider.' }
  validates :invitation_id, allow_nil: true,
                            uniqueness: { scope: :invitation_id, message: 'already used by another AoOrgLink.' }
  validates :verification_reason, allow_nil: true, allow_blank: true,
                                  inclusion: { in: :verification_reason }

  enum verification_reason: %i[ao_removal user_med_sanction no_approved_enrollments org_med_sanction]

  belongs_to :user, required: true
  belongs_to :provider_organization, required: true
  belongs_to :invitation, required: false
end
