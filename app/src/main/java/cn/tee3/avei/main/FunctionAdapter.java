package cn.tee3.avei.main;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import cn.tee3.avei.Constants;
import cn.tee3.avei.R;
import cn.tee3.avei.model.FunctionModel;

/**
 * 功能列表Adapter
 * Created by shengf on 2017/6/13.
 */

public class FunctionAdapter extends BaseAdapter {

    Context context;
    private LayoutInflater inflater;
    private ArrayList<FunctionModel> functionModels;//视频列表

    public FunctionAdapter(Context context, ArrayList<FunctionModel> functionModels) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.functionModels = functionModels;
    }

    @Override
    public int getCount() {
        return functionModels.size();
    }

    @Override
    public Object getItem(int position) {
        return functionModels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View curView = convertView;
        if (curView == null) {
            curView = inflater.inflate(R.layout.functions_item, parent, false);
        }
        ViewHolder tHolder = (ViewHolder) curView.getTag();
        if (tHolder == null) {
            tHolder = new ViewHolder();
        }
        FunctionModel function = functionModels.get(position);
        tHolder.tvFunctionName = (TextView) curView.findViewById(R.id.tv_function_name);
        tHolder.tvFunctionName.setText(function.getName());

        if (Constants.SELECT_FUNCTION_NAME.equals(function.getName())) {
            curView.setBackgroundResource(R.color.itemSelectBg);
        } else {
            if (function.getType() == FunctionModel.TITLE) {
                tHolder.tvFunctionName.setTextColor(Color.parseColor("#FF0000"));
            } else {
                tHolder.tvFunctionName.setTextColor(Color.parseColor("#333333"));
            }
            curView.setBackgroundResource(R.color.itemBg);
        }
        curView.setTag(tHolder);
        return curView;
    }

    public final class ViewHolder {
        private TextView tvFunctionName;
    }
}