package com.tristar.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tristar.adapters.CustomAdapter;
import com.tristar.main.R;
import com.tristar.object.ProcessAddressForServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by cbe-tecmwsp-017 on 27/1/17.
 */
public class ListViewSwipeGesture implements View.OnTouchListener {
    Activity activity;


    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private ListView mListView;


    //private DismissCallbacks mCallbacks;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero
    private int smallWidth = 1;
    private int largewidth = 1;
    private int textwidth = 1;
    private int textwidth2 = 1;
    private int textheight = 1;

    // Transient properties
    private List<PendingDismissData> mPendingDismisses = new ArrayList<PendingDismissData>();
    private int mDismissAnimationRefCount = 0;
    private float mDownX;
    private boolean mSwiping;
    private VelocityTracker mVelocityTracker;
    private int temp_position,opened_position,stagged_position;
    private ViewGroup mDownView,old_mDownView;
    private ViewGroup mDownView_parent;
    private TextView mDownView_parent_txt1,mDownView_parent_txt2;
    private boolean mPaused;
    public boolean moptionsDisplay=false;
    static TouchCallbacks tcallbacks;

    //Intermediate Usages
    String TextColor="#FFFFFF";      //#FF4444
    String RangeOneColor="#FFD060";   //"#FFD060"
    String singleColor="#FF4444";

    //Functional  Usages
    public String HalfColor;          //Green
    public String FullColor;         //Orange
    public String HalfText;
    public String FullText;
    public String HalfTextFinal;
    public String FullTextFinal;
    public Drawable HalfDrawable;
    public Drawable FullDrawable;


    //Swipe Types
    public int SwipeType;
    public static int Single	=	1;
    public static int Double	=	2;
    public static int Dismiss	=	3;
    ArrayList<ProcessAddressForServer> processOrderlist = new ArrayList<ProcessAddressForServer>();
    private int const_width = 0;

