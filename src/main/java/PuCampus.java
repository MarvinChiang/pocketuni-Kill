import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author MengFei
 * @date 2020/11/10 0:47
 */

public class PuCampus {
    static String defaultSettingsFile = "src/main/resources/setting.properties";
    static String schoolName = getSetting("schoolName_for_short");
    static String schoolId = getSetting("school_Id");
    static String Accept = getSetting("Accept");
    static String UserAgent = getSetting("User-Agent");
    static String remarkName = getSetting("remarkName");
    static String mySchedule = getSetting("mySchedule");
    static String xueHao = getSetting("Student_ID");
    static String password = getSetting("Password");
    static String myLocalCookies = getSetting("myCookies");
    static int activityID = Integer.parseInt(getSetting("activityID_1"));
    static int activityID_2 = Integer.parseInt(getSetting("activityID_2"));
    static int THREAD_POOL_SIZE = Math.min(Integer.parseInt(getSetting("ThreadPool_Size")), 30);
    static int taskMAX = Integer.parseInt(getSetting("taskMAX"));
    static boolean ifSchedule = Boolean.parseBoolean(getSetting("ifSchedule"));
    static boolean useLocalCookies = Boolean.parseBoolean(getSetting("useLocalCookies"));

    /**
     * 提前10秒启动即可
     * 默认输出"报名未开始"
     */
    static String StatusStr = null;
    static volatile String hash = null;
    static String respText = null;
    static String phpSsid = null;
    static String loggedUser = null;
    static String cookie = null;
    static boolean Activity2 = false;
    static AtomicInteger ai = new AtomicInteger(0);
    static final SimpleDateFormat ScheduleFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static AtomicInteger CheckLogin = new AtomicInteger(0);


    /**
     * 获得配置文件
     *
     * @param keyWord 配置项
     * @return 配置项
     */
    public static String getSetting(String keyWord) {
        Properties prop = new Properties();
        String value = "";
        InputStream in1 = null;
        InputStream in2 = null;
        //尝试获取绝对文件路径
        String jarWholePath = PuCampus.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        try {
            jarWholePath = java.net.URLDecoder.decode(jarWholePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println(e.toString());
        }
        String jarPath = new File(jarWholePath).getParentFile().getAbsolutePath();
        //找到配置文件的相对路径

        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("win")) {
            jarPath = jarPath + "\\setting.properties";
        } else {
            jarPath = jarPath + "/setting.properties";
            //系统适配
        }
        //判断target目录下是否有配置文件
        if (!new File(jarPath).exists()) {
            jarPath = defaultSettingsFile;
            if (!new File(jarPath).exists()) {
                System.out.println(" ≥▂▂≤   没找到配置文件" + jarPath);
                System.exit(0);
            }
        }
        try {
            //jarPath 优先使用同路径下jarWholePath,其次是defaultSettingsFile,找不到就退出
            in2 = new FileInputStream(jarPath);
            prop.load(in2);
            value = prop.getProperty(keyWord);
            if (value == null) System.out.println(" ≥▂≤  找不到该配置项" + keyWord);
        } catch (IOException IOException) {
            System.out.println(IOException);
            System.out.println(" ≥▂≤  找不到该配置项" + keyWord);
            System.exit(0);
        }
        return value;
    }

