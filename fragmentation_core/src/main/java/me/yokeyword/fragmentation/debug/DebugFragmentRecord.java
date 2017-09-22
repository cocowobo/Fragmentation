package me.yokeyword.fragmentation.debug;

import java.util.List;

/**
 * 为了调试时 查看栈视图  （实际上包含的信息就是类名）
 * Created by YoKeyword on 16/2/21.
 */
public class DebugFragmentRecord {
    /**Fragment 自身的名字*/
    public CharSequence fragmentName;
    /**子Fragment 的集合。*/
    public List<DebugFragmentRecord> childFragmentRecord;

    public DebugFragmentRecord(CharSequence fragmentName, List<DebugFragmentRecord> childFragmentRecord) {
        this.fragmentName = fragmentName;
        this.childFragmentRecord = childFragmentRecord;
    }
}
