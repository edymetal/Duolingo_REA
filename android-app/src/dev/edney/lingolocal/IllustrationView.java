package dev.edney.lingolocal;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Ilustrações vetoriais locais para o vocabulário das referências. */
final class IllustrationView extends View {
    private final String kind; private final Paint p=new Paint(3);
    IllustrationView(Context context,String kind) { super(context); this.kind=kind; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    private void color(int c) { p.setColor(c); }
    private void rect(Canvas c,float x,float y,float w,float h,float radius,int color) { color(color); c.drawRoundRect(x,y,x+w,y+h,radius,radius,p); }
    private void path(Canvas c,int color,float... points) { Path path=new Path();path.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);path.close();color(color);c.drawPath(path,p); }
    @Override protected void onDraw(Canvas c) {
        float size=Math.min(getWidth(),getHeight()); c.save();
        if(kind.equals("hat")){c.scale(getWidth()/100f,getHeight()/65f);c.translate(0,-15);}
        else {c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/100,size/100);}
        if(kind.equals("hat")) {
            Path hat=new Path();hat.moveTo(16,70);hat.lineTo(26,26);hat.cubicTo(28,15,35,17,40,20);hat.lineTo(55,27);hat.lineTo(70,20);hat.cubicTo(79,15,84,20,85,28);hat.lineTo(94,70);hat.close();color(0xfff24448);c.drawPath(hat,p);
            path(c,0xff9b4746,20,57,89,57,93,72,16,72); path(c,0xffffc72c,66,57,74,41,84,31,82,28,97,20,92,37,83,57);
            Path brim=new Path();brim.moveTo(3,72);brim.lineTo(98,72);brim.quadTo(96,78,90,78);brim.lineTo(11,78);brim.quadTo(5,78,3,72);color(0xfff24448);c.drawPath(brim,p);
        } else if(kind.equals("bag")) {
            rect(c,22,40,58,48,3,0xffd93a3e);rect(c,34,16,35,41,5,0xfff44349);rect(c,43,25,17,28,0,0xff131f24);rect(c,34,25,9,30,2,0xfff44349);rect(c,60,25,9,30,2,0xfff44349);
        } else if(kind.equals("hindi")) {
            rect(c,12,16,77,66,17,0xffd9d9d9);path(c,0xffd9d9d9,24,70,24,94,47,75);rect(c,21,25,59,46,10,0xffffffff);rect(c,21,25,59,15,5,0xffff9600);rect(c,21,56,59,15,4,0xff78c800);color(0xff1cb0f6);c.drawCircle(50,48,7,p);color(Color.WHITE);c.drawCircle(50,48,3.5f,p);
        } else {
            int fill=kind.equals("green")?0xff78c800:0xffe8f0f1;
            color(fill);c.drawOval(8,49,53,79,p);c.drawOval(13,66,27,92,p);c.drawCircle(8,58,3,p);c.drawCircle(43,90,3,p);
            c.save();c.rotate(28,61,51);rect(c,42,25,40,51,2,0xffdce1e3);rect(c,42,25,4,51,1,0xffaeb5b7);rect(c,78,25,4,51,1,0xffaeb5b7);rect(c,56,25,11,51,0,fill);
            color(0xffaeb5b7);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawLine(45,43,63,27,p);p.setStyle(Paint.Style.FILL);c.restore();color(Color.WHITE);c.drawOval(24,51,34,55,p);
        }
        c.restore();
    }
}
