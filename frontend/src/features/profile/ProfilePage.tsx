/**
 * ProfilePage — authenticated user profile with embedded address management.
 *
 * Route: /profile (protected)
 *
 * Displays:
 * - User details from AuthContext (first name, last name, email, role, status,
 *   member since / createdAt formatted in IST)
 * - Saved Addresses section reusing the existing AddressesPage implementation.
 *
 * The profile details are read-only. The OpenAPI contract does not define a
 * PATCH /users/me or PUT /users/me endpoint; no update form is invented here.
 *
 * Address management is fully functional: list, create, edit, delete, and set
 * default — all via the existing address API and AddressesPage component.
 */

import { useAuth } from '../auth/AuthContext'
import { AddressesPage } from '../address/AddressesPage'
import { formatDateTime } from '../../utils/formatDateTime'
import styles from './ProfilePage.module.css'

export function ProfilePage() {
  const { user } = useAuth()

  // Guard: the ProtectedRoute ensures user is always non-null here,
  // but TypeScript cannot infer that from context alone.
  if (!user) return null

  const statusClass =
    user.status === 'ACTIVE'
      ? styles.statusACTIVE
      : user.status === 'LOCKED'
        ? styles.statusLOCKED
        : styles.statusINACTIVE

  return (
    <div className={styles.page}>
      {/* ── Profile details ── */}
      <section className={styles.profileCard} aria-label="Profile details">
        <h1 className={styles.profileHeading}>My Profile</h1>

        <dl className={styles.infoGrid}>
          <dt className={styles.infoLabel}>Name</dt>
          <dd className={styles.infoValue}>
            {user.firstName} {user.lastName}
          </dd>

          <dt className={styles.infoLabel}>Email</dt>
          <dd className={styles.infoValue}>{user.email}</dd>

          <dt className={styles.infoLabel}>Role</dt>
          <dd className={styles.infoValue}>{user.role}</dd>

          <dt className={styles.infoLabel}>Account status</dt>
          <dd className={styles.infoValue}>
            <span className={`${styles.statusBadge} ${statusClass}`}>
              {user.status}
            </span>
          </dd>

          {user.createdAt && (
            <>
              <dt className={styles.infoLabel}>Member since</dt>
              <dd className={styles.infoValue}>
                {formatDateTime(user.createdAt)}
              </dd>
            </>
          )}
        </dl>
      </section>

      {/* ── Saved Addresses ── */}
      <section aria-label="Saved addresses">
        <h2 className={styles.sectionHeading}>Saved Addresses</h2>
        <AddressesPage />
      </section>
    </div>
  )
}
