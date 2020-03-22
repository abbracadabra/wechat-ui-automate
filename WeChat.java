package com.ctrip.ttd.connect.pay.service;


import com.ctrip.ttd.connect.pay.bll.HttpService;
import com.ctrip.ttd.connect.pay.entity.QrViewer;
import com.ctrip.ttd.connect.pay.entity.crawler.CrawlerPayEntity;
import com.ctrip.ttd.connect.pay.shared.constants.PayConst;
import com.ctrip.ttd.connect.pay.util.CommandUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

//打开appiumserver->adb->夜神模拟器
@Component("WeChat")
@Scope("singleton")
public class WeChat extends Device{

    @Autowired
    HttpService service;

    HttpService payResultService = new HttpService();

    String APPIUM_SERVER_IP = "http://127.0.0.1:4723/wd/hub";//appium监听地址
    //String winappdriver_SERVER_IP = "http://127.0.0.1:4727";//winappdriver监听地址
    String phoneimgdir = "/sdcard/Pictures/";//手机图片文件夹地址
    String phoneimgdirname = "Pictures";//手机图片文件夹名字
    DesiredCapabilities androidcapabilities;//安卓的配置
    //DesiredCapabilities windowscapabilities;//windows的配置
    AppiumDriver<WebElement> phonedriver;//安卓模拟机驱动器
    //AppiumDriver windriver;//windows模拟机驱动器
    String imgdir = WeChat.class.getClassLoader().getResource("imgs/").getFile();//windows图片文件夹
    int phonetimeout = 5;
    int wintimeout = 5;
    CrawlerPayEntity wp;//支付状态：0未支付1支付中2支付成功3支付失败4支付取消
    QrViewer qv;

    //支付状态0未支付1支付中2支付成功3支付失败4支付取消

    /*WeChat(String deviceid) throws MalformedURLException, InterruptedException {
        this.deviceid = deviceid;
        init();
    }*/


    /**
     * 初始化，
     * new WindowsDriver(new URL(winappdriver_SERVER_IP), windowscapabilities)这个语句执行第二遍会永久阻塞
     */
    public void init() throws MalformedURLException, InterruptedException {
        System.out.println("初始化开始");
        /**
         * windows设置
         */

        /*windowscapabilities = new DesiredCapabilities();
        windowscapabilities.setCapability("platformName", "Windows");
        windowscapabilities.setCapability("deviceName", "WindowsPC");
        windowscapabilities.setCapability("app", "D:\\Program Files\\Nox\\bin\\Nox.exe");
        windriver = new WindowsDriver(new URL(winappdriver_SERVER_IP), windowscapabilities);
        windriver.manage().timeouts().implicitlyWait(wintimeout, TimeUnit.SECONDS);*/
        /**
         * adb连接夜神模拟器
         */
        CommandUtil.executeCommand("adb connect 127.0.0.1:62001", null);
        /**
         * 设置安卓的设置
         */
        androidcapabilities = new DesiredCapabilities();
        androidcapabilities.setCapability("deviceName", deviceid);
        androidcapabilities.setCapability("udid", deviceid);
        androidcapabilities.setCapability("platformVersion", getDroidVer());
        androidcapabilities.setCapability("platformName", "Android");
        androidcapabilities.setCapability("appPackage", "com.tencent.mm");
        androidcapabilities.setCapability("appActivity", "com.tencent.mm.ui.LauncherUI");
        androidcapabilities.setCapability("fastReset", "false");
        androidcapabilities.setCapability("fullReset", "false");
        androidcapabilities.setCapability("noReset", "true");
        androidcapabilities.setCapability("resetKeyboard", "true");
        androidcapabilities.setCapability("newCommandTimeout", 999999);
        phonedriver = new AndroidDriver<WebElement>(new URL(APPIUM_SERVER_IP), androidcapabilities);
        phonedriver.manage().timeouts().implicitlyWait(phonetimeout, TimeUnit.SECONDS);
        //cwait(v -> v.currentActivity().equals(".ui.LauncherUI"), 10);
        cwait(By.xpath("//android.widget.TextView[@text='通讯录']"),10);
        System.out.println("初始化成功");
    }

