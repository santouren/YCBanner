package com.yc.cn.ycbannerlib.gallery;

import android.graphics.PointF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * <pre>
 *     @author yangchong
 *     blog  : https://github.com/yangchong211
 *     time  : 2018/3/18
 *     desc  : 自定义SnapHelper
 *     revise: 关于SnapHelper源码分析可以看我博客：https://blog.csdn.net/m0_37700275/article/details/83901677
 * </pre>
 */
public class GalleryLinearSnapHelper extends LinearSnapHelper {

    private static final float INVALID_DISTANCE = 1f;
    private static final float MILLISECONDS_PER_INCH = 40f;
    private RecyclerView mRecyclerView;
    private OrientationHelper mHorizontalHelper;

    public GalleryLinearSnapHelper(@Nullable RecyclerView recyclerView){
        mRecyclerView = recyclerView;
    }

    @Nullable
    protected LinearSmoothScroller createSnapScroller(final RecyclerView.LayoutManager layoutManager) {
        if (!(layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
            return null;
        }
        return new LinearSmoothScroller(mRecyclerView.getContext()) {
            @Override
            protected void onTargetFound(View targetView, RecyclerView.State state, RecyclerView.SmoothScroller.Action action) {
                int[] snapDistances = calculateDistanceToFinalSnap(mRecyclerView.getLayoutManager(), targetView);
                final int dx;
                final int dy;
                if (snapDistances != null) {
                    dx = snapDistances[0];
                    dy = snapDistances[1];
                    final int time = calculateTimeForDeceleration(Math.max(Math.abs(dx), Math.abs(dy)));
                    if (time > 0) {
                        action.update(dx, dy, time, mDecelerateInterpolator);
                    }
                }
            }

            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                //这个地方可以自己设置
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
            }
        };
    }

    @Override
    public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
        if (!(layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
            return RecyclerView.NO_POSITION;
        }

        final int itemCount = layoutManager.getItemCount();
        if (itemCount == 0) {
            return RecyclerView.NO_POSITION;
        }

        final View currentView = findSnapView(layoutManager);
        if (currentView == null) {
            return RecyclerView.NO_POSITION;
        }

        final int currentPosition = layoutManager.getPosition(currentView);
        if (currentPosition == RecyclerView.NO_POSITION) {
            return RecyclerView.NO_POSITION;
        }

        RecyclerView.SmoothScroller.ScrollVectorProvider vectorProvider =
                (RecyclerView.SmoothScroller.ScrollVectorProvider) layoutManager;
        PointF vectorForEnd = vectorProvider.computeScrollVectorForPosition(itemCount - 1);
        if (vectorForEnd == null) {
            return RecyclerView.NO_POSITION;
        }

        //在松手之后,列表最多只能滚多一屏的item数
        int deltaThreshold = layoutManager.getWidth() / getHorizontalHelper(layoutManager).getDecoratedMeasurement(currentView);

        int hDeltaJump;
        if (layoutManager.canScrollHorizontally()) {
            hDeltaJump = estimateNextPositionDiffForFling(layoutManager,
                    getHorizontalHelper(layoutManager), velocityX, 0);

            if (hDeltaJump > deltaThreshold) {
                hDeltaJump = deltaThreshold;
            }
            if (hDeltaJump < -deltaThreshold) {
                hDeltaJump = -deltaThreshold;
            }

            if (vectorForEnd.x < 0) {
                hDeltaJump = -hDeltaJump;
            }
        } else {
            hDeltaJump = 0;
        }

        if (hDeltaJump == 0) {
            return RecyclerView.NO_POSITION;
        }

        int targetPos = currentPosition + hDeltaJump;
        if (targetPos < 0) {
            targetPos = 0;
        }
        if (targetPos >= itemCount) {
            targetPos = itemCount - 1;
        }
        return targetPos;
    }


    private int estimateNextPositionDiffForFling(RecyclerView.LayoutManager layoutManager,
                                                 OrientationHelper helper, int velocityX, int velocityY) {
        int[] distances = calculateScrollDistance(velocityX, velocityY);
        float distancePerChild = computeDistancePerChild(layoutManager, helper);
        if (distancePerChild <= 0) {
            return 0;
        }
        int distance = distances[0];
        if (distance > 0) {
            return (int) Math.floor(distance / distancePerChild);
        } else {
            return (int) Math.ceil(distance / distancePerChild);
        }
    }


    private float computeDistancePerChild(RecyclerView.LayoutManager layoutManager, OrientationHelper helper) {
        View minPosView = null;
        View maxPosView = null;
        int minPos = Integer.MAX_VALUE;
        int maxPos = Integer.MIN_VALUE;
        int childCount = layoutManager.getChildCount();
        if (childCount == 0) {
            return INVALID_DISTANCE;
        }
        for (int i = 0; i < childCount; i++) {
            View child = layoutManager.getChildAt(i);
            final int pos = layoutManager.getPosition(child);
            if (pos == RecyclerView.NO_POSITION) {
                continue;
            }
            if (pos < minPos) {
                minPos = pos;
                minPosView = child;
            }
            if (pos > maxPos) {
                maxPos = pos;
                maxPosView = child;
            }
        }
        if (minPosView == null || maxPosView == null) {
            return INVALID_DISTANCE;
        }
        int start = Math.min(helper.getDecoratedStart(minPosView), helper.getDecoratedStart(maxPosView));
        int end = Math.max(helper.getDecoratedEnd(minPosView), helper.getDecoratedEnd(maxPosView));
        int distance = end - start;
        if (distance == 0) {return INVALID_DISTANCE;}
        return 1f * distance / ((maxPos - minPos) + 1);
    }

    private OrientationHelper getHorizontalHelper(RecyclerView.LayoutManager layoutManager) {
        if (mHorizontalHelper == null) {
            mHorizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager);
        }
        return mHorizontalHelper;
    }

}
