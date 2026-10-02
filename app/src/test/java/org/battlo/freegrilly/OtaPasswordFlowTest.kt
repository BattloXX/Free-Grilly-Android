package org.battlo.freegrilly

import org.battlo.freegrilly.data.security.EncryptedSecretBlob
import org.battlo.freegrilly.data.security.decodeEncryptedSecret
import org.battlo.freegrilly.data.security.encodeEncryptedSecret
import org.battlo.freegrilly.ui.update.OtaPasswordAction
import org.battlo.freegrilly.ui.update.nextOtaPasswordAction
import org.battlo.freegrilly.ui.update.shouldDiscardStoredPassword
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class OtaPasswordFlowTest {
    @Test fun `auth hint prompts unless a password is stored`() {
        assertEquals(OtaPasswordAction.ASK_FOR_PASSWORD, nextOtaPasswordAction(true, false))
        assertEquals(OtaPasswordAction.UPLOAD_WITH_STORED_PASSWORD, nextOtaPasswordAction(true, true))
        assertEquals(OtaPasswordAction.UPLOAD_WITHOUT_PASSWORD, nextOtaPasswordAction(false, false))
    }

    @Test fun `only a 401 after stored password discards it`() {
        assertEquals(true, shouldDiscardStoredPassword(401, true))
        assertFalse(shouldDiscardStoredPassword(500, true))
        assertFalse(shouldDiscardStoredPassword(401, false))
    }

    @Test fun `encrypted blob round trips and rejects malformed input`() {
        val decoded = decodeEncryptedSecret(encodeEncryptedSecret(EncryptedSecretBlob(byteArrayOf(1, 2), byteArrayOf(3, 4))))!!
        assertArrayEquals(byteArrayOf(1, 2), decoded.iv)
        assertArrayEquals(byteArrayOf(3, 4), decoded.ciphertext)
        assertNull(decodeEncryptedSecret("not-a-blob"))
    }
}
