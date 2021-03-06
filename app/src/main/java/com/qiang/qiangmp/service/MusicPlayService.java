package com.qiang.qiangmp.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.qiang.qiangmp.QiangMpApplication;
import com.qiang.qiangmp.bean.Song;
import com.qiang.qiangmp.util.DbUtil;
import com.qiang.qiangmp.util.MyLog;
import com.qiang.qiangmp.util.Player;
import com.qiang.qiangmp.util.QiangMPConstants;
import com.qiang.qiangmp.util.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author xiaoq
 * @date 19-1-26
 */
public class MusicPlayService extends Service {
    private static final String TAG = "MusicPlayService";

    private Player player = Player.getInstance();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startNewMusic(intent);
        storeSongOnThread();
        return super.onStartCommand(intent, flags, startId);
    }

    private void storeSongOnThread() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNamePrefix("operate-database-thread")
                .setDaemon(false)
                .build();
        ExecutorService singleThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024), namedThreadFactory);
        singleThreadPool.execute(this::storePlayedSong);
        singleThreadPool.shutdown();
    }

    /**
     * 将最近播放歌曲写入数据库
     */
    private void storePlayedSong() {
        // 根据将要播放的歌曲在播放列表中的位置，获取当前播放歌曲信息
        Song song = QiangMpApplication.globalSongList.get(QiangMpApplication.globalSongPos);
        String name = song.getName();
        String singer = song.getSinger();
        String url = song.getUrl();

        // 删除最近播放列表中已存在的歌曲
        DbUtil.deleteOnSong(name, singer, url);

        // 如果最近播放列表已满，则把表头元素删除。
        if (player.getPlayedSongCount() >= QiangMPConstants.MAX_SONG_PLAY_COUNT) {
            int count = DbUtil.deleteOnSong(1);
            if (count == 0) {
                MyLog.e(TAG, "删除数据库表头字段失败");
                return;
            }
            DbUtil.insertOnSong(player.getPlayedSongCount(), name, singer, url);
        } else {
            DbUtil.insertOnSong(name, singer, url);
            player.setPlayedSongCount(player.getPlayedSongCount() + 1);
        }
    }


    private void startNewMusic(Intent intent) {
        String url = intent.getStringExtra("url");
        player.playUrl(url, this);
    }

    public void startMusicTimeThread() {
        new MusicTimeThread().start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class MusicTimeThread extends Thread {
        @Override
        public void run() {
            int duration = player.getDuration();
            while (player != null) {
                if (player.isPlaying()) {
                    int time = player.getCurrentPosition();
                    // 若当前歌曲播放完成，结束线程
                    if (time == duration) {
                        break;
                    }
                    Intent intent = new Intent(QiangMPConstants.ACTION_SONG_CURRENT_POSITION);
                    intent.putExtra("time", time);
                    intent.putExtra("serial_num", QiangMPConstants.NUM_SONG_CURRENT_POSITION);
                    sendBroadcast(intent);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
