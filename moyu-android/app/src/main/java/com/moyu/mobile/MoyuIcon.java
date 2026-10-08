package com.moyu.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Single-stroke, aspect-preserving monochrome icons for the secondary toolbox. */
public final class MoyuIcon extends View {
    private final String type;
    private final int color;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public MoyuIcon(Context ctx, String type, int color) {
        super(ctx);
        this.type=type;
        this.color=color;
        setContentDescription(null);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private void line(Canvas c,float a,float b,float x,float y){c.drawLine(a,b,x,y,paint);}
    private void rect(Canvas c,float l,float t,float r,float b,float round){
        c.drawRoundRect(l,t,r,b,round,round,paint);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        int save=canvas.save();
        canvas.scale(getWidth()/24f,getHeight()/24f);
        paint.setColor(color);paint.setStrokeWidth(1.8f);paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        switch(type){
            case "✎":
                line(canvas,5,19,8,14);line(canvas,8,14,17,5);line(canvas,17,5,20,8);
                line(canvas,20,8,10,18);line(canvas,10,18,5,19);
                line(canvas,5,19,5,16);
                break;
            case "☑":case "□":
                rect(canvas,3,4,21,21,3);
                if("☑".equals(type)){line(canvas,7,12,11,16);line(canvas,11,16,17,8);}
                break;
            case "▤":
                rect(canvas,5,3,19,21,2);line(canvas,8,9,16,9);line(canvas,8,13,16,13);line(canvas,8,17,14,17);
                break;
            case "▦":
                rect(canvas,3,3,10,10,1.5f);rect(canvas,14,3,21,10,1.5f);
                rect(canvas,3,14,10,21,1.5f);rect(canvas,14,14,21,21,1.5f);
                break;
            case "文":
                rect(canvas,3,4,21,21,2);line(canvas,7,9,16,9);line(canvas,12,6,12,15);
                line(canvas,8,12,16,19);line(canvas,16,12,8,19);
                break;
            case "⌕":case "O":case "A":case "G":case "M":
                rect(canvas,4,3,20,21,2);line(canvas,7,9,17,9);line(canvas,7,13,17,13);line(canvas,7,17,14,17);
                break;
            case "＝":
                rect(canvas,4,2,20,22,2);line(canvas,7,8,17,8);line(canvas,7,12,10,12);
                line(canvas,14,12,17,12);line(canvas,7,16,10,16);line(canvas,14,16,17,16);
                break;
            case "☁":
                Path cloud=new Path();cloud.moveTo(6,18);cloud.cubicTo(2,18,2,12,7,12);
                cloud.cubicTo(7,4,17,4,18,11);cloud.cubicTo(24,11,23,19,18,19);
                cloud.lineTo(6,19);canvas.drawPath(cloud,paint);break;
            case "日":
                rect(canvas,3,5,21,21,2);line(canvas,3,10,21,10);
                line(canvas,8,2,8,7);line(canvas,16,2,16,7);line(canvas,7,15,10,15);line(canvas,14,15,17,15);
                break;
            case "▣":
                canvas.drawCircle(9,9,4.5f,paint);line(canvas,12,12,20,20);line(canvas,17,17,20,14);
                break;
            case "↗":
                rect(canvas,3,8,17,21,2);line(canvas,11,13,21,3);line(canvas,14,3,21,3);line(canvas,21,3,21,10);
                break;
            default:
                canvas.drawCircle(12,12,8,paint);line(canvas,7,12,17,12);
        }
        canvas.restoreToCount(save);
    }
}
