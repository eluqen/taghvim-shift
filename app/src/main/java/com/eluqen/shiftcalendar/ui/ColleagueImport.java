package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.WeakHashMap;

/** Privacy-first: only user-selected contact data is read. No READ_CONTACTS. */
public final class ColleagueImport {
    private static final int REQUEST=3847;
    private static final String PICK_CONTACTS="android.provider.action.PICK_CONTACTS";
    private static final String REQUESTED_DATA="android.provider.extra.PICK_CONTACTS_REQUESTED_DATA_FIELDS";
    public interface Receiver { void received(List<ColleagueNumbers.NumberEntry> numbers); }

    private static final class Fields {
        final WeakReference<EditText> name;
        final Receiver receiver;
        final boolean session;
        Fields(EditText name,Receiver receiver,boolean session){
            this.name=new WeakReference<>(name);this.receiver=receiver;this.session=session;
        }
    }
    private static final WeakHashMap<Activity,Fields> pending=new WeakHashMap<>();
    private ColleagueImport(){}

    public static void attach(Activity activity,LinearLayout form,EditText name,Receiver receiver){
        Button select=Ui.tonalButton(activity,"انتخاب شماره از مخاطبین گوشی");
        form.addView(select,0,new LinearLayout.LayoutParams(-1,Ui.dp(activity,50)));
        select.setOnClickListener(v->launch(activity,name,receiver));
    }

    private static void launch(Activity a,EditText name,Receiver receiver) {
        // Android 17+ supports a one-time Contact Picker session exposing every
        // phone entry of the explicitly chosen contact without any global permission.
        boolean modern=Build.VERSION.SDK_INT>=37;
        Intent i;
        if(modern){
            i=new Intent(PICK_CONTACTS);
            ArrayList<String> wanted=new ArrayList<>();
            // Requesting Phone and StructuredName selects a contact record,
            // rather than a single phone row, on Android 17+.
            wanted.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            wanted.add(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
            i.putStringArrayListExtra(REQUESTED_DATA,wanted);
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,false);
        }else{
            // Older Android only grants one chosen Phone data URI. Repeat as needed.
            i=new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        }
        pending.put(a,new Fields(name,receiver,modern));
        try{
            a.startActivityForResult(i,REQUEST);
        }catch(RuntimeException ex){
            if(modern){
                try {
                    pending.put(a,new Fields(name,receiver,false));
                    a.startActivityForResult(new Intent(Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI),REQUEST);
                    return;
                }catch(RuntimeException ignored){}
            }
            pending.remove(a);
            Toast.makeText(a,"انتخاب مخاطب در این دستگاه در دسترس نیست",Toast.LENGTH_LONG).show();
        }
    }

    public static boolean result(Activity a,int request,int code,Intent result){
        if(request!=REQUEST)return false;
        Fields fields=pending.remove(a);
        if(code!=Activity.RESULT_OK||result==null||result.getData()==null||
           fields==null)return true;
        Uri selected=result.getData();
        if(!"content".equals(selected.getScheme()))return true;
        List<ColleagueNumbers.NumberEntry> numbers=new ArrayList<>();
        String contactName="";
        try(Cursor c=a.getContentResolver().query(selected,null,null,null,null)){
            if(c!=null){
                int numberCol=column(c,ContactsContract.CommonDataKinds.Phone.NUMBER);
                int displayCol=column(c,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int mimeCol=column(c,ContactsContract.Data.MIMETYPE);
                int typeCol=column(c,ContactsContract.CommonDataKinds.Phone.TYPE);
                int labelCol=column(c,ContactsContract.CommonDataKinds.Phone.LABEL);
                while(c.moveToNext()){
                    if(fields.session && mimeCol>=0 &&
                       !ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                          .equals(c.getString(mimeCol)))continue;
                    String number=numberCol<0?"":c.getString(numberCol);
                    if(number==null||number.trim().isEmpty())continue;
                    if(displayCol>=0 && contactName.isEmpty()) {
                        String display=c.getString(displayCol);
                        if(display!=null)contactName=display;
                    }
                    String label="شماره تماس";
                    if(typeCol>=0){
                        int type=c.getInt(typeCol);
                        String custom=labelCol>=0?c.getString(labelCol):null;
                        label=ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                            a.getResources(),type,custom).toString();
                    }
                    boolean duplicate=false;
                    for(ColleagueNumbers.NumberEntry e:numbers)
                        if(ColleagueNumbers.key(e.number).equals(
                            ColleagueNumbers.key(number)))duplicate=true;
                    if(!duplicate)numbers.add(new ColleagueNumbers.NumberEntry(label,number));
                }
            }
        }catch(SecurityException ex){
            android.util.Log.w("ELUQEN_CONTACTS","Picker URI access denied",ex);
            Toast.makeText(a,"اجازه خواندن شماره انتخاب‌شده داده نشد",Toast.LENGTH_LONG).show();
            return true;
        }catch(Exception ex){
            android.util.Log.w("ELUQEN_CONTACTS","Selected contact import failed",ex);
            Toast.makeText(a,"خواندن شماره انتخاب‌شده ممکن نبود",Toast.LENGTH_LONG).show();
            return true;
        }
        if(numbers.isEmpty()){
            Toast.makeText(a,"شماره‌ای از انتخاب شما دریافت نشد",Toast.LENGTH_LONG).show();
            return true;
        }
        EditText name=fields.name.get();
        if(name!=null && name.getText().toString().trim().isEmpty()
            && !contactName.trim().isEmpty())name.setText(contactName);
        fields.receiver.received(numbers);
        Toast.makeText(a,numbers.size()+" شماره دریافت شد؛ برای ثبت، ذخیره را بزنید",
            Toast.LENGTH_SHORT).show();
        return true;
    }

    public static void cancel(Activity activity) { pending.remove(activity); }

    private static int column(Cursor c,String name) {
        return c.getColumnIndex(name);
    }
}
