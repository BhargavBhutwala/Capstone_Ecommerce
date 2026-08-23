/**
 * useAsync — lightweight hook for data fetching without a server-state library.
 *
 * Manages loading / data / error state for a single async operation.
 * Re-runs the fetcher when the `deps` array changes (like useEffect).
 */

import { useCallback, useEffect, useRef, useState } from 'react'

export interface AsyncState<T> {
  data: T | null
  loading: boolean
  error: string | null
  /** Re-run the fetch immediately */
  reload: () => void
}

export function useAsync<T>(
  fetcher: () => Promise<T>,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  deps: any[],
): AsyncState<T> {
  const [state, setState] = useState<{
    data: T | null
    loading: boolean
    error: string | null
  }>({ data: null, loading: true, error: null })

  const [tick, setTick] = useState(0)

  const mounted = useRef(true)
  useEffect(() => {
    mounted.current = true
    return () => { mounted.current = false }
  }, [])

  // Keep a stable ref to the latest fetcher — updated in an effect, not during render
  const fetcherRef = useRef(fetcher)
  useEffect(() => {
    fetcherRef.current = fetcher
  })

  useEffect(() => {
    let cancelled = false

    // Set loading asynchronously to avoid synchronous setState-in-effect
    Promise.resolve().then(() => {
      if (!cancelled && mounted.current) {
        setState((prev) => ({ ...prev, loading: true, error: null }))
      }
    })

    fetcherRef.current()
      .then((result) => {
        if (!cancelled && mounted.current) {
          setState({ data: result, loading: false, error: null })
        }
      })
      .catch((err: unknown) => {
        if (!cancelled && mounted.current) {
          setState({
            data: null,
            loading: false,
            error: err instanceof Error ? err.message : 'An error occurred.',
          })
        }
      })

    return () => { cancelled = true }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick])

  const reload = useCallback(() => setTick((t) => t + 1), [])

  return { ...state, reload }
}
