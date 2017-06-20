package cn.tee3.avei.files;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import cn.tee3.avei.R;
import cn.tee3.avei.utils.FilesUtils;

/**
 * FileAdapter
 * 文件列表Adapter
 * Created by shengf on 2017/6/12.
 */

public class FileAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater;
    private ArrayList<File> fileList = new ArrayList<File>();

    public FileAdapter(Context context, ArrayList<File> fileList) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.fileList = fileList;
    }

    @Override
    public int getCount() {
        return fileList.size();
    }

    @Override
    public Object getItem(int position) {
        return fileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View curView = convertView;
        if (curView == null) {
            curView = inflater.inflate(R.layout.file_item, parent, false);
        }
        ViewHolder tHolder = (ViewHolder) curView.getTag();
        if (tHolder == null) {
            tHolder = new FileAdapter.ViewHolder();
        }
        File file = fileList.get(position);
        tHolder.tvFileName = (TextView) curView.findViewById(R.id.tv_file_name);
        tHolder.tvFileSize = (TextView) curView.findViewById(R.id.tv_file_size);

        tHolder.tvFileName.setText(file.getName());
        tHolder.tvFileSize.setText(FilesUtils.getAutoFileOrFilesSize(file.getPath()));

        curView.setTag(tHolder);
        return curView;
    }

    public final class ViewHolder {
        private TextView tvFileName;
        private TextView tvFileSize;
    }
}
