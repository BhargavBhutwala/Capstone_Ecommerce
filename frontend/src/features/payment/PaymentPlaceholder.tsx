/**
 * PaymentPlaceholder — stub route for /orders/:orderId/payment.
 * Implemented in FE-06.
 */

import { useParams } from 'react-router-dom'

export function PaymentPlaceholder() {
  const { orderId } = useParams<{ orderId: string }>()
  return (
    <div>
      <h2>Payment</h2>
      <p>Order #{orderId} created. Payment is implemented in a later milestone.</p>
    </div>
  )
}
