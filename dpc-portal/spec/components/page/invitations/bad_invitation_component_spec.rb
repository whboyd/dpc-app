# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::BadInvitationComponent, type: :component do
  include ComponentSupport

  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    before do
      render_inline(component)
    end

    let(:provider_organization) { create(:provider_organization, name: 'Health Hut') }
    let(:invitation) { create(:invitation, :ao, provider_organization:) }
    context 'invalid invitation' do
      let(:component) { described_class.new(invitation, 'invalid') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{I18n.t('verification.invalid_status')}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
    end

    context 'PII mismatch' do
      let(:component) { described_class.new(invitation, 'pii_mismatch') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{I18n.t('verification.pii_mismatch_status')}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
    end

    context 'Already accepted' do
      let(:component) { described_class.new(invitation, 'accepted') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{I18n.t('verification.accepted_status')}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
    end

    context 'AO expired' do
      let(:status) { :pending }
      let(:invitation) { create(:invitation, :ao, provider_organization:, status:) }
      let(:component) { described_class.new(invitation, 'ao_expired') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{I18n.t('verification.ao_expired_status')}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
      it 'should have renew button' do
        button_url = "/organizations/#{provider_organization.id}/invitations/#{invitation.id}/renew"
        is_expected.to include(button_url)
      end
      context 'already renewed' do
        let(:status) { :renewed }
        let(:component) { described_class.new(invitation, 'ao_renewed') }
        it 'should match header' do
          header = <<~HTML
            <h1>#{I18n.t('verification.ao_renewed_status')}</h1>
          HTML
          is_expected.to include(normalize_space(header))
        end
        it 'should have no renew button' do
          button_url = "/organizations/#{provider_organization.id}/invitations/#{invitation.id}/renew"
          is_expected.not_to include(button_url)
        end
      end
    end
  end
end
