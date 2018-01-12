[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/rRemix/APlayer/blob/master/LICENSE)

# APlayer
# 安卓本地音乐播放器

## 简介
遵循 Material Design 设计,UI清新简洁,操作简单,功能齐全<br>
下载地址:https://www.coolapk.com/apk/remix.myplayer


## 截图
<img src="/pictures/Screenshot_20180112-112950.png" alt="screenshot" title="screenshot" width="270" height="486" /><img src="/pictures/Screenshot_20180112-113007.png" alt="screenshot" title="screenshot" width="270" height="486" />  <img src="/pictures/Screenshot_20180112-113128.png" alt="screenshot" title="screenshot" width="270" height="486" /><br><br>
<img src="/pictures/Screenshot_20180112-113144.png" alt="screenshot" title="screenshot" width="270" height="486" />  <img src="/pictures/Screenshot_20180112-113153.png" alt="screenshot" title="screenshot" width="270" height="486" />  <img src="/pictures/Screenshot_20180112-113550.png" alt="screenshot" title="screenshot" width="270" height="486" /> <br><br>
<img src="/pictures/Screenshot_20180112-113637.png" alt="screenshot" title="screenshot" width="270" height="486" />  <img src="/pictures/Screenshot_20180112-113707.png" alt="screenshot" title="screenshot" width="270" height="486" />  <img src="/pictures/Screenshot_20180112-113820.png" alt="screenshot" title="screenshot" width="270" height="486" />  <img src="/pictures/Screenshot_20180112-113855.png" alt="screenshot" title="screenshot" width="270" height="486" />  


## 特点
- 首页Tab可配置，最多支持五个,包括歌曲、艺术家、专辑、文件夹、播放列表
- 专辑、艺术家封面自动补全
- 支持显示本地和在线歌词(网易)，可设置歌词搜索的优先级；本地歌词可以自由选择，或者忽略歌词
- 支持耳机线控操作,拔出耳机自动暂停
- 支持桌面歌词、桌面部件
- 已适配Android8.0通知栏
- 锁屏控制,可选择原生或者软件实现
- 心情可生成海报分享
- 日夜间模式切换，动态改变主题颜色
- 其他必备和便捷操作如歌曲信息编辑、睡眠定时、均衡器等


