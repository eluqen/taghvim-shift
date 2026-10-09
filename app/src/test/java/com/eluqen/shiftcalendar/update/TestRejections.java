package com.eluqen.shiftcalendar.update;
import static org.junit.Assert.fail;
final class TestRejections {
    interface Checked { void run() throws Exception; }
    static void rejects(Checked action) throws Exception {
        try { action.run(); } catch (Exception expected) { return; }
        fail("Unsafe input accepted");
    }
}
