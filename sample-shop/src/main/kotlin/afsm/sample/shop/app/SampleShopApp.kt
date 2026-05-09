package afsm.sample.shop.app

import afsm.sample.shop.feature.auth.AuthRoute
import afsm.sample.shop.feature.catalog.CatalogRoute
import afsm.sample.shop.feature.checkout.CheckoutRoute
import afsm.sample.shop.feature.editor.ProductEditorRoute
import afsm.sample.shop.feature.product.ProductDetailRoute
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun SampleShopApp(
    container: ShopAppContainer,
) {
    val navController = rememberNavController()

    MaterialTheme(
        colorScheme = lightColorScheme(),
    ) {
        NavHost(
            navController = navController,
            startDestination = ShopRoute.Auth.route,
        ) {
            composable(ShopRoute.Auth.route) {
                AuthRoute(
                    container = container,
                    onAuthenticated = {
                        navController.navigate(ShopRoute.Catalog.route) {
                            popUpTo(ShopRoute.Auth.route) {
                                inclusive = true
                            }
                        }
                    },
                )
            }

            composable(ShopRoute.Catalog.route) {
                CatalogRoute(
                    container = container,
                    onProductClick = { productId ->
                        navController.navigate(ShopRoute.ProductDetail.create(productId))
                    },
                    onAddProductClick = {
                        navController.navigate(ShopRoute.ProductEditor.route)
                    },
                    onLoggedOut = {
                        navController.navigate(ShopRoute.Auth.route) {
                            popUpTo(ShopRoute.Catalog.route) {
                                inclusive = true
                            }
                        }
                    },
                )
            }

            composable(ShopRoute.ProductEditor.route) {
                ProductEditorRoute(
                    container = container,
                    onDone = {
                        navController.popBackStack()
                    },
                )
            }

            composable(
                route = ShopRoute.ProductDetail.route,
                arguments = listOf(navArgument("productId") { type = NavType.LongType }),
            ) { entry ->
                val productId = entry.arguments?.getLong("productId") ?: return@composable
                ProductDetailRoute(
                    container = container,
                    productId = productId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onBuy = {
                        navController.navigate(ShopRoute.Checkout.create(productId))
                    },
                )
            }

            composable(
                route = ShopRoute.Checkout.route,
                arguments = listOf(navArgument("productId") { type = NavType.LongType }),
            ) { entry ->
                val productId = entry.arguments?.getLong("productId") ?: return@composable
                CheckoutRoute(
                    container = container,
                    productId = productId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onPaymentComplete = {
                        navController.popBackStack(
                            route = ShopRoute.Catalog.route,
                            inclusive = false,
                        )
                    },
                )
            }
        }
    }
}

private sealed class ShopRoute(
    val route: String,
) {
    data object Auth : ShopRoute("auth")

    data object Catalog : ShopRoute("catalog")

    data object ProductEditor : ShopRoute("product-editor")

    data object ProductDetail : ShopRoute("product/{productId}") {
        fun create(productId: Long): String = "product/$productId"
    }

    data object Checkout : ShopRoute("checkout/{productId}") {
        fun create(productId: Long): String = "checkout/$productId"
    }
}
