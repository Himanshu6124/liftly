package com.himanshu.liftly.core.platform

/**
 * WidgetKit is Swift-only: the app registers a Swift bridge here at startup (writes the snapshot to
 * the App Group and reloads timelines). The last snapshot is replayed when the delegate arrives.
 */
object HomeWidgetBridgeHolder : HomeWidgetBridge {
    private var last: WidgetSnapshot? = null

    var delegate: HomeWidgetBridge? = null
        set(value) {
            field = value
            last?.let { value?.update(it) }
        }

    override fun update(snapshot: WidgetSnapshot) {
        last = snapshot
        delegate?.update(snapshot)
    }
}
