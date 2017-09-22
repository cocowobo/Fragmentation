package me.yokeyword.fragmentation.debug;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import me.yokeyword.fragmentation.R;

/**
 * Created by YoKeyword on 16/2/21.
 * <p>
 *  自定义树状图view容器，用于显示Fragment的层级嵌套结构
 *  结构是
 *  容器包裹条目，每个条目代表一个Fragment，每一个条目都可能拥有子Fragment，则此条目可展开
 *
 *  view简介：
 *  01.此view根View是个竖直ScroollView，ScroollView内部必须是个线性布局
 *  02.此View构造其实并没有真正进入逻辑业务，只是初始化了。真实义务调用是在设置数据的时候{{@link #bindFragmentRecords(List)}}
 *  03.{@link #bindFragmentRecords(List)}方法调用时即用户显示此View查看Fragment结构 的时候，所以此方法会将保存的Fragment结构数据分析并绘制
 *  04.每次显示界面，都会清空线性布局的子view，然后{@link #getTitleLayout()}加入标题view，
 *  05.{@link #setView(List, int, TextView)}此方法是核心方法，渲染条目
 *  06.item的高度是固定的50dp ，缩进Padding是16dp，当Fragment层级逐层展开，每一层的缩进padding成倍数算法增长
 * </p>
 */
public class DebugHierarchyViewContainer extends ScrollView {
    private Context mContext;

    private LinearLayout mLinearLayout;
    private LinearLayout mTitleLayout;

    private int mItemHeight;
    private int mPadding;

    public DebugHierarchyViewContainer(Context context) {
        super(context);
        initView(context);
    }

    public DebugHierarchyViewContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public DebugHierarchyViewContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        mContext = context;
        HorizontalScrollView hScrollView = new HorizontalScrollView(context);
        mLinearLayout = new LinearLayout(context);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        hScrollView.addView(mLinearLayout);
        addView(hScrollView);

