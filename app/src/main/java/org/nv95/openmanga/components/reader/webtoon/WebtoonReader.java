package org.nv95.openmanga.components.reader.webtoon;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ScaleGestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.nv95.openmanga.components.reader.MangaReader;
import org.nv95.openmanga.components.reader.OnOverScrollListener;
import org.nv95.openmanga.components.reader.PageLoadListener;
import org.nv95.openmanga.components.reader.PageLoader;
import org.nv95.openmanga.components.reader.PageWrapper;
import org.nv95.openmanga.components.reader.recyclerpager.RecyclerViewPager;
import org.nv95.openmanga.items.MangaPage;
import org.nv95.openmanga.utils.InternalLinkMovement;

import java.util.List;
import java.util.Vector;

/**
 * Created by admin on 01.08.17.
 */

public class WebtoonReader extends SurfaceView implements MangaReader, SurfaceHolder.Callback,
        PageLoadListener, Handler.Callback, ChangesListener {

    private static final int MSG_PAGE_CHANGED = 1;
    private static final int MSG_OVERSCROLL_START = 2;
    private static final int MSG_OVERSCROLL_END = 3;

    private ImagesPool mPool;
    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleDetector;
    private DrawThread mDrawThread;
    private int mCurrentPage;
    private boolean mTapNavs;
    private final ScrollController mScrollCtrl;
    private final Vector<RecyclerViewPager.OnPageChangedListener> mPageChangeListeners;
    private final Handler handler = new Handler(this);

    public WebtoonReader(Context context) {
        this(context, null, 0);
    }

    public WebtoonReader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WebtoonReader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPageChangeListeners = new Vector<>();
        mGestureDetector = new GestureDetector(context, new GestureListener());
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        ScaleGestureDetectorCompat.setQuickScaleEnabled(mScaleDetector, false);
        getHolder().addCallback(this);
        mScrollCtrl = new ScrollController();
    }

    @Override
    public void applyConfig(boolean vertical, boolean reverse, boolean sticky) {

    }

    @Override
    public boolean scrollToNext(boolean animate) {
        int h = mScrollCtrl.viewportHeight();
        if (animate && h > 0) {
            mScrollCtrl.smoothScrollBy(0, -h * 0.9f);
            return true;
        } else if (mCurrentPage == getItemCount() - 1) {
            return false;
        } else {
            scrollToPosition(mCurrentPage + 1);
            return true;
        }
    }

    @Override
    public boolean scrollToPrevious(boolean animate) {
        int h = mScrollCtrl.viewportHeight();
        if (animate && h > 0) {
            mScrollCtrl.smoothScrollBy(0, h * 0.9f);
            return true;
        } else if (mCurrentPage == 0) {
            return false;
        } else {
            scrollToPosition(mCurrentPage - 1);
            return true;
        }
    }

    @Override
    public int getCurrentPosition() {
        return mCurrentPage;
    }

    @Override
    public void scrollToPosition(int position) {
        mCurrentPage = position;
        mScrollCtrl.setOffsetY(0);
        notifyDataSetChanged();
    }

    @Override
    public void setTapNavs(boolean val) {
        mTapNavs = val;
    }

    @Override
    public void addOnPageChangedListener(RecyclerViewPager.OnPageChangedListener listener) {
        mPageChangeListeners.add(listener);
    }

    @Override
    public void setOnOverScrollListener(OnOverScrollListener listener) {

    }

    @Override
    public boolean isReversed() {
        return false;
    }

    @Override
    public int getItemCount() {
        return getLoader().getWrappersList().size();
    }

    @Override
    public void initAdapter(Context context, InternalLinkMovement.OnLinkClickListener linkListener) {
        mPool = new ImagesPool(context, this);
        mPool.getLoader().addListener(this);
    }

    @Override
    public PageLoader getLoader() {
        return mPool.getLoader();
    }

    @Override
    public void notifyDataSetChanged() {
        /*try {
            mDrawThread.notify();
        } catch (Exception ignored) {

        }*/
        if (mDrawThread != null) {
            mDrawThread.mChanged = true;
        }
    }

    @Override
    public PageWrapper getItem(int position) {
        return mPool.getLoader().getWrappersList().get(position);
    }

    @Override
    public void setScaleMode(int scaleMode) {

    }

    @Override
    public void reload(int position) {
        notifyDataSetChanged();
    }

    @Override
    public void setPages(List<MangaPage> mangaPages) {
        getLoader().setPages(mangaPages);
    }

    @Override
    public void finish() {
        getLoader().cancelAll();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //mScrollCtrl.setViewportWidth(surfaceHolder.getSurfaceFrame().width());
        mDrawThread = new DrawThread(getHolder());
        mDrawThread.setRunning(true);
        mDrawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        mScrollCtrl.setViewportWidth(width);
        mScrollCtrl.setViewportHeight(height);
        notifyDataSetChanged();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // завершаем работу потока
        mDrawThread.setRunning(false);
        notifyDataSetChanged();
        try {
            mDrawThread.join();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void onLoadingStarted(PageWrapper page, boolean shadow) {

    }

    @Override
    public void onProgressUpdated(PageWrapper page, boolean shadow, int percent) {

    }

    @Override
    public void onLoadingComplete(PageWrapper page, boolean shadow) {
        notifyDataSetChanged();
    }

    @Override
    public void onLoadingFail(PageWrapper page, boolean shadow) {

    }

    @Override
    public void onLoadingCancelled(PageWrapper page, boolean shadow) {

    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_PAGE_CHANGED:
                int oldPage = mCurrentPage;
                mCurrentPage = message.arg1;
                mScrollCtrl.setOffsetY(message.arg2);
                for (RecyclerViewPager.OnPageChangedListener o : mPageChangeListeners) {
                    o.OnPageChanged(oldPage, mCurrentPage);
                }
                return true;
            default:
                return false;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private float mInitialScale;
        private boolean mCenterDefined;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mInitialScale = mScrollCtrl.getScale();
            mCenterDefined = false;
            return super.onScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float dX = 0, dY = 0;
            float scale = Math.max(1, mInitialScale * detector.getScaleFactor());
            if (!mCenterDefined) {
                mCenterDefined = true;
                dX = detector.getFocusX();
                dY = detector.getFocusY();
            }
            mScrollCtrl.setZoom(
                    scale,
                    -dX,
                    -dY
            );
            notifyDataSetChanged();
            return super.onScale(detector);
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e2.getPointerCount() > 1) return false;
            mScrollCtrl.scrollBy(-distanceX, -distanceY);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mScrollCtrl.getScale() > 1) {
                mScrollCtrl.resetZoom(true);
            } else {
                mScrollCtrl.zoomTo(1.8f, e.getX(), e.getY());
            }
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mTapNavs) {
                if (e.getY() > mScrollCtrl.viewportHeight() * 0.7f) {
                    return scrollToNext(true);
                } else if (e.getY() < mScrollCtrl.viewportHeight() * 0.3f) {
                    return scrollToPrevious(true);
                }
            }
            return super.onSingleTapUp(e);
        }
    }

    private class DrawThread extends Thread {

        private final SurfaceHolder mHolder;
        private final Paint mPaint;
        volatile private boolean mIsRunning;
        volatile private boolean mChanged;
        private float oX, oY, s;

        DrawThread(SurfaceHolder surfaceHolder) {
            mIsRunning = false;
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mHolder = surfaceHolder;
            mChanged = true;
        }

        void setRunning(boolean run) {
            mIsRunning = run;
        }

        @Override
        public void run() {
            while (mIsRunning) {
                Canvas canvas = null;
                while (!mChanged && oX == mScrollCtrl.offsetX()
                        && oY == mScrollCtrl.offsetY() && s == mScrollCtrl.getScale()) {
                    try {
                        sleep(5);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                oX = mScrollCtrl.offsetX();
                oY = mScrollCtrl.offsetY();
                s = mScrollCtrl.getScale();
                mChanged = false;
                try {
                    // получаем объект Canvas и выполняем отрисовку
                    canvas = mHolder.lockCanvas(null);
                    if (canvas == null) {
                        continue;
                    }
                    synchronized (mHolder) {
                        canvas.drawColor(Color.LTGRAY);
                        if (mPool != null) {
                            int offset = (int) mScrollCtrl.offsetY();
                            int page = mCurrentPage;
                            //draw previous pages
                            while (offset > 0) {
                                PageImage image = mPool.get(page - 1);
                                if (image == null) break;
                                float scale = canvas.getWidth() / (float)image.getWidth();
                                scale += s - 1;
                                image.scale(scale);
                                offset -= image.getHeight();
                                Rect rect = image.draw(canvas, mPaint, (int) oX, offset);
                                page--;
                                Log.d("WTR", "Draw page: " + page);
                                if (!mScrollCtrl.isFlying()) {
                                    oY = rect.top;
                                    notifyPageChanged(mCurrentPage - 1, rect.top);
                                }
                            }
                            //draw current page and next
                            while (offset < canvas.getHeight() && mIsRunning) {
                                PageImage image = mPool.get(page);
                                if (image == null) break;
                                float scale = canvas.getWidth() / (float)image.getWidth();
                                scale += s - 1;
                                image.scale(scale);
                                Rect rect = image.draw(canvas, mPaint, (int) oX, offset);
                                Log.d("WTR", "Draw page: " + page);
                                if (rect.bottom < 0) {
                                    //if last page
                                    if (page == getItemCount() - 1)  {

                                        break;
                                    }
                                    if (!mScrollCtrl.isFlying()) {
                                        oY = rect.bottom;
                                        notifyPageChanged(mCurrentPage + 1, rect.bottom);
                                    }
                                }
                                page++;
                                offset = rect.bottom;
                            }
                            //prefetch next
                            mPool.get(page);
                        }
                    }
                } finally {
                    if (canvas != null) {
                        // отрисовка выполнена. выводим результат на экран
                        mHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }

        private void notifyPageChanged(int page, int offsetY) {
            Message msg = new Message();
            msg.what = MSG_PAGE_CHANGED;
            msg.arg1 = page;
            msg.arg2 = offsetY;
            handler.sendMessage(msg);
        }
    }
}