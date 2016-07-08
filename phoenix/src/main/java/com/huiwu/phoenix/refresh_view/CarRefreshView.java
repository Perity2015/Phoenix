package com.huiwu.phoenix.refresh_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import com.huiwu.phoenix.PullToRefreshView;
import com.huiwu.phoenix.R;


/**
 * Created by Huiwu-Y on 2016/6/21.
 */
public class CarRefreshView extends BaseRefreshView implements Animatable {

    private static final float SCALE_START_PERCENT = 0.5f;
    private static final int ANIMATION_DURATION = 1000;

    private final static float EARTH_RATIO = 0.35f;
    private static final float EARTH_INITIAL_SCALE = 1.05f;

    private final static float BALL_RATIO = 0.76f;
    private static final float BALL_INITIAL_SCALE = 1.05f;

    private static final float SUN_FINAL_SCALE = 0.75f;
    private static final float SUN_INITIAL_ROTATE_GROWTH = 1.2f;
    private static final float SUN_FINAL_ROTATE_GROWTH = 1.5f;

    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();

    private PullToRefreshView mParent;
    private Matrix mMatrix;
    private Animation mAnimation;

    private int mTop;
    private int mScreenWidth;

    private int mEarthHeight;

    private int mBallHeight;
    private float mBallLeftOffset;
    private float mBallTopOffset;

    private int mCarWidth;
    private float mSunLeftOffset;
    private float mSunTopOffset;

    private float mPercent = 0.0f;
    private float mRotate = 0.0f;

    private Bitmap mSky;
    private Bitmap mSun;
    private Bitmap mTown;

    private boolean isRefreshing = false;

    public CarRefreshView(Context context, final PullToRefreshView parent) {
        super(context, parent);
        mParent = parent;
        mMatrix = new Matrix();

        setupAnimations();
        parent.post(new Runnable() {
            @Override
            public void run() {
                initiateDimens(parent.getWidth());
            }
        });
    }

    public void initiateDimens(int viewWidth) {
        if (viewWidth <= 0 || viewWidth == mScreenWidth) return;

        mScreenWidth = viewWidth;
        mEarthHeight = (int) (EARTH_RATIO * mScreenWidth);

        mBallHeight = (int) (BALL_RATIO * mScreenWidth);

        mBallLeftOffset = 0.11f * (float) mScreenWidth;
        mBallTopOffset = (mParent.getTotalDragDistance() * 0.50f);

        mCarWidth = (int) (0.11f * (float) mScreenWidth);
        mSunLeftOffset = 0.44f * (float) mScreenWidth;
        mSunTopOffset = (mParent.getTotalDragDistance() * 0.55f);

        mTop = -mParent.getTotalDragDistance();

        createBitmaps();
    }

