package cn.tee3.avei.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import cn.tee3.avei.R;

/**
 * 重要事件的展示
 * Created by shengf on 2017/6/19.
 */

public class EventLogView extends LinearLayout {

    private LinearLayout llMessage;//主要信息模块
    private ScrollView svMessage;
    Context mContext;

    public EventLogView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        /**
         * 初始化控件
         */
        View view = LayoutInflater.from(context).inflate(
                R.layout.event_log_view_layout, this);

        llMessage = (LinearLayout) findViewById(R.id.ll_message);
        svMessage = (ScrollView) findViewById(R.id.sv_message);
    }

    public void addEventLog(String messageStr, TextView tvMessage) {
        tvMessage.setText(messageStr);
        llMessage.addView(tvMessage);
//        需要注意的是，
//        fullScroll(ScrollView.FOCUS_DOWN);该方法不能直接被调用
//        因为Android很多函数都是基于消息队列来同步，所以需要一部操作，
//        addView完之后，不等于马上就会显示，而是在队列中等待处理，虽然很快，但是如果立即调用fullScroll， view可能还没有显示出来，所以会失败
//                应该通过handler在新线程中更新
        svMessage.post(new Runnable() {
            @Override
            public void run() {
                svMessage.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    /**
     * 加入zhengc
     * @param messageStr
     */
    public void addDetailsLog(String messageStr) {
        TextView tvMessage = new TextView(mContext);
        addEventLog(messageStr, tvMessage);
    }

    public void addNormalLog(String messageStr) {
        TextView tvMessage = new TextView(mContext);
        tvMessage.setTextColor(getResources().getColor(R.color.colorAccent));
        addEventLog(messageStr, tvMessage);
    }

    public void addImportantLog(String messageStr) {
        TextView tvMessage = new TextView(mContext);
        tvMessage.setTextColor(getResources().getColor(R.color.colorAccent));
        addEventLog(messageStr, tvMessage);
    }

    public void addVeryImportantLog(String messageStr) {
        TextView tvMessage = new TextView(mContext);
        tvMessage.setTextColor(getResources().getColor(R.color.red));
        addEventLog(messageStr, tvMessage);
    }
}
