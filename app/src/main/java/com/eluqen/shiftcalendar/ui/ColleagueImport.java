package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
public final class ColleagueImport {
 private static final int REQUEST=3847;
 private static final class Fields {
  final WeakReference<EditText> name,phone;
  Fields(EditText n,EditText p){name=new WeakReference<>(n);phone=new WeakReference<>(p);}
 }
 private static final WeakHashMap<Activity,Fields> pending=new WeakHashMap<>();
 private ColleagueImport(){}
 public static void attach(Activity a,LinearLayout form,EditText name,EditText phone){
  Button select=new Button(a);select.setText("انتخاب از مخاطبین گوشی");select.setAllCaps(false);
  select.setTextColor(0xff23527c);
  form.addView(select,0,new LinearLayout.LayoutParams(-1,(int)(48*a.getResources().getDisplayMetrics().density)));
  select.setOnClickListener(v->{
   pending.put(a,new Fields(name,phone));
   try{
    Intent intent=new Intent(Intent.ACTION_PICK,ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
    a.startActivityForResult(intent,REQUEST);
   }catch(Exception e){Toast.makeText(a,"انتخاب مخاطب در این دستگاه در دسترس نیست",Toast.LENGTH_LONG).show();}
  });
 }
 public static boolean result(Activity a,int request,int result,Intent data){
  if(request!=REQUEST)return false;
  if(result!=Activity.RESULT_OK||data==null||data.getData()==null)return true;
  Fields fields=pending.get(a);if(fields==null)return true;
  String[] projection={ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER};
  try(Cursor c=a.getContentResolver().query(data.getData(),projection,null,null,null)){
   if(c!=null&&c.moveToFirst()){
    String name=c.getString(0),number=c.getString(1);
    EditText n=fields.name.get(),p=fields.phone.get();
    if(n!=null&&name!=null)n.setText(name);
    if(p!=null&&number!=null)p.setText(number);
    Toast.makeText(a,"اطلاعات وارد شد؛ فقط با زدن ذخیره ثبت می‌شود",Toast.LENGTH_LONG).show();
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_CONTACTS","Contact pick failed",e);Toast.makeText(a,"خواندن مخاطب موفق نبود",Toast.LENGTH_LONG).show();}
  pending.remove(a);return true;
 }
}
