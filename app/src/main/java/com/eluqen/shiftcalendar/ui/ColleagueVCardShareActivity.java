package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Restricted ACTION_SEND entry for a user-shared .vcf file.
 * Never accesses the contact provider or persists a URI permission.
 */
public final class ColleagueVCardShareActivity extends Activity {
    public static final String ACTION_IMPORT =
            "com.eluqen.shiftcalendar.action.IMPORT_SELECTED_VCARD";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent original=getIntent();
        if(original==null || !Intent.ACTION_SEND.equals(original.getAction())){
            finish();return;
        }
        Uri uri;
        try {
            uri=original.getParcelableExtra(Intent.EXTRA_STREAM);
        }catch(RuntimeException e){finish();return;}
        if(uri==null||!"content".equalsIgnoreCase(uri.getScheme())){
            finish();return;
        }
        Intent forward=new Intent(this,ColleaguesActivity.class);
        forward.setAction(ACTION_IMPORT);
        forward.putExtra(Intent.EXTRA_STREAM,uri);
        forward.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ClipData clip=original.getClipData();
        if(clip==null)clip=ClipData.newRawUri("Selected contact vCard",uri);
        forward.setClipData(clip);
        startActivity(forward);
        finish();
    }
}
