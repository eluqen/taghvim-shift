package com.eluqen.shiftcalendar.update;

import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import org.json.JSONObject;
import org.junit.Test;

public class UpdateNegativeSecurityTest {
    interface Checked { void run() throws Exception; }
    static void rejects(Checked action) throws Exception {
        try { action.run(); } catch (Exception expected) { return; }
        fail("Unsafe input accepted");
    }
    JSONObject envelope() throws Exception {
        return new JSONObject().put("schemaVersion",1).put("algorithm","SHA256withRSA")
                .put("encoding","base64").put("keyId",UpdateConfig.MANIFEST_KEY_ID)
                .put("signature",java.util.Base64.getEncoder().encodeToString(new byte[384]));
    }
    void envelopeRejected(String key, Object value) throws Exception {
        byte[] body = "{\"versionCode\":2}".getBytes(StandardCharsets.UTF_8);
        byte[] signature = envelope().put(key,value).toString().getBytes(StandardCharsets.UTF_8);
        rejects(() -> SecureUpdateTransport.verifyRawManifest(body,signature));
    }
    @Test public void wrongKeyId() throws Exception { envelopeRejected("keyId","other"); }
    @Test public void wrongAlgorithm() throws Exception { envelopeRejected("algorithm","SHA1withRSA"); }
    @Test public void wrongEncoding() throws Exception { envelopeRejected("encoding","hex"); }
    @Test public void wrongEnvelopeSchema() throws Exception { envelopeRejected("schemaVersion",2); }
    @Test public void fractionalEnvelopeSchema() throws Exception { envelopeRejected("schemaVersion",1.5); }
    @Test public void invalidSignature() throws Exception { envelopeRejected("signature",java.util.Base64.getEncoder().encodeToString(new byte[384])); }
    @Test public void invalidBase64() throws Exception { envelopeRejected("signature","%%%bad"); }
    @Test public void oversizedManifest() throws Exception { rejects(() -> SecureUpdateTransport.verifyRawManifest(new byte[65537],new byte[1])); }
    @Test public void oversizedSignature() throws Exception { rejects(() -> SecureUpdateTransport.verifyRawManifest(new byte[1],new byte[4097])); }
    @Test public void pinnedRsa3072() throws Exception {
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(
                java.util.Base64.getDecoder().decode(UpdateConfig.MANIFEST_PUBLIC_KEY_BASE64)));
        assertEquals(3072,((RSAPublicKey)key).getModulus().bitLength());
    }
    @Test public void httpRejected() throws Exception { rejects(() -> SecureUpdateTransport.requireHttpsHost("http://downloads.eluqen.com/app.apk",UpdateConfig.DOWNLOAD_HOST)); }
    @Test public void wrongHostRejected() throws Exception { rejects(() -> SecureUpdateTransport.requireHttpsHost("https://downloads.eluqen.com.evil.test/app.apk",UpdateConfig.DOWNLOAD_HOST)); }
    @Test public void userInfoRejected() throws Exception { rejects(() -> SecureUpdateTransport.requireHttpsHost("https://user@downloads.eluqen.com/app.apk",UpdateConfig.DOWNLOAD_HOST)); }
    @Test public void alternatePortRejected() throws Exception { rejects(() -> SecureUpdateTransport.requireHttpsHost("https://downloads.eluqen.com:8443/app.apk",UpdateConfig.DOWNLOAD_HOST)); }
    @Test public void correctHostAccepted() throws Exception { SecureUpdateTransport.requireHttpsHost("https://downloads.eluqen.com/app.apk",UpdateConfig.DOWNLOAD_HOST); }
    @Test public void oversizedApkRejected() throws Exception { rejects(() -> UpdateValidation.size(new JSONObject().put("size",UpdateConfig.MAX_APK_BYTES+1),"size",UpdateConfig.MAX_APK_BYTES)); }
    @Test public void oversizedCalendarCompressedRejected() throws Exception { rejects(() -> UpdateValidation.size(new JSONObject().put("size",UpdateConfig.MAX_CALENDAR_GZIP_BYTES+1),"size",UpdateConfig.MAX_CALENDAR_GZIP_BYTES)); }
    @Test public void calendarWrongHashRejected() throws Exception { rejects(() -> UpdateValidation.downloaded(10,"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",10,"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",UpdateConfig.MAX_CALENDAR_GZIP_BYTES)); }
    @Test public void wrongHashRejected() throws Exception { rejects(() -> UpdateValidation.downloaded(100,"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",100,"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",UpdateConfig.MAX_APK_BYTES)); }
    @Test public void wrongSizeRejected() throws Exception { rejects(() -> UpdateValidation.downloaded(99,"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",100,"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",UpdateConfig.MAX_APK_BYTES)); }
    @Test public void wrongPackageRejected() throws Exception { rejects(() -> UpdateValidation.archive("other",2,UpdateConfig.APP_SIGNING_CERT_SHA256,2,UpdateConfig.APP_SIGNING_CERT_SHA256)); }
    @Test public void wrongVersionRejected() throws Exception { rejects(() -> UpdateValidation.archive(UpdateConfig.PACKAGE_NAME,1,UpdateConfig.APP_SIGNING_CERT_SHA256,2,UpdateConfig.APP_SIGNING_CERT_SHA256)); }
    @Test public void wrongCertificateRejected() throws Exception { rejects(() -> UpdateValidation.archive(UpdateConfig.PACKAGE_NAME,2,"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",2,UpdateConfig.APP_SIGNING_CERT_SHA256)); }
    @Test public void validArchiveMetadataAccepted() { UpdateValidation.archive(UpdateConfig.PACKAGE_NAME,2,UpdateConfig.APP_SIGNING_CERT_SHA256,2,UpdateConfig.APP_SIGNING_CERT_SHA256); }
    @Test public void invalidGzipRejected() throws Exception {
        File file=File.createTempFile("bad-bank-",".gz");
        try { try(FileOutputStream out=new FileOutputStream(file)){out.write(new byte[]{1,2,3});}
            rejects(() -> SecureUpdateTransport.gunzipLimited(file,1024));
        } finally {file.delete();}
    }
    @Test public void streamLimitRejectsWithoutContentLength() throws Exception { rejects(() -> SecureUpdateTransport.readLimited(new ByteArrayInputStream(new byte[65537]),65536)); }
    @Test public void invalidPublicationDateRejected() throws Exception { rejects(() -> UpdateValidation.publishedAt("2026-02-30T12:00:00Z")); }
    @Test public void nonUtcPublicationRejected() throws Exception { rejects(() -> UpdateValidation.publishedAt("2026-10-01T12:00:00+03:30")); }
    @Test public void utcPublicationAccepted() { UpdateValidation.publishedAt("2026-10-01T12:00:00Z"); UpdateValidation.publishedAt("2026-10-01T12:00:00.1Z"); UpdateValidation.publishedAt("2026-10-01T12:00:00.123456789Z"); }
    @Test public void fractionalVersionRejected() throws Exception { rejects(() -> UpdateValidation.integer(new JSONObject().put("versionCode",2.5),"versionCode")); }
    @Test public void appIdentityRejected() throws Exception { rejects(() -> UpdateValidation.identity(new JSONObject().put("schemaVersion",1).put("appSlug","other"),true)); }
}
