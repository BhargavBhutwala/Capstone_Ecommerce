/** Empty state with an icon and configurable message */
import styles from './EmptyState.module.css'

interface EmptyStateProps {
  message: string
  hint?: string
}

export function EmptyState({ message, hint }: EmptyStateProps) {
  return (
    <div className={styles.wrapper}>
      <p className={styles.message}>{message}</p>
      {hint && <p className={styles.hint}>{hint}</p>}
    </div>
  )
}
