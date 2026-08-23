-- V11: additional query-path indexes
--
-- Only indexes NOT already implied by an existing UNIQUE constraint are created here.
-- Covered by UNIQUE (no index needed):
--   users.email, products.isbn, categories.name, brands.name,
--   carts.user_id, cart_items(cart_id, product_id),
--   orders.order_number, payments.order_id, payments.payment_reference

-- Product search / catalogue browsing
CREATE INDEX idx_products_title       ON products (title);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_brand_id    ON products (brand_id);

-- Order history and status filtering
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status  ON orders (status);

-- Cart and order detail retrieval
CREATE INDEX idx_cart_items_cart_id    ON cart_items (cart_id);
CREATE INDEX idx_order_items_order_id  ON order_items (order_id);
