package com.uproxone.dotloader;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.lang.ref.WeakReference;

public class UDotsAnim extends View {
    private static final int DELAY_BETWEEN_DOTS = 80;
    private static int BOUNCE_ANIMATION_DURATION = 800;
    private static int COLOR_ANIMATION_DURATION = 80;
    private Dot[] mDots;
    Integer[] mColors;
    private int mDotRadius;
    private Rect mClipBounds;
    private float mCalculatedGapBetweenDotCenters;
    private float mFromY;
    private float mToY;
    private int resId = 0;
    private int stop_after_duration = 0;

    private Interpolator bounceAnimationInterpolator =
            new CubicInterpolator(0.62f, 0.28f, 0.23f, 0.99f);

    public UDotsAnim(Context context) {
        super(context);
        init(context, null);
    }

    public UDotsAnim(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public UDotsAnim(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context c, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray a = c.getTheme().obtainStyledAttributes(attrs, R.styleable.UDotsAnim, 0, 0);
        try {
            float dotRadius = a.getDimension(R.styleable.UDotsAnim_dot_radius, 0);
            int numberOfPods = 2;
            resId = a.getResourceId(R.styleable.UDotsAnim_color_array, 0);
            int left_dot_color = a.getColor(R.styleable.UDotsAnim_left_dot_color, Color.GRAY);
            int right_dot_color = a.getColor(R.styleable.UDotsAnim_right_dot_color, Color.DKGRAY);
            int color_switch_duration = a.getInteger(R.styleable.UDotsAnim_color_switch_duration, 80);
            int dot_animation_duration = a.getInteger(R.styleable.UDotsAnim_dots_animation_duration, 1000);
            stop_after_duration = a.getInteger(R.styleable.UDotsAnim_stop_after_duration, 0);

            if (!(color_switch_duration < 0)){
                COLOR_ANIMATION_DURATION = color_switch_duration;
            }

            if (!(dot_animation_duration < 0)){
                BOUNCE_ANIMATION_DURATION = dot_animation_duration;
            }

            Integer[] colors;
            if (resId == 0) {
                colors = new Integer[numberOfPods];
                for (int i = 0; i < numberOfPods; i++) {
                    colors[i] = 0x0;
                }
            } else {
                int[] temp = getResources().getIntArray(resId);
                colors = new Integer[temp.length];
                for (int i = 0; i < temp.length; i++) {
                    colors[i] = temp[i];
                }
            }
            init(numberOfPods, colors, (int) dotRadius, left_dot_color, right_dot_color);
        } finally {
            a.recycle();
        }
    }

    private void _stopAnimations() {
        mDots[0].positionAnimator.end();
        mDots[0].colorAnimator.end();
        mDots[1].positionAnimator.end();
        mDots[1].colorAnimator.end();
    }

    private void init(int numberOfDots, Integer[] colors, int dotRadius, int left_dot_color, int right_dot_color) {
        mColors = colors;
        mClipBounds = new Rect(0, 0, 0, 0);
        mDots = new Dot[numberOfDots];
        mDotRadius = dotRadius;
        for (int i = 0; i < numberOfDots; i++) {
            mDots[0] = new Dot(this, dotRadius, 0);
            mDots[0].setColor(left_dot_color);
            mDots[1] = new Dot(this, dotRadius, 1);
            mDots[1].setColor(right_dot_color);
        }
        //noinspection deprecation
        startAnimation();
        stopAnimations();
    }

    public void initAnimation() {
        mDots[0].positionAnimator = createValueAnimatorForDot(mDots[0], 0);
        mDots[0].positionAnimator.setStartDelay(DELAY_BETWEEN_DOTS);
        mDots[0].colorAnimator = createColorAnimatorForDot(mDots[0]);

        mDots[1].positionAnimator = createValueAnimatorForDot(mDots[1], 1);
        mDots[1].positionAnimator.setStartDelay(DELAY_BETWEEN_DOTS);
        mDots[1].colorAnimator = createColorAnimatorForDot(mDots[1]);
    }


    private void startAnimation() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                _startAnimation();
            }
        }, 10);
    }


    private void stopAnimations() {
        int stopDuration = 5000;
        if (!(stop_after_duration <= 0)){
            stopDuration = stop_after_duration;
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    _stopAnimations();
                }
            },stopDuration);
        }
    }

    private void _startAnimation() {
        initAnimation();
        mDots[0].positionAnimator.start();
        mDots[1].positionAnimator.start();
    }

    private ValueAnimator createValueAnimatorForDot(final Dot dot, int pos) {
        ValueAnimator animator = null;
        if (pos == 0) {
            animator = ValueAnimator.ofFloat(mFromY, mToY);
        } else if (pos == 1) {
            animator = ValueAnimator.ofFloat(mToY, mFromY);
        }
        animator.setInterpolator(bounceAnimationInterpolator);
        animator.setDuration(BOUNCE_ANIMATION_DURATION);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(new DotYUpdater(dot, this));
        animator.addListener(new RepeatAnim(dot, mColors));
        return animator;
    }

    private ValueAnimator createColorAnimatorForDot(Dot dot) {
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), mColors[dot.mCurrentColorIndex],
                mColors[dot.incrementColorIndex()]);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(COLOR_ANIMATION_DURATION);
        animator.addUpdateListener(new DotColorUpdater(dot, this));
        return animator;
    }

    private static class DotColorUpdater implements ValueAnimator.AnimatorUpdateListener {
        private Dot mDot;
        private WeakReference<UDotsAnim> mDotLoaderRef;
        private int res_colorid;

        private DotColorUpdater(Dot dot, UDotsAnim dotLoader) {
            mDot = dot;
            res_colorid = dotLoader.resId;
            mDotLoaderRef = new WeakReference<>(dotLoader);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            if (!(res_colorid == 0)) {
                mDot.setColor((Integer) valueAnimator.getAnimatedValue());
                UDotsAnim dotLoader = mDotLoaderRef.get();
                if (dotLoader != null) {
                    dotLoader.invalidateOnlyRectIfPossible();
                }
            }
        }
    }

    private static class DotYUpdater implements ValueAnimator.AnimatorUpdateListener {
        private Dot mDot;
        private WeakReference<UDotsAnim> mDotLoaderRef;

        private DotYUpdater(Dot dot, UDotsAnim dotLoader) {
            mDot = dot;
            mDotLoaderRef = new WeakReference<>(dotLoader);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            mDot.cx = (float) valueAnimator.getAnimatedValue();
            UDotsAnim dotLoader = mDotLoaderRef.get();
            if (dotLoader != null) {
                dotLoader.invalidateOnlyRectIfPossible();
            }
        }
    }


    private void invalidateOnlyRectIfPossible() {
        if (mClipBounds != null && mClipBounds.left != 0 &&
                mClipBounds.top != 0 && mClipBounds.right != 0 && mClipBounds.bottom != 0)
            invalidate(mClipBounds);
        else
            invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.getClipBounds(mClipBounds);
        mDots[0].draw(canvas);
        mDots[1].draw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;
        // desired height is 6 times the diameter of a dot.
        int desiredHeight = (mDotRadius * 2 * 3) + getPaddingTop() + getPaddingBottom();

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }
        mCalculatedGapBetweenDotCenters = calculateGapBetweenDotCenters();
        int desiredWidth = (int) (mCalculatedGapBetweenDotCenters * mDots.length);
        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }
        for (int i = 0, size = mDots.length; i < size; i++) {
            mDots[0].cy = (mDotRadius + i * mCalculatedGapBetweenDotCenters);
            mDots[0].cx = height - mDotRadius;

            mDots[1].cy = (mDotRadius + i * mCalculatedGapBetweenDotCenters);
            mDots[1].cx = height - mDotRadius;
        }
        mFromY = height - mDotRadius;
        mToY = mDotRadius;
        setMeasuredDimension(width, height);
    }

    private float calculateGapBetweenDotCenters() {
        return (mDotRadius * 2) + mDotRadius;
    }
}
