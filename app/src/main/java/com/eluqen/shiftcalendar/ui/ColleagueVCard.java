package com.eluqen.shiftcalendar.ui;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Local, bounded reader for user-selected vCard 2.1/3.0/4.0 documents.
 * Extracts ONLY contact display name and telephone entries, never photos,
 * addresses, or other sensitive fields.
 */
public final class ColleagueVCard {
    public static final int MAX_BYTES=2*1024*1024;
    public static final int MAX_CARDS=300;
    public static final int MAX_NUMBERS_PER_CARD=120;

    public static final class Contact {
        public final String name;
        public final List<ColleagueNumbers.NumberEntry> numbers;
        public Contact(String name,List<ColleagueNumbers.NumberEntry> numbers){
            this.name=name;
            this.numbers=Collections.unmodifiableList(new ArrayList<>(numbers));
        }
    }
    private ColleagueVCard(){}

    public static List<Contact> parse(byte[] file) {
        if(file==null||file.length==0||file.length>MAX_BYTES)
            throw new IllegalArgumentException("vCard size is invalid");
        String raw=new String(file,StandardCharsets.UTF_8);
        if(raw.length()>0&&raw.charAt(0)=='\ufeff')raw=raw.substring(1);
        String[] physical=raw.replace("\r\n","\n").replace('\r','\n').split("\n",-1);
        List<String> logical=new ArrayList<>();
        StringBuilder current=new StringBuilder();
        for(String line:physical){
            boolean folded=line.startsWith(" ")||line.startsWith("\t");
            boolean quotedSoft=current.length()>0&&current.charAt(current.length()-1)=='='
                    &&current.toString().toUpperCase(Locale.ROOT).contains("ENCODING=QUOTED-PRINTABLE");
            if(folded&&current.length()>0){
                current.append(line.substring(1));
            }else if(quotedSoft){
                current.deleteCharAt(current.length()-1);
                current.append(line);
            }else{
                if(current.length()>0)logical.add(current.toString());
                current.setLength(0);
                current.append(line);
            }
        }
        if(current.length()>0)logical.add(current.toString());

        List<Contact> cards=new ArrayList<>();
        boolean inside=false;
        String display="", structured="";
        List<ColleagueNumbers.NumberEntry> phones=new ArrayList<>();
        for(String line:logical){
            if("BEGIN:VCARD".equalsIgnoreCase(line.trim())){
                if(inside)throw new IllegalArgumentException("Nested vCard");
                inside=true;display="";structured="";phones=new ArrayList<>();
                continue;
            }
            if("END:VCARD".equalsIgnoreCase(line.trim())){
                if(!inside)throw new IllegalArgumentException("Unexpected end");
                inside=false;
                if(!phones.isEmpty()){
                    if(cards.size()>=MAX_CARDS)throw new IllegalArgumentException("Too many vCards");
                    String name=!display.isEmpty()?display:structured;
                    cards.add(new Contact(name.isEmpty()?"مخاطب بدون نام":name,phones));
                }
                continue;
            }
            if(!inside)continue;
            int split=line.indexOf(':');
            if(split<0)continue;
            String head=line.substring(0,split);
            String[] opts=head.split(";");
            String property=opts[0];
            int dot=property.lastIndexOf('.');
            if(dot>=0)property=property.substring(dot+1);
            property=property.toUpperCase(Locale.ROOT);
            if(!property.equals("TEL")&&!property.equals("FN")&&!property.equals("N"))continue;
            String val=line.substring(split+1);
            if(head.toUpperCase(Locale.ROOT).contains("ENCODING=QUOTED-PRINTABLE")){
                String charSet=getParam(opts,"CHARSET");
                val=decodeQuotedPrintable(val,charSet);
            }
            val=unescape(val).trim();
            if(property.equals("FN")){
                display=limited(val,160);
            }else if(property.equals("N")){
                structured=limited(val.replace(';',' ').replaceAll("\\s+"," ").trim(),160);
            }else{
                if(val.toLowerCase(Locale.ROOT).startsWith("tel:"))
                    val=val.substring(4);
                int question=val.indexOf('?');
                if(question>=0)val=val.substring(0,question);
                if(val.isEmpty())continue;
                val=limited(val,100);
                if(ColleagueNumbers.key(val).length()<3)continue;
                String label=phoneLabel(opts);
                boolean duplicate=false;
                for(ColleagueNumbers.NumberEntry old:phones)
                    if(ColleagueNumbers.key(old.number).equals(ColleagueNumbers.key(val))){
                        duplicate=true;break;
                    }
                if(!duplicate){
                    if(phones.size()>=MAX_NUMBERS_PER_CARD)
                        throw new IllegalArgumentException("Too many phone entries");
                    phones.add(new ColleagueNumbers.NumberEntry(label,val));
                }
            }
        }
        if(inside)throw new IllegalArgumentException("Unfinished vCard");
        if(cards.isEmpty())throw new IllegalArgumentException("No phone numbers in vCard");
        return cards;
    }

    private static String phoneLabel(String[] opts){
        String labels=getParam(opts,"TYPE").toLowerCase(Locale.ROOT);
        if(labels.isEmpty()){
            for(int i=1;i<opts.length;i++){
                String t=opts[i].toLowerCase(Locale.ROOT);
                if(t.equals("cell")||t.equals("home")||t.equals("work")||t.equals("fax"))
                    labels=t;
            }
        }
        if(labels.contains("cell")||labels.contains("mobile"))return "موبایل";
        if(labels.contains("home"))return "منزل";
        if(labels.contains("work"))return "محل کار";
        if(labels.contains("fax"))return "فکس";
        if(labels.contains("main"))return "اصلی";
        return "شماره تماس";
    }

    private static String getParam(String[] opts,String name){
        for(int i=1;i<opts.length;i++){
            String t=opts[i].trim();
            int x=t.indexOf('=');
            if(x>0&&t.substring(0,x).equalsIgnoreCase(name))
                return t.substring(x+1).replace("\"","").trim();
        }
        return "";
    }
    private static String limited(String s,int length){
        return s.length()<=length?s:s.substring(0,length);
    }
    private static String unescape(String s){
        return s.replace("\\n","\n").replace("\\N","\n")
                .replace("\\, ",", ").replace("\\,",",").replace("\\;",";")
                .replace("\\\\","\\");
    }
    private static String decodeQuotedPrintable(String raw,String name){
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        // Decode quoted printable hex octets while preserving unescaped UTF-8.
        byte[] input=raw.getBytes(StandardCharsets.UTF_8);
        for(int i=0;i<input.length;i++){
            if(input[i]=='='&&i+2<input.length){
                int a=Character.digit((char)input[i+1],16);
                int b=Character.digit((char)input[i+2],16);
                if(a>=0&&b>=0){bytes.write((a<<4)|b);i+=2;continue;}
            }
            bytes.write(input[i]);
        }
        try{
            String encoding=name.isEmpty()?"UTF-8":name;
            return new String(bytes.toByteArray(),Charset.forName(encoding));
        }catch(Exception e){
            return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
        }
    }
}