        mItemHeight = dip2px(50);
        mPadding = dip2px(16);
    }

    private int dip2px(float dp) {
        float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public void bindFragmentRecords(List<DebugFragmentRecord> fragmentRecords) {
        mLinearLayout.removeAllViews();
        LinearLayout ll = getTitleLayout();
        mLinearLayout.addView(ll);

        if (fragmentRecords == null) return;

        DebugHierarchyViewContainer.this.setView(fragmentRecords, 0, null);
    }

    @NonNull
    private LinearLayout getTitleLayout() {
        if (mTitleLayout != null) return mTitleLayout;

        mTitleLayout = new LinearLayout(mContext);
        mTitleLayout.setPadding(dip2px(24), dip2px(24), 0, dip2px(8));
        mTitleLayout.setOrientation(LinearLayout.HORIZONTAL);
        ViewGroup.LayoutParams flParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mTitleLayout.setLayoutParams(flParams);

        TextView title = new TextView(mContext);
        title.setText("栈视图(Stack)");
        title.setTextSize(20);
        title.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.CENTER_VERTICAL;
        title.setLayoutParams(p);
        mTitleLayout.addView(title);

        ImageView img = new ImageView(mContext);
        img.setImageResource(R.drawable.fragmentation_help);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dip2px(16);
        params.gravity = Gravity.CENTER_VERTICAL;
        img.setLayoutParams(params);
        mTitleLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "* means not in backBack.", Toast.LENGTH_SHORT).show();
            }
        });
        mTitleLayout.addView(img);
        return mTitleLayout;
    }

    private void setView(final List<DebugFragmentRecord> fragmentRecordList, final int hierarchy, final TextView tvItem) {
        for (int i = fragmentRecordList.size() - 1; i >= 0; i--) {
            DebugFragmentRecord child = fragmentRecordList.get(i);
            int tempHierarchy = hierarchy;  //条目层次id标示

            final TextView childTvItem;
            childTvItem = getTextView(child, tempHierarchy);
            childTvItem.setTag(R.id.hierarchy, tempHierarchy);        //给每一个TextView谁知标记Tag，代表此TextView的层次id

            final List<DebugFragmentRecord> childFragmentRecord = child.childFragmentRecord;
            if (childFragmentRecord != null && childFragmentRecord.size() > 0) {
                /**处理当前条目是否拥有子条目，即此Fragment是否包含子Fragment，如果包含子View则当前父view可点击触发展开列表*/
                tempHierarchy++;         //层次id加一
                childTvItem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.fragmentation_ic_right, 0, 0, 0);   //如果有子View。则设置左边箭头图片
                final int finalChilHierarchy = tempHierarchy;
                childTvItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //校验当前被点击的TextView是否已经是展开列表形态
                        if (v.getTag(R.id.isexpand) != null) {
                            boolean isExpand = (boolean) v.getTag(R.id.isexpand);
                            if (isExpand) {
                                //如果是展开就闭合，同时修正左边的箭头图标
                                childTvItem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.fragmentation_ic_right, 0, 0, 0);
                                DebugHierarchyViewContainer.this.removeView(finalChilHierarchy);
                            } else {
                                handleExpandView(childFragmentRecord, finalChilHierarchy, childTvItem);

                            }
                            v.setTag(R.id.isexpand, !isExpand);
                        } else {       //如果未取到此标志（展开状态），则认定是第一次点击，则设置上去，同时确认触发展开事件
                            childTvItem.setTag(R.id.isexpand, true);
                            handleExpandView(childFragmentRecord, finalChilHierarchy, childTvItem);
                        }
                    }
                });
            } else {            //如果没有子View，则设置在计算的左Padding之上再加上16dp。
                childTvItem.setPadding(childTvItem.getPaddingLeft() + mPadding, 0, mPadding, 0);
            }

            if (tvItem == null) {               //如果未传入父层级TextView则当前TextView是第一层的TextView。
                mLinearLayout.addView(childTvItem);
            } else {                            //如果未传入父层级TextView则当前TextView是第一层的TextView。添加的位置是父层次加1
                mLinearLayout.addView(childTvItem, mLinearLayout.indexOfChild(tvItem) + 1);
            }
        }
    }

    private void handleExpandView(List<DebugFragmentRecord> childFragmentRecord, int finalChilHierarchy, TextView childTvItem) {
        DebugHierarchyViewContainer.this.setView(childFragmentRecord, finalChilHierarchy, childTvItem);
        childTvItem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.fragmentation_ic_expandable, 0, 0, 0);
    }

    /**移除view的方法，传入的hierarchy 含义是展开后view 的tag*/
    private void removeView(int hierarchy) {
        int size = mLinearLayout.getChildCount();
        for (int i = size - 1; i >= 0; i--) {
            View view = mLinearLayout.getChildAt(i);
            if (view.getTag(R.id.hierarchy) != null && (int) view.getTag(R.id.hierarchy) >= hierarchy) {
                /**移除展开后显示的view */
                mLinearLayout.removeView(view);
            }
        }
    }

    /** 生成Itme的内容view */
    private TextView getTextView(DebugFragmentRecord fragmentRecord, int hierarchy) {
        TextView tvItem = new TextView(mContext);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mItemHeight);
        tvItem.setLayoutParams(params);  //设置高度通过布局参数
        if (hierarchy == 0) {            //如果是第一层item则设置
            tvItem.setTextColor(Color.parseColor("#333333"));
            tvItem.setTextSize(16);    //设置字体为16sp
        }
        tvItem.setGravity(Gravity.CENTER_VERTICAL);
        tvItem.setPadding((int) (mPadding + hierarchy * mPadding * 1.5), 0, mPadding, 0); //左Padding成倍数增加
        tvItem.setCompoundDrawablePadding(mPadding / 2);  //设置图片padding为1/2

        TypedArray a = mContext.obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
        tvItem.setBackgroundDrawable(a.getDrawable(0));   //设置背景
        a.recycle();

        tvItem.setText(fragmentRecord.fragmentName);       //设置内容

        return tvItem;
    }
}
