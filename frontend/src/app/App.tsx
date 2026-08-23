/**
 * Application root.
 *
 * Wraps the RouterProvider in the application-level providers so all
 * context (auth, server state in later milestones) is available to routes.
 */

import { RouterProvider } from 'react-router-dom'
import { AppProviders } from './providers/AppProviders'
import { router } from '../routes'

function App() {
  return (
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>
  )
}

export default App
