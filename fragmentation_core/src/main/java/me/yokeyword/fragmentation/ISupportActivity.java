package me.yokeyword.fragmentation;

import android.view.MotionEvent;

import me.yokeyword.fragmentation.anim.FragmentAnimator;

/**
 * 接口标准定义，即强制暴露出此框架的功能提供
 */

public interface ISupportActivity {
    /**得到支持委托类，由作者定义，功能强大的*/
    SupportActivityDelegate getSupportDelegate();

    /**额外的事务：自定义Tag，添加SharedElement动画，操作非回退栈Fragment*/
    ExtraTransaction extraTransaction();

    
    /**获取设置的全局动画 copy*/
    FragmentAnimator getFragmentAnimator();

    void setFragmentAnimator(FragmentAnimator fragmentAnimator);

    FragmentAnimator onCreateFragmentAnimator();

    void onBackPressed();

    void onBackPressedSupport();

    boolean dispatchTouchEvent(MotionEvent ev);
}
