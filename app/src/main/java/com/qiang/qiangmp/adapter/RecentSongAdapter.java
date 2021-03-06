package com.qiang.qiangmp.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.qiang.qiangmp.R;
import com.qiang.qiangmp.bean.Song;
import com.qiang.qiangmp.service.MusicPlayService;
import com.qiang.qiangmp.util.Player;

import java.util.List;

import static com.qiang.qiangmp.QiangMpApplication.globalSongList;
import static com.qiang.qiangmp.QiangMpApplication.globalSongPos;

/**
 * @author xiaoqiang
 * @date 19-3-7
 */
public class RecentSongAdapter extends RecyclerView.Adapter<RecentSongAdapter.MyViewHolder> {
    private static final String TAG = "RecentSongAdapter";
    private Context context;
    private List<Song> data;

    public RecentSongAdapter(Context context, List<Song> data) {
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_navigation_recycle_view, viewGroup, false);
        view.setOnClickListener(v -> {
            globalSongList.clear();
            globalSongList.addAll(data);
            globalSongPos = (int) v.getTag();
            Song song = globalSongList.get(globalSongPos);
            String url = song.getUrl();
            Player player = Player.getInstance();
            player.setName(song.getName());
            player.setSinger(song.getSinger());
            Intent intent = new Intent(context, MusicPlayService.class);
            intent.putExtra("url", url);
            context.startService(intent);
        });
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
        Song song = data.get(i);
        myViewHolder.tvName.setText(song.getName());
        myViewHolder.tvSinger.setText(song.getSinger());
        myViewHolder.itemView.setTag(i);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSinger;
        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvSinger = itemView.findViewById(R.id.tv_singer);
        }
    }
}
