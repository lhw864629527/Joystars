package com.justonebean.shortvideo.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.justonebean.shortvideo.R;
import com.justonebean.shortvideo.common.Constants;

import java.io.File;

/**
 * Created on 2017-10-09.
 * Created by JustOneBean.
 * Description:
 */

public class VideoEditAdapter extends RecyclerView.Adapter {

    private File[] lists;
    private LayoutInflater inflater;

    private int itemW;
    private Context context;

    public VideoEditAdapter(Context context, int itemW) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.itemW = itemW;
    }

    public void updateVideoEditAdapter() {
        File dir = new File(Constants.ThumbnailsDirPath);

        if (dir.exists()) {
            lists = dir.listFiles();
            notifyDataSetChanged();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new EditViewHolder(inflater.inflate(R.layout.video_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        EditViewHolder viewHolder = (EditViewHolder) holder;

        Glide.with(context)
                .load(lists[position])
                .into(viewHolder.img);
    }

    @Override
    public int getItemCount() {
        if (lists == null || lists.length == 0) {
            return 0;
        }
        return lists.length;
    }

    private final class EditViewHolder extends RecyclerView.ViewHolder {
        public ImageView img;

        EditViewHolder(View itemView) {
            super(itemView);
            img = (ImageView) itemView.findViewById(R.id.video_item_image);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) img.getLayoutParams();
            layoutParams.width = itemW;
            img.setLayoutParams(layoutParams);
        }
    }


    /**
     * 恢复初始化
     */
    public void resetVideoInfo() {
        lists = null;
        notifyDataSetChanged();
    }
}

