package de.belkis.spyhound.ui.navigation

enum class RootScreen {
    START,
    SCAN,
    RESULT,
}

enum class TransitionDirection {
    FORWARD,
    BACKWARD,
}

enum class TransitionStyle {
    SHARED_RADAR,
    FADE_SHIFT,
    INSTANT,
}

data class TransitionPlan(
    val style: TransitionStyle,
    val direction: TransitionDirection,
)

fun rootTransitionPlan(
    from: RootScreen,
    to: RootScreen,
    motionEnabled: Boolean,
): TransitionPlan {
    if (!motionEnabled || from == to) {
        return TransitionPlan(
            style = TransitionStyle.INSTANT,
            direction = TransitionDirection.FORWARD,
        )
    }
    return when {
        from == RootScreen.START && to == RootScreen.SCAN -> TransitionPlan(TransitionStyle.SHARED_RADAR, TransitionDirection.FORWARD)
        from == RootScreen.SCAN && to == RootScreen.RESULT -> TransitionPlan(TransitionStyle.SHARED_RADAR, TransitionDirection.FORWARD)
        from == RootScreen.SCAN && to == RootScreen.START -> TransitionPlan(TransitionStyle.SHARED_RADAR, TransitionDirection.BACKWARD)
        from == RootScreen.RESULT && to == RootScreen.SCAN -> TransitionPlan(TransitionStyle.SHARED_RADAR, TransitionDirection.BACKWARD)
        from.ordinal < to.ordinal -> TransitionPlan(TransitionStyle.FADE_SHIFT, TransitionDirection.FORWARD)
        else -> TransitionPlan(TransitionStyle.FADE_SHIFT, TransitionDirection.BACKWARD)
    }
}

fun overlayTransitionPlan(
    opening: Boolean,
    motionEnabled: Boolean,
): TransitionPlan {
    if (!motionEnabled) {
        return TransitionPlan(
            style = TransitionStyle.INSTANT,
            direction = TransitionDirection.FORWARD,
        )
    }
    return TransitionPlan(
        style = TransitionStyle.FADE_SHIFT,
        direction = if (opening) TransitionDirection.FORWARD else TransitionDirection.BACKWARD,
    )
}
