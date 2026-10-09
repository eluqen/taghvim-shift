package com.eluqen.shiftcalendar.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

final class AtomicCalendarStore {
    interface Validator { void validate(byte[] bytes) throws Exception; }
    interface Activation { void activate() throws Exception; }
    private AtomicCalendarStore() {}
    static synchronized void replace(File installed, byte[] bytes, Validator validator) throws Exception {
        replace(installed, bytes, validator, () -> {});
    }
    static synchronized void replace(File installed, byte[] bytes, Validator validator,
                                     Activation activation) throws Exception {
        validator.validate(bytes);
        File parent = installed.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) throw new IOException("Cannot create bank directory");
        File staging = File.createTempFile("event-bank-", ".download", parent);
        File previous = null;
        boolean committed = false;
        boolean preservePrevious = false;
        try {
            try (FileOutputStream out = new FileOutputStream(staging)) {
                out.write(bytes); out.flush(); out.getFD().sync();
            }
            if (installed.exists()) {
                previous = File.createTempFile("event-bank-", ".previous", parent);
                try (FileInputStream in = new FileInputStream(installed);
                     FileOutputStream out = new FileOutputStream(previous)) {
                    byte[] buffer = new byte[32768]; int read;
                    while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                    out.flush(); out.getFD().sync();
                }
            }
            // Same-directory Linux rename replaces the target without a missing-file interval.
            if (!staging.renameTo(installed)) throw new IOException("Atomic bank replacement failed");
            committed = true;
            activation.activate();
        } catch (Exception e) {
            if (committed) {
                boolean restored = previous != null ? previous.renameTo(installed) : installed.delete();
                if (!restored) {
                    preservePrevious = true;
                    throw new IOException("Rollback failed; preserved recovery copy: " + previous, e);
                }
            }
            throw e;
        } finally {
            staging.delete();
            if (previous != null && !preservePrevious) previous.delete();
        }
    }
}
