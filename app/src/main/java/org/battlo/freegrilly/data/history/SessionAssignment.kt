package org.battlo.freegrilly.data.history

/** Pure policy for assigning samples to a cook session. */
internal enum class SessionAction { KEEP_CURRENT, RESUME_EXISTING, CREATE_NEW }

internal data class SessionDecision(val action: SessionAction, val closeCurrent: Boolean = false)

internal fun decideSession(
    currentSessionId: Long?,
    currentFirmwareSessionId: String?,
    firmwareSessionId: String?,
    existingFirmwareSessionId: Long?,
    canResumeLatest: Boolean,
): SessionDecision = when {
    firmwareSessionId != null && currentSessionId != null && currentFirmwareSessionId == firmwareSessionId ->
        SessionDecision(SessionAction.KEEP_CURRENT)
    // A firmware id transition denotes a new cook, even if an old row happens to share that id.
    firmwareSessionId != null && currentSessionId != null ->
        SessionDecision(SessionAction.CREATE_NEW, closeCurrent = true)
    firmwareSessionId != null && existingFirmwareSessionId != null ->
        SessionDecision(SessionAction.RESUME_EXISTING)
    firmwareSessionId != null -> SessionDecision(SessionAction.CREATE_NEW)
    currentSessionId != null -> SessionDecision(SessionAction.KEEP_CURRENT)
    canResumeLatest -> SessionDecision(SessionAction.RESUME_EXISTING)
    else -> SessionDecision(SessionAction.CREATE_NEW)
}
