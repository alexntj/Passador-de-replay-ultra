package com.Privvideo.app;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;

/** Identidade RSA persistente e transporte ADB usado pelo aplicativo. */
public final class AdbConnectionManager extends AbsAdbConnectionManager {
    private static AdbConnectionManager instance;
    private final Context context;
    private PrivateKey privateKey;
    private Certificate certificate;

    private AdbConnectionManager(@NonNull Context context) throws Exception {
        this.context = context.getApplicationContext();
        setApi(Build.VERSION.SDK_INT);
        loadOrCreateIdentity();
    }

    public static synchronized AdbConnectionManager getInstance(@NonNull Context context) throws Exception {
        if (instance == null) instance = new AdbConnectionManager(context);
        return instance;
    }

    private void loadOrCreateIdentity() throws Exception {
        File keyFile = new File(context.getFilesDir(), "adb_private.pk8");
        File certFile = new File(context.getFilesDir(), "adb_cert.der");
        if (keyFile.exists() && certFile.exists()) {
            byte[] encodedKey = readAll(keyFile);
            privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
            try (FileInputStream input = new FileInputStream(certFile)) {
                certificate = CertificateFactory.getInstance("X.509").generateCertificate(input);
            }
            return;
        }

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, SecureRandom.getInstance("SHA1PRNG"));
        KeyPair pair = generator.generateKeyPair();
        privateKey = pair.getPrivate();
        certificate = AdbCertificate.create(
                pair,
                "CN=PassadorReplay",
                System.currentTimeMillis() + 365L * 24L * 60L * 60L * 1000L);

        try (FileOutputStream output = new FileOutputStream(keyFile)) {
            output.write(privateKey.getEncoded());
        }
        try (FileOutputStream output = new FileOutputStream(certFile)) {
            output.write(certificate.getEncoded());
        }
    }

    private static byte[] readAll(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            return input.readAllBytes();
        }
    }

    @Override protected PrivateKey getPrivateKey() { return privateKey; }
    @Override protected Certificate getCertificate() { return certificate; }
    @Override protected String getDeviceName() { return "PassadorReplay"; }
}
