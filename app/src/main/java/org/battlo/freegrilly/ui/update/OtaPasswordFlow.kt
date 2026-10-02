package org.battlo.freegrilly.ui.update

/** Pure policy for the OTA password challenge, deliberately independent of Android/Keystore. */
enum class OtaPasswordAction { UPLOAD_WITHOUT_PASSWORD, UPLOAD_WITH_STORED_PASSWORD, ASK_FOR_PASSWORD }

fun nextOtaPasswordAction(authHint: Boolean, hasStoredPassword: Boolean): OtaPasswordAction = when {
    hasStoredPassword -> OtaPasswordAction.UPLOAD_WITH_STORED_PASSWORD
    authHint -> OtaPasswordAction.ASK_FOR_PASSWORD
    else -> OtaPasswordAction.UPLOAD_WITHOUT_PASSWORD
}

fun shouldDiscardStoredPassword(httpCode: Int?, usedStoredPassword: Boolean): Boolean =
    httpCode == 401 && usedStoredPassword
