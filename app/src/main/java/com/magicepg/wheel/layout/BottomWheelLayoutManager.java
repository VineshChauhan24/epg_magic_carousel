package com.magicepg.wheel.layout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v7.widget.RecyclerView;

import com.magicepg.wheel.rotator.AbstractWheelRotator;
import com.magicepg.wheel.rotator.BottomAnticlockwiseWheelRotator;
import com.magicepg.wheel.rotator.BottomClockwiseWheelRotator;
import com.magicepg.wheel.WheelComputationHelper;

/**
 * Does children layout for wheel's bottom part.
 *
 * @author Alexey Kovalev
 * @since 05.02.2017
 */
public final class BottomWheelLayoutManager extends AbstractWheelLayoutManager {

    private static final long BOTTOM_WHEEL_STARTUP_ANIMATION_DURATION = 2000;

    public BottomWheelLayoutManager(Context context,
                                    WheelComputationHelper computationHelper,
                                    WheelOnInitialLayoutFinishingListener initialLayoutFinishingListener) {
        super(context, computationHelper, initialLayoutFinishingListener);
    }

    @Override
    protected double computeLayoutStartAngleInRad() {
        return wheelConfig.getAngularRestrictions().getGapAreaBottomEdgeAngleRestrictionInRad();
    }

    @Override
    protected double computeLayoutEndAngleInRad() {
        return wheelConfig.getAngularRestrictions().getWheelBottomEdgeAngleRestrictionInRad();
    }

    @Override
    protected AbstractWheelRotator createClockwiseRotator() {
        return new BottomClockwiseWheelRotator(this, computationHelper);
    }

    @Override
    protected AbstractWheelRotator createAnticlockwiseRotator() {
        return new BottomAnticlockwiseWheelRotator(this, computationHelper);
    }

    @Override
    protected int onLayoutChildrenForStartupAnimation(RecyclerView.Recycler recycler, RecyclerView.State state) {
        final double sectorAngleInRad = angularRestrictions.getSectorAngleInRad();
        final double bottomLimitAngle = getLayoutEndAngleInRad();

        double layoutAngle = computationHelper.getSectorAlignmentAngleInRadBySectorTopEdge(
                angularRestrictions.getWheelTopEdgeAngleRestrictionInRad()
        );

        int childPos = getStartLayoutFromAdapterPosition();
        int layoutedChildrenCount = 0;
        boolean isInsideLayoutBounds = computationHelper.getSectorAngleTopEdgeInRad(layoutAngle) > bottomLimitAngle;
        while (isInsideLayoutBounds && layoutedChildrenCount < state.getItemCount()) {
            setupSectorForPosition(recycler, childPos, layoutAngle, true);
            layoutAngle -= sectorAngleInRad;
            isInsideLayoutBounds = computationHelper.getSectorAngleTopEdgeInRad(layoutAngle) > bottomLimitAngle;
            layoutedChildrenCount++;
            childPos--;
        }

        return childPos;
    }

    @Override
    protected int onLayoutChildrenRegular(RecyclerView.Recycler recycler, RecyclerView.State state) {

        final double startLayoutAngleInRad = angularRestrictions.getGapAreaBottomEdgeAngleRestrictionInRad();
        setLayoutStartAngleInRad(startLayoutAngleInRad);

        final double sectorAngleInRad = wheelConfig.getAngularRestrictions().getSectorAngleInRad();
        final double bottomLimitAngle = getLayoutEndAngleInRad();

        double layoutAngle = computationHelper.getSectorAlignmentAngleInRadBySectorTopEdge(startLayoutAngleInRad);

        int childPos = getStartLayoutFromAdapterPosition();
        int layoutedChildrenCount = 0;
        // when sector's top edge goes outside bottom edge layout angle - then stop children layout
        boolean isInsideLayoutBounds = computationHelper.getSectorAngleTopEdgeInRad(layoutAngle) > bottomLimitAngle;
        while (isInsideLayoutBounds && layoutedChildrenCount < state.getItemCount()) {
            setupSectorForPosition(recycler, childPos, layoutAngle, true);
            layoutAngle -= sectorAngleInRad;
            isInsideLayoutBounds = computationHelper.getSectorAngleTopEdgeInRad(layoutAngle) > bottomLimitAngle;
            layoutedChildrenCount++;
            childPos--;
        }

        return childPos;
    }

    @Override
    protected void notifyLayoutFinishingListener(int lastlyLayoutedChildPos) {
        initialLayoutFinishingListener.onInitialLayoutFinished(lastlyLayoutedChildPos - 1);
    }

    @Override
    protected Animator createWheelStartupAnimator(final RecyclerView.Recycler recycler, final RecyclerView.State state) {
        final LayoutParams childClosestToLayoutStartEdgeLp = getChildLayoutParams(getChildClosestToLayoutStartEdge());

        final float fromAngleInRad = (float) childClosestToLayoutStartEdgeLp.anglePositionInRad;
        final float toAngleInRad = (float) computationHelper.getSectorAlignmentAngleInRadBySectorTopEdge(
                angularRestrictions.getGapAreaBottomEdgeAngleRestrictionInRad()
        );

        final ValueAnimator wheelStartupAnimator = ValueAnimator.ofFloat(fromAngleInRad, toAngleInRad);

        wheelStartupAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float firstChildAnglePositionInRad = (float) childClosestToLayoutStartEdgeLp.anglePositionInRad;
                final float currentlyAnimatedAngleInRad = (Float) animation.getAnimatedValue();

                final double rotationDeltaInRad = firstChildAnglePositionInRad - currentlyAnimatedAngleInRad;
                clockwiseRotator.rotateWheelBy(rotationDeltaInRad);
                notifyOnAnimationUpdate(WheelStartupAnimationStatus.InProgress);
            }
        });

        wheelStartupAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                notifyOnAnimationUpdate(WheelStartupAnimationStatus.Start);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                notifyOnAnimationUpdate(WheelStartupAnimationStatus.Finished);
            }
        });

        wheelStartupAnimator.setDuration(BOTTOM_WHEEL_STARTUP_ANIMATION_DURATION);

        return wheelStartupAnimator;
    }

}
