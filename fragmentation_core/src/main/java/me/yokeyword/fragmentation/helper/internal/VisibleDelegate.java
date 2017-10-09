package me.yokeyword.fragmentation.helper.internal;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentationHack;

import java.util.List;

import me.yokeyword.fragmentation.ISupportFragment;

/**
 * Created by YoKey on 17/4/4.
 * 简略看过去，是本类这个委托类保存了很多boolean的状态，主要是保存，处理，更改这些状态来辅助获取传入的Fragment的状态
 * <p>
 *
 * 1.构造中传入要处理的Fragment，并将之存为两个类型的同一实体，ISupportFragment和普通的Fragment
 * 2.在onCreate中保存了 savedInstanceState实例，
 * 3.在本类依附的Fragment 保存销毁重建状态的时候，会主动注入两个 标志， mInvisibleWhenLeave 是否是在离开的时候 隐藏的和
 *   mFirstCreateViewCompatReplace是否是第一次创建View空间并Repleace
 * 4.此类所依附的Fragment执行 onActivityCreate 的时候 会多方面检查本类所持有的 Fragment 的各种显示状态。来确认持有的Fragment 是否
 *   被重建过，是否之前加载过View。，如果是第一次加载View且未重建过， 就会执行一些代码，反之直接return，除此还要检查
 * 5.先看onPause() 这个特俗方法。这个方法给我们引入了3个Boolean参数
 *    01：mNeedDispatch属性表示某种是否需要处理的标识，
 *    02：mIsSupportVisible 表示  是否维持在显示状态，没有被人动过，这个属性，我先表明mIsSupportVisible存在于本类，充分的和依赖的
 *    Fragment的真实显示状态挂钩，即按照设计这个值应该是 Fragment显示它为true，隐藏他是false，这个结论是看完后总结出来的，
 *    03：mInvisibleWhenLeave，表示onPause退到后台的时候，Fragment处于什么状态，
 *    当onPause 的时候，通过 两个判断来决定当前Fragment是什么状态的，if (mIsSupportVisible && isFragmentVisible(mFragment))
 *    这句代码通过两种方式判定是否处于显示状态，mIsSupportVisible 表示  是否维持在显示状态，没有被人动过 ，
 *    isFragmentVisible(mFragment)则是通过系统的固有属性判断Fragment的状态，当确定onPause的时候Fragment处于显示状态的时候
 *    利用mNeedDispatch属性是打上无需处理的标记，并且调用{@link #dispatchSupportVisible(boolean)}方法
 * 06.dispatchSupportVisible(boolean)，通过代码追寻，发现，比方法调用基本上都先判过 mIsSupportVisible属性，且取mIsSupportVisible
 *    相反值传入dispatchSupportVisible(boolean)方法 ，看此方法代码，发现，其内部还，保守性的再次判断了 mIsSupportVisible属性和
 *    传入的是否相同，如果相同，则和上面的取反规则相悖论，直接return，并将mNeedDispatch = true;那这个mNeedDispatch的意思大致可
 *    以推断出来，
 * 在退到后台onPause的时候会辨别当前持有的类是否已经是处于隐藏状态，那就两种情况，隐藏和显示，如果隐藏了。那就是推到后太的时候，这个Fragment
 *   是出于隐藏状态的，比较特殊，一般我们都是退到后台的时候就是把可见的那个退到后台，但是当一个Activity有多个Fragment的时候，就会
 *   肯定除开可见的那个其他的Fragment是出于隐藏状态的，这个状态由默认为true的mInvisibleWhenLeave在退到后台的时候自动记录下来，表示
 *   为退到后台的时候，这个Fragment已经是不可见状态，这种状态有什么特俗的吗，还真有，看完代码，烧掉3个脑袋后我看懂了作者对于这种状态
 *   的分析，这个状态下 mNeedDispatch属性是 true，表示这种状态需要处理，打上这个标记后后面哪里用得到，后面说
 *   当onPause 的时候，通过 两个判断来决定当前Fragment是什么状态的，if (mIsSupportVisible && isFragmentVisible(mFragment))
 *   这句代码通过两种方式判定是否处于显示状态，  mIsSupportVisible 表示  是否维持在显示状态，没有被人动过 ，
 *   isFragmentVisible(mFragment)则是通过系统的固有属性判断Fragment的状态，当确定onPause的时候Fragment处于显示状态的时候
 *   利用mNeedDispatch属性是打上无需处理的标记，
 *
 * </p>
 */

public class VisibleDelegate {
    private static final String FRAGMENTATION_STATE_SAVE_IS_INVISIBLE_WHEN_LEAVE = "fragmentation_invisible_when_leave";
    private static final String FRAGMENTATION_STATE_SAVE_COMPAT_REPLACE = "fragmentation_compat_replace";

    // SupportVisible相关
    /**是否维持在显示状态，没有被人动过*/
    private boolean mIsSupportVisible;
    /**需要处理(调度，安排，分发)*/
    private boolean mNeedDispatch = true;
    /**当离开（出于后台）的时候是否可见，此属性是可以销毁后重建的时候回读取的*/
    private boolean mInvisibleWhenLeave;
    /**是否第一次显示*/
    private boolean mIsFirstVisible = true;
    /**执行了PagerAdapter展示*/
    private boolean mFixStatePagerAdapter;
    /**是否第一次创建View 组件并Replace到窗口，此属性是可以销毁后重建的时候回读取的*/
    private boolean mFirstCreateViewCompatReplace = true;

    private Handler mHandler;
    private Bundle mSaveInstanceState;

    private ISupportFragment mSupportF;
    private Fragment mFragment;

