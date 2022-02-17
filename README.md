# android_mp3_player

安卓mp3播放器


仿网易云（中间有个圆形图片在转）


用法：
在别的非handler线程中使用以下语句，且尽量保证该循环是某个死循环内。

android.content.Intent i2 = new android.content.Intent(movie_lobby.activity, mp3_player.class);

movie_lobby.activity.startActivity(i2);/先打开页面

telecom.tsleep(300);

String mp3_image_path ="https://……";

String mp3_path = "http://……";

mp3_player.mp3_ini(mp3_path, mp3_image_path);//再使用这个函数初始化播放源路径和图片路径即可