## 感谢
- [RxJava](https://github.com/ReactiveX/RxJava)
- [RxAndroid](https://github.com/ReactiveX/RxAndroid)
- [Retrofit](https://github.com/square/retrofit)
- [Butter Knife](https://github.com/JakeWharton/butterknife)
- [Material-dialogs](https://github.com/afollestad/material-dialogs)
- [Rxpermissions](https://github.com/tbruyelle/RxPermissions)
- [Android-crop](https://github.com/jdamcd/android-crop)
- [TinyPinyin](https://github.com/promeG/TinyPinyin)


## 最后
- 如果喜欢或者能给你提供帮助，欢迎Star
- 因为是刚学安卓的时候就开始做了，很多代码待完善或者重构，还有一些待开发的功能，欢迎Pull Request
- 有任何问题可以提出Issue或者发邮件到我的邮箱: rRemix.me@gmail.com


## 更新日志
### v1.1.2
1.全新UI
2.增加9种色彩主题
3.增加更改专辑、歌手、播放列表封面功能
4.增加更改歌曲标签功能
5.增加夜间模式
6.完善新建歌曲列表功能
7.完善歌词功能
8.修复若干已知bug
### v1.1.3
1.改善夜间模式和其他多处界面
2.修复部分机型播放和锁屏界面状态栏为纯白
3.修复部分机型通知栏关闭按钮失效
4.修改删除歌曲为从歌曲库移除(不删除本地文件)
5.设置界面新增锁屏开关和支持我们
6.播放界面新增下滑关闭
7.优化歌词本地搜索
8.修复其他bug
### v1.1.4
1.替换歌词在线搜索接口，优化歌词本地搜索
2.全部歌曲与次级目录界面新增按字母或添加时间排序
3.修复部分机型通知栏字体无法看清
4.播放界面新增收藏功能
5.优化动画
6.优化内存占用
7.优化细节与修复其他bug
### v1.1.5
1.添加滚动条
2.新增银白色主题
3.优化歌曲列表滑动卡顿的问题
4.修复部分机型无法加载封面的问题
5.修复没有播放歌曲时进度条拖动异常的问题
6.修复播放歌曲时被后台管理软件杀死的问题
### v1.1.6
1.新增4*1与4*2桌面部件
2.调整歌词字体
3.修复部分机型打开软件时闪退的问题
### v1.1.6.2
1.优化歌词显示
2.添加忽略歌词功能(歌词界面长按)
3.修复点击音乐标签编辑闪退的问题
### v1.1.6.3
1.修复安卓5.0以下闪退的问题
2.修复搜索歌词时可能闪退的问题
3.新增手动选择歌词功能(歌词界面长按)
### v.1.1.6.4
1.修复切换歌曲时的若干问题
### v1.1.6.5
1.修复切换播放列表无法保存
2.修复搜索本地歌词时的一处错误
3.优化内存占用
### v1.1.6.6
1.修复一个可能使软件进入未知领域的问题
2.优化本地搜索
### v1.1.6.7
1.优化随机播放
2.歌词界面屏幕常亮(默认关闭，可在设置中打开)
3.打开播放队列定位到正在播放的歌曲
4.修复部分用户点击反馈崩溃的问题
5.修复5.0以下系统仍可以开启导航栏变色的问题
6.修复最近添加列表最后一首歌曲显示异常的问题
7.修复从歌曲列表添加歌曲到播放列表添加失败的问题
8.修复从通知栏进入播放界面后点击关闭按钮动画异常的问题
9.修复部分机型打开播放队列可能崩溃的问题
### v1.1.7
1.新增桌面歌词
2.如果正在播放，定时停止将在播放完当前歌曲后关闭
4.调整播放界面封面大小
3.修复无歌词的时候无法手动选择歌词的问题
3.修复首次进入播放界面进度条的问题
5.修复退出后再次进入歌曲从头开始播放的问题
6.优化随机播放模式下切换上一首歌曲
7.优化锁屏页加载速度
### v1.1.7.1
1.优化桌面歌词
2.新增4*4桌面部件
### v1.1.8
1.部分代码重构并修改编译版本至安卓N
2.添加动态权限的判断
3.优化首次打开app，滑动列表可能闪烁或卡顿的问题
4.修复播放界面左右滑动会不停出现忽略歌词弹框的问题
5.修复部分机型上移除文件夹无效的问题
### v1.1.8.2
1.调整多处ui细节
2.修复均衡器多处bug
### v1.1.9
1.新增Shortcut
2.修复文件夹内部移除无效的问题
3.修复安卓8.0编辑歌曲信息崩溃的问题
4.修复当歌曲修改后列表刷新会崩溃的问题
5.修复部分机型定时停止界面显示异常的问题
6.修复播放界面左右切换时歌词界面会显示一直正在搜索的问题
7.播放界面进度条、封面、分享页样式修改，部分按钮图标替换
8.优化首次进入app时的加载
### v1.1.9.1
1.设置新增手动扫描
2.优化外部歌曲改变时列表的刷新
### v1.1.9.2
1.修复添加到播放队列的歌曲点击播放提示参数错误的问题
### v1.2.0
1.适配Android O与通知栏
2.修改桌面歌词解锁方式,修复桌面歌词锁定后无法点击桌面的问题
3.修复设置铃声崩溃的问题
4.播放队列对话框高亮显示正在播放的歌曲
5.尝试优化长时间播放下程序稳定性
### v1.2.1
1.自动下载缺省专辑与艺术家封面
2.优化桌面歌词与桌面部件若干问题 
3.优化内存占用
### v1.2.1.1
1.尝试修复部分机型无法识别歌曲的问题
### v1.2.1.2
1.新增对不同网络情况下是否自动下载封面的设置并添加缓存
### v.1.2.1.3
1.通知栏、桌面部件的专辑封面添加在线匹配
2.桌面部件样式略微调整
3.修复添加歌曲到播放列表后可能无限闪退的问题
4.修复播放列表封面可能无法正常加载的问题
### v1.2.2
1.修复设置自定义封面失败的问题
2.优化切换到播放界面封面加载闪烁的问题
3.修复可能内存泄漏的问题
### v1.2.3
1.歌词接口替换网易云
2.优化歌词控件显示
3.修复随机播放可能导致设置播放列表失败的问题
### v1.2.3.2
1.修复点击随机播放设置列表失败
2.下一首
3.网易歌词
2.心情分享缓存
