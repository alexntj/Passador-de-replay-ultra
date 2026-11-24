package com.Privvideo.app

import android.app.Application
import android.content.Context
import com.passador.integrity.IntegrityVerifier

/** Executa a verificação da assinatura antes de Activities e ContentProviders. */
class IntegrityApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (!IntegrityVerifier.verifyInstalledSignature(base)) {
            throw SecurityException("Assinatura ou integridade do APK inválida.")
        }
    }
}
