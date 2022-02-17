package com.example.xielm.myapplication;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.content.Context;
import android.widget.Toast;

import java.net.URLEncoder;

public class mp3_player extends Activity {
    public static android.app.Activity activity;
    public static String mp3_start_imgae_path;//开始键图片路径
    public static String mp3_pause_imgae_path;//开始键图片路径
    public static String mp3_image_path="";//唱片图片路径
    public static String mp3_path="";//mp3路径

    private static String last_current_time;//记录最后播放到的时长
    private static String last_duration;//记录最后一首单曲的总时长

    private static android.os.Handler handler = new android.os.Handler();
    private static android.media.MediaPlayer mediaPlayer =null;
    private static android.widget.TextView current_time;//滑动进度条时同步更新时间栏，而不是滑动完了才更新（如果不加static程序会崩溃）
    private static android.widget.TextView max_duration;//总时长
    private static android.widget.SeekBar skbProgress;//获取进度条
    private static android.widget.ImageButton mp3_start_button;//开始/暂停键

    private static com.example.xielm.myapplication.ImageViewPlus disk_image;//圆形图片

    public static final int state_loaded=1;
    public static final int state_empty=2;
    public static final int state_prepare=3;
    public static int mp3_state=state_empty;


    public static boolean mp3_interface;//确认用户当前界面状态，防止 用户快速切换界面 ，该操作会导致 控件销毁后仍然有对控件变更的操作，操作失败造成的闪退
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3_player);
        activity=this;

        //mp3_pause_imgae_path="https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fhbimg.b0.upaiyun.com%2F0f6e01ca34e436096ac12633f3dd8085dc98dc3536a9-Tk2Y5U_fw658&refer=http%3A%2F%2Fhbimg.b0.upaiyun.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1620958666&t=d96738fdbe5e9bf94f34169eb78cd8b4";
        //mp3_start_imgae_path="https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=2892465335,1906412641&fm=26&gp=0.jpg";
        //mp3_pause_imgae_path =R.mipmap.
        //mp3_start_imgae_path
        //mp3_image_path="";
        //mp3_image_path="https://bkimg.cdn.bcebos.com/pic/b7fd5266d0160924ded4cb66d90735fae7cd34dd?x-bce-process=image/watermark,image_d2F0ZXIvYmFpa2UxMTY=,g_7,xp_5,yp_5/format,f_auto";
        //mp3_image_path="https://p1.music.126.net/Qh-1w8ctMggDPc5RzdgEGg==/109951165700524844.jpg?param=177y177";
        //mp3_path="https://tspc.vvvdj.com/c4/2021/03/210203-0ef112/210203.m3u8?upt=0003232d1621180799&news";

        set_button();//按钮初始化配置
        /*//使用方法：在由主进程onCreate开启的线程中执行以下代码
        android.content.Intent i2 = new android.content.Intent(???.activity, mp3_player.class);
        ???.activity.startActivity(i2);
        telecom.tsleep(500);
        String mp3_image_path="https://p1.music.126.net/Qh-1w8ctMggDPc5RzdgEGg==/109951165700524844.jpg?param=177y177";
        String mp3_path="https://tspc.vvvdj.com/c4/2021/03/210203-0ef112/210203.m3u8?upt=0003232d1621180799&news";
        mp3_player.mp3_ini(mp3_path, mp3_image_path);*/

    }
    @Override
    protected void onDestroy() //销毁该页面时，在activity结束的时候回收资源
    {
        mp3_interface=false;
        nature_refresh=false;
        //mediaPlayer=null;//这句话会导致直接卡死。。。不得不加状态量：video_state
        super.onDestroy();
    }

    //--------------------网络资源测试，如果资源无效或无法正常访问情况，进行提示--------------------------------
    public static int network_state;//网络状态


    public static String E_message;
    private static void reportwrong(String e_message)
    {
        E_message=e_message;
        handler.post(new Runnable() {
            @Override
            public void run() {
                android.widget.Toast.makeText(activity, "错误信息"+E_message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }


    private static boolean source_test_is_wrong()//检测地址源是否有错，此函数要在非handler线程中执行
    {
                try {
                    java.net.URL urlObj = new java.net.URL(mp3_path);
                    java.net.HttpURLConnection oc = (java.net.HttpURLConnection) urlObj.openConnection();
                    oc.setUseCaches(false);
                    oc.setConnectTimeout(5000); // 设置超时时间
                    oc.connect();
                    network_state = oc.getResponseCode();// 请求状态
                    if (java.net.HttpURLConnection.HTTP_OK == network_state) //如果地址源没错
                    {
                        //什么也不用做
                        return false;
                    } else {
                        android.util.Log.d("Mainactivity", "11111111111111");
                        String e_message;
                        switch (network_state) {
                            case 403:
                                e_message = "你没有权限访问该资源！（403）";
                                break;
                            case 404:
                                e_message = "该网站无该资源！（404）";
                                break;
                            default:
                                e_message = "错误编码：" + network_state;
                                break;
                        }
                        reportwrong(e_message);
                    }
                } catch (Exception e) {//如果出错了
                    android.util.Log.d("Mainactivity", "报错" + e.getMessage());
                    //reportwrong(e.getMessage());
                }
        return true;

    }

    //------------------------------------------------mp3界面各种初始化配置------------------------------------------------------

    public static void mp3_ini(String mp3path,String mp3image_path) //初始化mp3媒体设备配置
    {
        mp3_image_path=mp3image_path;//图片直接赋予链接
        try {
            mp3path=Uri.encode(mp3path, "-![.:/,%?&-]");//网络端资源需要在开头加"http：//"
        }catch (Exception e)
        {
            android.util.Log.d("Mainactivity", "转换url时报错" + e.getMessage());
            return;
        }

        if(mp3_path.equals(mp3path))
        {//同一个单曲，不做变动
        }else
        if(mp3_state==state_loaded||mp3_state==state_prepare) {//播放其他单曲
            mediaPlayer.release();
            mp3_state=state_empty;
            telecom.tsleep(300);
        }

        if(mp3_state==state_empty) {
            mp3_path=mp3path;
            if(source_test_is_wrong()) return;

            try {
                mediaPlayer = new android.media.MediaPlayer();
                //mediaPlayer.setDataSource(mp3_path);//指定音频文件路径,API27以上程序需要添加usesCleartextTraffic权限
                mediaPlayer.setDataSource(mp3_path);
            } catch (Exception e) {
                android.widget.Toast.makeText(activity, "源地址加载失败"+e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                android.util.Log.d("Mainactivity", "源地址加载失败" + e.getMessage());
                mediaPlayer.release();
                return;
            }
            mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
            mediaPlayer.prepareAsync();
            mp3_state=state_prepare;

            mediaPlayer.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
                int i;

                @Override
                public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                    android.util.Log.d("MEMEME", "OnError - Error code: " + what + " Extra code: " + extra);
                    switch (what) {
                        case -1004:
                            android.util.Log.d("MEMEME", "MEDIA_ERROR_IO");
                            break;
                        case -1007:
                            android.util.Log.d("MEMEME", "MEDIA_ERROR_MALFORMED");
                            break;
                        case 200:
                            android.util.Log.d("MEMEME", "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
                            break;
                        case 100:
                            android.util.Log.d("MEMEME", "MEDIA_ERROR_SERVER_DIED");
                            break;
                        case -110:
                            android.util.Log.d("MEMEME", "MEDIA_ERROR_TIMED_OUT");
                            break;
                        case 1:
                            android.util.Log.d("MEMEME", "MEDIA_ERROR_UNKNOWN");
                            break;
                        case -1010:
                            android.util.Log.d("MEMEME", "MEDIA_ERROR_UNSUPPORTED");
                            break;
                    }
                    switch (extra) {
                        case 800:
                            android.util.Log.d("MEMEME", "MEDIA_INFO_BAD_INTERLEAVING");
                            break;
                        case 702:
                            android.util.Log.d("MEMEME", "MEDIA_INFO_BUFFERING_END");
                            break;
                        case 701:
                            android.util.Log.d("MEMEME", "MEDIA_INFO_METADATA_UPDATE");
                            break;
                        case 802:
                            android.util.Log.d("MEMEME", "MEDIA_INFO_METADATA_UPDATE");
                            break;
                        case 801:
                            android.util.Log.d("MEMEME", "MEDIA_INFO_NOT_SEEKABLE");
                            break;
                        case 1:
                            android.util.Log.d("MEMEME", "MEDIA_INFO_UNKNOWN");
                            break;
                        case 3:
                            android.util.Log.d("MEMEME", "MEDIA_INFO_VIDEO_RENDERING_START");
                            break;
                        case 700:
                            android.util.Log.d("MEMEME", "MEDIA_INFO_VIDEO_TRACK_LAGGING");
                            break;
                    }
                    return false;
                }
            });

            mediaPlayer.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
                int i;

                @Override
                public void onPrepared(android.media.MediaPlayer mp) {// 装载完毕激发该函数
                    mp3_start();//开启播放
                    mp3_state = state_loaded;
                    if (mp3_interface) {
                        mp3_start_button.setEnabled(true);
                        skbProgress.setEnabled(true);
                    }
                }
            });

        }
        rotate();//让图片旋转
        mediaPlayer.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
            int i;
            @Override//播放完成时调用,由于stop()会彻底释放资源，所以调用pause()
            public void onCompletion(android.media.MediaPlayer arg0) {
                if (mp3_interface&&mp3_state==state_loaded)
                    current_time.setText(time_switch_int_to_string(mediaPlayer.getDuration()));//将播放到的时间点变为总时长
                mp3_pause();
            }
        });
        mediaPlayer.setOnBufferingUpdateListener(new android.media.MediaPlayer.OnBufferingUpdateListener() {
            int i;

            @Override
            public void onBufferingUpdate(android.media.MediaPlayer arg0, int bufferingProgress) {//更新进度条点所在位置
                if (mp3_interface)
                    skbProgress.setSecondaryProgress(bufferingProgress);//显示已加载缓冲量
                //int currentProgress = skbProgress.getMax() * mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration();
                //android.util.Log.e(currentProgress + "% play", bufferingProgress + "% buffer");
            }
        });
        if (mTimer != null)
            mTimer.cancel();
        mTimer = new java.util.Timer();
        java.util.TimerTask mTimerTask = new java.util.TimerTask() {
            int k;

            @Override
            public void run() {
                if (mp3_state == state_empty || mediaPlayer == null||!mp3_interface) {//如果mp3没有播放任何单曲，或播放页面被销毁了mp3_interface，则停止更新当前播放到的时间
                    mTimer.cancel();//结束计时
                    return;
                }

                if (mp3_state == state_loaded)//如果播放源加载完成（非empty且非prepared）
                    if (mediaPlayer.isPlaying())
                        if (!skbProgress.isPressed())
                            if (nature_refresh) {//如果当前媒体正在播放，且进度条没被手指拖动，且没有手指位移划动快进/后退动作
                                relocate_time();
                            }

            }
        };
        mTimer.schedule(mTimerTask, 0, 100);//进度条随时间变动

        //--------------------------------------------进度条seek bar触碰-------------------------------
        skbProgress.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            private int progress;

            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {//当手指触碰进度条上点想对它进行划动变动时会触发该函数
                // 原本是(progress/seekBar.getMax())*player.mediaPlayer.getDuration()
                this.progress = progress * mediaPlayer.getDuration() / seekBar.getMax();
                if (mp3_interface && mp3_state == state_loaded)
                    current_time.setText(time_switch_int_to_string(this.progress));//手指拖动进度条时，进度条上的点所移动到的时间点数据同步更新到textview current_time上
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {//当手指刚触碰进度条上点想对它进行划动变动时会触发该函数
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {//进度条拖动结束时
                // seekTo()的参数是相对与影片时间的数字，而不是与seekBar.getMax()相对的数字
                if (mp3_interface && mp3_state == state_loaded) {
                    mediaPlayer.seekTo(progress);//定位到视频某时间位置
                    if (!mediaPlayer.isPlaying())//如果当前处于暂停没播放状态
                        mp3_start();// 重新激活播放
                }
            }
        });//监视进度条是否被拖动
        //--------------------------------------------进度条seek bar触碰-------------------------------



    }
    private static void set_button()//各种按钮图片设置
    {
        mp3_interface=true;
        //android.widget.ImageView  disk_image=(android.widget.ImageView)findViewById(R.id.disk_image);
        disk_image=(com.example.xielm.myapplication.ImageViewPlus)activity.findViewById(R.id.disk_image);
        disk_image.setScaleType(ImageView.ScaleType.FIT_START);

        mp3_start_button=(android.widget.ImageButton)activity.findViewById(R.id.mp3_start);
        mp3_start_button.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        //mp3_start_button.setScaleType(android.widget.ImageButton.ScaleType.FIT_CENTER);
        //mp3_start_button.setAdjustViewBounds(true);
        //mp3_start_button.setMaxWidth(movie_monitor.mobileWidth / 4);
        mp3_start_button.setOnClickListener(listener);

        current_time=(android.widget.TextView)activity.findViewById(R.id.mp3_current_time);
        max_duration=(android.widget.TextView)activity.findViewById(R.id.mp3_duration);

        skbProgress=(android.widget.SeekBar) activity.findViewById(R.id.seekBar2);//获取进度条
        nature_refresh=true;
        if(mp3_state==state_empty) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    com.bumptech.glide.Glide.with(activity).load(R.mipmap.media_start).into(mp3_start_button);//源地址内容显示到目标View中
                }
            });
            skbProgress.setEnabled(false);
            mp3_start_button.setEnabled(false);
        }
        else//若已播放单曲
        {
            if(mediaPlayer.isPlaying())//若单曲处于正在播放状态
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        com.bumptech.glide.Glide.with(activity).load(R.mipmap.media_pause).into(mp3_start_button);//源地址内容显示到目标View中
                    }
                });
            else//若单曲处于暂停状态
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        com.bumptech.glide.Glide.with(activity).load(R.mipmap.media_start).into(mp3_start_button);//源地址内容显示到目标View中
                    }
                });
            skbProgress.setProgress(skbProgress.getMax() * mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration());//更新进度条位置
            skbProgress.setEnabled(true);
            mp3_start_button.setEnabled(true);
            current_time.setText(time_switch_int_to_string(mediaPlayer.getCurrentPosition()));//当前时间显示
            max_duration.setText(time_switch_int_to_string(mediaPlayer.getDuration()));//总时长显示
        }


    }


    private static void mp3_pause()//暂停音乐同时修改图片
    {
        if(mp3_interface&&mp3_state==state_loaded) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    com.bumptech.glide.Glide.with(activity).load(R.mipmap.media_start).into(mp3_start_button);
                }
            });
            mediaPlayer.pause(); // TODO Auto-generated method stub

            if(mCircleAnimator!=null&&!mCircleAnimator.isPaused())
                mCircleAnimator.pause();
        }


    }
    private static void mp3_start()//开始音乐同时修改图片
    {
        if(mp3_interface&&mp3_state==state_loaded) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    {
                        com.bumptech.glide.Glide.with(activity).load(R.mipmap.media_pause).into(mp3_start_button);//源地址图片内容显示到目标View中
                    }
                    mediaPlayer.start(); // TODO Auto-generated method stub
                }
            });
            if(mCircleAnimator!=null&&mCircleAnimator.isPaused())
                mCircleAnimator.resume();
        }


    }

