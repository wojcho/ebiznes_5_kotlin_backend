package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.http.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: Int,
    val name: String,
    val description: String = "",
    val priceCents: Int = 0,
    val inStock: Int = 0
)

@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String? = null,
    val priceCents: Int,
    val inStock: Int = 0
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val priceCents: Int? = null,
    val inStock: Int? = null
)

@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String
)

@Serializable
data class BasketItem(
    val productId: Int,
    val quantity: Int
)

@Serializable
data class PaymentRequest(
    val userId: Int,
    val paymentMethod: String = "card" // mocked, not used
)

@Serializable
data class PaymentResponse(
    val success: Boolean,
    val message: String,
    val totalCents: Int = 0
)

fun Application.configureRouting() {

    // In-memory data stores, mocking database
    val productIdSeq = AtomicInteger(1)
    val userIdSeq = AtomicInteger(1)

    val products = ConcurrentHashMap<Int, Product>()
    val users = ConcurrentHashMap<Int, User>()

    // These are user baskets: userId -> map(productId -> quantity)
    val baskets = ConcurrentHashMap<Int, ConcurrentHashMap<Int, Int>>()

    // Example data
    fun seed() {
        val p1 = Product(productIdSeq.getAndIncrement(), "T-shirt", "Comfortable cotton, with a pattern", 1999, 10)
        val p2 = Product(productIdSeq.getAndIncrement(), "Mug", "Ceramic mug, with an image", 999, 30)
        products[p1.id] = p1
        products[p2.id] = p2
        val u1 = User(userIdSeq.getAndIncrement(), "Alice", "alice@example.com")
        val u2 = User(userIdSeq.getAndIncrement(), "Bob", "bob@example.com")
        users[u1.id] = u1
        users[u2.id] = u2
        baskets[u1.id] = ConcurrentHashMap()
        baskets[u2.id] = ConcurrentHashMap()
        baskets[u1.id]!![p1.id] = 2 // Alice has in basket 2 T-shirts
        baskets[u1.id]!![p2.id] = 1 // and 1 Mug
        baskets[u2.id]!![p2.id] = 3 // Bob has 3 Mugs
    }
    seed()

    routing {
        // Products CRUD
        route("/products") {
            get {
                call.respond(products.values.toList())
            }
            post {
                val req = call.receive<CreateProductRequest>()
                if (req.name.isBlank() || req.priceCents < 0) {
                    call.respond(HttpStatusCode.BadRequest, "Wrong product data")
                    return@post
                }
                val id = productIdSeq.getAndIncrement()
                val p = Product(
                    id = id,
                    name = req.name,
                    description = req.description ?: "",
                    priceCents = req.priceCents,
                    inStock = req.inStock
                )
                products[id] = p
                call.respond(HttpStatusCode.Created, p)
            }
            route("/{id}") {
                get {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val p = products[id]
                    if (p == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }
                    call.respond(p)
                }
                put {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest); return@put
                    }
                    val existing = products[id]
                    if (existing == null) {
                        call.respond(HttpStatusCode.NotFound); return@put
                    }
                    val req = call.receive<UpdateProductRequest>()
                    val updated = existing.copy(
                        name = req.name ?: existing.name,
                        description = req.description ?: existing.description,
                        priceCents = req.priceCents ?: existing.priceCents,
                        inStock = req.inStock ?: existing.inStock
                    )
                    products[id] = updated
                    call.respond(updated)
                }
                delete {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest); return@delete
                    }
                    val removed = products.remove(id)
                    if (removed == null) {
                        call.respond(HttpStatusCode.NotFound); return@delete
                    }
                    // Also remove from baskets
                    baskets.forEach { (_, map) -> map.remove(id) }
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        // Users
        route("/users") {
            post {
                val req = call.receive<CreateUserRequest>()
                if (req.name.isBlank() || req.email.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Wrong user data")
                    return@post
                }
                val id = userIdSeq.getAndIncrement()
                val u = User(id = id, name = req.name, email = req.email)
                users[id] = u
                baskets[id] = ConcurrentHashMap()
                call.respond(HttpStatusCode.Created, u)
            }
            get {
                call.respond(users.values.toList())
            }
            route("/{id}") {
                get {
                    val id = call.parameters["id"]?.toIntOrNull()
                    val u = id?.let { users[it] }
                    if (u == null) {
                        call.respond(HttpStatusCode.NotFound); return@get
                    }
                    call.respond(u)
                }
                delete {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) { call.respond(HttpStatusCode.BadRequest); return@delete }
                    val removed = users.remove(id)
                    baskets.remove(id)
                    if (removed == null) {
                        call.respond(HttpStatusCode.NotFound); return@delete
                    }
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        // Basket endpoints
        route("/users/{userId}/basket") {
            get {
                val userId = call.parameters["userId"]?.toIntOrNull()
                if (userId == null || users[userId] == null) {
                    call.respond(HttpStatusCode.NotFound, "User not found"); return@get
                }
                val map = baskets[userId] ?: ConcurrentHashMap()
                val items = map.entries.mapNotNull { (productId, qty) ->
                    products[productId]?.let { product ->
                        BasketItem(productId, qty)
                    }
                }
                call.respond(items)
            }

            post { // Adds quantity, not idempotent
                val userId = call.parameters["userId"]?.toIntOrNull()
                if (userId == null || users[userId] == null) {
                    call.respond(HttpStatusCode.NotFound, "User not found"); return@post
                }
                val item = call.receive<BasketItem>()
                val product = products[item.productId]
                if (product == null) {
                    call.respond(HttpStatusCode.BadRequest, "Product not found"); return@post
                }
                if (item.quantity <= 0) {
                    call.respond(HttpStatusCode.BadRequest, "Quantity must be > 0"); return@post
                }
                if (product.inStock < item.quantity) {
                    call.respond(HttpStatusCode.BadRequest, "Not enough stock"); return@post
                }
                val userBasket = baskets.computeIfAbsent(userId) { ConcurrentHashMap() }
                userBasket.merge(item.productId, item.quantity) { a, b -> a + b }
                call.respond(HttpStatusCode.OK, userBasket.entries.map { BasketItem(it.key, it.value) })
            }

            delete {
                val userId = call.parameters["userId"]?.toIntOrNull()
                if (userId == null || users[userId] == null) {
                    call.respond(HttpStatusCode.NotFound, "User not found"); return@delete
                }
                val item = call.receive<BasketItem>()
                if (item.quantity <= 0) {
                    call.respond(HttpStatusCode.BadRequest, "Quantity must be > 0"); return@delete
                }
                val userBasket = baskets[userId] ?: ConcurrentHashMap()

                val currentQty = userBasket[item.productId]
                if (currentQty == null) {
                    call.respond(HttpStatusCode.NotFound, "Item not in basket"); return@delete
                }

                val newQty = currentQty - item.quantity
                when {
                    newQty > 0 -> {
                        userBasket[item.productId] = newQty
                        call.respond(HttpStatusCode.OK, BasketItem(item.productId, newQty))
                    }
                    newQty <= 0 -> {
                        userBasket.remove(item.productId)
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }

            post("/checkout") {
                val userId = call.parameters["userId"]?.toIntOrNull()
                if (userId == null || users[userId] == null) {
                    call.respond(HttpStatusCode.NotFound, "User not found"); return@post
                }
                val req = call.receive<PaymentRequest>()
                // Compute total
                val userBasket = baskets[userId] ?: ConcurrentHashMap()
                if (userBasket.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, PaymentResponse(false, "Basket empty", 0)); return@post
                }
                var total = 0
                val insufficient = mutableListOf<Int>()
                userBasket.forEach { (productId, qty) ->
                    val p = products[productId]
                    if (p == null || p.inStock < qty) {
                        insufficient.add(productId)
                    } else {
                        total += p.priceCents * qty
                    }
                }
                if (insufficient.isNotEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        PaymentResponse(false, "Insufficient stock for products: $insufficient", total)
                    )
                    return@post
                }
                // Always succeed the payment, it is mocked, just decrease stock amount of items
                userBasket.forEach { (productId, qty) ->
                    products.computeIfPresent(productId) { _, old ->
                        old.copy(inStock = old.inStock - qty)
                    }
                }
                // Clear basket
                baskets[userId]?.clear()
                call.respond(HttpStatusCode.OK, PaymentResponse(true, "Mock-payment processed", total))
            }
        }
    }
}
