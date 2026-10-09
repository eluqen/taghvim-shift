package com.eluqen.shiftcalendar.update;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.zip.GZIPOutputStream;
import org.junit.Test;

public class UpdateSecurityTest {
    @Test
    public void rawSignaturePassesAndTamperFails() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        KeyPair pair = generator.generateKeyPair();

        byte[] manifest = "{\"versionCode\":2}".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(manifest);
        byte[] signature = signer.sign();
        org.json.JSONObject envelope = new org.json.JSONObject().put("schemaVersion",1)
                .put("algorithm","SHA256withRSA").put("encoding","base64")
                .put("keyId",UpdateConfig.MANIFEST_KEY_ID)
                .put("signature",java.util.Base64.getEncoder().encodeToString(signature));
        byte[] envelopeBytes = envelope.toString().getBytes(StandardCharsets.UTF_8);
        assertEquals(2, SecureUpdateTransport.verifyRawManifestWithKey(manifest,envelopeBytes,pair.getPublic()).getInt("versionCode"));

        assertTrue(SecureUpdateTransport.verifySignature(
                pair.getPublic(), manifest, signature));

        byte[] tampered = "{\"versionCode\":3}".getBytes(StandardCharsets.UTF_8);
        assertFalse(SecureUpdateTransport.verifySignature(
                pair.getPublic(), tampered, signature));
        TestRejections.rejects(() -> SecureUpdateTransport.verifyRawManifestWithKey(tampered,envelopeBytes,pair.getPublic()));
    }

    @Test
    public void versionComparisonsAreStrictlyMonotonic() {
        assertTrue(AppUpdateManager.isRemoteNewer(2, 1));
        assertFalse(AppUpdateManager.isRemoteNewer(1, 1));
        assertFalse(AppUpdateManager.isRemoteNewer(1, 2));

        assertTrue(DataUpdateManager.isRemoteNewer(6, 5));
        assertFalse(DataUpdateManager.isRemoteNewer(5, 5));
        assertFalse(DataUpdateManager.isRemoteNewer(4, 5));
    }

    @Test
    public void sha256FormatIsStrictLowercaseHex() {
        assertTrue(SecureUpdateTransport.isSha256(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
        assertFalse(SecureUpdateTransport.isSha256(
                "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"));
        assertFalse(SecureUpdateTransport.isSha256("abcd"));
    }

    @Test
    public void gzipDecompressedLimitIsEnforced() throws Exception {
        File file = File.createTempFile("eluqen-calendar-", ".gz");
        try {
            try (GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(file))) {
                gzip.write(new byte[4096]);
            }
            assertEquals(4096,
                    SecureUpdateTransport.gunzipLimited(file, 4096).length);
            try {
                SecureUpdateTransport.gunzipLimited(file, 1024);
                fail("Expected decompressed-size rejection");
            } catch (java.io.IOException expected) {
                // expected
            }
        } finally {
            file.delete();
        }
    }
}