    public VisibleDelegate(ISupportFragment fragment) {
        this.mSupportF = fragment;
        this.mFragment = (Fragment) fragment;
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mSaveInstanceState = savedInstanceState;
            if (!mFixStatePagerAdapter) { // setUserVisibleHint() 或许在 onCreate()之前调用，因为Fragment的生命周期执行分时机
                mInvisibleWhenLeave = savedInstanceState.getBoolean(FRAGMENTATION_STATE_SAVE_IS_INVISIBLE_WHEN_LEAVE);
                mFirstCreateViewCompatReplace = savedInstanceState.getBoolean(FRAGMENTATION_STATE_SAVE_COMPAT_REPLACE);
            }
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(FRAGMENTATION_STATE_SAVE_IS_INVISIBLE_WHEN_LEAVE, mInvisibleWhenLeave);
        outState.putBoolean(FRAGMENTATION_STATE_SAVE_COMPAT_REPLACE, mFirstCreateViewCompatReplace);
    }

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (!mFirstCreateViewCompatReplace && mFragment.getTag() != null && mFragment.getTag().startsWith("android:switcher:")) {
            return;    //确定第一次此方法调用，并且默认设置tag，就不会return，会直接走下去
        }

        if (mFirstCreateViewCompatReplace) {
            mFirstCreateViewCompatReplace = false;      //第一次默认是true，就会在第一次调用此方法后改为false
        }

        if (!mInvisibleWhenLeave && !mFragment.isHidden() &&       //前面判默认第一次mInvisibleWhenLeave为false，且Fragment没有被hidden，
                (mFragment.getUserVisibleHint() || mFixStatePagerAdapter)) {    //后面判传入的Fragment是否被显示或者已经被ViewPagerAdapter加载
              /**Fragment的setUserVisibleHint详解 - 一座小楼的专栏 - CSDN博客  http://blog.csdn.net/czhpxl007/article/details/51277319*/
            if ((mFragment.getParentFragment() != null && isFragmentVisible(mFragment.getParentFragment()))
                    || mFragment.getParentFragment() == null) {
                //前面判断如果有父Fragment。则就判断父Fragment正在显示吗，未显示不走进来，无父Fragment也不进来
                mNeedDispatch = false;      //需要分发改为false
                safeDispatchUserVisibleHint(true);  //安全的设置显示状态为true
            }
        }
    }

    public void onResume() {               //显示
        if (!mIsFirstVisible) {               //并非第一次显示了就进来
            if (!mIsSupportVisible && !mInvisibleWhenLeave && isFragmentVisible(mFragment)) {
                mNeedDispatch = false;
                dispatchSupportVisible(true);
            }
        }
    }

    public void onPause() {                //隐藏
        if (mIsSupportVisible && isFragmentVisible(mFragment)) {
            mNeedDispatch = false;
            mInvisibleWhenLeave = false;
            dispatchSupportVisible(false);
        } else {
            mInvisibleWhenLeave = true;
        }
    }

    public void onHiddenChanged(boolean hidden) {
        if (!hidden && !mFragment.isResumed()) {
            //if fragment is shown but not resumed, ignore...
            mInvisibleWhenLeave = false;
            return;
        }
        if (hidden) {
            safeDispatchUserVisibleHint(false);
        } else {
            enqueueDispatchVisible();
        }
    }

    public void onDestroyView() {
        mIsFirstVisible = true;
        mFixStatePagerAdapter = false;
    }

    /**记录对用户是否可见*/
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (mFragment.isResumed() || (mFragment.isDetached() && isVisibleToUser)) {
            if (!mIsSupportVisible && isVisibleToUser) {
                safeDispatchUserVisibleHint(true);
            } else if (mIsSupportVisible && !isVisibleToUser) {
                dispatchSupportVisible(false);
            }
        } else if (isVisibleToUser) {
            mInvisibleWhenLeave = false;
            mFixStatePagerAdapter = true;
        }
    }

    /**安全的设置显示状态*/
    private void safeDispatchUserVisibleHint(boolean visible) {
        if (mIsFirstVisible) {
            if (!visible) return;
            enqueueDispatchVisible();
        } else {
            dispatchSupportVisible(visible);
        }
    }

    private void enqueueDispatchVisible() {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                dispatchSupportVisible(true);
            }
        });
    }

    private void dispatchSupportVisible(boolean visible) {
        if (mIsSupportVisible == visible) {
            mNeedDispatch = true;
            return;
        }

        mIsSupportVisible = visible;

        if (!mNeedDispatch) {
            mNeedDispatch = true;
        } else {
            if (!mFragment.isAdded()) return;
            FragmentManager fragmentManager = mFragment.getChildFragmentManager();
            if (fragmentManager != null) {
                List<Fragment> childFragments = FragmentationHack.getActiveFragments(fragmentManager);
                if (childFragments != null) {
                    for (Fragment child : childFragments) {
                        if (child instanceof ISupportFragment && !child.isHidden() && child.getUserVisibleHint()) {
                            ((ISupportFragment) child).getSupportDelegate().getVisibleDelegate().dispatchSupportVisible(visible);
                        }
                    }
                }
            }
        }

        if (visible) {
            mSupportF.onSupportVisible();

            if (mIsFirstVisible) {
                mIsFirstVisible = false;
                mSupportF.onLazyInitView(mSaveInstanceState);
            }
        } else {
            mSupportF.onSupportInvisible();
        }
    }

    private boolean isFragmentVisible(Fragment fragment) {
        return !fragment.isHidden() && fragment.getUserVisibleHint();
    }

    public boolean isSupportVisible() {
        return mIsSupportVisible;
    }

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }
}
