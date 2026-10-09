package com.eluqen.shiftcalendar.update;

import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

/** In-memory HTTPS connections exercise transport bounds without production writes. */
public class TransportBoundaryTest {
    static final Map<String, Reply> replies = new HashMap<>();
    static class Reply {
        final int status, length; final String location, retry; final byte[] body;
        Reply(int status, int length, String location, String retry, byte[] body) {
            this.status=status;this.length=length;this.location=location;this.retry=retry;this.body=body;
        }
    }
    static class Connection extends HttpURLConnection {
        final Reply reply;
        Connection(URL url) {super(url);reply=replies.get(url.toString());}
        public int getResponseCode(){return reply==null ? 404:reply.status;}
        public int getContentLength(){return reply.length;}
        public long getContentLengthLong(){return reply.length;}
        public String getHeaderField(String name){return "Location".equals(name)?reply.location:"Retry-After".equals(name)?reply.retry:null;}
        public InputStream getInputStream(){return new ByteArrayInputStream(reply.body);}
        public void disconnect(){} public boolean usingProxy(){return false;} public void connect(){}
    }
    @BeforeClass public static void fakeHttpsOnlyInUnitJvm() {
        URL.setURLStreamHandlerFactory(protocol -> "https".equals(protocol) ? new URLStreamHandler(){
            protected URLConnection openConnection(URL url){return new Connection(url);}
        }:null);
    }
    String url(String test){return "https://update.eluqen.com/qa/"+test;}
    byte[] get(String url,int max) throws Exception {return SecureUpdateTransport.getHttps(url,max,"application/json",1000);}
    void response(String url,int length,byte[] bytes){replies.put(url,new Reply(200,length,null,null,bytes));}
    @Test public void threeRedirectsAccepted() throws Exception {
        String start=url("three");for(int i=0;i<3;i++)replies.put(start+i,new Reply(302,0,start+(i+1),null,new byte[0]));response(start+3,1,new byte[]{9});assertArrayEquals(new byte[]{9},get(start+0,64));
    }
    @Test public void fourthRedirectRejected() throws Exception {
        String start=url("four");for(int i=0;i<4;i++)replies.put(start+i,new Reply(302,0,start+(i+1),null,new byte[0]));response(start+4,1,new byte[]{9});TestRejections.rejects(() -> get(start+0,64));
    }
    @Test public void httpRedirectRejected() throws Exception {String u=url("downgrade");replies.put(u,new Reply(302,0,"http://update.eluqen.com/insecure",null,new byte[0]));TestRejections.rejects(() -> get(u,64));}
    @Test public void crossHostRedirectRejected() throws Exception {String u=url("cross");replies.put(u,new Reply(302,0,"https://evil.test/data",null,new byte[0]));TestRejections.rejects(() -> get(u,64));}
    @Test public void missingRedirectLocationRejected() throws Exception {String u=url("missing");replies.put(u,new Reply(302,0,null,null,new byte[0]));TestRejections.rejects(() -> get(u,64));}
    @Test public void oversizedManifestHeaderRejected() throws Exception {String u=url("big-manifest");response(u,65537,new byte[1]);TestRejections.rejects(() -> get(u,UpdateConfig.MAX_MANIFEST_BYTES));}
    @Test public void oversizedManifestStreamRejected() throws Exception {String u=url("big-stream");response(u,-1,new byte[65537]);TestRejections.rejects(() -> get(u,UpdateConfig.MAX_MANIFEST_BYTES));}
    @Test public void oversizedSignatureStreamRejected() throws Exception {String u=url("big-signature");response(u,-1,new byte[4097]);TestRejections.rejects(() -> get(u,UpdateConfig.MAX_SIGNATURE_BYTES));}
    @Test public void notPublished404IsTyped() throws Exception {String u=url("404");replies.put(u,new Reply(404,0,null,null,new byte[0]));try{get(u,64);fail();}catch(SecureUpdateTransport.HttpStatusException e){assertEquals(404,e.statusCode);}}
    @Test public void retryAfter429Preserved() throws Exception {String u=url("429");replies.put(u,new Reply(429,0,null,"30",new byte[0]));try{get(u,64);fail();}catch(SecureUpdateTransport.HttpStatusException e){assertEquals(429,e.statusCode);assertEquals(30,e.retryAfterSeconds);}}
}
