package com.justonebean.shortvideo.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.justonebean.shortvideo.R;
import com.justonebean.shortvideo.adapter.VideoEditAdapter;
import com.justonebean.shortvideo.common.Constants;
import com.justonebean.shortvideo.tools.DisplayUtil;
import com.justonebean.shortvideo.tools.FileUtil;
import com.justonebean.shortvideo.view.RangeSeekBar;

import java.io.File;


/**
 * Created on 2017-10-7.
 * Created by JustOneBean.
 * Address：JustOneBean@outlook.com
 * Description:
 */

public class VideoEditActivity extends AppCompatActivity {

    //权限请求
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 0x100;

    //视频最多剪切多长时间
    private static final long MAX_CUT_DURATION = 15_000L;

    //UI声明
    private TextView mSelectVideo;
    private VideoView mVideoView;
    private RecyclerView mVideoList;
    private LinearLayout mSeekBarLayout;

    //总时长
    private long duration;
    //视频拖动 进度条
    private RangeSeekBar mVideoSeekBar;


    //保存当前的播放状态
    private int currentPlayPosition = 0;

    //当前视频地址
    private String mVideoPath;

    //正在滑动
    private boolean isSeeking = false;

    private VideoEditAdapter videoEditAdapter;

    private long leftProgress, rightProgress;
    private long scrollPos = 0;

    private ProgressDialog progressDialog;

