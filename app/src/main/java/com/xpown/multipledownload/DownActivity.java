package com.xpown.multipledownload;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * @author Robert
 * */
public class DownActivity extends Activity{
    //线程锁，如果对这个不懂，百度一下
    private Lock lock = new ReentrantLock();
    //任务集合
    List<DownLoadThread> threads = new ArrayList<DownLoadThread>();
    //任务状态的数据
    List<Map<String,String>> data = new ArrayList<Map<String,String>>();

    private EditText editText;
    private ListView listView;
    private Button button;

    private int threadFinishedCount = 0;//已完成任务的数量
    private int count = 0;				//已添加多少个任务



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_down);
        initViews();
    }

    /**
     * 设置任务列表的数据并刷新显示任务列表
     * */
    private void showListView(){
        SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2, new String[]{"count", "status"}, new int[]{
                android.R.id.text1, android.R.id.text2
        });
        listView.setAdapter(adapter);
        listView.invalidate();
    }
    /**
     * 初始化界面元素
     * */
    private void initViews() {
        editText = (EditText)findViewById(R.id.download_edt);
        button   = (Button)findViewById(R.id.download_btn);
        listView = (ListView)findViewById(R.id.listview1);
        button.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                try {
                    count++;

                    lock.lock();
                    threads.add(new DownLoadThread(
                            new URL(editText.getText().toString()),"/mnt/sdcard/a" + count + ".jpg",myHandler));
                    lock.unlock();

                    Map<String,String> map = new HashMap<String,String>();
                    map.put("status", "等待下载");
                    map.put("count", "线程" + count);
                    data.add(map);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                showListView();

                Message message = new Message();
                message.what = DownLoadThread.THREAD_BEGIN;
                myHandler.sendMessage(message);
            }

        });
    }

    private Handler myHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case DownLoadThread.THREAD_BEGIN:
                    lock.lock();
                    if(threads.size() <= count && threads.size() > threadFinishedCount){
                        if(!threads.get(threadFinishedCount).isStarted()){
                            //开始一个新的下载任务
                            threads.get(threadFinishedCount).start();
                            //设置显示当前任务状态为正在下载
                            data.get(threadFinishedCount).put("status", "下载中……");

                            showListView();
                        }
                    }else{
                        Toast.makeText(DownActivity.this, "无任务了", Toast.LENGTH_LONG);
                    }
                    lock.unlock();
                    break;

                case DownLoadThread.THREAD_FINISHED:
                    lock.lock();
                    if(threads.size() >= threadFinishedCount){
                        //设置当前下载任务已完成
                        data.get(threadFinishedCount).put("status", "下载完成");
                        threadFinishedCount++;
                        //开始下一个任务
                        Message message = new Message();
                        message.what = DownLoadThread.THREAD_BEGIN;
                        sendMessage(message);
                        showListView();
                    }
                    lock.unlock();
                    break;
            }
        }

    };


}
