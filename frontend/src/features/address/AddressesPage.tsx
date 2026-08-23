/**
 * AddressesPage — address management (list, create, edit, delete).
 *
 * operationId: listAddresses  → GET    /addresses
 * operationId: createAddress  → POST   /addresses
 * operationId: updateAddress  → PUT    /addresses/{addressId}
 * operationId: deleteAddress  → DELETE /addresses/{addressId}
 *
 * Behaviour:
 * - Loads all saved addresses on mount.
 * - Inline "Add address" panel opens the AddressForm in create mode.
 * - Each address card has Edit and Delete buttons.
 * - Editing opens the form in edit mode; Cancel collapses it.
 * - Delete confirms then calls the API; the list refetches.
 * - Only one form is open at a time (create OR edit a specific address).
 * - isDefault is shown visually and settable via the form.
 * - 400 / other API errors surface in the form's own error handling.
 */

import { useState } from 'react'
import * as addressApi from '../../api/addressApi'
import { ApiError } from '../../api/client'
import { useAsync } from '../../hooks/useAsync'
import { LoadingSpinner } from '../../components/states/LoadingSpinner'
import { ErrorState } from '../../components/states/ErrorState'
import { EmptyState } from '../../components/states/EmptyState'
import { AddressForm } from './AddressForm'
import type { AddressRequest } from '../../types/api'
import styles from './AddressesPage.module.css'

// Sentinel value for the "create new" panel
const CREATE_KEY = '__create__'

export function AddressesPage() {
  // Which form is open: null = none, CREATE_KEY = new, number = addressId being edited
  const [openForm, setOpenForm] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  // Per-address delete error (keyed by addressId)
  const [deleteErrors, setDeleteErrors] = useState<Record<number, string>>({})

  const { data: addresses, loading, error, reload } = useAsync(
    () => addressApi.listAddresses(),
    [],
  )

  // ── Create ──────────────────────────────────────────────────────────────────

  async function handleCreate(data: AddressRequest) {
    setSubmitting(true)
    try {
      await addressApi.createAddress(data)
      setOpenForm(null)
      reload()
    } finally {
      setSubmitting(false)
    }
  }

  // ── Update ──────────────────────────────────────────────────────────────────

  async function handleUpdate(addressId: number, data: AddressRequest) {
    setSubmitting(true)
    try {
      await addressApi.updateAddress(addressId, data)
      setOpenForm(null)
      reload()
    } finally {
      setSubmitting(false)
    }
  }

  // ── Delete ──────────────────────────────────────────────────────────────────

  async function handleDelete(addressId: number) {
    setDeleteErrors((prev) => {
      const next = { ...prev }
      delete next[addressId]
      return next
    })
    try {
      await addressApi.deleteAddress(addressId)
      reload()
    } catch (err) {
      let msg = 'Failed to delete address.'
      if (err instanceof ApiError) msg = err.message
      else if (err instanceof Error) msg = err.message
      setDeleteErrors((prev) => ({ ...prev, [addressId]: msg }))
    }
  }

  // ── Render ──────────────────────────────────────────────────────────────────

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1 className={styles.heading}>My Addresses</h1>
        {openForm !== CREATE_KEY && (
          <button
            className={styles.addBtn}
            onClick={() => setOpenForm(CREATE_KEY)}
          >
            + Add address
          </button>
        )}
      </div>

      {/* ── Create form ── */}
      {openForm === CREATE_KEY && (
        <div className={styles.formPanel}>
          <h2 className={styles.formTitle}>New address</h2>
          <AddressForm
            key="create"
            onSubmit={handleCreate}
            onCancel={() => setOpenForm(null)}
            submitting={submitting}
          />
        </div>
      )}

      {/* ── Loading / error / empty ── */}
      {loading && <LoadingSpinner label="Loading addresses…" />}
      {!loading && error && (
        <ErrorState message={error} onRetry={reload} />
      )}
      {!loading && !error && addresses?.length === 0 && (
        <EmptyState
          message="No saved addresses yet."
          hint="Add an address to use at checkout."
        />
      )}

      {/* ── Address list ── */}
      {!loading && !error && addresses && addresses.length > 0 && (
        <ul className={styles.list}>
          {addresses.map((addr) => (
            <li key={addr.id} className={styles.card}>
              {/* ── Address details ── */}
              <div className={styles.cardBody}>
                <div className={styles.cardTop}>
                  {addr.label && (
                    <span className={styles.label}>{addr.label}</span>
                  )}
                  {addr.isDefault && (
                    <span className={styles.defaultBadge}>Default</span>
                  )}
                </div>
                <p className={styles.line}>{addr.addressLine1}</p>
                {addr.addressLine2 && (
                  <p className={styles.line}>{addr.addressLine2}</p>
                )}
                <p className={styles.line}>
                  {addr.city}, {addr.state} {addr.postalCode}
                </p>
                <p className={styles.line}>{addr.country}</p>

                {deleteErrors[addr.id] && (
                  <p className={styles.deleteError} role="alert">
                    {deleteErrors[addr.id]}
                  </p>
                )}
              </div>

              {/* ── Card actions ── */}
              <div className={styles.cardActions}>
                <button
                  className={styles.editBtn}
                  onClick={() =>
                    setOpenForm(openForm === String(addr.id) ? null : String(addr.id))
                  }
                >
                  {openForm === String(addr.id) ? 'Cancel edit' : 'Edit'}
                </button>
                <button
                  className={styles.deleteBtn}
                  onClick={() => handleDelete(addr.id)}
                >
                  Delete
                </button>
              </div>

              {/* ── Inline edit form ── */}
              {openForm === String(addr.id) && (
                <div className={styles.editPanel}>
                  <AddressForm
                    key={`edit-${addr.id}`}
                    initial={addr}
                    onSubmit={(data) => handleUpdate(addr.id, data)}
                    onCancel={() => setOpenForm(null)}
                    submitting={submitting}
                  />
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
