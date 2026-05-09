package afsm.sample.shop.app

import afsm.sample.shop.core.data.AuthRepository
import afsm.sample.shop.core.data.FavoriteRepository
import afsm.sample.shop.core.data.PaymentRepository
import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.ReviewRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.sample.shop.core.database.ShopDatabase
import android.content.Context
import androidx.room.Room

class ShopAppContainer(
    context: Context,
) {
    private val database: ShopDatabase = Room.databaseBuilder(
        context = context.applicationContext,
        klass = ShopDatabase::class.java,
        name = "afsm-shop.db",
    ).build()

    val sessionRepository: SessionRepository = SessionRepository()

    val authRepository: AuthRepository = AuthRepository(
        userDao = database.userDao(),
    )

    val productRepository: ProductRepository = ProductRepository(
        productDao = database.productDao(),
    )

    val favoriteRepository: FavoriteRepository = FavoriteRepository(
        favoriteDao = database.favoriteDao(),
    )

    val reviewRepository: ReviewRepository = ReviewRepository(
        reviewDao = database.reviewDao(),
    )

    val paymentRepository: PaymentRepository = PaymentRepository(
        orderDao = database.orderDao(),
    )
}