    private FFmpeg mFFmpeg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_edit);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mSelectVideo = (TextView) findViewById(R.id.activity_video_edit_select_video);
        mVideoView = (VideoView) findViewById(R.id.activity_video_edit_videoview);
        mVideoList = (RecyclerView) findViewById(R.id.activity_video_edit_videolist);
        mSeekBarLayout = (LinearLayout) findViewById(R.id.activity_video_edit_seekBarLayout);

        mVideoList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        videoEditAdapter = new VideoEditAdapter(this, (DisplayUtil.getScreenWidth(this) - DisplayUtil.dip2px(this, 70)) / 10);
        mVideoList.setAdapter(videoEditAdapter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);

        mSelectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //恢复初始化
                videoEditAdapter.resetVideoInfo();
                try {
                    Intent intent = new Intent();
                    intent.setType("video/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "请选择视频文件"), REQUEST_TAKE_GALLERY_VIDEO);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        loadFFMpegBinary();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {

                Uri selectedVideoUri = data.getData();

                mVideoPath = selectedVideoUri.getPath();

                initEditVideo();
            }
        }
    }

    private void loadFFMpegBinary() {
        try {
            if (mFFmpeg == null) {
                mFFmpeg = FFmpeg.getInstance(this);
            }
            mFFmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }

                @Override
                public void onSuccess() {
                    Toast.makeText(VideoEditActivity.this, "FFmpeg初始化成功", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放 视频
     */
    private void startPlayVideo(String videoPath) {

        mVideoView.setVideoPath(videoPath);

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);

                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        if (!isSeeking) {
                            mVideoView.start();
                        }
                    }
                });
            }
        });
        mVideoView.start();
    }

    /**
     * 暂停 视频
     */
    private void pausePlayVideo() {
        isSeeking = false;
        if (mVideoView != null && mVideoView.isPlaying()) {
            currentPlayPosition = mVideoView.getCurrentPosition();
            mVideoView.pause();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (mVideoView != null) {
            mVideoView.seekTo(currentPlayPosition);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null && mVideoView.isPlaying()) {
            pausePlayVideo();
        }
    }

    private void initEditVideo() {
        MediaMetadataRetriever mMetadataRetriever = new MediaMetadataRetriever();
        mMetadataRetriever.setDataSource(mVideoPath);

        duration = Long.valueOf(mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

        extractImagesVideo(0L, duration);

        if (duration <= 15_000L) {
            mVideoSeekBar = new RangeSeekBar(this, 0L, duration);
            mVideoSeekBar.setSelectedMinValue(0L);
            mVideoSeekBar.setSelectedMaxValue(duration);

            rightProgress = duration;
        } else {
            mVideoSeekBar = new RangeSeekBar(this, 0L, 15_000L);
            mVideoSeekBar.setSelectedMinValue(0L);
            mVideoSeekBar.setSelectedMaxValue(15_000L);

            rightProgress = MAX_CUT_DURATION;
        }
        //设置最小裁剪时间
        mVideoSeekBar.setMin_cut_time(5_000L);
        mVideoSeekBar.setNotifyWhileDragging(true);
        mVideoSeekBar.setOnRangeSeekBarChangeListener(mOnRangeSeekBarChangeListener);
        mSeekBarLayout.addView(mVideoSeekBar);

        mVideoList.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width = mVideoList.getWidth();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSeekBarLayout.getLayoutParams();
                params.width = width;
                mSeekBarLayout.setLayoutParams(params);
            }
        });
    }

    private final RangeSeekBar.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = new RangeSeekBar.OnRangeSeekBarChangeListener() {

        @Override
        public void onRangeSeekBarValuesChanged(RangeSeekBar bar, long minValue, long maxValue, int action, boolean isMin, RangeSeekBar.Thumb pressedThumb) {

            leftProgress = minValue + scrollPos;
            rightProgress = maxValue + scrollPos;

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isSeeking = false;
                    pausePlayVideo();
                    break;

                case MotionEvent.ACTION_MOVE:
                    isSeeking = true;
                    mVideoView.seekTo((int) (pressedThumb == RangeSeekBar.Thumb.MIN ? leftProgress : rightProgress));
                    break;

                case MotionEvent.ACTION_UP:
                    isSeeking = false;
                    //从minValue开始播
                    mVideoView.seekTo((int) leftProgress);
                    break;

                default:
                    break;
            }
        }
    };

    /**
     * 显示不支持 对话框
     */
    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(VideoEditActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("提示")
                .setMessage("设备不支持")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        VideoEditActivity.this.finish();
                    }
                })
                .create()
                .show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //返回按钮
            case android.R.id.home:
                finish();
                break;

            case R.id.main_action_next:
                if (!TextUtils.isEmpty(mVideoPath)) {

                    if (mVideoView != null) {
                        mVideoView.stopPlayback();
                    }

                    long start = mVideoSeekBar.getSelectedMinValue();
                    long last = mVideoSeekBar.getSelectedMaxValue();
                    executeCutVideoCommand(start, last);
                } else {
                    Toast.makeText(this, "请选择视频文件", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 剪切视频
     */
    private void executeCutVideoCommand(long startMs, long endMs) {

        File videoDir = new File(Constants.CutVideoDirPath);
        if (!videoDir.exists()) {
            videoDir.mkdirs();
        }
        File dest = new File(videoDir, "Mycut_video.mp4");
        int fileNo = 0;
        while (dest.exists()) {
            fileNo++;
            dest = new File(videoDir, "Mycut_video" + fileNo + ".mp4");
        }

        final String MyvideoPath = dest.getAbsolutePath();

        String[] complexCommand = {"-ss", "" + startMs / 1000, "-y", "-i", mVideoPath, "-t", "" + (endMs - startMs) / 1000, "-vcodec", "mpeg4", "-b:v", "2097152", "-b:a", "48000", "-ac", "2", "-ar", "22050", MyvideoPath};

        try {
            mFFmpeg.execute(complexCommand, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Toast.makeText(VideoEditActivity.this, "faild with output : " + s, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(String s) {
                    Intent intent = new Intent(VideoEditActivity.this, VideoPreviewActivity.class);
                    intent.putExtra(Constants.VIDEO_PATH, MyvideoPath);
                    VideoEditActivity.this.startActivity(intent);
                }

                @Override
                public void onProgress(String s) {
                    progressDialog.setMessage("Processing...");
                }

                @Override
                public void onStart() {
                    progressDialog.show();
                }

                @Override
                public void onFinish() {
                    progressDialog.dismiss();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得视频 图片
     */
    private void extractImagesVideo(long startMs, long endMs) {

        File dir = new File(Constants.ThumbnailsDirPath);
        if (dir.exists()) {
            FileUtil.deleteDirectoryAndUnderFiles(Constants.ThumbnailsDirPath);
        }
        dir.mkdirs();

        String filePrefix = "thumbnail_picture";
        String fileExtn = ".jpg";

        File dest = new File(dir, filePrefix + "%03d" + fileExtn);

        String[] complexCommand = {"-y", "-i", mVideoPath, "-an", "-r", "1", "-ss", "" + startMs / 1000, "-t", "" + (endMs - startMs) / 1000, dest.getAbsolutePath()};

        try {
            mFFmpeg.execute(complexCommand, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Toast.makeText(VideoEditActivity.this, "faild with output : " + s, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(String s) {
                    mHandler.sendEmptyMessage(UPDATE_UI_MSG);
                }

                @Override
                public void onProgress(String s) {
                    progressDialog.setMessage("正在生成图片...");
                }

                @Override
                public void onStart() {
                    progressDialog.show();
                }

                @Override
                public void onFinish() {
                    progressDialog.dismiss();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }


    //更新界面
    private static final int UPDATE_UI_MSG = 0x100;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_UI_MSG) {
                if (videoEditAdapter != null) {
                    videoEditAdapter.updateVideoEditAdapter();
                }

                if (!TextUtils.isEmpty(mVideoPath)) {
                    startPlayVideo(mVideoPath);
                } else {
                    Toast.makeText(VideoEditActivity.this, "请选择视频文件", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }

        mHandler.removeMessages(UPDATE_UI_MSG);
    }
}
