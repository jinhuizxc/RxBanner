package cn.levey.bannerlib.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import cn.levey.bannerlib.base.RxBannerUtil;
import cn.levey.bannerlib.impl.RxBannerIndicatorChangeListener;
import cn.levey.bannerlib.indicator.IndicatorManager;
import cn.levey.bannerlib.indicator.animation.type.AnimationType;
import cn.levey.bannerlib.indicator.animation.type.BaseAnimation;
import cn.levey.bannerlib.indicator.animation.type.ColorAnimation;
import cn.levey.bannerlib.indicator.animation.type.FillAnimation;
import cn.levey.bannerlib.indicator.animation.type.ScaleAnimation;
import cn.levey.bannerlib.indicator.draw.controller.DrawController;
import cn.levey.bannerlib.indicator.draw.data.IndicatorConfig;
import cn.levey.bannerlib.indicator.draw.data.Orientation;
import cn.levey.bannerlib.indicator.draw.data.PositionSavedState;
import cn.levey.bannerlib.indicator.draw.data.RtlMode;
import cn.levey.bannerlib.indicator.utils.CoordinatesUtils;
import cn.levey.bannerlib.indicator.utils.IdUtils;
import cn.levey.bannerlib.manager.AutoPlayRecyclerView;
import cn.levey.bannerlib.manager.ScaleLayoutManager;


/**
 * forked from https://github.com/romandanylyk/PageIndicatorView
 * Modify by Levey
 * */
public class RxBannerIndicator extends View implements RxBannerIndicatorChangeListener,IndicatorManager.Listener {

    private IndicatorManager manager;
    private RecyclerView.AdapterDataObserver setObserver;
    private AutoPlayRecyclerView recyclerView;
    private ScaleLayoutManager layoutManager;
    private boolean isInteractionEnabled;


    public RxBannerIndicator(Context context) {
        super(context);
        init();
    }

