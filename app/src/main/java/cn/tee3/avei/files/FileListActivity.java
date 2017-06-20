package cn.tee3.avei.files;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

import cn.tee3.avei.AveiApp;
import cn.tee3.avei.R;
import cn.tee3.avei.utils.FilesUtils;
import cn.tee3.avei.utils.StringUtils;

/**
 * 已下载文件列表
 * Created by shengf on 2017/6/12.
 */

public class FileListActivity extends Activity implements AdapterView.OnItemClickListener {

    private ArrayList<File> fileList = new ArrayList<File>();
    private FileAdapter fAdapter;
    private ListView lvFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_list_layout);
        lvFiles = (ListView) findViewById(R.id.lv_files);
        lvFiles.setOnItemClickListener(this);
        fAdapter = new FileAdapter(this, fileList);
        lvFiles.setAdapter(fAdapter);
        searchFile(AveiApp.getTee3Dir());
    }


    private void searchFile(String Path) {
        File[] files = new File(Path).listFiles();
        for (File file : files) {
            String filePath = file.getPath();
            String fileType = FilesUtils.getFileType(filePath);//文件后缀
            if (StringUtils.isNotEmpty(fileType)) {
                if (fileType.equals("mp4") || fileType.equals("webm")) {
                    fileList.add(file);
                }
            }
        }
        fAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File file = fileList.get(position);
        Intent intent_open = new Intent();
        intent_open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //设置intent的Action属性
        intent_open.setAction(Intent.ACTION_VIEW);
        //设置intent的data和Type属性。
        intent_open.setDataAndType(Uri.fromFile(file), "video/*");
        //跳转
        startActivity(intent_open);
    }
}