    private void createBitmaps() {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        mSky = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.earth, options);
        mSky = Bitmap.createScaledBitmap(mSky, mScreenWidth, mEarthHeight, true);
        mTown = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ball, options);
        mTown = Bitmap.createScaledBitmap(mTown, mBallHeight, mBallHeight, true);
        mSun = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.car, options);
        mSun = Bitmap.createScaledBitmap(mSun, mCarWidth, (int) (mCarWidth * 0.63f), true);
    }

    @Override
    public void setPercent(float percent, boolean invalidate) {
        setPercent(percent);
        if (invalidate) setRotate(percent);
    }

    @Override
    public void offsetTopAndBottom(int offset) {
        mTop += offset;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mScreenWidth <= 0) return;

        final int saveCount = canvas.save();

        canvas.translate(0, mTop);
        canvas.clipRect(0, -mTop, mScreenWidth, mParent.getTotalDragDistance());

        drawSky(canvas);
        drawTown(canvas);
        drawSun(canvas);

        canvas.restoreToCount(saveCount);
    }

    private void drawSky(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();

        float dragPercent = Math.min(1f, Math.abs(mPercent));

        float skyScale;
        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            /** Change skyScale between {@link #EARTH_INITIAL_SCALE} and 1.0f depending on {@link #mPercent} */
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            skyScale = EARTH_INITIAL_SCALE - (EARTH_INITIAL_SCALE - 1.0f) * scalePercent;
        } else {
            skyScale = EARTH_INITIAL_SCALE;
        }

        float offsetX = -(mScreenWidth * skyScale - mScreenWidth) / 2.0f;
        float offsetY = (1.0f - dragPercent) * mParent.getTotalDragDistance()
                - mEarthHeight * (skyScale - 1.0f) / 2; // Offset sky scaling

        matrix.postScale(skyScale, skyScale);
        matrix.postTranslate(offsetX, offsetY);
        canvas.drawBitmap(mSky, matrix, null);
    }

    private void drawTown(Canvas canvas) {


        Matrix matrix = mMatrix;
        matrix.reset();

        float dragPercent = mPercent;
        if (dragPercent > 1.0f) { // Slow down if pulling over set height
            dragPercent = (dragPercent + 9.0f) / 10;
        }

        float sunRadius = (float) mBallHeight / 2.0f;
        float sunRotateGrowth = BALL_INITIAL_SCALE;

        float offsetX = mBallLeftOffset;
        float offsetY = mBallTopOffset
                + (mParent.getTotalDragDistance() / 2) * (1.0f - dragPercent) // Move the sun up
                - mTop; // Depending on Canvas position

        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            float sunScale = 1.0f - (1.0f - SUN_FINAL_SCALE) * scalePercent;
            sunRotateGrowth += (SUN_FINAL_ROTATE_GROWTH - SUN_INITIAL_ROTATE_GROWTH) * scalePercent;

            matrix.preTranslate(offsetX + (sunRadius - sunRadius * sunScale), offsetY * (2.0f - sunScale));
            matrix.preScale(sunScale, sunScale);

            offsetX += sunRadius;
            offsetY = offsetY * (2.0f - sunScale) + sunRadius * sunScale;
        } else {
            matrix.postTranslate(offsetX, offsetY);

            offsetX += sunRadius;
            offsetY += sunRadius;
        }

        matrix.postRotate(
                (isRefreshing ? -360 : 360) * mRotate * (isRefreshing ? 1 : sunRotateGrowth) / 2,
                offsetX,
                offsetY);

        canvas.drawBitmap(mTown, matrix, null);
    }

    private void drawSun(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();

        float dragPercent = Math.min(1f, Math.abs(mPercent));

        float skyScale;
        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            /** Change skyScale between {@link #EARTH_INITIAL_SCALE} and 1.0f depending on {@link #mPercent} */
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            skyScale = EARTH_INITIAL_SCALE - (EARTH_INITIAL_SCALE - 1.0f) * scalePercent;
        } else {
            skyScale = EARTH_INITIAL_SCALE;
        }

        float offsetX = mSunLeftOffset;
        float offsetY = mSunTopOffset
                + (mParent.getTotalDragDistance() / 2) * (1.0f - dragPercent) // Move the sun up
                - mTop; // D

        matrix.postScale(skyScale, skyScale);
        matrix.postTranslate(offsetX, offsetY);
        canvas.drawBitmap(mSun, matrix, null);
    }

    public void setPercent(float percent) {
        mPercent = percent;
    }

    public void setRotate(float rotate) {
        mRotate = rotate;
        invalidateSelf();
    }

    public void resetOriginals() {
        setPercent(0);
        setRotate(0);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, mEarthHeight + top);
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void start() {
        mAnimation.reset();
        isRefreshing = true;
        mParent.startAnimation(mAnimation);
    }

    @Override
    public void stop() {
        mParent.clearAnimation();
        isRefreshing = false;
        resetOriginals();
    }

    private void setupAnimations() {
        mAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setRotate(interpolatedTime);
            }
        };
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.RESTART);
        mAnimation.setInterpolator(LINEAR_INTERPOLATOR);
        mAnimation.setDuration(ANIMATION_DURATION);
    }


}
