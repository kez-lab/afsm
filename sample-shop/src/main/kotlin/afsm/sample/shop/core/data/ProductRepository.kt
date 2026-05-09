package afsm.sample.shop.core.data

import afsm.sample.shop.core.database.ProductDao
import afsm.sample.shop.core.database.ProductEntity
import afsm.sample.shop.core.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val productDao: ProductDao,
) {
    fun observeProducts(): Flow<List<Product>> {
        return productDao.observeProducts().map { products ->
            products.map { product -> product.toModel() }
        }
    }

    fun observeProduct(productId: Long): Flow<Product?> {
        return productDao.observeProduct(productId).map { product ->
            product?.toModel()
        }
    }

    suspend fun findProduct(productId: Long): Product? {
        return productDao.findById(productId)?.toModel()
    }

    suspend fun addProduct(
        title: String,
        description: String,
        priceCents: Long,
        sellerUserId: Long?,
    ): Long {
        return productDao.insert(
            ProductEntity(
                title = title.trim(),
                description = description.trim(),
                priceCents = priceCents,
                sellerUserId = sellerUserId,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun ensureSeedProducts() {
        if (productDao.count() > 0) {
            return
        }

        productDao.insertAll(
            listOf(
                ProductEntity(
                    title = "Everyday Backpack",
                    description = "A compact city bag with padded laptop storage.",
                    priceCents = 8900,
                    sellerUserId = null,
                    createdAtMillis = System.currentTimeMillis() - 3_000,
                ),
                ProductEntity(
                    title = "Studio Headphones",
                    description = "Closed-back headphones tuned for long focus sessions.",
                    priceCents = 12900,
                    sellerUserId = null,
                    createdAtMillis = System.currentTimeMillis() - 2_000,
                ),
                ProductEntity(
                    title = "Desk Light",
                    description = "Low-glare LED task light with three brightness levels.",
                    priceCents = 5400,
                    sellerUserId = null,
                    createdAtMillis = System.currentTimeMillis() - 1_000,
                ),
            ),
        )
    }

    private fun ProductEntity.toModel(): Product {
        return Product(
            id = id,
            title = title,
            description = description,
            priceCents = priceCents,
            sellerUserId = sellerUserId,
        )
    }
}
