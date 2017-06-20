package cn.tee3.avei.export;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import cn.tee3.avd.MVideo;
import cn.tee3.avei.Constants;
import cn.tee3.avei.R;

/**
 * CamerasAdapter
 * 视频摄像头信息列表adapter
 * Created by shengf on 2017/6/5.
 */

public class CamerasAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater;
    private List<MVideo.Camera> MVideoCameras;//视频列表

    public CamerasAdapter(Context context, List<MVideo.Camera> MVideoCameras) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.MVideoCameras = MVideoCameras;
    }

    @Override
    public int getCount() {
        return MVideoCameras.size();
    }

    @Override
    public Object getItem(int position) {
        return MVideoCameras.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View curView = convertView;
        if (curView == null) {
            curView = inflater.inflate(R.layout.cameras_item, parent, false);
        }
        ViewHolder tHolder = (ViewHolder) curView.getTag();
        if (tHolder == null) {
            tHolder = new ViewHolder();
        }
        MVideo.Camera cameraInfo = MVideoCameras.get(position);
        tHolder.tvCameraName = (TextView) curView.findViewById(R.id.tv_camera_name);
        tHolder.tvCameraName.setText(cameraInfo.getName());
        if (cameraInfo.getId().equals(Constants.SELECT_CAMERA_ID)) {
            curView.setBackgroundResource(R.color.itemSelectBg);
        } else {
            curView.setBackgroundResource(R.color.itemBg);
        }
        curView.setTag(tHolder);
        return curView;
    }

    public final class ViewHolder {
        private TextView tvCameraName;
    }
}