    /**
     * 使用账号密码自动登录
     * 获取cookie中的PhpSsid TS_LOGGED_USER
     *
     * @throws Exception e
     */
    public static void getCookies() throws Exception {
        if (useLocalCookies) {
            cookie = myLocalCookies;
            System.out.println("已经手动设置Cookies绕过自动登录  ⊙ω⊙");
            return;
        }
        HttpResponse<String> response1 =
                Unirest.get("https://www.pocketuni.net/index.php?app=home&mod=Public&act=login")
                        .header("Host", "www.pocketuni.net")
                        .header("Connection", "keep-alive")
                        .header("sec-ch-ua", "\"Google Chrome\";v=\"89\", \"Chromium\";v=\"89\", \";Not A Brand\";v=\"99\"")
                        .header("sec-ch-ua-mobile", "?0")
                        .header("Upgrade-Insecure-Requests", "1")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                        .header("Sec-Fetch-Site", "none")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-User", "?1")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Accept-Language", "zh-CN,zh;q=0.9")
                        .asString();
        try {
            phpSsid = response1.getHeaders().toString();
            phpSsid = "PHPSESSID=" + phpSsid.split("PHPSESSID=")[1].split(";")[0] + "; ";
        } catch (Exception e) {
            System.out.println(phpSsid);
            System.exit(0);
        }
        Thread.sleep(200);
        HttpResponse<String> response2 =
                Unirest.post("https://www.pocketuni.net/index.php?app=home&mod=Public&act=doLogin")
                        .header("Host", "www.pocketuni.net")
                        .header("Connection", "keep-alive")
                        .header("sec-ch-ua", "\"Google Chrome\";v=\"89\", \"Chromium\";v=\"89\", \";Not A Brand\";v=\"99\"")
                        .header("Accept", "application/json, text/javascript, */*; q=0.01")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("sec-ch-ua-mobile", "?0")
                        .header("User-Agent", UserAgent)
                        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .header("Origin", "https://www.pocketuni.net")
                        .header("Sec-Fetch-Site", "same-origin")
                        .header("Sec-Fetch-Mode", "cors")
                        .header("Sec-Fetch-Dest", "empty")
                        .header("Referer", "https://www.pocketuni.net/index.php?app=home&mod=Public&act=login")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Accept-Language", "zh-CN,zh;q=0.9")
                        .header("Cookie", "PHPSESSID=" + phpSsid + "; TS_think_language=zh-CN ")
                        .body("sid=" + schoolId + "&number=" + xueHao + "&password=" + password)
                        .asString();
        loggedUser = response2.getHeaders().toString();
        try {
            System.out.println(decodeUni(response2.getBody()));
            loggedUser = "TS_LOGGED_USER=" + loggedUser.split("TS_LOGGED_USER=")[1].split(";")[0] + "; ";
        } catch (Exception ignored) {
            System.out.println("⊙﹏⊙ 登录出现异常 已结束 可能网站登录错误或发生变动 ");
            System.exit(0);
        }
        Thread.sleep(200);
        cookie = phpSsid + loggedUser + "TS_think_language=zh-CN";
        if (phpSsid == null & loggedUser == null) {
            System.out.println("⊙﹏⊙ cookies获取错误 可能网站发生变动 ");
            System.exit(0);
        }
    }

    /**
     * unicode转换成中文 工具类
     *
     * @param respBody 返回body
     */
    public static String decodeUni(String respBody) {
        Matcher m = Pattern.compile("\\\\u([0-9a-zA-Z]{4})").matcher(respBody);
        StringBuffer sb = new StringBuffer(respBody.length());
        while (m.find()) {
            sb.append((char) Integer.parseInt(m.group(1), 16));
        }
        return sb.toString();
    }


    /**
     * 获得活动名字 非关键
     *
     * @param id 活动id
     * @throws Exception e
     */
    public static void getActivityName(int id) throws Exception {
        HttpResponse<String> response3 =
                Unirest.get("https://" + schoolName + ".pocketuni.net/index.php?app=event&mod=Front&act=index&id=" + id)
                        .header("Host", schoolName + ".pocketuni.net")
                        .header("Connection", "keep-alive")
                        .header("Cache-Control", "max-age=0")
                        .header("Upgrade-Insecure-Requests", "1")
                        .header("User-Agent", UserAgent)
                        .header("Accept", Accept)
                        .header("Accept-Encoding", "gzip, deflate")
                        .header("Accept-Language", "zh-CN,zh;q=0.9")
                        .header("Cookie", cookie)
                        .asString();
        Element body = Jsoup.parseBodyFragment(response3.getBody());
        try {
            String activityName = body.getElementsByClass("b").text();
            System.out.println("活动名[[[ " + activityName + " ]]]");
            String activityInfo =
                    body.getElementsByClass("content_hd_c").text();
            try {
                activityInfo = activityInfo.split("报名起止[：|:]")[1].split("联系")[0];
            } catch (Exception ignored) {
            }
            System.out.println(activityInfo);
        } catch (Exception ignored) {
            System.out.println("o(╯□╰)o  无法获得活动信息!");
            System.exit(0);
        }
    }

    /**
     * 获取hash值和当前状态
     * 同时可以判断是否登录成功
     *
     * @throws Exception e
     */
    public static void getHashStatus(int id) throws Exception {
        Element body = null;
        try {
            HttpResponse<String> response4 =
                    Unirest.post("https://" + schoolName + ".pocketuni.net/index.php?app=event&mod=Front&act=join&id=" + id)
                            .header("Host", schoolName + ".pocketuni.net")
                            .header("Connection", "keep-alive")
                            .header("sec-ch-ua", "\"Google Chrome\";v=\"89\", \"Chromium\";v=\"89\", \";Not A Brand\";v=\"99\"")
                            .header("sec-ch-ua-mobile", "?0")
                            .header("Upgrade-Insecure-Requests", "1")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                            .header("Sec-Fetch-Site", "same-origin")
                            .header("Sec-Fetch-Mode", "navigate")
                            .header("Sec-Fetch-User", "?1")
                            .header("Sec-Fetch-Dest", "document")
                            .header("Referer", "https://" + schoolName + ".pocketuni.net/index.php?app=event&mod=School&act=board&cat=all")
                            .header("Accept-Encoding", "gzip, deflate, br")
                            .header("Accept-Language", "zh-CN,zh;q=0.9")
                            .header("Cookie", cookie)
                            .asString();
            body = Jsoup.parseBodyFragment(response4.getBody());
        } catch (Exception e) {
            System.out.println("网页有改动,如可以正常报名成功就忽略");
            return;
        }
        if (CheckLogin.get() > 0) {
            try {
                respText = body.getElementsByClass("b").text();
                hash = body.getElementsByAttributeValue("name", "__hash__").get(0).attr("value");
                System.out.println(respText);
                CheckLogin.incrementAndGet();
                if (CheckLogin.get() > 2 && respText.contains("成功")) {
                    Thread.yield();
                    System.exit(0);
                }
                return;
            } catch (Exception ignored) {
                return;
            }
        }
        //初次使用此函数需要验证登录
        try {
            respText = body.text();
            //检查HTML段
            if (respText.contains("活动不存在")) {
                System.out.println("o(╯□╰)o 活动不存在 ");
                System.exit(0);
            }
            if (respText.contains("苏州天宫")) {
                System.out.println(respText.split("苏州天宫")[0]);
            } else System.out.println(respText);
            hash = body.getElementsByAttributeValue("name", "__hash__").get(0).attr("value");
            StatusStr = body.getElementsByClass("b").text();
            if (StatusStr.length() != 0) {
                System.out.println(StatusStr);
            }
            CheckLogin.incrementAndGet();
        } catch (Exception ignored) {
            System.out.println("o(╯□╰)o   登录失败  o(╯□╰)o ");
            if (useLocalCookies) {
                System.out.println("手动输入了错误或过时的Cookies(T_T)  " + cookie);
            } else {
                System.out.println("账号密码或学校代码错误  (T_T)");
            }
            System.exit(0);
        }
    }

    /**
     * 延时启动  验证密码和 cookies 准备启动
     *
     * @throws Exception e
     */
    public static void validation() throws Exception {
        //准备暂停
        if (ifSchedule) {
            try {
                Long timestampStartTime = ScheduleFormat.parse(mySchedule).getTime();
                Long timestampSNowTime = System.currentTimeMillis();
                long sleepTime = timestampStartTime - timestampSNowTime;
                if (sleepTime > 0) {
                    System.out.println(mySchedule + "准备运行, 本进程现在休眠  " + sleepTime / 3600000 + "时"
                            + (sleepTime % 3600000) / 60000 + "分" + sleepTime % 60000 / 1000 + "秒...");
                    Thread.sleep(sleepTime);
                    //-------------------------------------------//
                    System.out.println("暂停结束 现在开始");
                }
            } catch (Exception e) {
                System.out.println("定时时间格式输入错误 ＞▂＜");
            }
        }
        //登录,获得cookies
        getCookies();
        //输出信息
        getHashStatus(activityID);
        System.out.println("≧▽≦ 登录成功 准备启动 ≧▽≦ \t\t\t 活动ID:" + activityID);
        getActivityName(activityID);
        //判断有几个活动
        if (activityID_2 > 0) {
            Activity2 = true;
        }
        if (Activity2) {
            System.out.println("第二活动id:" + activityID_2);
            getHashStatus(activityID_2);
            getActivityName(activityID_2);
        }
        System.out.println("--------------------START--------------------\n");
        Thread.sleep(200);
    }

    /**
     * 尝试并发请求
     */
    public static void tryOnce(int id) {
        try {
            Unirest.post("https://" + schoolName + ".pocketuni.net/index.php?app=event&mod=Front&act=doAddUser&id=" + id)
                    .header("Host", schoolName + ".pocketuni.net")
                    .header("Connection", "keep-alive")
                    .header("Cache-Control", "max-age=0")
                    .header("Origin", "https://" + schoolName + ".pocketuni.net")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", UserAgent)
                    .header("Accept", Accept)
                    .header("Referer", "https://" + schoolName + ".pocketuni.net/index.php?app=event&mod=Front&act=join&id=" + id)
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .header("Cookie", cookie)
                    .body("__hash__=" + hash)
                    .asString();
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("备注名:" + remarkName + "\t\t并发线程数:" + THREAD_POOL_SIZE +
                "\t\t尝试次数:" + taskMAX + "\t\t学号:" + xueHao +
                "\t\t密码:" + password.substring(0, password.length() - 4) + "****");
        Unirest.setTimeouts(2000, 2000);
        validation();
        // 创建线程池，其中任务队列需要结合实际情况设置合理的容量
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().build();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(THREAD_POOL_SIZE,
                THREAD_POOL_SIZE + 1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(taskMAX),
                namedThreadFactory,
                new ThreadPoolExecutor.AbortPolicy());
        long t1 = System.currentTimeMillis();
        //重设响应超时毫秒 超时的请求直接放弃
        Unirest.setTimeouts(800, 800);
        // 新建 taskMAX 个任务，每个任务是打印当前线程名称
        for (int i = 0; i < taskMAX; i++) {
            executor.execute(() -> {
                try {
                    tryOnce(activityID);
                    if (Activity2) {
                        tryOnce(activityID_2);
                    }
                    if (Math.random() < 0.1) {
                        //每十次检查一下是否报名成功
                        if (!Activity2) {
                            getHashStatus(activityID);
                        } else {
                            getHashStatus(activityID);
                            getHashStatus(activityID_2);
                        }
                        System.out.print(ai.get());
                    }
                    //原子计数器自增
                    ai.incrementAndGet();
                    System.out.println(LocalTime.now()); // 2019-11-20T15:04:29.017
                } catch (Exception ignored) {
                }
            });
        }

        // 关闭线程池
        executor.shutdown();
        executor.awaitTermination(1000L, TimeUnit.SECONDS);
        System.out.println(" ●▽●  ---Done--- ●▽●");

        long t2 = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        //统计运算时间
        c.setTimeInMillis(t2 - t1);
        System.out.println("耗时: " + c.get(Calendar.MINUTE) + "分 "
                + c.get(Calendar.SECOND) + "秒 " + c.get(Calendar.MILLISECOND) + " 微秒");
    }
}
