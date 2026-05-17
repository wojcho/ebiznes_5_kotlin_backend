printf '\n==== Create a user\n'
curl -v -s -X POST http://localhost:8080/users -H "Content-Type: application/json" -d '{"name":"Alice","email":"alice@example.com"}'
printf '\n==== List users\n'
curl -v -s http://localhost:8080/users
printf '\n==== Get user (id 1)\n'
curl -v -s http://localhost:8080/users/1
printf '\n==== Create a product\n'
curl -v -s -X POST http://localhost:8080/products -H "Content-Type: application/json" -d '{"name":"Hat","description":"Wool hat","priceCents":1500,"inStock":5}'
printf '\n==== List products\n'
curl -v -s http://localhost:8080/products
printf '\n==== Get product (id 1)\n'
curl -v -s http://localhost:8080/products/1
printf '\n==== Update product (id 1)\n'
curl -v -s -X PUT http://localhost:8080/products/1 -H "Content-Type: application/json" -d '{"priceCents":1299,"inStock":20}'
printf '\n==== Delete product (id 2)\n'
curl -v -s -X DELETE http://localhost:8080/products/2
printf '\n==== Get basket of user (userId 1)\n'
curl -v -s http://localhost:8080/users/1/basket
printf '\n==== Add item to basket (userId 1)\n'
curl -v -s -X POST http://localhost:8080/users/1/basket -H "Content-Type: application/json" -d '{"productId":1,"quantity":2}'
printf '\n==== Remove item from basket (userId 1, remove productId 1 with amount 1)\n'
curl -v -s -X DELETE http://localhost:8080/users/1/basket -H "Content-Type: application/json" -d '{"productId":1,"quantity":1}'
printf '\n==== Checkout basket (userId 1)\n'
curl -v -s -X POST http://localhost:8080/users/1/basket/checkout -H "Content-Type: application/json" -d '{"userId":1}'
