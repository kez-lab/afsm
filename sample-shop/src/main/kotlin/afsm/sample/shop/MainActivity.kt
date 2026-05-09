package afsm.sample.shop

import afsm.sample.shop.app.SampleShopApp
import afsm.sample.shop.app.ShopAppContainer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    private val container: ShopAppContainer by lazy {
        ShopAppContainer(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SampleShopApp(container = container)
        }
    }
}