//---------------------------------------------------让唱片图片旋转起来--------------------------------------------------
    public static float radius=0;//初始半径为0
    private static android.animation.ObjectAnimator mCircleAnimator=null;
    private static android.graphics.Bitmap bitmap;
    public static void rotate()//让唱片图片旋转起来
    {
        if(!mp3_interface) return;

        Thread x = new Thread() {
            int i;
            @Override
            public void run(){
                final com.example.xielm.myapplication.ImageViewPlus imageViewPlus = (com.example.xielm.myapplication.ImageViewPlus) activity.findViewById(R.id.disk_image);
                bitmap = getPicture(mp3_image_path);
                imageViewPlus.post(new Runnable() {
                    @Override
                    public void run() {
                        //movie_number.setText(movie_name_this);
                        imageViewPlus.setImageBitmap(bitmap);//在ImageView中显示从网络上获取到的图片
                    }
                });

                //如果不停一些时间的话，会导致首次加载该页面图片转不起来
                disk_image.post(new Runnable() {
                    @Override
                    public void run() {
                        //com.bumptech.glide.Glide.with(activity).load(mp3_image_path).into(disk_image);//源地址内容显示到目标View中
                        if(mCircleAnimator!=null)mCircleAnimator.end();
                        mCircleAnimator=null;
                        telecom.tsleep(500);
                        if(radius==0) {//sb解决办法，有时radius为0
                            disk_image.setPivotX(disk_image.getWidth()/2);
                            disk_image.setPivotY(disk_image.getWidth()/2);//设置旋转中心轴
                        }
                        else
                        {
                            disk_image.setPivotX(radius);
                            disk_image.setPivotY(radius);//设置旋转中心轴
                        }
                        //while(radius==0){telecom.tsleep(500);}//如果画圆半径一直没确定，等待圆画完
                        mCircleAnimator = android.animation.ObjectAnimator.ofFloat(disk_image, "rotation", 0.0f, 360.0f);
                        mCircleAnimator.setDuration(10000);

                        mCircleAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
                        mCircleAnimator.setRepeatCount(-1);
                        mCircleAnimator.setRepeatMode(android.animation.ObjectAnimator.RESTART);
                        mCircleAnimator.start();
                        if(mediaPlayer!=null&&!mediaPlayer.isPlaying())
                           mCircleAnimator.pause();
                        /*
                        android.view.animation.RotateAnimation rotateAnimation =new android.view.animation.RotateAnimation(0f,360f, Animation.ABSOLUTE, radius,Animation.ABSOLUTE,radius);
                        //android.widget.ImageView disk_image = (android.widget.ImageView) mp3_player.activity.findViewById(R.id.disk_image);
                        rotateAnimation.setFillAfter(true);
                        //android.view.animation.Animation rotateAnimation = android.view.animation.AnimationUtils.loadAnimation(movie_episode_select.activity, R.anim.rotate_anim);
                        android.view.animation.LinearInterpolator lin = new android.view.animation.LinearInterpolator();//设置为匀速
                        rotateAnimation.setInterpolator(lin);
                        rotateAnimation.setDuration(10000);//设置动画持续周期
                        rotateAnimation.setRepeatCount(-1);//设置重复次数
                        rotateAnimation.setFillAfter(true);//动画执行完后是否停留在执行完的状态
                        rotateAnimation.setStartOffset(10);//执行前的等待时间
                        if (rotateAnimation != null) {
                            disk_image.startAnimation(rotateAnimation);
                        }*/
                    }
                });
            }
        };
        if(!mp3_interface) return;
        x.start();
    }
    private static android.graphics.Bitmap getPicture(String path) {
        android.graphics.Bitmap bm = null;
        try {
            java.net.URL url = new java.net.URL(path);//创建URL对象

            java.net.URLConnection conn = url.openConnection();//获取URL对象对应的连接
            conn.connect();//打开连接
            java.io.InputStream is = conn.getInputStream();//获取输入流对象
            bm = android.graphics.BitmapFactory.decodeStream(is);//根据输入流对象创建bitmap对象
        } catch (java.net.MalformedURLException e1) {
            e1.printStackTrace();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return bm;
    }
//---------------------------------------------------让唱片图片旋转起来--------------------------------------------------

    public static android.view.View.OnClickListener listener = new android.view.View.OnClickListener() //监听用户点击按钮事件
    {
        int k;
        @Override
        public void onClick(android.view.View v) {
            //android.widget.Toast t = android.widget.Toast.makeText(getApplicationContext(), "网络异常！", android.widget.Toast.LENGTH_SHORT);
            switch (v.getId()) {
                case R.id.mp3_start:
                    if(mediaPlayer.isPlaying())
                        mp3_pause();
                    else
                        mp3_start();
                    break;
            }
            return;//返回
        }
    };


    //----------------------显示时间、进度条随播放进度变动----------------------
    private static boolean nature_refresh=true;//自然更新进度条，初始为是
    private static java.util.Timer mTimer=null;

    private static void relocate_time()
    {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer==null||!mediaPlayer.isPlaying())return;
                int duration = mediaPlayer.getDuration();//获取总时长
                int position = mediaPlayer.getCurrentPosition();//获取mediaplayer当前播放到的时间点
                String durationstring=time_switch_int_to_string(duration);// duration / 1000 后单位为“秒”
                if(position>=duration)position=duration;//莫名其妙有时候会比总时长多出一秒，用这句来修复
                String currentpositionstring=time_switch_int_to_string(position);//转化为字符串

                if (duration > 0&&mp3_interface) {
                    long pos = skbProgress.getMax() * position / duration;
                    skbProgress.setProgress((int) pos);//更新进度条上点的位置
                    max_duration.setText(durationstring);//更新mediaplayer播放内容总时长
                    current_time.setText(currentpositionstring);//实时更新mediaplayer播放的当前时间点
                    last_current_time=currentpositionstring;
                    last_duration=durationstring;
                }
            }
        });
    }



    private static String time_switch_int_to_string(int time)//将数字转换为“00:00:00”字符串形式表示，输入mediaplayer获取的当前time或总时长int后，将转化后的字符串结果返回
    {
        String result="";
        if(time / 1000<3600) {
            int minute=time / 1000 / 60;
            int second=(time / 1000) % 60;
            if(minute>=10)
                result = minute+":";
            else
                result ="0"+minute+":";
            if(second>=10)
                result=result+second;
            else
                result=result+"0"+second;
        }
        else {
            int hour    =time / 1000 / 60 / 60;
            int minute  =(time / 1000 - 3600 * (time / 1000 / 60 / 60)) / 60;
            int second  =(time / 1000) % 60;
            if(hour>=10)
                result=hour+":";
            else
                result="0"+hour+":";
            if(minute>=10)
                result=result+minute+":";
            else
                result=result+"0"+minute+":";
            if(second>=10)
                result=result+second;
            else
                result=result+"0"+second;
        }
        return result;

    }
    //----------------------显示时间、进度条随播放进度变动----------------------

}
