package com.justonebean.shortvideo.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.justonebean.shortvideo.R;
import com.justonebean.shortvideo.common.Constants;
import com.justonebean.shortvideo.tools.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created on 2017-10-10.
 * Created by JustOneBean.
 * Description:
 */

public class VideoPreviewActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private static final int REQUEST_SELECT_AUDIO = 0x100;

    private VideoView mVideoView;
    private SeekBar mAudioSeekBar, mMusicSeekBar;

    private MediaPlayer mAudioPlayer;
    private MediaPlayer mMusicPlayer;

    //选择的音乐路径
    private String mMusicPath;

    private ProgressDialog progressDialog;

    private FFmpeg mFFmpeg;

    //保存当前的播放状态
    private int currentVideoPlayPosition = 0;
    private int currentMusicPlayPosition = 0;
    private int currentAudioPlayPosition = 0;

    //音频，无声视频的保存位置
    private String audioDestPath, videoDestPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mVideoView = (VideoView) findViewById(R.id.activity_video_preview_videoview);
        mAudioSeekBar = (SeekBar) findViewById(R.id.activity_video_preview_audio_seekbar);
        mMusicSeekBar = (SeekBar) findViewById(R.id.activity_video_preview_music_seekbar);

        mAudioSeekBar.setOnSeekBarChangeListener(this);
        mMusicSeekBar.setOnSeekBarChangeListener(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);

        loadFFMpegBinary();

        findViewById(R.id.activity_video_preview_select_audio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent();
                    intent.setType("audio/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "请选择音频文件"), REQUEST_SELECT_AUDIO);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void loadFFMpegBinary() {
        try {
            if (mFFmpeg == null) {
                mFFmpeg = FFmpeg.getInstance(this);
            }
            mFFmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                }

                @Override
                public void onSuccess() {
                    String videoPath = getIntent().getStringExtra(Constants.VIDEO_PATH);
                    extractAudioFromVideo(videoPath);
                }
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_SELECT_AUDIO) {

                Uri selectedVideoUri = data.getData();

                mMusicPath = selectedVideoUri.getPath();
                if (!TextUtils.isEmpty(mMusicPath)) {

                    mMusicSeekBar.setProgress(50);
                    mAudioSeekBar.setProgress(50);

                    mVideoView.seekTo(0);
                    mVideoView.start();

                    mAudioPlayer.seekTo(0);
                    mAudioPlayer.start();

                    mMusicPlayer = new MediaPlayer();
                    try {
                        mMusicPlayer.setDataSource(mMusicPath);
                        mMusicPlayer.setLooping(true);
                        mMusicPlayer.setVolume(0.5f, 0.5f);
                        mMusicPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //刚开始时 需要自动播放
                    mMusicPlayer.start();
                }
            } else {
                Toast.makeText(this, "请选择音频文件", Toast.LENGTH_SHORT).show();
            }
        }
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
                if (!TextUtils.isEmpty(mMusicPath)) {
//                    executeCutVideoCommand(start, last);

                    MergeAllAudio();

                } else {
                    Toast.makeText(this, "请选择音频文件", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void pausePlayVideo() {
        if (mVideoView != null && mVideoView.isPlaying()) {
            currentVideoPlayPosition = mVideoView.getCurrentPosition();
            mVideoView.pause();
        }
    }

    private void pausePlayAudio() {
        if (mAudioPlayer != null && mAudioPlayer.isPlaying()) {
            currentAudioPlayPosition = mAudioPlayer.getCurrentPosition();
            mAudioPlayer.pause();
        }
    }

    private void pausePlayMusic() {
        if (mMusicPlayer != null && mMusicPlayer.isPlaying()) {
            currentMusicPlayPosition = mMusicPlayer.getCurrentPosition();
            mMusicPlayer.pause();
        }
    }

    private void stopPlayAudio() {
        try {
            if (mAudioPlayer != null) {
                mAudioPlayer.stop();
                mAudioPlayer.release();
                mAudioPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopPlayMusic() {
        try {
            if (mMusicPlayer != null) {
                mMusicPlayer.stop();
                mMusicPlayer.release();
                mMusicPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (mVideoView != null) {
            mVideoView.seekTo(currentVideoPlayPosition);
        }
        if (mAudioPlayer != null) {
            mAudioPlayer.seekTo(currentAudioPlayPosition);
            mAudioPlayer.start();
        }
        if (mMusicPlayer != null) {
            mMusicPlayer.seekTo(currentMusicPlayPosition);
            mMusicPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pausePlayVideo();
        pausePlayAudio();
        pausePlayMusic();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlayAudio();
        stopPlayMusic();
        if (mVideoView != null) {
            mVideoView.stopPlayback();
            mVideoView = null;
        }

        mHandler.removeMessages(EXTRACT_AUDIO_AND_VIDEO);
    }

    /**
     * 提取音频
     */
    private void extractAudioFromVideo(final String inputFileAbsolutePath) {

        File dir = new File(Constants.AudioTempDirPath);
        if (dir.exists()) {
            FileUtil.deleteDirectoryAndUnderFiles(Constants.AudioTempDirPath);
        }
        dir.mkdirs();

        File dest = new File(dir, "extract_audio.aac");
        int fileNo = 0;
        while (dest.exists()) {
            fileNo++;
            dest = new File(dir, "extract_audio" + fileNo + ".aac");
        }

        audioDestPath = dest.getAbsolutePath();

        String[] complexCommand = {"-y", "-i", inputFileAbsolutePath, "-vn", "-ar", "44100", "-ac", "2", "-b:a", "256k", "-f", "mp3", audioDestPath};
        try {
            mFFmpeg.execute(complexCommand, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Toast.makeText(VideoPreviewActivity.this, "faild with output : " + s, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(String s) {
                    removeAudioFromVideo(inputFileAbsolutePath);
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
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    /**
     * 提取视频
     */
    private void removeAudioFromVideo(String inputFileAbsolutePath) {

        File dir = new File(Constants.VideoTempDirPath);
        if (dir.exists()) {
            FileUtil.deleteDirectoryAndUnderFiles(Constants.VideoTempDirPath);
        }
        dir.mkdirs();

        File dest = new File(dir, "extract_video.mp4");
        int fileNo = 0;
        while (dest.exists()) {
            fileNo++;
            dest = new File(dir, "extract_video" + fileNo + ".mp4");
        }

        videoDestPath = dest.getAbsolutePath();

        String[] complexCommand = {"-y", "-i", inputFileAbsolutePath, "-an", "-b:v", "2097k", "-r", "60", "-vcodec", "mpeg4", videoDestPath};
        try {
            mFFmpeg.execute(complexCommand, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    progressDialog.dismiss();
                    Toast.makeText(VideoPreviewActivity.this, "faild with output : " + s, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(String s) {
                    progressDialog.dismiss();
                    mHandler.sendEmptyMessage(EXTRACT_AUDIO_AND_VIDEO);
                }

                @Override
                public void onProgress(String s) {
                    progressDialog.setMessage("Processing...");
                }

                @Override
                public void onStart() {
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }


    /**
     * 合成所有声音
     */
    private void MergeAllAudio() {

        File dir = new File(Constants.AudioTempDirPath);
        if (dir.exists()) {
            FileUtil.deleteDirectoryAndUnderFiles(Constants.AudioTempDirPath);
        }
        dir.mkdirs();

        File dest = new File(dir, "all_merged_audio.mp3");
        int fileNo = 0;
        while (dest.exists()) {
            fileNo++;
            dest = new File(dir, "all_merged_audio" + fileNo + ".mp3");
        }
        audioDestPath = dest.getAbsolutePath();

        String[] complexCommand = {"-i", audioDestPath, "-i", mMusicPath, "-filter_complex", "[0:0] [1:0]concat=n=2:v=0:a=1[out]", "-map", "[out]", audioDestPath};

        try {
            mFFmpeg.execute(complexCommand, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    progressDialog.dismiss();
                    System.out.println("+++++++++++ " + s);
                    Toast.makeText(VideoPreviewActivity.this, "faild with output : " + s, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(String s) {
                    progressDialog.dismiss();
                    Toast.makeText(VideoPreviewActivity.this, "onSuccess", Toast.LENGTH_SHORT).show();
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
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    private static final int EXTRACT_AUDIO_AND_VIDEO = 0x1000;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == EXTRACT_AUDIO_AND_VIDEO) {
                mVideoView.setVideoPath(videoDestPath);
                mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.setLooping(true);
                        mp.start();
                    }
                });
                mVideoView.start();

                mAudioPlayer = new MediaPlayer();
                try {
                    mAudioPlayer.setDataSource(audioDestPath);
                    mAudioPlayer.setLooping(true);
                    mAudioPlayer.setVolume(0.5f, 0.5f);
                    mAudioPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //刚开始时 需要自动播放
                mAudioPlayer.start();
            }
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        float volume = progress / 100f;
        if (mAudioSeekBar == seekBar) {
            mAudioPlayer.setVolume(volume, volume);
        } else if (mMusicPlayer != null) {
            mMusicPlayer.setVolume(volume, volume);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
