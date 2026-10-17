package com.eluqen.shiftcalendar.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.Colleague;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

public final class ColleaguesActivity extends BaseActivity {
    private AppDatabase database;
    private LinearLayout list;
    private static final int REQUEST_VCARD_FILE=3850;
    private interface CardReceiver { void accept(ColleagueVCard.Contact contact); }
    private CardReceiver currentVCardReceiver;

    private static final class PhoneRow {
        final LinearLayout root;
        final EditText label, number;
        PhoneRow(LinearLayout root,EditText label,EditText number){
            this.root=root;this.label=label;this.number=number;
        }
    }

    @Override protected void onActivityResult(int request,int result,Intent data) {
        if(request==REQUEST_VCARD_FILE){
            if(result==RESULT_OK && data!=null && data.getData()!=null)
                readVCard(data.getData(),currentVCardReceiver);
            return;
        }
        if(ColleagueImport.result(this,request,result,data)) return;
        super.onActivityResult(request,result,data);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        database=AppDatabase.get(this);
        buildUi();
        if(state==null && getIntent()!=null &&
            ColleagueVCardShareActivity.ACTION_IMPORT.equals(getIntent().getAction())){
            Uri shared=getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            getIntent().setAction(null);
            if(shared!=null && "content".equalsIgnoreCase(shared.getScheme()))
                edit(null,shared);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private int dp(float x){return Ui.dp(this,x);}

    private void buildUi() {
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(Ui.SURFACE);
        root.addView(Ui.toolbar(this,"هم‌شیفت‌ها",true));
        Button add=Ui.button(this,"+  افزودن همکار");
        add.setOnClickListener(v->edit(null));
        root.addView(add,Ui.margins(this,-1,dp(50),14,12,14,6));
        ScrollView scroll=new ScrollView(this);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        list.setPadding(dp(12),dp(5),dp(12),dp(24));
        scroll.addView(list);
        setContentView(root);
    }

    private void refresh() {
        if(list==null)return;
        list.removeAllViews();
        List<Colleague> colleagues=database.colleagues();
        if(colleagues.isEmpty()){
            TextView empty=Ui.text(this,"هنوز همکاری ثبت نشده است.",15,Ui.MUTED);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty,new LinearLayout.LayoutParams(-1,dp(130)));
            return;
        }
        for(Colleague c:colleagues)
            list.addView(card(c),Ui.margins(this,-1,-2,0,0,0,10));
    }

    private View card(Colleague c) {
        LinearLayout card=Ui.card(this);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.addView(Ui.title(this,c.name,18));
        String description=joinNonEmpty("  •  ",c.team,c.role);
        if(!description.isEmpty())card.addView(Ui.text(this,description,14,Ui.MUTED));
        List<ColleagueNumbers.NumberEntry> numbers=numbers(c);
        if(!numbers.isEmpty()){
            TextView summary=Ui.text(this,
                numbers.get(0).number+(numbers.size()>1?
                "   •   "+(numbers.size()-1)+" شماره دیگر":""),14,Ui.MUTED);
            summary.setMaxLines(2);
            card.addView(summary,Ui.margins(this,-1,-2,0,7,0,0));
        }
        if(c.info!=null&&!c.info.trim().isEmpty()){
            TextView info=Ui.text(this,c.info,13,Ui.MUTED);
            info.setMaxLines(2);
            info.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(info);
        }
        LinearLayout actions=new LinearLayout(this);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button call=Ui.tonalButton(this,"تماس");
        call.setEnabled(!numbers.isEmpty());
        call.setOnClickListener(v->chooseNumber(c,false));
        Button sms=Ui.tonalButton(this,"پیامک");
        sms.setEnabled(!numbers.isEmpty());
        sms.setOnClickListener(v->chooseNumber(c,true));
        Button edit=Ui.tonalButton(this,"ویرایش");
        edit.setOnClickListener(v->edit(c));
        actions.addView(call,actionParams());
        actions.addView(sms,actionParams());
        actions.addView(edit,actionParams());
        card.addView(actions,Ui.margins(this,-1,dp(48),0,10,0,0));
        card.setOnLongClickListener(v->{confirmDelete(c);return true;});
        return card;
    }

    private List<ColleagueNumbers.NumberEntry> numbers(Colleague c){
        List<ColleagueNumbers.NumberEntry> all=new ArrayList<>();
        if(!c.mobile1.trim().isEmpty())
            all.add(new ColleagueNumbers.NumberEntry("موبایل اول",c.mobile1));
        if(!c.mobile2.trim().isEmpty())
            all.add(new ColleagueNumbers.NumberEntry("موبایل دوم",c.mobile2));
        if(!c.phone.trim().isEmpty())
            all.add(new ColleagueNumbers.NumberEntry("تلفن ثابت",c.phone));
        all.addAll(ColleagueNumbers.entries(c.extraPhonesJson));
        return all;
    }

    private void chooseNumber(Colleague colleague,boolean sendSms){
        List<ColleagueNumbers.NumberEntry> all=numbers(colleague);
        if(all.isEmpty())return;
        if(all.size()==1){openNumber(all.get(0).number,sendSms);return;}
        String[] labels=new String[all.size()];
        for(int i=0;i<all.size();i++)
            labels[i]=all.get(i).label+"   •   "+all.get(i).number;
        new AlertDialog.Builder(this)
            .setCustomTitle(Ui.rtlDialogTitle(this,
                sendSms?"انتخاب شماره برای پیامک":"انتخاب شماره برای تماس"))
            .setItems(labels,(dialog,which)->openNumber(all.get(which).number,sendSms))
            .setNegativeButton("انصراف",null).show();
    }

    private void openNumber(String number,boolean message){
        if(number==null||number.trim().isEmpty())return;
        try{
            String scheme=message?"smsto:":"tel:";
            startActivity(new Intent(message?Intent.ACTION_SENDTO:Intent.ACTION_DIAL,
                Uri.parse(scheme+Uri.encode(number.trim()))));
        }catch(Exception e){
            Ui.toast(this,message?"برنامه پیامک پیدا نشد":"برنامه تماس پیدا نشد");
        }
    }

    private void edit(Colleague existing) { edit(existing,null); }

    private void edit(Colleague existing,Uri sharedVCard) {
        Colleague draft=existing==null?new Colleague():existing;
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout form=new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        form.setPadding(dp(15),dp(8),dp(15),dp(17));
        scroll.addView(form);

        section(form,"اطلاعات همکار");
        EditText name=field(form,"نام و نام خانوادگی",draft.name);
        EditText team=field(form,"گروه یا شیفت (اختیاری)",draft.team);
        EditText role=field(form,"سمت (اختیاری)",draft.role);

        section(form,"شماره‌های تماس");
        String privacyHint=android.os.Build.VERSION.SDK_INT>=37
            ? "شماره‌های مخاطب انتخاب‌شده وارد می‌شوند؛ دسترسی کلی به دفترچه مخاطبین لازم نیست."
            : "در این نسخه اندروید، هر بار یک شماره انتخاب می‌شود. برای افزودن بقیه، انتخاب را تکرار کنید؛ مجوز مخاطبین لازم نیست.";
        TextView privacy=Ui.text(this,privacyHint,12.5f,Ui.MUTED);
        privacy.setLineSpacing(dp(2),1.05f);
        form.addView(privacy,Ui.margins(this,-1,-2,0,2,0,7));

        LinearLayout phoneArea=new LinearLayout(this);
        phoneArea.setOrientation(LinearLayout.VERTICAL);
        phoneArea.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        form.addView(phoneArea);
        EditText mobile1=phoneField(phoneArea,"موبایل اول",draft.mobile1);
        EditText mobile2=phoneField(phoneArea,"موبایل دوم",draft.mobile2);
        EditText phone=phoneField(phoneArea,"تلفن ثابت",draft.phone);

        LinearLayout extras=new LinearLayout(this);
        extras.setOrientation(LinearLayout.VERTICAL);
        extras.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        phoneArea.addView(extras);
        List<PhoneRow> additional=new ArrayList<>();
        for(ColleagueNumbers.NumberEntry entry:ColleagueNumbers.entries(draft.extraPhonesJson))
            addPhoneRow(extras,additional,entry.label,entry.number);

        Button addNumber=Ui.tonalButton(this,"+  افزودن شماره دلخواه");
        phoneArea.addView(addNumber,Ui.margins(this,-1,dp(48),0,6,0,5));
        addNumber.setOnClickListener(v->addPhoneRow(extras,additional,"شماره دیگر",""));

        ColleagueImport.attach(this,phoneArea,name,selected->{
            List<String> saved=new ArrayList<>();
            for(EditText f:new EditText[]{mobile1,mobile2,phone})
                saved.add(text(f));
            for(PhoneRow row:additional)saved.add(text(row.number));
            for(ColleagueNumbers.NumberEntry entry:selected){
                if(ColleagueNumbers.has(saved,entry.number))continue;
                if(text(mobile1).isEmpty())mobile1.setText(entry.number);
                else if(text(mobile2).isEmpty())mobile2.setText(entry.number);
                else if(text(phone).isEmpty())phone.setText(entry.number);
                else addPhoneRow(extras,additional,entry.label,entry.number);
                saved.add(entry.number);
            }
        });

        Button fromVCard=Ui.tonalButton(this,"واردکردن چند شماره از فایل مخاطب (VCF)");
        form.addView(fromVCard,Ui.margins(this,-1,dp(52),0,7,0,3));
        TextView vcfHint=Ui.text(this,
            "برای واردکردن همه شماره‌ها، فایل VCF را انتخاب کنید یا از برنامه مخاطبین به ELUQEN بفرستید؛ بدون اجازه دسترسی به دفترچه تلفن.",
            12.5f,Ui.MUTED);
        form.addView(vcfHint,Ui.margins(this,-1,-2,0,3,0,8));
        currentVCardReceiver=selectedCard->{
            if(text(name).isEmpty() && !selectedCard.name.isEmpty())
                name.setText(selectedCard.name);
            List<String> saved=new ArrayList<>();
            for(EditText f:new EditText[]{mobile1,mobile2,phone})
                saved.add(text(f));
            for(PhoneRow row:additional)saved.add(text(row.number));
            int count=0;
            for(ColleagueNumbers.NumberEntry item:selectedCard.numbers){
                if(ColleagueNumbers.has(saved,item.number))continue;
                addPhoneRow(extras,additional,item.label,item.number);
                saved.add(item.number);
                count++;
            }
            Ui.toast(this,count+" شماره جدید به فرم افزوده شد؛ برای ثبت، ذخیره کنید");
        };
        fromVCard.setOnClickListener(v->openVCardPicker());

        section(form,"یادداشت (اختیاری)");
        EditText note=field(form,"توضیحات درباره همکار",draft.info);
        note.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setSingleLine(false);
        note.setGravity(Gravity.TOP|Gravity.RIGHT);
        note.setMinLines(3);
        note.setMinHeight(dp(96));

        AlertDialog dialog=new AlertDialog.Builder(this)
            .setCustomTitle(Ui.rtlDialogTitle(this,
                existing==null?"افزودن همکار":"ویرایش همکار"))
            .setView(scroll)
            .setPositiveButton("ذخیره",null)
            .setNegativeButton("انصراف",null)
            .setNeutralButton(existing==null?null:"حذف",null)
            .create();
        dialog.setOnDismissListener(d->{
            ColleagueImport.cancel(this);
            currentVCardReceiver=null;
        });
        dialog.setOnShowListener(d->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                String person=text(name);
                if(person.isEmpty()){
                    name.setError("نام همکار الزامی است");
                    name.requestFocus();
                    return;
                }
                List<ColleagueNumbers.NumberEntry> added=new ArrayList<>();
                for(PhoneRow row:additional)
                    if(!text(row.number).isEmpty())added.add(
                        new ColleagueNumbers.NumberEntry(text(row.label),text(row.number)));
                try{
                    draft.name=person;
                    draft.team=text(team);
                    draft.role=text(role);
                    draft.mobile1=text(mobile1);
                    draft.mobile2=text(mobile2);
                    draft.phone=text(phone);
                    draft.info=text(note);
                    draft.extraPhonesJson=ColleagueNumbers.encode(added);
                    database.saveColleague(draft);
                    dialog.dismiss();
                    refresh();
                }catch(RuntimeException err){
                    android.util.Log.e("ELUQEN_COLLEAGUES","Could not save colleague",err);
                    Ui.toast(this,"ذخیره همکار انجام نشد");
                }
            });
            if(existing!=null)dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(v->{dialog.dismiss();confirmDelete(existing);});
        });
        dialog.show();
        if(sharedVCard!=null) readVCard(sharedVCard,currentVCardReceiver);
    }

    private void openVCardPicker(){
        Intent browse=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        browse.addCategory(Intent.CATEGORY_OPENABLE);
        browse.setType("*/*");
        browse.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{
            "text/x-vcard","text/vcard","text/directory",
            "application/vcard","application/x-vcard","application/octet-stream"});
        browse.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{
            startActivityForResult(browse,REQUEST_VCARD_FILE);
        }catch(RuntimeException e){
            Ui.toast(this,"فایل‌گزین این گوشی در دسترس نیست");
        }
    }

    private void readVCard(Uri selected,CardReceiver receiver){
        if(receiver==null||selected==null||
                !"content".equalsIgnoreCase(selected.getScheme()))return;
        new Thread(()->{
            try(InputStream input=getContentResolver().openInputStream(selected)){
                if(input==null)throw new IllegalArgumentException("No document");
                ByteArrayOutputStream buffer=new ByteArrayOutputStream();
                byte[] piece=new byte[8192];
                int count;
                while((count=input.read(piece))!=-1){
                    if(buffer.size()+count>ColleagueVCard.MAX_BYTES)
                        throw new IllegalArgumentException("File exceeds safe import limit");
                    buffer.write(piece,0,count);
                }
                List<ColleagueVCard.Contact> contacts=ColleagueVCard.parse(buffer.toByteArray());
                runOnUiThread(()->{
                    if(isFinishing()||currentVCardReceiver!=receiver)return;
                    chooseVCardContact(contacts,receiver);
                });
            }catch(Exception error){
                android.util.Log.w("ELUQEN_VCARD","User-selected VCF import failed",error);
                runOnUiThread(()->{
                    if(!isFinishing() && currentVCardReceiver==receiver)
                        Ui.toast(this,"این فایل VCF قابل خواندن نیست یا شماره‌ای ندارد");
                });
            }
        },"EluqenSelectedVCardReader").start();
    }

    private void chooseVCardContact(List<ColleagueVCard.Contact> contacts,
                                    CardReceiver receiver){
        if(contacts.size()==1){
            confirmVCardNumbers(contacts.get(0),receiver);
            return;
        }
        String[] options=new String[contacts.size()];
        for(int i=0;i<contacts.size();i++){
            ColleagueVCard.Contact item=contacts.get(i);
            options[i]=item.name+"   •   "+item.numbers.size()+" شماره";
        }
        new AlertDialog.Builder(this)
            .setCustomTitle(Ui.rtlDialogTitle(this,"انتخاب یک مخاطب از فایل"))
            .setItems(options,(dialog,which)->
                confirmVCardNumbers(contacts.get(which),receiver))
            .setNegativeButton("انصراف",null).show();
    }

    private void confirmVCardNumbers(ColleagueVCard.Contact contact,
                                     CardReceiver receiver){
        List<ColleagueNumbers.NumberEntry> numbers=contact.numbers;
        String[] options=new String[numbers.size()];
        boolean[] selected=new boolean[numbers.size()];
        for(int i=0;i<numbers.size();i++){
            ColleagueNumbers.NumberEntry item=numbers.get(i);
            options[i]=item.label+"   •   "+item.number;
            selected[i]=true;
        }
        new AlertDialog.Builder(this)
            .setCustomTitle(Ui.rtlDialogTitle(this,"شماره‌های "+contact.name))
            .setMultiChoiceItems(options,selected,(dialog,position,enabled)->
                selected[position]=enabled)
            .setPositiveButton("افزودن شماره‌های انتخاب‌شده",(dialog,which)->{
                if(currentVCardReceiver!=receiver)return;
                List<ColleagueNumbers.NumberEntry> picked=new ArrayList<>();
                for(int i=0;i<numbers.size();i++)
                    if(selected[i])picked.add(numbers.get(i));
                if(picked.isEmpty()){
                    Ui.toast(this,"شماره‌ای انتخاب نشده است");
                    return;
                }
                receiver.accept(new ColleagueVCard.Contact(contact.name,picked));
            })
            .setNegativeButton("انصراف",null).show();
    }

    private void section(LinearLayout form,String heading){
        TextView text=Ui.title(this,heading,16);
        text.setPadding(0,dp(14),0,dp(7));
        form.addView(text);
    }

    private EditText field(LinearLayout box,String hint,String value){
        EditText field=Ui.input(this,hint);
        field.setText(value==null?"":value);
        box.addView(field,Ui.margins(this,-1,dp(51),0,4,0,5));
        return field;
    }

    private EditText phoneField(LinearLayout box,String hint,String value){
        EditText input=field(box,hint,value);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setTextDirection(View.TEXT_DIRECTION_LTR);
        input.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        return input;
    }

    private void addPhoneRow(LinearLayout container,List<PhoneRow> rows,
                             String label,String number){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(dp(9),dp(7),dp(9),dp(7));
        row.setBackground(Ui.roundedStroke(Ui.SURFACE_ALT,Ui.BORDER,12,this));
        EditText labelField=field(row,"عنوان شماره (مثلاً محل کار)",label);
        EditText numberField=phoneField(row,"شماره تماس",number);
        Button remove=Ui.tonalButton(this,"حذف این شماره");
        remove.setTextColor(Ui.HOLIDAY);
        row.addView(remove,new LinearLayout.LayoutParams(-1,dp(41)));
        PhoneRow entry=new PhoneRow(row,labelField,numberField);
        rows.add(entry);
        container.addView(row,Ui.margins(this,-1,-2,0,4,0,7));
        remove.setOnClickListener(v->{rows.remove(entry);container.removeView(row);});
    }

    private void confirmDelete(Colleague colleague) {
        new AlertDialog.Builder(this)
            .setCustomTitle(Ui.rtlDialogTitle(this,"حذف همکار"))
            .setMessage("«"+colleague.name+"» از فهرست حذف شود؟")
            .setPositiveButton("حذف",(dialog,which)->{
                database.deleteColleague(colleague.f0id);
                refresh();
            })
            .setNegativeButton("انصراف",null).show();
    }

    private LinearLayout.LayoutParams actionParams(){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);
        p.setMargins(dp(2),0,dp(2),0);
        return p;
    }

    private static String text(EditText field){
        return field.getText().toString().trim();
    }

    private static String joinNonEmpty(String separator,String... values){
        StringBuilder b=new StringBuilder();
        for(String value:values){
            if(value==null||value.trim().isEmpty())continue;
            if(b.length()>0)b.append(separator);
            b.append(value.trim());
        }
        return b.toString();
    }
}
