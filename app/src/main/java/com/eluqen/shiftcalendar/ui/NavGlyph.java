package com.eluqen.shiftcalendar.ui;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
public final class NavGlyph extends View {
 private final int type;
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private int color=0xff63758d;
 public NavGlyph(Context c,int type){super(c);this.type=type;}
 public void setColor(int color){this.color=color;invalidate();}
 private void line(Canvas c,float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,p);}
 private void rect(Canvas c,float x,float y,float xx,float yy,float radius){c.drawRoundRect(new RectF(x,y,xx,yy),radius,radius,p);}
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);c.save();c.scale(getWidth()/24f,getHeight()/24f);
  p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);
  p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
  switch(type){
   case 0:
    rect(c,3,4,21,21,2.6f);line(c,3.5f,9,20.5f,9);
    line(c,8,2.5f,8,6.5f);line(c,16,2.5f,16,6.5f);
    p.setStyle(Paint.Style.FILL);
    rect(c,6,12,9,15,1);rect(c,11,12,14,15,1);
    rect(c,16,12,19,15,1);rect(c,6,17,9,19,1);
    break;
   case 1:
    c.drawCircle(12,12,9,p);
    line(c,12,5.5f,12,12);line(c,12,12,16,14.5f);
    p.setStyle(Paint.Style.FILL);c.drawCircle(12,12,1.3f,p);
    c.drawCircle(12,4,1.15f,p);c.drawCircle(20,12,1.15f,p);
    c.drawCircle(12,20,1.15f,p);c.drawCircle(4,12,1.15f,p);
    break;
   case 2:
    rect(c,5,4.5f,19,22,2);rect(c,9,2.5f,15,6.5f,1.5f);
    line(c,8,10,16,10);line(c,8,13.5f,16,13.5f);
    line(c,8,17,10,19);line(c,10,19,15.5f,15.5f);
    break;
   case 3:
    c.drawCircle(12,12,6.5f,p);c.drawCircle(12,12,2.3f,p);
    for(int i=0;i<8;i++){double angle=i*Math.PI/4.0;
     float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
     line(c,12+dx*7.4f,12+dy*7.4f,12+dx*10.3f,12+dy*10.3f);}
    break;
   default:
    rect(c,3,3,10.5f,10.5f,1.8f);rect(c,13.5f,3,21,10.5f,1.8f);
    rect(c,3,13.5f,10.5f,21,1.8f);rect(c,13.5f,13.5f,21,21,1.8f);
  }
  c.restore();
 }
}
