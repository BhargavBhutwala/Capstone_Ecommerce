/**
 * Application-level providers.
 *
 * Wraps the entire component tree with all React context providers needed
 * across the application. Add new providers here as later milestones
 * introduce server-state, theming, or other global concerns.
 *
 * Ordering matters: providers listed earlier are outer wrappers.
 */

import type { ReactNode } from 'react'
import { AuthProvider } from '../../features/auth/AuthContext'

interface AppProvidersProps {
  children: ReactNode
}

export function AppProviders({ children }: AppProvidersProps) {
  return <AuthProvider>{children}</AuthProvider>
}
