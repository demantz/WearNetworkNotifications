package com.mantz_it.wearnetworknotifications;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by dennis on 4/13/17.
 */

public class PlotSurface extends SurfaceView implements SurfaceHolder.Callback {
    private static final String LOGTAG = "PlotSurface";

    private Paint wifiPaint = null;
    private Paint cellularPaint = null;
    private Paint infoPaint = null;
    private Paint grayPaint = null;

    private int width;
    private int height;
    private int zoom = 1;
    private boolean ambient = false;

    public PlotSurface(Context context) {
        super(context);
        init();
    }

    public PlotSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlotSurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PlotSurface(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        wifiPaint = new Paint();
        wifiPaint.setColor(Color.MAGENTA);
        wifiPaint.setStrokeWidth(4);
        cellularPaint = new Paint();
        cellularPaint.setColor(Color.CYAN);
        cellularPaint.setStrokeWidth(4);
        infoPaint = new Paint();
        infoPaint.setColor(Color.LTGRAY);
        infoPaint.setTextSize((float) (getGridSize()*0.1));
        grayPaint = new Paint();
        grayPaint.setColor(Color.DKGRAY);

        // Add a Callback to get informed when the dimensions of the SurfaceView changes:
        this.getHolder().addCallback(this);
    }

    public void setAmbient(boolean ambient) {
        this.ambient = ambient;
    }

//------------------- <SurfaceHolder.Callback> ------------------------------//
    /**
     * SurfaceHolder.Callback function. Gets called when the surface view is created.
     * We do all the work in surfaceChanged()...
     *
     * @param holder	reference to the surface holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    /**
     * SurfaceHolder.Callback function. This is called every time the dimension changes
     * (and after the SurfaceView is created).
     *
     * @param holder	reference to the surface holder
     * @param format
     * @param width		current width of the surface view
     * @param height	current height of the surface view
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * SurfaceHolder.Callback function. Gets called before the surface view is destroyed
     *
     * @param holder	reference to the surface holder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
//------------------- </SurfaceHolder.Callback> -----------------------------//

    /**
     * Returns the height/width of the frequency/power grid in px
     *
     * @return size of the grid (frequency grid height / power grid width) in px
     */
    private int getGridSize() {
        float xdpi = getResources().getDisplayMetrics().xdpi;
        float xpixel = getResources().getDisplayMetrics().widthPixels;
        float xinch = xpixel / xdpi;

        if(xinch < 30)
            return (int) (75 * xdpi/200);		// Smartphone / Tablet / Computer screen
        else
            return (int) (400 * xdpi/200);		// TV screen
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            // zoom in one step more:
            zoom += 2;
            if (zoom > 10)
                zoom = 1;
        }
        return super.onTouchEvent(event);
    }

    public void draw(long[] timestamps, int[] wifiData, int[] cellularData) {
        Canvas c = null;
        try {
            c = this.getHolder().lockCanvas();

            synchronized (this.getHolder()) {
                if(c != null) {
                    // Draw all the components
                    drawGrid(c);
                    drawPlot(c, timestamps, wifiData, wifiPaint);
                    drawPlot(c, timestamps, cellularData, cellularPaint);
                    drawInfo(c, infoPaint);
                } else
                    Log.d(LOGTAG, "draw: Canvas is null.");
            }
        } catch (Exception e)
        {
            Log.e(LOGTAG,"draw: Error while drawing on the canvas. Stop!");
            e.printStackTrace();
        } finally {
            if (c != null) {
                this.getHolder().unlockCanvasAndPost(c);
            }
        }
    }

    private void drawGrid(Canvas c) {
        c.drawColor(Color.BLACK);
        if(!ambient) {
            c.drawRect(0, height * 0.2f, width, height * 0.4f, grayPaint);
            c.drawRect(0, height * 0.6f, width, height * 0.8f, grayPaint);
        }
        c.drawLine(0, height*0.001f, width, height*0.001f, infoPaint);
        c.drawLine(width*0.999f, 0, width*0.999f, height, infoPaint);
        c.drawLine(width*0.001f, 0, width*0.001f, height, infoPaint);
        c.drawLine(0, height*0.999f, width, height*0.999f, infoPaint);
    }

    private void drawPlot(Canvas c, long[] timestamps, int[] vals, Paint paint) {
        int timeSpan = 1000*60*5;
        long timeOffset = System.currentTimeMillis() - timeSpan;
        for(int i = 0; i < vals.length; i++) {
            long relTimestamp = timestamps[i] - timeOffset;
            float y = height - height/100f * vals[i];
            if(y < 0)
                y = 0;
            float xStart = width/(float)timeSpan * relTimestamp;
            float xEnd = width/(float)timeSpan * (relTimestamp + 10000);
            c.drawLine(xStart, y, xEnd, y, paint);
        }
    }

    private void drawInfo(Canvas c, Paint paint) {
        Rect bounds = new Rect();

        String text = "5 min ago";
        paint.getTextBounds(text, 0, text.length(), bounds);
        c.drawText(text, width*0.02f, height*0.98f - bounds.height(), paint);

        text = "now";
        paint.getTextBounds(text, 0, text.length(), bounds);
        c.drawText(text, width*0.98f - bounds.width(), height*0.98f - bounds.height(), paint);
    }

}
