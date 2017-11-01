package com.justonebean.shortvideo.common;

import android.os.Environment;

import java.io.File;

/**
 * Created on 2017-10-10.
 * Created by JustOneBean.
 * Description:
 */

public class Constants {

    //音频文件夹
    public static final String AudioTempDirPath =
            Environment.getExternalStorageDirectory()
                    + File.separator
                    + "JustOneBean"
                    + File.separator
                    + "ShortVideo"
                    + File.separator
                    + "AudioTemp"
                    + File.separator;

    //视频 文件夹
    public static final String VideoTempDirPath =
            Environment.getExternalStorageDirectory()
                    + File.separator
                    + "JustOneBean"
                    + File.separator
                    + "ShortVideo"
                    + File.separator
                    + "VideoTemp"
                    + File.separator;

    //图片文件夹
    public static final String PhotosDirPath =
            Environment.getExternalStorageDirectory()
                    + File.separator
                    + "JustOneBean"
                    + File.separator
                    + "ShortVideo"
                    + File.separator
                    + "photos"
                    + File.separator;

    //图片缓存文件夹
    public static final String ThumbnailsDirPath =
            Environment.getExternalStorageDirectory()
                    + File.separator
                    + "JustOneBean"
                    + File.separator
                    + "ShortVideo"
                    + File.separator
                    + "thumbnails"
                    + File.separator;

    //短视频文件夹
    public static final String CutVideoDirPath =
            Environment.getExternalStorageDirectory()
                    + File.separator
                    + "JustOneBean"
                    + File.separator
                    + "ShortVideo"
                    + File.separator
                    + "MyCutVideo"
                    + File.separator;

    public static final String VIDEO_PATH="video_path";
}