    public RxBannerIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RxBannerIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RxBannerIndicator(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    @Override
    protected void onDetachedFromWindow() {
        unRegisterSetObserver();
        super.onDetachedFromWindow();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        IndicatorConfig indicatorConfig = manager.indicator();
        PositionSavedState positionSavedState = new PositionSavedState(super.onSaveInstanceState());
        positionSavedState.setSelectedPosition(indicatorConfig.getSelectedPosition());
        positionSavedState.setSelectingPosition(indicatorConfig.getSelectingPosition());
        positionSavedState.setLastSelectedPosition(indicatorConfig.getLastSelectedPosition());

        return positionSavedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof PositionSavedState) {
            IndicatorConfig indicatorConfig = manager.indicator();
            PositionSavedState positionSavedState = (PositionSavedState) state;
            indicatorConfig.setSelectedPosition(positionSavedState.getSelectedPosition());
            indicatorConfig.setSelectingPosition(positionSavedState.getSelectingPosition());
            indicatorConfig.setLastSelectedPosition(positionSavedState.getLastSelectedPosition());
            super.onRestoreInstanceState(positionSavedState.getSuperState());

        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Pair<Integer, Integer> pair = manager.drawer().measureViewSize(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(pair.first, pair.second);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        manager.drawer().draw(canvas);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        manager.drawer().touch(event);
        return true;
    }

    @Override
    public void onIndicatorUpdated() {
        invalidate();
    }


    @Override
    public void onBannerSelected(int position) {
        onPageSelect(position);
    }

    @Override
    public void onBannerScrollStateChanged(int state) {
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            manager.indicator().setInteractiveAnimation(isInteractionEnabled);
        }
    }


    /**
     * Set static number of circle indicators to be displayed.
     *
     * @param count total count of indicators.
     */
    public void setCount(int count) {
//        if (count >= 0 && manager.indicator().getCount() != count) {

            manager.indicator().setCount(count);
            setVisibility(GONE);
            updateVisibility();
    }

    /**
     * Return number of circle indicators
     */
    public int getCount() {
        return manager.indicator().getCount();
    }

    /**
     * Dynamic count will automatically update number of circle indicators
     * if {@link ViewPager} page count updates on run-time. If new count will be bigger than current count,
     * selected circle will stay as it is, otherwise it will be set to last one.
     * Note: works if {@link AutoPlayRecyclerView} set and already have it's adapter. See {@link #setRecyclerView(AutoPlayRecyclerView)}.
     *
     * @param dynamicCount boolean value to add/remove indicators dynamically.
     */
    public void setDynamicCount(boolean dynamicCount) {
        manager.indicator().setDynamicCount(dynamicCount);

        if (dynamicCount) {
            registerSetObserver();
        } else {
            unRegisterSetObserver();
        }
    }

    /**
     * Set radius in dp of each circle indicator. Default value is {@link IndicatorConfig#DEFAULT_RADIUS_DP}.
     * Note: make sure you set circle Radius, not a Diameter.
     *
     * @param radiusDp radius of circle in dp.
     */
    public void setRadius(int radiusDp) {
        if (radiusDp < 0) {
            radiusDp = 0;
        }

        int radiusPx = RxBannerUtil.dp2px(radiusDp);
        manager.indicator().setRadius(radiusPx);
        invalidate();
    }

    /**
     * Set radius in px of each circle indicator. Default value is {@link IndicatorConfig#DEFAULT_RADIUS_DP}.
     * Note: make sure you set circle Radius, not a Diameter.
     *
     * @param radiusPx radius of circle in px.
     */
    public void setRadius(float radiusPx) {
        if (radiusPx < 0) {
            radiusPx = 0;
        }

        manager.indicator().setRadius((int) radiusPx);
        invalidate();
    }

    /**
     * Return radius of each circle indicators in px. If custom radius is not set, return
     * default value {@link IndicatorConfig#DEFAULT_RADIUS_DP}.
     */
    public int getRadius() {
        return manager.indicator().getRadius();
    }

    /**
     * Set padding in dp between each circle indicator. Default value is {@link IndicatorConfig#DEFAULT_PADDING_DP}.
     *
     * @param paddingDp padding between circles in dp.
     */
    public void setPadding(int paddingDp) {
        if (paddingDp < 0) {
            paddingDp = 0;
        }

        int paddingPx = RxBannerUtil.dp2px(paddingDp);
        manager.indicator().setPadding(paddingPx);
        invalidate();
    }

    /**
     * Set padding in px between each circle indicator. Default value is {@link IndicatorConfig#DEFAULT_PADDING_DP}.
     *
     * @param paddingPx padding between circles in px.
     */
    public void setPadding(float paddingPx) {
        if (paddingPx < 0) {
            paddingPx = 0;
        }

        manager.indicator().setPadding((int) paddingPx);
        invalidate();
    }

    /**
     * Return padding in px between each circle indicator. If custom padding is not set,
     * return default value {@link IndicatorConfig#DEFAULT_PADDING_DP}.
     */
    public int getPadding() {
        return manager.indicator().getPadding();
    }

    /**
     * Set scale factor used in {@link AnimationType#SCALE} animation.
     * Defines size of unselected indicator circles in comparing to selected one.
     * Minimum and maximum values are {@link ScaleAnimation#MAX_SCALE_FACTOR} and {@link ScaleAnimation#MIN_SCALE_FACTOR}.
     * See also {@link ScaleAnimation#DEFAULT_SCALE_FACTOR}.
     *
     * @param factor float value in range between 0 and 1.
     */
    public void setScale(float factor) {
        if (factor > ScaleAnimation.MAX_SCALE_FACTOR) {
            factor = ScaleAnimation.MAX_SCALE_FACTOR;

        } else if (factor < ScaleAnimation.MIN_SCALE_FACTOR) {
            factor = ScaleAnimation.MIN_SCALE_FACTOR;
        }

        manager.indicator().setScale(factor);
    }

    /**
     * Returns scale factor values used in {@link AnimationType#SCALE} animation.
     * Defines size of unselected indicator circles in comparing to selected one.
     * Minimum and maximum values are {@link ScaleAnimation#MAX_SCALE_FACTOR} and {@link ScaleAnimation#MIN_SCALE_FACTOR}.
     * See also {@link ScaleAnimation#DEFAULT_SCALE_FACTOR}.
     *
     * @return float value that indicate scale factor.
     */
    public float getScale() {
        return manager.indicator().getScale();
    }

    /**
     * Set stroke width in px to set while {@link AnimationType#FILL} is selected.
     * Default value is {@link FillAnimation#DEFAULT_STROKE_DP}
     *
     * @param strokePx stroke width in px.
     */
    public void setStrokeWidth(float strokePx) {
        int radiusPx = manager.indicator().getRadius();

        if (strokePx < 0) {
            strokePx = 0;

        } else if (strokePx > radiusPx) {
            strokePx = radiusPx;
        }

        manager.indicator().setStroke((int) strokePx);
        invalidate();
    }

    /**
     * Set stroke width in dp to set while {@link AnimationType#FILL} is selected.
     * Default value is {@link FillAnimation#DEFAULT_STROKE_DP}
     *
     * @param strokeDp stroke width in dp.
     */

    public void setStrokeWidth(int strokeDp) {
        int strokePx = RxBannerUtil.dp2px(strokeDp);
        int radiusPx = manager.indicator().getRadius();

        if (strokePx < 0) {
            strokePx = 0;

        } else if (strokePx > radiusPx) {
            strokePx = radiusPx;
        }

        manager.indicator().setStroke(strokePx);
        invalidate();
    }

    /**
     * Return stroke width in px if {@link AnimationType#FILL} is selected, 0 otherwise.
     */
    public int getStrokeWidth() {
        return manager.indicator().getStroke();
    }

    /**
     * Set color of selected state to circle indicator. Default color is {@link ColorAnimation#DEFAULT_SELECTED_COLOR}.
     *
     * @param color color selected circle.
     */
    public void setSelectedColor(int color) {
        manager.indicator().setSelectedColor(color);
        invalidate();
    }

    /**
     * Return color of selected circle indicator. If custom unselected color
     * is not set, return default color {@link ColorAnimation#DEFAULT_SELECTED_COLOR}.
     */
    public int getSelectedColor() {
        return manager.indicator().getSelectedColor();
    }

    /**
     * Set color of unselected state to each circle indicator. Default color {@link ColorAnimation#DEFAULT_UNSELECTED_COLOR}.
     *
     * @param color color of each unselected circle.
     */
    public void setUnselectedColor(int color) {
        manager.indicator().setUnselectedColor(color);
        invalidate();
    }

    /**
     * Return color of unselected state of each circle indicator. If custom unselected color
     * is not set, return default color {@link ColorAnimation#DEFAULT_UNSELECTED_COLOR}.
     */
    public int getUnselectedColor() {
        return manager.indicator().getUnselectedColor();
    }

    /**
     * Automatically hide (View.INVISIBLE) PageIndicatorView while indicator count is <= 1.
     * Default is true.
     *
     * @param autoVisibility auto hide indicators.
     */
    public void setAutoVisibility(boolean autoVisibility) {
        if (!autoVisibility) {
            setVisibility(VISIBLE);
        }
        manager.indicator().setAutoVisibility(autoVisibility);
        updateVisibility();
    }

    /**
     * Set orientation for indicator, one of HORIZONTAL or VERTICAL.
     * Default is HORIZONTAL.
     *
     * @param orientation an orientation to display page indicators.
     */
    public void setOrientation(@Nullable Orientation orientation) {
        if (orientation != null) {
            manager.indicator().setOrientation(orientation);
            requestLayout();
        }
    }

    /**
     * Set animation duration time in millisecond. Default animation duration time is {@link BaseAnimation#DEFAULT_ANIMATION_TIME}.
     * (Won't affect on anything unless {@link #setAnimationType(AnimationType type)} is specified
     * and {@link #setInteractiveAnimation(boolean isInteractive)} is false).
     *
     * @param duration animation duration time.
     */
    public void setAnimationDuration(long duration) {
        manager.indicator().setAnimationDuration(duration);
    }

    /**
     * Return animation duration time in milliseconds. If custom duration is not set,
     * return default duration time {@link BaseAnimation#DEFAULT_ANIMATION_TIME}.
     */
    public long getAnimationDuration() {
        return manager.indicator().getAnimationDuration();
    }

    /**
     * Set animation type to perform while selecting new circle indicator.
     * Default animation type is {@link AnimationType#NONE}.
     *
     * @param type type of animation, one of {@link AnimationType}
     */
    public void setAnimationType(@Nullable AnimationType type) {
        manager.onValueUpdated(null);

        if (type != null) {
            manager.indicator().setAnimationType(type);
        } else {
            manager.indicator().setAnimationType(AnimationType.NONE);
        }
        invalidate();
    }

    /**
     * Interactive animation will animate indicator smoothly
     * from position to position based on user's current swipe progress.
     * (Won't affect on anything unless {@link #setRecyclerView(AutoPlayRecyclerView)} is specified).
     *
     * @param isInteractive value of animation to be interactive or not.
     */
    public void setInteractiveAnimation(boolean isInteractive) {
        manager.indicator().setInteractiveAnimation(isInteractive);
        this.isInteractionEnabled = isInteractive;
    }

    /**
     * Set {@link ViewPager} to add {@link ViewPager.OnPageChangeListener} and automatically
     * handle selecting new indicators (and interactive animation effect if it is enabled).
     *
     * @param pager instance of {@link ViewPager} to work with
     */
    public void setRecyclerView(@Nullable AutoPlayRecyclerView pager) {
        releaseView();
        if (pager == null) {
            return;
        }
        recyclerView = pager;
        layoutManager = (ScaleLayoutManager) recyclerView.getLayoutManager();
        layoutManager.setRxBannerIndicatorChangeListener(this);
        manager.indicator().setRecyclerViewId(recyclerView.getId());
        setDynamicCount(manager.indicator().isDynamicCount());
        int count = getRecyclerViewCount();

        if (isRtl()) {
            int selectedPosition = (count - 1) - layoutManager.getCurrentPosition();
            manager.indicator().setSelectedPosition(selectedPosition);
        }
        setCount(count);
    }

    /**
     * Release {@link ViewPager} and stop handling events of {@link ViewPager.OnPageChangeListener}.
     */
    public void releaseView() {
        if (recyclerView != null) {
            recyclerView = null;
        }
    }

    /**
     * Specify to display PageIndicatorView with Right to left layout or not.
     * One of {@link RtlMode}: Off (Left to right), On (Right to left)
     * or Auto (handle this mode automatically based on users language preferences).
     * Default is Off.
     *
     * @param mode instance of {@link RtlMode}
     */
    public void setRtlMode(@Nullable RtlMode mode) {
        IndicatorConfig indicatorConfig = manager.indicator();
        if (mode == null) {
            indicatorConfig.setRtlMode(RtlMode.Off);
        } else {
            indicatorConfig.setRtlMode(mode);
        }

        if (recyclerView == null) {
            return;
        }

        int selectedPosition = indicatorConfig.getSelectedPosition();
        int position = selectedPosition;

        if (isRtl()) {
            position = (indicatorConfig.getCount() - 1) - selectedPosition;

        } else if (recyclerView != null) {
            position = layoutManager.getCurrentPosition();
        }

        indicatorConfig.setLastSelectedPosition(position);
        indicatorConfig.setSelectingPosition(position);
        indicatorConfig.setSelectedPosition(position);
        invalidate();
    }

    /**
     * Set specific circle indicator position to be selected. If position < or > total count,
     * accordingly first or last circle indicator will be selected.
     *
     * @param position position of indicator to select.
     */
    public void setSelection(int position) {
        IndicatorConfig indicatorConfig = manager.indicator();
        position = adjustPosition(position);

        if (position == indicatorConfig.getSelectedPosition() || position == indicatorConfig.getSelectingPosition()) {
            return;
        }

        indicatorConfig.setInteractiveAnimation(false);
        indicatorConfig.setLastSelectedPosition(indicatorConfig.getSelectedPosition());
        indicatorConfig.setSelectingPosition(position);
        indicatorConfig.setSelectedPosition(position);
        manager.animate().basic();
    }

    /**
     * Set specific circle indicator position to be selected without any kind of animation. If position < or > total count,
     * accordingly first or last circle indicator will be selected.
     *
     * @param position position of indicator to select.
     */
    public void setSelected(int position) {
        IndicatorConfig indicatorConfig = manager.indicator();
        AnimationType animationType = indicatorConfig.getAnimationType();
        indicatorConfig.setAnimationType(AnimationType.NONE);

        setSelection(position);
        indicatorConfig.setAnimationType(animationType);
    }

    /**
     * Return position of currently selected circle indicator.
     */
    public int getSelection() {
        return manager.indicator().getSelectedPosition();
    }

    /**
     * Set progress value in range [0 - 1] to specify state of animation while selecting new circle indicator.
     *
     * @param selectingPosition selecting position with specific progress value.
     * @param progress          float value of progress.
     */
    public void setProgress(int selectingPosition, float progress) {
        IndicatorConfig indicatorConfig = manager.indicator();
        if (!indicatorConfig.isInteractiveAnimation()) {
            return;
        }

        int count = indicatorConfig.getCount();
        if (count <= 0 || selectingPosition < 0) {
            selectingPosition = 0;

        } else if (selectingPosition > count - 1) {
            selectingPosition = count - 1;
        }

        if (progress < 0) {
            progress = 0;

        } else if (progress > 1) {
            progress = 1;
        }

        if (progress == 1) {
            indicatorConfig.setLastSelectedPosition(indicatorConfig.getSelectedPosition());
            indicatorConfig.setSelectedPosition(selectingPosition);
        }

        indicatorConfig.setSelectingPosition(selectingPosition);
        manager.animate().interactive(progress);
    }

    public void setClickListener(@Nullable DrawController.ClickListener listener) {
        manager.drawer().setClickListener(listener);
    }

    private void init() {
        setupId();
    }

    private void setupId() {
        if (getId() == NO_ID) {
            setId(IdUtils.generateViewId());
        }
    }

    public void setIndicatorConfig(IndicatorConfig indicatorConfig){
            manager = new IndicatorManager(this, indicatorConfig);
            manager.drawer().setIndicatorConfig(indicatorConfig);
            indicatorConfig.setPaddingStart(getPaddingLeft());
            indicatorConfig.setPaddingTop(getPaddingTop());
            indicatorConfig.setPaddingEnd(getPaddingRight());
            indicatorConfig.setPaddingBottom(getPaddingBottom());
            isInteractionEnabled = indicatorConfig.isInteractiveAnimation();

    }

    public IndicatorConfig getConfig(){
        if(manager != null && manager.drawer() != null){
            return manager.drawer().indicator();
        }else {
            throw new NullPointerException("please init indicator");
        }
    }


    private void registerSetObserver() {
        if (setObserver != null || recyclerView == null || recyclerView.getAdapter() == null) {
            return;
        }

        setObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updateCount();
            }

        };