    public ListViewSwipeGesture(ListView listView, TouchCallbacks Callbacks, ArrayList<ProcessAddressForServer> processOrderlist, Activity context){
        ViewConfiguration vc    =   ViewConfiguration.get(listView.getContext());
        mSlop                   =   vc.getScaledTouchSlop();
        mMinFlingVelocity       =   vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity       =   vc.getScaledMaximumFlingVelocity();
        mListView               =   listView;
        activity                =   context;
        tcallbacks              =   Callbacks;
        SwipeType               =   Double;
        this.processOrderlist   =   processOrderlist;
        GetResourcesValues();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {            //Invokes OnClick Functionality
                if(!moptionsDisplay){
                    tcallbacks.OnClickListView(arg2);
                }

            }
        });
    }



    public interface TouchCallbacks {                                           //Callback functions
        void FullSwipeListView(int position, ProcessAddressForServer address , String type);
        void HalfSwipeListView(int position);
        void OnClickListView(int position);
        void LoadDataForScroll();
        void onDismiss(int[] reverseSortedPositions);
    }

    private void GetResourcesValues() {
        mAnimationTime          =   mListView.getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        HalfColor           =   RangeOneColor;          //Green
        FullColor           =   activity.getResources().getString(R.string.str_orange);         //Orange
        HalfText            =   activity.getResources().getString(R.string.basic_action_1);
        HalfTextFinal         = activity.getResources().getString(R.string.basic_action_1);
        FullText            =   activity.getResources().getString(R.string.basic_action);
        FullTextFinal         = activity.getResources().getString(R.string.basic_action);
        HalfDrawable        =   activity.getResources().getDrawable( R.drawable.content_discard );
        FullDrawable        =   activity.getResources().getDrawable(R.drawable.content_discard);
    }


    public void setEnabled(boolean enabled) {
        mPaused     =   !enabled;
    }

    public GestureScroll makeScrollListener() {
        return new GestureScroll();
    }



    class  GestureScroll implements AbsListView.OnScrollListener{
        //Scroll Usages
        private int visibleThreshold    =   4;
        private int currentPage         =   0;
        private int previousTotal       =   0;
        private boolean loading         =   true;
        private int previousFirstVisibleItem = 0;
        private long previousEventTime = 0, currTime, timeToScrollOneElement;
        private double speed = 0;

        @Override
        public void onScrollStateChanged(AbsListView absListView, int scrollState) {
            setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal   =   totalItemCount;
                    currentPage++;
                }
            }

            if ( !loading	&& (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                tcallbacks.LoadDataForScroll();
                loading = true;
            }

            if (previousFirstVisibleItem != firstVisibleItem) {
                currTime = System.currentTimeMillis();
                timeToScrollOneElement = currTime - previousEventTime;
                speed = ((double) 1 / timeToScrollOneElement) * 1000;

                previousFirstVisibleItem = firstVisibleItem;
                previousEventTime = currTime;

            }

        }

        public double getSpeed() {
            return speed;
        }
    }



    @SuppressLint("ResourceAsColor")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Override
    public boolean onTouch(final View view, MotionEvent event) {
        if (mViewWidth < 2) {
            mViewWidth  =   mListView.getWidth();
            smallWidth	=	mViewWidth/7;
            textwidth2	=	mViewWidth/3;
            textwidth	=	textwidth2;
            largewidth	=	textwidth+textwidth2;
        }

        int tempwidth	=	0;
        if(SwipeType==1)
            tempwidth = smallWidth;
        else
            tempwidth = textwidth2;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                int mDownPosition;
            {
                if (mPaused) {
                    return false;
                }
                Rect rect               =   new Rect();
                int childCount          =   mListView.getChildCount();
                int[] listViewCoords    = new int[2];
                mListView.getLocationOnScreen(listViewCoords);
                int x                   = (int) event.getRawX() - listViewCoords[0];
                int y                   = (int) event.getRawY() - listViewCoords[1];
                ViewGroup child;
                for (int i = 0; i < childCount; i++) {
                    child = (ViewGroup) mListView.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y)) {
                        mDownView_parent    =   child;
                        mDownView           =   (ViewGroup) child.findViewById(R.id.list_display_view_container);

                        if(mDownView_parent.getChildCount()==1){
                            textheight	=	mDownView_parent.getHeight();
                            if(SwipeType==Dismiss){
                                HalfColor		=	singleColor;
                                HalfDrawable	=	activity.getResources().getDrawable(R.drawable.cancel_2);
                            }
                            SetBackGroundforList();
                        }


                        if(old_mDownView!=null && mDownView!=old_mDownView){
                            ResetListItem(old_mDownView);
                            old_mDownView=null;
                            return false;
                        }
                        break;
                    }
                }

                if (mDownView != null) {
                    mDownX              =   event.getRawX();
                    mDownPosition =   mListView.getPositionForView(mDownView);
                    mVelocityTracker    =   VelocityTracker.obtain();
                    mVelocityTracker.addMovement(event);
                } else {
                    mDownView               =   null;
                }

                //mSwipeDetected              =   false;
                temp_position               =   mListView.pointToPosition((int) event.getX(), (int) event.getY());
                view.onTouchEvent(event);
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }
                float deltaX                =   event.getRawX() - mDownX;
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000); // 1000 by defaut but
                float velocityX             =   mVelocityTracker.getXVelocity();											// it was too much
                float absVelocityX          =   Math.abs(velocityX);
                float absVelocityY          =   Math.abs(mVelocityTracker.getYVelocity());
                boolean swipe               =   false;
                boolean swipeRight          =   false;

                if (Math.abs(deltaX) > tempwidth) {
                    swipe               =   true;
                    swipeRight          =   deltaX > 0;

                }else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity && absVelocityY < absVelocityX) {
                    // dismiss only if flinging in the same direction as dragging
                    swipe   = (velocityX < 0) == (deltaX < 0);
                    swipeRight          = mVelocityTracker.getXVelocity() > 0;
                }

                if (deltaX < 0 && swipe) {
                    mListView.setDrawSelectorOnTop(false);

                    if (swipe && !swipeRight && deltaX <= -tempwidth) {
                        FullSwipeTrigger();
                    } else if (deltaX >= -textwidth && SwipeType==Double) {
                        ResetListItem(mDownView);
                    }else {
                        ResetListItem(mDownView);
                    }
                } else {
                    ResetListItem(mDownView);
                }

                mVelocityTracker.recycle();
                mVelocityTracker    =   null;
                mDownX              =   0;
                mDownView           =   null;
                mDownPosition =   ListView.INVALID_POSITION;
                mSwiping            =   false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {

                float deltaX = event.getRawX() - mDownX;
                if (mVelocityTracker == null || mPaused ||deltaX>0) {
                    break;
                }

                mVelocityTracker.addMovement(event);


                if (Math.abs(deltaX) > mSlop) {
                    mSwiping = true;
                    mListView.requestDisallowInterceptTouchEvent(true);

                    // Cancel ListView's touch (un-highlighting the item)
                    MotionEvent cancelEvent     =   MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (event.getActionIndex()
                                    << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mListView.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }
                if (mSwiping && deltaX < 0 ) {

                    int width;
                    if(SwipeType==1){
                        width	=	textwidth;
                    }
                    else{
                        width	=	largewidth;
                    }

                    if (-deltaX<width){
                        mDownView.setTranslationX(deltaX);
                        return false;
                    }
                    return false;
                }else if(mSwiping){
                    ResetListItem(mDownView);
                }
                break;
            }
        }
        return false;
    }




    @SuppressLint("ResourceType")
    private void SetBackGroundforList() {
        // TODO Auto-generated method stub


        Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        float width = display.getWidth();
        float height = display.getHeight();

        Log.d("swip_width", " " + width);

        const_width = (int) (0.25 * width);

        mDownView_parent_txt1 =	new TextView(activity.getApplicationContext());
        RelativeLayout.LayoutParams lp1 =new RelativeLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        mDownView_parent_txt1.setId(111111);
        mDownView_parent_txt1.setLayoutParams(lp1);
        mDownView_parent_txt1.setGravity( Gravity.CENTER);
        mDownView_parent_txt1.setText(HalfText);
        mDownView_parent_txt1.setWidth(const_width);
        mDownView_parent_txt1.setHeight(textheight);
        mDownView_parent_txt1.setBackgroundColor(Color.parseColor("#FF4444"));
        mDownView_parent_txt1.setTextColor(Color.parseColor(TextColor));
        mDownView_parent_txt1.setTypeface(mDownView_parent_txt1.getTypeface(), Typeface.BOLD);
        mDownView_parent_txt1.setCompoundDrawablesWithIntrinsicBounds(null , null, null, null );
        mDownView_parent.addView(mDownView_parent_txt1, 0);

        if(SwipeType==Double){
            mDownView_parent_txt2 = new TextView(activity.getApplicationContext());
            mDownView_parent_txt2.setId(222222);
            RelativeLayout.LayoutParams lp2 =new RelativeLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp2.addRule(RelativeLayout.LEFT_OF,mDownView_parent_txt1.getId());
            mDownView_parent_txt2.setLayoutParams(lp2);
            mDownView_parent_txt2.setGravity(Gravity.CENTER);
            mDownView_parent_txt2.setText(FullText);
            mDownView_parent_txt2.setWidth(const_width);
            mDownView_parent_txt2.setHeight(textheight);
            mDownView_parent_txt2.setBackgroundColor(Color.parseColor("#007AE0"));
            mDownView_parent_txt2.setTextColor(Color.parseColor(TextColor));
            mDownView_parent_txt2.setTypeface(mDownView_parent_txt2.getTypeface(), Typeface.BOLD);
            mDownView_parent_txt2.setCompoundDrawablesWithIntrinsicBounds(null , null, null, null );
            mDownView_parent.addView(mDownView_parent_txt2, 1);
        }
    }


    public void ResetListItem(View tempView){

        tempView.animate().translationX(0).alpha(1f).setListener(new AnimatorListenerAdapter(){
            @SuppressLint("LongLogTag")
            @Override
            public void onAnimationEnd(
                    Animator animation) {
                super.onAnimationEnd(animation);
                int count=mDownView_parent.getChildCount()-1;
                for(int i=0;i<count;i++){
                    View V= mDownView_parent.getChildAt(i);
                    Log.d("removing process_order_child class",""+V.getClass());
                    mDownView_parent.removeViewAt(0);
                }
                moptionsDisplay = false;

            }
        });

        stagged_position=-1;
        opened_position=-1;

    }

    private void FullSwipeTrigger(){
        Log.d("FUll Swipe trigger call", "Works**********************" + mDismissAnimationRefCount);
        old_mDownView	=	mDownView;
        int width;
        if(SwipeType==Single || SwipeType==Dismiss){
            width	=	200;
            if(SwipeType==Dismiss) {
                if (mDismissAnimationRefCount == 1){
                    mDismissAnimationRefCount = 0;
                    ++mDismissAnimationRefCount;
                }else {
                    ++mDismissAnimationRefCount;
                }

            }
        }
        else{
            width = const_width * 2;
        }
        mDownView.animate().translationX(-width).setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        moptionsDisplay     =   true;
                        stagged_position	=	temp_position;
                        mDownView_parent_txt1.setOnTouchListener(new touchClass());
                        if(SwipeType==Double)
                        mDownView_parent_txt2.setOnTouchListener(new touchClass());


                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                    }
                });
    }



    class PendingDismissData implements Comparable<PendingDismissData> {
        public int position;
        public View view;

        public PendingDismissData(int position, View view) {
            this.position = position;
            this.view = view;
        }

        @Override
        public int compareTo(PendingDismissData other) {
            // Sort by descending position
            return other.position - position;
        }
    }

    private void performDismiss(final View dismissView, final int dismissPosition) {

        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalHeight = dismissView.getHeight();

        ((ViewGroup)dismissView).getChildAt(1).animate().translationX(0).alpha(1f).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                ((ViewGroup)dismissView).removeViewAt(0);
                Log.d("Selected view", dismissView.getClass()+"..."+dismissView.getId()+mDismissAnimationRefCount);
                ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 0).setDuration(mAnimationTime);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        --mDismissAnimationRefCount;
                        if (mDismissAnimationRefCount == 0) {

                            Collections.sort(mPendingDismisses);

                            int[] dismissPositions = new int[mPendingDismisses.size()];
                            for (int i = mPendingDismisses.size() - 1; i >= 0; i--) {
                                dismissPositions[i] = mPendingDismisses.get(i).position;
                                Log.d("Dismiss positions....",dismissPositions[i]+"");
                            }
                            tcallbacks.onDismiss(dismissPositions);
                            mPendingDismisses.clear();
                        }
                    }
                });

                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        lp.height = (Integer) valueAnimator.getAnimatedValue();
                        dismissView.setLayoutParams(lp);
                    }
                });

                mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
                animator.start();
            }
        });


    }


    class touchClass implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            opened_position     =   mListView.getPositionForView((View) v.getParent());
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    if(opened_position==stagged_position && moptionsDisplay){
                        switch (v.getId()) {
                            case 111111:
                                if(SwipeType==Dismiss){
                                    moptionsDisplay = false;
                                    performDismiss(mDownView_parent,temp_position);
                                    tcallbacks.HalfSwipeListView(temp_position);

                                }else
                                {
                                    CustomAdapter adapter = (CustomAdapter) mListView.getAdapter();
                                    ProcessAddressForServer address = adapter.getList().get(temp_position);
                                    tcallbacks.FullSwipeListView(temp_position, address, "Reject");
                                    CustomAdapter customAdapter = (CustomAdapter) mListView.getAdapter();
                                    customAdapter.notifyDataSetChanged();
                                    mDownView = (ViewGroup) mDownView_parent.findViewById(R.id.list_display_view_container);
                                    ResetListItem(mDownView);

                                }

                                return true;
                            case 222222:
                                CustomAdapter adapter = (CustomAdapter) mListView.getAdapter();
                                ProcessAddressForServer address = adapter.getList().get(temp_position);
                                tcallbacks.FullSwipeListView(temp_position, address, "email");
                                mDownView = (ViewGroup) mDownView_parent.findViewById(R.id.list_display_view_container);
                                ResetListItem(mDownView);
                                return true;
                        }
                    }
                }
                return false;
            }

            return false;
        }

    }


}
