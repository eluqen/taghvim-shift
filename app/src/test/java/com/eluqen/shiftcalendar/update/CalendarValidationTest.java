package com.eluqen.shiftcalendar.update;
import static org.junit.Assert.*;
import com.eluqen.shiftcalendar.events.EventRepository;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.*;
import org.junit.Test;
public class CalendarValidationTest {
    JSONObject bank() throws Exception {return new JSONObject(new String(Files.readAllBytes(new File("src/main/assets/event_bank.json").toPath()),StandardCharsets.UTF_8));}
    void validate(JSONObject b) throws Exception {EventRepository.validateCanonical(b.toString().getBytes(StandardCharsets.UTF_8),2,1405,1409,b.optString("publishedAt"));}
    @Test public void validBank() throws Exception {validate(bank());}
    @Test public void wrongSchema() throws Exception {TestRejections.rejects(() -> validate(bank().put("schemaVersion",2)));}
    @Test public void missingExactEvents() throws Exception {JSONObject b=bank();b.remove("exactEvents");TestRejections.rejects(() -> validate(b));}
    @Test public void metadataMismatch() throws Exception {TestRejections.rejects(() -> validate(bank().put("dataVersion",3)));}
    @Test public void fractionalDataVersion() throws Exception {TestRejections.rejects(() -> validate(bank().put("dataVersion",1.5)));}
    @Test public void invalidCoverage() throws Exception {TestRejections.rejects(() -> validate(bank().put("coverageTo",1404)));}
    @Test public void invalidEventDay() throws Exception {JSONObject b=bank();b.getJSONArray("events").getJSONObject(0).put("day",32);TestRejections.rejects(() -> validate(b));}
    @Test public void invalidCalendar() throws Exception {JSONObject b=bank();b.getJSONArray("events").getJSONObject(0).put("calendar","invalid");TestRejections.rejects(() -> validate(b));}
    @Test public void invalidExactDate() throws Exception {JSONObject b=bank();b.put("exactEvents",new JSONArray().put(new JSONObject().put("year",1405).put("month",7).put("day",31).put("title","x").put("holiday",false).put("category","x")));TestRejections.rejects(() -> validate(b));}
    @Test public void invalidPublishedAt() throws Exception {TestRejections.rejects(() -> validate(bank().put("publishedAt","2026-99-01T00:00:00Z")));}
    @Test public void failedValidationKeepsExistingBank() throws Exception {
        File dir=Files.createTempDirectory("bank-qa-").toFile(),file=new File(dir,"bank.json");byte[] old="old bank".getBytes(StandardCharsets.UTF_8);
        Files.write(file.toPath(),old);
        try {TestRejections.rejects(() -> AtomicCalendarStore.replace(file,new byte[]{1},bytes -> {throw new SecurityException("bad payload");}));assertArrayEquals(old,Files.readAllBytes(file.toPath()));assertEquals(1,dir.list().length);} finally {file.delete();dir.delete();}
    }
    @Test public void validatedReplacementCommits() throws Exception {
        File dir=Files.createTempDirectory("bank-qa-").toFile(),file=new File(dir,"bank.json");Files.write(file.toPath(),new byte[]{1});byte[] body=bank().toString().getBytes(StandardCharsets.UTF_8);
        try {AtomicCalendarStore.replace(file,body,bytes -> EventRepository.validateCanonical(bytes,2,1405,1409,bank().getString("publishedAt")));assertArrayEquals(body,Files.readAllBytes(file.toPath()));assertEquals(1,dir.list().length);}finally{file.delete();dir.delete();}
    }
    @Test public void invalidTargetKeepsExistingContents() throws Exception {
        File dir=Files.createTempDirectory("bank-qa-").toFile(),target=new File(dir,"bank.json");target.mkdir();File sentinel=new File(target,"sentinel");Files.write(sentinel.toPath(),new byte[]{7});
        try {TestRejections.rejects(() -> AtomicCalendarStore.replace(target,new byte[]{1},bytes -> {}));assertArrayEquals(new byte[]{7},Files.readAllBytes(sentinel.toPath()));assertEquals(1,dir.list().length);}finally{sentinel.delete();target.delete();dir.delete();}
    }
    @Test public void activationFailureRollsBackAtomically() throws Exception {
        File dir=Files.createTempDirectory("bank-qa-").toFile(),file=new File(dir,"bank.json");byte[] old=new byte[]{8,9};Files.write(file.toPath(),old);
        try {TestRejections.rejects(() -> AtomicCalendarStore.replace(file,new byte[]{1,2},bytes -> {},() -> {throw new IOException("activation failed");}));assertArrayEquals(old,Files.readAllBytes(file.toPath()));assertEquals(1,dir.list().length);}finally{file.delete();dir.delete();}
    }

}
