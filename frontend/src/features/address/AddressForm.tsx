/**
 * AddressForm — reusable create / edit form for a single address.
 *
 * Used by:
 * - AddressesPage (create new address, edit existing address)
 *
 * Validation mirrors AddressRequest constraints from the OpenAPI contract:
 *   label:        optional, maxLength 50
 *   addressLine1: required, maxLength 255
 *   addressLine2: optional, maxLength 255
 *   city:         required, maxLength 100
 *   state:        required, maxLength 100
 *   postalCode:   required, minLength 3, maxLength 20
 *   country:      required, minLength 2, maxLength 100
 *   isDefault:    boolean, default false
 */

import { type FormEvent, useState } from 'react'
import { FormField } from '../../components/forms/FormField'
import { getFieldErrors } from '../../api/errors'
import type { AddressRequest, AddressResponse } from '../../types/api'
import styles from './AddressForm.module.css'

// ─── Props ─────────────────────────────────────────────────────────────────────

interface AddressFormProps {
  /** When provided the form operates in edit mode pre-populated with this address */
  initial?: AddressResponse
  /** Called with the validated AddressRequest when the user submits */
  onSubmit: (data: AddressRequest) => Promise<void>
  /** Called when the user clicks Cancel */
  onCancel: () => void
  submitting: boolean
}

// ─── Client-side validation ────────────────────────────────────────────────────

function validate(fields: AddressRequest): Record<string, string> {
  const errs: Record<string, string> = {}
  if (fields.label && fields.label.length > 50)
    errs.label = 'Label must be at most 50 characters.'
  if (!fields.addressLine1.trim())
    errs.addressLine1 = 'Address line 1 is required.'
  else if (fields.addressLine1.length > 255)
    errs.addressLine1 = 'Address line 1 must be at most 255 characters.'
  if (fields.addressLine2 && fields.addressLine2.length > 255)
    errs.addressLine2 = 'Address line 2 must be at most 255 characters.'
  if (!fields.city.trim())
    errs.city = 'City is required.'
  else if (fields.city.length > 100)
    errs.city = 'City must be at most 100 characters.'
  if (!fields.state.trim())
    errs.state = 'State / region is required.'
  else if (fields.state.length > 100)
    errs.state = 'State must be at most 100 characters.'
  if (!fields.postalCode.trim())
    errs.postalCode = 'Postal code is required.'
  else if (fields.postalCode.trim().length < 3)
    errs.postalCode = 'Postal code must be at least 3 characters.'
  else if (fields.postalCode.length > 20)
    errs.postalCode = 'Postal code must be at most 20 characters.'
  if (!fields.country.trim())
    errs.country = 'Country is required.'
  else if (fields.country.trim().length < 2)
    errs.country = 'Country must be at least 2 characters.'
  else if (fields.country.length > 100)
    errs.country = 'Country must be at most 100 characters.'
  return errs
}

// ─── Component ────────────────────────────────────────────────────────────────

export function AddressForm({ initial, onSubmit, onCancel, submitting }: AddressFormProps) {
  const [label, setLabel] = useState(initial?.label ?? '')
  const [addressLine1, setAddressLine1] = useState(initial?.addressLine1 ?? '')
  const [addressLine2, setAddressLine2] = useState(initial?.addressLine2 ?? '')
  const [city, setCity] = useState(initial?.city ?? '')
  const [state, setState] = useState(initial?.state ?? '')
  const [postalCode, setPostalCode] = useState(initial?.postalCode ?? '')
  const [country, setCountry] = useState(initial?.country ?? '')
  const [isDefault, setIsDefault] = useState(initial?.isDefault ?? false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [globalError, setGlobalError] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setGlobalError(null)

    const data: AddressRequest = {
      label: label.trim() || undefined,
      addressLine1: addressLine1.trim(),
      addressLine2: addressLine2.trim() || undefined,
      city: city.trim(),
      state: state.trim(),
      postalCode: postalCode.trim(),
      country: country.trim(),
      isDefault,
    }

    const clientErrs = validate(data)
    if (Object.keys(clientErrs).length > 0) {
      setFieldErrors(clientErrs)
      return
    }
    setFieldErrors({})

    try {
      await onSubmit(data)
    } catch (err) {
      const serverFieldErrors = getFieldErrors(err)
      if (Object.keys(serverFieldErrors).length > 0) {
        setFieldErrors(serverFieldErrors)
      } else {
        setGlobalError(err instanceof Error ? err.message : 'Failed to save address.')
      }
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className={styles.form}>
      {globalError && (
        <div className={styles.globalError} role="alert">
          {globalError}
        </div>
      )}

      <FormField
        id="addr-label"
        label="Label (optional)"
        type="text"
        placeholder='e.g. "Home", "Work"'
        value={label}
        onChange={(e) => setLabel(e.target.value)}
        maxLength={50}
        error={fieldErrors.label}
        disabled={submitting}
      />

      <FormField
        id="addr-line1"
        label="Address line 1"
        type="text"
        value={addressLine1}
        onChange={(e) => setAddressLine1(e.target.value)}
        maxLength={255}
        error={fieldErrors.addressLine1}
        disabled={submitting}
        autoComplete="address-line1"
      />

      <FormField
        id="addr-line2"
        label="Address line 2 (optional)"
        type="text"
        value={addressLine2}
        onChange={(e) => setAddressLine2(e.target.value)}
        maxLength={255}
        error={fieldErrors.addressLine2}
        disabled={submitting}
        autoComplete="address-line2"
      />

      <div className={styles.row}>
        <FormField
          id="addr-city"
          label="City"
          type="text"
          value={city}
          onChange={(e) => setCity(e.target.value)}
          maxLength={100}
          error={fieldErrors.city}
          disabled={submitting}
          autoComplete="address-level2"
        />
        <FormField
          id="addr-state"
          label="State / Region"
          type="text"
          value={state}
          onChange={(e) => setState(e.target.value)}
          maxLength={100}
          error={fieldErrors.state}
          disabled={submitting}
          autoComplete="address-level1"
        />
      </div>

      <div className={styles.row}>
        <FormField
          id="addr-postal"
          label="Postal code"
          type="text"
          value={postalCode}
          onChange={(e) => setPostalCode(e.target.value)}
          maxLength={20}
          error={fieldErrors.postalCode}
          disabled={submitting}
          autoComplete="postal-code"
        />
        <FormField
          id="addr-country"
          label="Country"
          type="text"
          value={country}
          onChange={(e) => setCountry(e.target.value)}
          maxLength={100}
          error={fieldErrors.country}
          disabled={submitting}
          autoComplete="country-name"
        />
      </div>

      <label className={styles.checkLabel}>
        <input
          type="checkbox"
          checked={isDefault}
          onChange={(e) => setIsDefault(e.target.checked)}
          disabled={submitting}
        />
        Set as default address
      </label>

      <div className={styles.actions}>
        <button type="submit" disabled={submitting} className={styles.submitBtn}>
          {submitting ? 'Saving…' : initial ? 'Update address' : 'Add address'}
        </button>
        <button type="button" onClick={onCancel} disabled={submitting} className={styles.cancelBtn}>
          Cancel
        </button>
      </div>
    </form>
  )
}