    /**
     * 等timeout时间，直到满足条件
     */
    public boolean cwait(Predicate pred, int timeout) {
        phonedriver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        int count = 0;
        boolean flag = false;
        long thresh = new Date().getTime();
        thresh += timeout * timeout * 1000;
        while ((count++ == 0 || new Date().getTime() < thresh) && !(flag = pred.test(null))) {
            if (timeout != 0) {
                try {
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        phonedriver.manage().timeouts().implicitlyWait(phonetimeout, TimeUnit.SECONDS);
        return flag;
    }

    public boolean cwait(int timeout, By... by) {
        boolean valid = false;
        phonedriver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        List<WebElement> els = new ArrayList<>();
        int count = 0;
        long thresh = new Date().getTime();
        thresh += timeout * timeout * 1000;
        while ((count++ == 0 || new Date().getTime() < thresh) && !valid) {
            for (By b : by) {
                if (phonedriver.findElements(b).size() > 0) valid = true;
                break;
            }
            if (timeout != 0) {
                try {
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        phonedriver.manage().timeouts().implicitlyWait(phonetimeout, TimeUnit.SECONDS);
        return valid;
    }

    public WebElement cwait(By by, int timeout) throws InterruptedException {
        phonedriver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        List<WebElement> els = new ArrayList<>();
        int count = 0;
        long thresh = new Date().getTime();
        thresh += timeout * timeout * 1000;
        while ((count++ == 0 || new Date().getTime() < thresh) && (els = phonedriver.findElements(by)).isEmpty()) {
            if (timeout != 0) Thread.currentThread().sleep(500);
        }
        phonedriver.manage().timeouts().implicitlyWait(phonetimeout, TimeUnit.SECONDS);
        return els.isEmpty() ? null : els.get(0);
    }

    public void start() {
        try {
            init();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        while (true) {
            try {
                System.out.println("检查开始");
                check();//检查微信登陆、文件夹是否存在等等东西
                System.out.println("检查完毕");
                doStuff();//支付
            } catch (Throwable e) {
                e.printStackTrace();
                System.out.println("支付失败");
                if (wp != null) {
                    String msg = e.getMessage();
                    if (msg != null && e.getMessage().length() > 100) {
                        msg = msg.substring(0, 100);
                    }
                    updaterow(wp, wp.getPayStatus()== PayConst.State.PaySuccess?PayConst.State.PaySuccess:PayConst.State.PayFail, null, System.currentTimeMillis(), msg);
                }
            } finally {
                wp = null;
                if (qv != null) {
                    qv.closeViewer();
                    qv = null;
                }
            }
        }
    }

    private void updaterow(CrawlerPayEntity cp, Integer state, Integer count, Long paytime, String message) {
        CrawlerPayEntity resp = new CrawlerPayEntity();
        resp.setId(cp.getId());
        resp.setMessage(cp.getMessage());
        resp.setPayCount(cp.getPayCount());
        resp.setPayStatus(cp.getPayStatus());
        resp.setPayform(cp.getPayform());
        resp.setPayTime(cp.getPayTime());
        resp.setConnectno(cp.getConnectno());
        resp.setVendorUserId(cp.getVendorUserId());
        if (state != null) resp.setPayStatus(state);
        if (count != null) resp.setPayCount(count);
        if (paytime != null)
            resp.setPayTime(new Timestamp(paytime));
        else
            resp.setPayTime(new Timestamp(System.currentTimeMillis()));
        if (message != null) resp.setMessage(message);
        payResultService.payRsultCallBack(deviceid.split(":")[1],resp);
    }


    private void doStuff() throws Exception {
        /**
         * 从pay表取一条任务
         */
        System.out.println("获取任务中");
        getPayTask();
        System.out.println("取到一个任务");

        /**
         * 将payform里的string转成图片二维码
         */
        String fn = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS'.png'").format(new Date());
        String fp = new File(imgdir, fn).getCanonicalPath();
        generateQRCodeImage(wp.getPayform(), fp);
        String method = "相册";
        if ("相册".equals(method)) {
            System.out.println("相册支付开始");
            //将手机相册清空
            CommandUtil.executeCommand("adb shell rm -R -f " + phoneimgdir + "*", deviceid);
            //将二维码从电脑传到手机相册
            CommandUtil.executeCommand("adb push " + fp + " " + phoneimgdir, deviceid);
            //消息推送至微信
            CommandUtil.executeCommand("adb shell am broadcast -a android.intent.action.MEDIA_MOUNTED -d file://" + phoneimgdir, deviceid);
            //扫一扫里点通过手机相册支付
            albumPay();
        } else if ("相机".equals(method)) {
            System.out.println("相机支付开始");
            //在windows屏幕打开二维码
            qv = new QrViewer();
            qv.imagepath = fp;
            qv.showViewer();
            //相机扫码支付
            scanPay();
        }
    }

    private void scanPay() throws Exception {
        WebElement el = phonedriver.findElementById("com.tencent.mm:id/ra");
        System.out.println("点击右上角+");
        click(el);//右上角+按钮
        System.out.println("点击扫一扫");
        click(phonedriver.findElement(By.xpath("//android.widget.TextView[@text='扫一扫']")));//扫一扫按钮
        System.out.println("实时截取屏幕");
        //windriver.findElement(By.name("实时截取屏幕")).click();
        /**
         * 扫图逻辑
         */
        /*assert cwait(phonetimeout,By.xpath("//android.widget.Button[@text='立即支付']"),By.xpath("//android.widget.Button[@text='确定']"));
        if((el=cwait(By.xpath("//android.widget.Button[@text='确定']"),0))!=null){
            String msg = "";
            if((el=cwait(By.xpath("//android.widget.TextView"),0))!=null) msg=el.getText();
            click(el);
            throw new PayException(msg,3);
        }*/
        System.out.println("点击立即支付");
        click(phonedriver.findElement(By.xpath("//android.widget.Button[@text='立即支付']")));
        /**
         * 有时候会出现“你已在当前商户支付过一笔相同金额的订单，请确认是否继续支付”，让你点“确认支付”才能进行
         */
        cwait(x -> phonedriver.findElements(By.xpath("//android.widget.Button[@text='确认支付']")).size() > 0 ||
                phonedriver.findElementsById("com.tencent.mm:id/aor").size() > 0, phonetimeout);
        if ((el = cwait(By.xpath("//android.widget.Button[@text='确认支付']"), 0)) != null) {
            click(el);
        }
        System.out.println("输入密码");
        tapPayPassword("111111");
        updaterow(wp, PayConst.State.PaySuccess, null, System.currentTimeMillis(), "输入支付密码成功");
        click(phonedriver.findElement(By.xpath("//android.widget.TextView[@text='支付成功']")));
        System.out.println("支付成功");
        /*WebDriverWait wait = new WebDriverWait(phonedriver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//android.widget.TextView[@text='支付成功']")));*/
        System.out.println("点击完成");
        click(phonedriver.findElement(By.xpath("//android.widget.Button[@text='完成']")));
    }

    private void albumPay() throws Exception {
        WebElement el = phonedriver.findElementById("com.tencent.mm:id/ra");
        Point pp = el.getLocation();
        System.out.println("点击右上角+");
        click(el);//右上角+按钮
        System.out.println("点击扫一扫");
        click(phonedriver.findElement(By.xpath("//android.widget.TextView[@text='扫一扫']")));//扫一扫按钮
        System.out.println("点击...");
        Thread.currentThread().sleep(1000);
        new TouchAction(phonedriver).tap(PointOption.point(pp.x, pp.y)).perform();//扫码界面右上角“...”按钮
        System.out.println("从相册选取二维码");
        click(phonedriver.findElement(By.xpath("//android.widget.TextView[@text='从相册选取二维码']")));//从相册选取二维码按钮
        System.out.println("所有图片");
        click(phonedriver.findElement(By.xpath("//android.widget.TextView[@text='所有图片']")));//所有图片按钮
        List<WebElement> _el = phonedriver.findElements(By.className("android.widget.TextView"));
        System.out.println("点击特定相册");
        click(_el.stream().filter(x -> phoneimgdirname.equals(x.getText())).findFirst().get());
        System.out.println("点击第一张图");
        click(phonedriver.findElementById("com.tencent.mm:id/cel"));//文件夹中第一个图片
        assertt(x->cwait(phonetimeout, By.xpath("//android.widget.Button[@text='立即支付']"), By.xpath("//android.widget.Button[@text='确定']")));
        /*if((el=cwait(By.xpath("//android.widget.Button[@text='确定']"),0))!=null){
            String msg = "";
            if((el=cwait(By.xpath("//android.widget.TextView"),0))!=null) msg=el.getText();
            click(el);
            throw new PayException(msg,3);
        }*/
        System.out.println("点击立即支付");
        click(phonedriver.findElement(By.xpath("//android.widget.Button[@text='立即支付']")));
        /**
         * 有时候会出现“你已在当前商户支付过一笔相同金额的订单，请确认是否继续支付”，让你点“确认支付”才能进行
         */
        assertt(z->cwait(x -> phonedriver.findElements(By.xpath("//android.widget.Button[@text='确认支付']")).size() > 0 ||
                phonedriver.findElementsById("com.tencent.mm:id/aor").size() > 0, phonetimeout));
        if ((el = cwait(By.xpath("//android.widget.Button[@text='确认支付']"), 0)) != null) {
            click(el);
        }
        assertt(z->cwait(d -> phonedriver.findElementsById("com.tencent.mm:id/aor").size() > 0, phonetimeout));
        System.out.println("输入密码");
        tapPayPassword("111111");
        updaterow(wp, PayConst.State.PaySuccess, null, System.currentTimeMillis(), "输入支付密码成功");
        if (!cwait(d -> phonedriver.findElements(By.xpath("//android.widget.TextView[@text='支付成功']")).size() > 0, phonetimeout)) {
            throw new Exception("已输入密码，未出现支付成功提示。不确定是否已成功付款。");
        }
        click(phonedriver.findElement(By.xpath("//android.widget.TextView[@text='支付成功']")));
        System.out.println("支付成功");
        /*WebDriverWait wait = new WebDriverWait(phonedriver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//android.widget.TextView[@text='支付成功']")));*/
        System.out.println("点击完成");
        click(phonedriver.findElement(By.xpath("//android.widget.Button[@text='完成']")));
    }

    private void click(WebElement element) throws Exception {
        /*int middleX = element.getLocation().getX()+element.getSize().getWidth()/2;
        int middleY = element.getLocation().getY()+element.getSize().getHeight()/2;
        double x = middleX + (double)(System.currentTimeMillis()%90)/100*(element.getSize().getWidth()/2)*(System.currentTimeMillis()%2==0?1:-1);
        double y = middleY + (double)(System.currentTimeMillis()%90)/100*(element.getSize().getHeight()/2)*(System.currentTimeMillis()%2==0?1:-1);
        driver.tap(1,(int)x,(int)y,(int)(System.currentTimeMillis()%100+300));*/
        Dimension di = element.getSize();
        Point pp = element.getLocation();
        new TouchAction(phonedriver).tap(PointOption.point(pp.x + di.width / 2, pp.y + di.height / 2)).perform();
        //randomWait();
    }

    private void randomWait() throws InterruptedException {
        Thread.currentThread().sleep(System.currentTimeMillis() % 100 + 400);
    }

    private CrawlerPayEntity getPayTask() throws Exception {
        while (wp == null) {
            List<CrawlerPayEntity> ls = service.getPayOrderList(deviceid, PayConst.Source.Wechatpay,null,null, 1);
            if (ls != null && !ls.isEmpty()) {
                wp = ls.get(0);
            } else {
                try {
                    Thread.currentThread().sleep(/*Integer.valueOf(QConfigUtil.getRedisConfig(""))*/3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        wp.setPayCount(Optional.ofNullable(wp.getPayCount()).orElse(0)+1);
        return wp;
    }

    /*private CrawlerPay getrow() {
        CrawlerWechatPayGetRequestType req = new CrawlerWechatPayGetRequestType();
        req.setAuthCode("");
        CrawlerWechatPayGetResponseType resp = null;
        try {
            resp = SOA.invoke().crawlerWechatPayGet(req);//PayStatus=0 and PayType=1 and PayCount=0 ORDER BY Id
        } catch (Exception e) {
            System.out.println("取不到任务");
            //e.printStackTrace();
        }
        return resp==null||resp.getCrawlerPayList()==null||resp.getCrawlerPayList().size()==0?null:resp.getCrawlerPayList().get(0);
    }*/


    private String getDroidVer() {
        String ver = CommandUtil.executeCommand("adb shell getprop ro.build.version.release", deviceid);
        return ver.trim();
    }

    public void check() throws MalformedURLException, InterruptedException {
        try {
            phonedriver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
            /*if(phonedriver==null){
                phonedriver = new AndroidDriver<WebElement>(new URL(APPIUM_SERVER_IP), androidcapabilities);
                //月亮界面会好久
                Thread.sleep(5000);
            }*/

            /**
             * 二维码失效等提示，点返回没作用，必须得按确定按钮
             */
            WebElement el;
            if ((el = cwait(By.xpath("//android.widget.Button[@text='确定']"), 0)) != null) {
                click(el);
            }
            ;

            /**
             * 如果当前不在微信首页，不断点返回
             */
            /*String currAct;
            currAct = ((AndroidDriver) phonedriver).currentActivity();*/
            int count = 0;
            while (/*!currAct.equals(".ui.LauncherUI")*/cwait(By.xpath("//android.widget.TextView[@text='通讯录']"),0)==null && count++ < 7) {
                System.out.println("当前不在微信首页，点返回");
                /*((AndroidDriver) phonedriver).pressKeyCode(AndroidKeyCode.BACK);*/
                ((AndroidDriver) phonedriver).pressKey(new KeyEvent(AndroidKey.BACK));
                //currAct = ((AndroidDriver) phonedriver).currentActivity();
            }

            /**
             * 如果微信在后台运行，把它调到前台
             */
            CommandUtil.executeCommand("adb shell am start --activity-single-top com.tencent.mm/.ui.LauncherUI", deviceid);
            /*currAct = ((AndroidDriver)phonedriver).currentActivity();
            if(currAct.equals(".ui.LauncherUI") && cwait(By.xpath("//android.widget.TextView[@text='微信']"),0)==null){
                System.out.println("微信在后台运行，把它调到前台");
            }*/


            //
            /*if(".plugin.setting.ui.setting.SettingsSwitchAccountUI".equals(((AndroidDriver) driver).currentActivity())){
                TouchAction(driver).tap(None, 560, 750, 1).perform()
            }*/
            /*if (!existElement(driver, "//*[@text='通讯录']")) {//com.tencent.mm:id/d3r
                if(existElement(driver,"//android.widget.Button[@text='登录'][@enabled='true']")){//登陆、注册页面按钮
                    click(driver.findElement(By.xpath("//android.widget.Button[@text='登录']")));//登陆、注册页面按钮
                    click(driver.findElement(By.xpath("//*[@text='用微信号/QQ号/邮箱登录']")));
                }
                if(existElement(driver,"//android.widget.Button[@text='更多']")){
                    click(driver.findElement(By.xpath("//android.widget.Button[@text='更多']")));
                    click(driver.findElement(By.xpath("//*[@text='登录其他帐号']")));
                    click(driver.findElement(By.xpath("//*[@text='用微信号/QQ号/邮箱登录']")));
                }
                List<WebElement> list = driver.findElements(By.xpath("//android.widget.EditText"));
                sendkey(list.get(0),"zhanghao");
                list = driver.findElements(By.xpath("//android.widget.EditText"));
                sendkey(list.get(1),"mima");
                Util.executeCommand("adb shell input keyevent 111",deviceid);
                Thread.currentThread().sleep(1000);
                click(driver.findElement(By.xpath("//*[@text='登录']")));
                while(!existElement(driver, "//*[@text='通讯录']")){
                    if(existElementById(driver,"com.tencent.mm:id/az9")){
                        click(driver.findElementById("com.tencent.mm:id/az9"));
                    }
                }
            }*/
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("检查异常");
            init();
        } finally {
            System.out.println("检查结束");
            phonedriver.manage().timeouts().implicitlyWait(phonetimeout, TimeUnit.SECONDS);
        }
    }

    private void sendkey(WebElement webElement, String s) throws InterruptedException {
        webElement.sendKeys(s);
        /*for(int i=0;i<s.length();i++){
            String v = s.substring(i,i+1);
            webElement.sendKeys(v);
            Thread.currentThread().sleep(System.currentTimeMillis()%30+30);
        }*/
    }

    private void tapPayPassword(String s) throws InterruptedException {
        WebElement _k = phonedriver.findElementById("com.tencent.mm:id/aor");//密码键盘
        int x = _k.getLocation().getX();
        int y = _k.getLocation().getY();
        double cellheight = _k.getSize().height / 4;
        double cellwidth = _k.getSize().width / 3;
        int xpos;
        int ypos;

        for (int i = 0; i < s.length(); i++) {
            int ss = Integer.valueOf(s.substring(i, i + 1));
            switch (ss) {
                case 0:
                    xpos = x + (int) (1.5 * cellwidth);
                    ypos = y + (int) (3.5 * cellheight);
                    break;
                default:
                    double row = Math.floor((ss - 1) / 3) + 0.5;
                    double col = (ss - 1) % 3 + 0.5;
                    xpos = x + (int) (col * cellwidth);
                    ypos = y + (int) (row * cellheight);
            }
            /*xpos = xpos+(int)(System.currentTimeMillis()%20+10);
            ypos = ypos+(int)(System.currentTimeMillis()%20+10);*/
            //driver.tap(1, xpos, ypos, (int)(System.currentTimeMillis()%20+20));
            new TouchAction(phonedriver).tap(PointOption.point(xpos, ypos)).perform();
            //randomWait();
        }
    }

    private static void generateQRCodeImage(String text, String filePath)
            throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 220, 220);

        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }

    private static Boolean existElement(AppiumDriver<WebElement> driver, String id) {
        List<WebElement> __ = driver.findElements(By.xpath(id));
        if (__ != null && __.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    private static Boolean existElementById(AppiumDriver<WebElement> driver, String id) {
        List<WebElement> __ = driver.findElementsById(id);
        if (__ != null && __.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    private static void hideKeyboard(AppiumDriver<WebElement> driver) {
        try {
            driver.hideKeyboard();
        } catch (Exception ex) {
        }
    }

    public void assertt(Predicate f) throws Exception {
        if(!f.test(null)){
            throw new Exception(f.toString());
        }
    }
}
