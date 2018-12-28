package com.xery.whatsappclone.Chat;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.xery.whatsappclone.R;

import java.util.ArrayList;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaVH> {

    ArrayList<String> mediaList;
    Context context;

    public MediaAdapter(Context context, ArrayList<String> mediaList) {
        this.mediaList = mediaList;
        this.context = context;
    }

    @NonNull
    @Override
    public MediaVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media, null, false);
        MediaVH mvh = new MediaVH(layoutView);

        return mvh;
    }

    @Override
    public void onBindViewHolder(@NonNull MediaVH holder, int position) {
        Glide.with(context).load(Uri.parse(mediaList.get(position))).into(holder.mMedia);
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public class MediaVH extends RecyclerView.ViewHolder {
        ImageView mMedia;

        public MediaVH(View itemView) {
            super(itemView);

            mMedia = itemView.findViewById(R.id.media);
        }
    }

}
