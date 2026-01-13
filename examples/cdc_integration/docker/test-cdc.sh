#!/bin/bash
#
# Test CDC by making changes in PostgreSQL
#

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║               Testing CDC with PostgreSQL Changes              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Function to run SQL
run_sql() {
    docker exec -i postgres psql -U postgres inventory -c "$1"
}

echo "═══════════════════════════════════════════════════════════════════"
echo "Test 1: INSERT - Adding a new customer"
echo "═══════════════════════════════════════════════════════════════════"
run_sql "INSERT INTO inventory.customers (first_name, last_name, email, city) VALUES ('CDC', 'Test', 'cdc.test@example.com', 'CDC City');"
echo ""
sleep 2

echo "═══════════════════════════════════════════════════════════════════"
echo "Test 2: UPDATE - Modifying the customer"
echo "═══════════════════════════════════════════════════════════════════"
run_sql "UPDATE inventory.customers SET city = 'Updated City' WHERE email = 'cdc.test@example.com';"
echo ""
sleep 2

echo "═══════════════════════════════════════════════════════════════════"
echo "Test 3: UPDATE - Changing product quantity"
echo "═══════════════════════════════════════════════════════════════════"
run_sql "UPDATE inventory.products SET quantity = quantity + 100 WHERE id = 1;"
echo ""
sleep 2

echo "═══════════════════════════════════════════════════════════════════"
echo "Test 4: INSERT - Creating a new order"
echo "═══════════════════════════════════════════════════════════════════"
run_sql "INSERT INTO inventory.orders (customer_id, status, total_amount, shipping_address) VALUES (1, 'NEW', 99.99, 'CDC Test Address');"
echo ""
sleep 2

echo "═══════════════════════════════════════════════════════════════════"
echo "Test 5: DELETE - Removing the test customer"
echo "═══════════════════════════════════════════════════════════════════"
run_sql "DELETE FROM inventory.customers WHERE email = 'cdc.test@example.com';"
echo ""

echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "CDC Tests Complete!"
echo "Check the Java console to see the CDC events processed."
echo "═══════════════════════════════════════════════════════════════════"
