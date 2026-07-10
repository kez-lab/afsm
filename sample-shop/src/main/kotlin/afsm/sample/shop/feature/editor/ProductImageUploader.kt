package afsm.sample.shop.feature.editor

import kotlinx.coroutines.delay

/**
 * Feature-owned suspend boundary for product draft image transfer.
 */
fun interface ProductImageUploader {
    suspend fun upload(draft: ProductDraft): String
}

/**
 * Demo-only cooperative uploader used by the sample route.
 *
 * The delay keeps the phase-owned cancel action visible. It is not a
 * production timeout or an Afsm runtime policy.
 */
class MockProductImageUploader(
    private val visibilityDelayMillis: Long = 2_000,
) : ProductImageUploader {
    init {
        require(visibilityDelayMillis > 0) {
            "visibilityDelayMillis must be > 0."
        }
    }

    override suspend fun upload(draft: ProductDraft): String {
        delay(visibilityDelayMillis)
        return "mock-upload-token-${draft.reviewAttempt + 1}"
    }
}
