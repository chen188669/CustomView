package com.ssf.chen.customview.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.ssf.chen.customview.R;
import com.ssf.chen.customview.util.DensityUtil;

import java.text.DecimalFormat;

/**
 * Created by CHEN on 2018/8/30.
 * 旋转表盘
 */
public class WeightOvlView extends View {
    private Paint ovlPaint, textPaint, pointerPaint, weightPaint;
    private int width, height, radius;
    private int mSection = 200;
    private String[] weightNum;
    private float  angle, newAngle, lastWeight;
    public static final float MAX_VALUE = 200.0f;
    private DecimalFormat df = new DecimalFormat("##");
    private OnWeightListener listener;
    private int oriWeight;

    public WeightOvlView(Context context) {
        super(context);
    }

    public WeightOvlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WeightOvlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        ovlPaint = new Paint();
        ovlPaint.setAntiAlias(true);
        ovlPaint.setStrokeWidth(DensityUtil.dp2px(1));
        ovlPaint.setColor(getResources().getColor(R.color.head_text_gray));

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(DensityUtil.dp2px(10));
        textPaint.setColor(getResources().getColor(R.color.black));

        pointerPaint = new Paint();
        pointerPaint.setAntiAlias(true);
        pointerPaint.setStrokeWidth(DensityUtil.dp2px(2));
        pointerPaint.setColor(getResources().getColor(R.color.colorAccent));

        weightPaint = new Paint();
        weightPaint.setAntiAlias(true);
        weightPaint.setTextSize(DensityUtil.dp2px(26));
        weightPaint.setColor(getResources().getColor(R.color.black));

        weightNum = new String[20];
        for (int i = 0; i < 20; i++) {
            weightNum[i] = i * 10 + "";
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = getMeasuredWidth();
        height = getMeasuredHeight();
        radius = width / 2;
        oriWeight = (int) (width * 0.5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        init();
        if (lastWeight < 30) {
            lastWeight = 30;
        } else if (lastWeight > 200) {
            lastWeight = 200;
        }
        String text = df.format(lastWeight);
        Rect weightRect = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), weightRect);
        canvas.drawText(text, width / 2 - weightRect.width(), DensityUtil.dp2px(100), weightPaint);
        canvas.drawText("kg", width / 2 + weightRect.width() * 2f, DensityUtil.dp2px(100), textPaint);

        canvas.rotate(90f, width / 2, width / 2);
        canvas.drawLine(DensityUtil.dp2px(20), width / 2, DensityUtil.dp2px(70), width / 2, pointerPaint);
        canvas.drawCircle(DensityUtil.dp2px(75), width / 2, DensityUtil.dp2px(5), pointerPaint);
        canvas.save();
        Rect rect = new Rect();
        textPaint.getTextBounds(weightNum[0], 0, weightNum[0].length(), rect);
        canvas.rotate(newAngle, width / 2, width / 2);
        angle = 360f / mSection;

        for (int i = 0; i < mSection; i++) {
            if (i % 10 == 0) {
                canvas.drawLine(DensityUtil.dp2px(20), width / 2, DensityUtil.dp2px(40),
                        width / 2, ovlPaint);
                canvas.drawText(weightNum[(i / 10)].equals("0") ? "200" : weightNum[(i / 10)], DensityUtil.dp2px(45), width / 2 + rect.height() / 2, textPaint);
            } else if (i % 5 == 0) {
                canvas.drawLine(DensityUtil.dp2px(20), width / 2, DensityUtil.dp2px(35), width / 2, ovlPaint);
            } else {
                canvas.drawLine(DensityUtil.dp2px(20), width / 2, DensityUtil.dp2px(30), width / 2, ovlPaint);
            }
            canvas.rotate(angle, width / 2, width / 2);
        }
        canvas.restore();
    }


    private Point downPoint = new Point();
    private Point movePoint = new Point();
    private Point cenPoint = new Point();
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float downX = event.getX();
                float downY = event.getY();
                downPoint.set((int)downX,(int)downY);

                break;
            case MotionEvent.ACTION_MOVE:

                float moveX = event.getX();
                float moveY = event.getY();
                cenPoint.set(width / 2,width / 2);
                movePoint.set((int)moveX,(int)moveY);
                newAngle = angle(cenPoint,downPoint,movePoint);
                if (newAngle >= 0) {
                    lastWeight = MAX_VALUE * (1 - newAngle / 360.0f);
                } else {
                    lastWeight = MAX_VALUE * (Math.abs(newAngle) / 360.0f);
                }

                invalidate();
                if (listener != null) {
                    listener.onWeightSet(oriWeight, (int) lastWeight);
                }
                break;
        }
        return true;
    }

    public void setWeight(float weight) {
        lastWeight = weight;
        newAngle = (1 - weight / MAX_VALUE) * 360;
        invalidate();
        if (listener != null) {
            listener.onWeightSet(oriWeight, (int) weight);
        }
    }

    public String getWeight() {
        return df.format(lastWeight);
    }

    public void setOnWeightListener(OnWeightListener listener) {
        this.listener = listener;
    }

    public interface OnWeightListener {
        void onWeightSet(int orginalWeight, int weight);
    }

    /**
     * 根据坐标系中的3点确定夹角的方法（注意：夹角是有正负的）
     */
    public float angle(Point cen, Point first, Point second) {
        float dx1, dx2, dy1, dy2;

        dx1 = first.x - cen.x;
        dy1 = first.y - cen.y;
        dx2 = second.x - cen.x;
        dy2 = second.y - cen.y;

        // 计算三边的平方
        float ab2 = (second.x - first.x) * (second.x - first.x) + (second.y - first.y) * (second.y - first.y);
        float oa2 = dx1 * dx1 + dy1 * dy1;
        float ob2 = dx2 * dx2 + dy2 * dy2;

        // 根据两向量的叉乘来判断顺逆时针
        boolean isClockwise = ((first.x - cen.x) * (second.y - cen.y) - (first.y - cen.y) * (second.x - cen.x)) > 0;
        // 根据余弦定理计算旋转角的余弦值
        double cosDegree = (oa2 + ob2 - ab2) / (2 * Math.sqrt(oa2) * Math.sqrt(ob2));

        // 异常处理，因为算出来会有误差绝对值可能会超过一，所以需要处理一下
        if (cosDegree > 1) {
            cosDegree = 1;
        } else if (cosDegree < -1) {
            cosDegree = -1;
        }
        // 计算弧度
        double radian = Math.acos(cosDegree);

        // 计算旋转过的角度，顺时针为正，逆时针为负
        return (float) (isClockwise ? Math.toDegrees(radian) : -Math.toDegrees(radian));

    }
}