        try {
            recyclerView.getAdapter().registerAdapterDataObserver(setObserver);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void unRegisterSetObserver() {
        if (setObserver == null || recyclerView == null || recyclerView.getAdapter() == null) {
            return;
        }

        try {
            recyclerView.getAdapter().unregisterAdapterDataObserver(setObserver);
            setObserver = null;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void updateCount() {
        if (recyclerView == null || recyclerView.getAdapter() == null) {
            return;
        }

        int newCount = recyclerView.getAdapter().getItemCount();
        int currItem = layoutManager.getCurrentPosition();

        manager.indicator().setSelectedPosition(currItem);
        manager.indicator().setSelectingPosition(currItem);
        manager.indicator().setLastSelectedPosition(currItem);
        manager.animate().end();
        setCount(newCount);
    }

    private void updateVisibility() {
        int count = manager.indicator().getCount();
        if ( count > IndicatorConfig.MIN_COUNT) {
            setVisibility(VISIBLE);
        } else{
            setVisibility(View.GONE);
        }
        requestLayout();
    }

    private int getRecyclerViewCount() {
        if (recyclerView != null && recyclerView.getAdapter() != null) {
            return recyclerView.getAdapter().getItemCount();
        } else {
            return manager.indicator().getCount();
        }
    }

    private void onPageSelect(int position) {
        IndicatorConfig indicatorConfig = manager.indicator();
        boolean canSelectIndicator = isViewMeasured();
        int count = indicatorConfig.getCount();

        if (canSelectIndicator) {
            if (isRtl()) {
                position = (count - 1) - position;
            }
            setSelection(position);
        }
    }

	private void onPageScroll(int position, float positionOffset) {
        IndicatorConfig indicatorConfig = manager.indicator();
        AnimationType animationType = indicatorConfig.getAnimationType();
        boolean interactiveAnimation = indicatorConfig.isInteractiveAnimation();
        boolean canSelectIndicator = isViewMeasured() && interactiveAnimation && animationType != AnimationType.NONE;

        if (!canSelectIndicator) {
            return;
        }

        Pair<Integer, Float> progressPair = CoordinatesUtils.getProgress(indicatorConfig, position, positionOffset, isRtl());
        int selectingPosition = progressPair.first;
        float selectingProgress = progressPair.second;
        setProgress(selectingPosition, selectingProgress);
    }

    private boolean isRtl() {
        switch (manager.indicator().getRtlMode()) {
            case On:
                return true;

            case Off:
                return false;

            case Auto:
                return TextUtilsCompat.getLayoutDirectionFromLocale(getContext().getResources().getConfiguration().locale) == ViewCompat.LAYOUT_DIRECTION_RTL;
        }

        return false;
    }

    private boolean isViewMeasured() {
        return getMeasuredHeight() != 0 || getMeasuredWidth() != 0;
    }


    private int adjustPosition(int position){
        IndicatorConfig indicatorConfig = manager.indicator();
        int count = indicatorConfig.getCount();
        int lastPosition = count - 1;

        if (position < 0) {
            position = 0;

        } else if (position > lastPosition) {
            position = lastPosition;
        }

        return position;
    }


}
