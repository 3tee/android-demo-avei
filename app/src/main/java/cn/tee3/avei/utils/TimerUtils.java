package cn.tee3.avei.utils;

import android.widget.TextView;

/**
 * 更新时间
 * Created by shengf on 2017/6/19.
 */

public class TimerUtils {
    private TextView tvImportTime;
    private int importTime = 0;//导入时长
    private static String prefixLabel;

    public TimerUtils(TextView tvImportTime) {
        this.tvImportTime = tvImportTime;
    }

    /**
     * 更新定时器计时
     */
    public void updateTimer() {
        importTime = 0;
        tvImportTime.post(update_thread);
    }

    /**
     * 清除定时器
     */
    public void clearTimer() {
        tvImportTime.removeCallbacks(update_thread);
    }

    /**
     * 更新计时器前的信息
     *
     * @param label
     */
    public static void updatePrefixLabel(String label) {
        prefixLabel = label;
    }

    Runnable update_thread = new Runnable() {
        public void run() {
            String hs, ms, ss;
            long h, m, s;
            h = importTime / 3600;
            m = (importTime % 3600) / 60;
            s = (importTime % 3600) % 60;
            if (h < 10) {
                hs = "0" + h;
            } else {
                hs = "" + h;
            }
            if (m < 10) {
                ms = "0" + m;
            } else {
                ms = "" + m;
            }
            if (s < 10) {
                ss = "0" + s;
            } else {
                ss = "" + s;
            }
            String curTime = hs + ":" + ms + ":" + ss;
            if (StringUtils.isNotEmpty(prefixLabel)) {
                curTime = prefixLabel + curTime;
            }
            tvImportTime.setText(curTime);
            importTime++;
            tvImportTime.postDelayed(update_thread, 1000);
        }
    };
}
