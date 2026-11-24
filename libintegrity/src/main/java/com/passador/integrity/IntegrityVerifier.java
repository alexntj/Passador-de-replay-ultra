package com.passador.integrity;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import java.security.MessageDigest;

/** Verifica a assinatura atual do pacote instalado contra o certificado aprovado. */
public final class IntegrityVerifier {
    private static final String EXPECTED_SIGNER_SHA256 =
            "bd68b288664443f4482ac295bf67dc774339d8f334697b51087e49d8383458a8";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private IntegrityVerifier() {
    }

    @SuppressWarnings("deprecation")
    public static boolean verifyInstalledSignature(Context context) {
        if (context == null) return false;

        try {
            PackageManager packageManager = context.getPackageManager();
            Signature[] currentSigners;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo packageInfo = packageManager.getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES
                );
                SigningInfo signingInfo = packageInfo.signingInfo;
                if (signingInfo == null) return false;
                currentSigners = signingInfo.getApkContentsSigners();
            } else {
                PackageInfo packageInfo = packageManager.getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNATURES
                );
                currentSigners = packageInfo.signatures;
            }

            // Este app usa um único certificado fixo; assinantes ausentes/múltiplos falham fechados.
            if (currentSigners == null || currentSigners.length != 1 || currentSigners[0] == null) {
                return false;
            }
            return EXPECTED_SIGNER_SHA256.equals(sha256(currentSigners[0].toByteArray()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String sha256(byte[] certificateBytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificateBytes);
        char[] encoded = new char[digest.length * 2];
        for (int i = 0; i < digest.length; i++) {
            int value = digest[i] & 0xff;
            encoded[i * 2] = HEX[value >>> 4];
            encoded[i * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(encoded);
    }
}
