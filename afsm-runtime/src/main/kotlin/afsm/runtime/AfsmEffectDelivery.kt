package afsm.runtime

import kotlinx.coroutines.channels.BufferOverflow

public class AfsmEffectDelivery private constructor(
    public val replay: Int,
    public val extraBufferCapacity: Int,
    internal val onBufferOverflow: BufferOverflow,
) {
    init {
        require(replay >= 0) { "replay must be >= 0." }
        require(extraBufferCapacity >= 0) { "extraBufferCapacity must be >= 0." }
    }

    public companion object {
        /**
         * Default one-shot UI effect delivery.
         *
         * No replay prevents accidental relaunch after recreation. A small
         * buffer reduces loss during immediate collector gaps.
         */
        public val Default: AfsmEffectDelivery =
            AfsmEffectDelivery(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        /**
         * Strict delivery mode that can suspend runtime processing while a
         * collector receives an effect.
         */
        public val Rendezvous: AfsmEffectDelivery =
            AfsmEffectDelivery(
                replay = 0,
                extraBufferCapacity = 0,
                onBufferOverflow = BufferOverflow.SUSPEND,
            )

        /**
         * Best-effort buffered one-shot delivery with no replay.
         */
        public fun buffered(capacity: Int): AfsmEffectDelivery {
            require(capacity > 0) { "capacity must be > 0." }
            return AfsmEffectDelivery(
                replay = 0,
                extraBufferCapacity = capacity,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }
    }
}
