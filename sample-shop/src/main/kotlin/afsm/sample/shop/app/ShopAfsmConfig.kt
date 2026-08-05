package afsm.sample.shop.app

import afsm.runtime.AfsmConfig
import afsm.runtime.AfsmDiagnostic
import afsm.runtime.AfsmLogger
import afsm.sample.shop.BuildConfig
import android.util.Log

/**
 * The recommended host configuration for a real Android app.
 *
 * Debug builds fail fast so flow mistakes are impossible to miss during
 * development. Release builds record the same mistakes instead, because a
 * single invalid transition should never stop a shipped screen from accepting
 * further events.
 *
 * Command handlers keep running on the ViewModel scope because this sample's
 * repositories are main-safe. Set `AfsmConfig.commandContext` to
 * `Dispatchers.IO` instead when a command calls work that is not main-safe.
 */
internal fun shopAfsmConfig(): AfsmConfig {
    return if (BuildConfig.DEBUG) {
        AfsmConfig.strict(logger = ShopAfsmLogger)
    } else {
        AfsmConfig(logger = ShopAfsmLogger)
    }
}

private object ShopAfsmLogger : AfsmLogger {
    override fun log(diagnostic: AfsmDiagnostic) {
        Log.w(
            "Afsm",
            "${diagnostic.code}: ${diagnostic.message} " +
                "state=${diagnostic.stateType} event=${diagnostic.eventType} " +
                "command=${diagnostic.commandType} failure=${diagnostic.failureType} " +
                "metadata=${diagnostic.metadata}",
        )
    }
}
