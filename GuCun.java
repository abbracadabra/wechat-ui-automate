package com.ctrip.ttd.connect.pay.service;

import com.ctrip.ttd.connect.pay.bll.HttpService;
import com.ctrip.ttd.connect.pay.entity.QrViewer;
import com.ctrip.ttd.connect.pay.entity.crawler.CrawlerPayEntity;
import com.ctrip.ttd.connect.pay.entity.crawler.order.CrawlerOrderVoucherEntity;
import com.ctrip.ttd.connect.pay.shared.constants.PayConst;
import com.ctrip.ttd.connect.pay.shared.constants.VoucherContants;
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
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.*;
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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//打开appiumserver->adb->夜神模拟器
@Component("GuCun")
@Scope("singleton")
public class GuCun extends Device{

    String APPIUM_SERVER_IP = "http://127.0.0.1:4723/wd/hub";//appium监听地址
    DesiredCapabilities androidcapabilities;//安卓的配置
    AppiumDriver<WebElement> phonedriver;//安卓模拟机驱动器
    int phonetimeout = 5;
    CrawlerPayEntity wp;//支付状态：0未支付1支付中2支付成功3支付失败4支付取消
    HttpService payResultService = new HttpService();
    String connectno="2059";

    @Autowired
    HttpService service;

    public void init() throws MalformedURLException, InterruptedException {
        System.out.println("初始化开始");

        CommandUtil.executeCommand("adb connect 127.0.0.1:62001", null);

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


    public void placeorder() throws Exception {
        if(cwait(By.xpath("//android.widget.TextView[@text='通讯录']"),0)!=null){
            click(phonedriver.findElement(By.xpath("//android.view.View[@text='顾村公园服务号']")));
        }

        click(phonedriver.findElement(By.xpath("//android.widget.TextView[@text='在线购票']")));
        click(phonedriver.findElement(By.xpath("//android.widget.TextView[@text='门票预订']")));

        //加载首页
        if(cwait(By.xpath("//android.view.View[@text='上海市宝山区沪太路4788号']"),8)==null) throw new Exception("By.xpath(\"//android.view.View[@text='上海市宝山区沪太路4788号']\")");

        //加载完后短时间，有首页样式调整
        Thread.currentThread().sleep(1000);

        String playdate = wp.getOrderItemEntity().getUseStartDateStr()/*"2020-04-02"*/;
        String tkc = wp.getOrderItemEntity().getConnectAttributes().get("GuCunGongYuan.TicketType")/*"顾村公园成人票"*/;
        Integer tc = wp.getOrderItemEntity().getQuantity();
        String period = wp.getOrderItemEntity().getConnectAttributes().get("GuCunGongYuan.UseTimeSpan")/*"上午"*/;

        String playyear = playdate.split("-")[0];
        Integer playmonth = Integer.valueOf(playdate.split("-")[1]);
        Integer playday = Integer.valueOf(playdate.split("-")[2]);

        //选日期
        WebElement dateselector;
        WebElement yearselector;
        WebElement month=null;

        if(!playdate.equals(phonedriver.findElement(By.xpath("//android.widget.Image[@text='HeuzCfvVnroPgkJD3PIX17cO1fwBERq70fdaTSwAAAABJRU5ErkJggg==']/preceding-sibling::*[1]")).getText())){
            //点击日历控件
            click((dateselector=phonedriver.findElement(By.xpath("//android.widget.Image[@text='HeuzCfvVnroPgkJD3PIX17cO1fwBERq70fdaTSwAAAABJRU5ErkJggg==']"))));
            //年份
            String year = (yearselector=phonedriver.findElement(By.xpath("//android.widget.Image[@text='HeuzCfvVnroPgkJD3PIX17cO1fwBERq70fdaTSwAAAABJRU5ErkJggg==']/following-sibling::*[1]/*[2]/*[2]"))).getAttribute("text");
            if(!playyear.equals(year)){
                //点击正确年份
                click(yearselector);
                click(phonedriver.findElement(By.xpath("//android.view.View[@text='2029']/../*[@text="+playyear+"]")));
            }
            //月份
            List<WebElement> months = phonedriver.findElements(By.xpath("//android.view.View[@text='十二月'][1]/../android.view.View"));
            //取出当前控件的月份
            for(WebElement ee : months){
                int area = ee.getSize().getHeight()*ee.getSize().getWidth();
                if(area>300){
                    month = ee;
                    break;
                }
            }
            String[] ccms = new String[]{"一月","二月","三月","四月","五月","六月","七月","八月","九月","十月","十一月","十二月"};
            List<String> ccmss = Arrays.asList(ccms);
            if(ccmss.indexOf(month.getText())==-1) throw new Exception("ccmss.indexOf(month.getText())==-1");
            int incrmn = playmonth - ccmss.indexOf(month.getText())-1;
            //调至正确月份
            if(incrmn>0){
                WebElement nextm = phonedriver.findElement(By.xpath("//android.widget.Image[@text='HeuzCfvVnroPgkJD3PIX17cO1fwBERq70fdaTSwAAAABJRU5ErkJggg==']/following-sibling::*[1]/*[3]"));
                for(int i=0;i<incrmn;i++){
                    click(nextm);
                }
            }
            if(incrmn<0){
                WebElement prevtm = phonedriver.findElement(By.xpath("//android.widget.Image[@text='HeuzCfvVnroPgkJD3PIX17cO1fwBERq70fdaTSwAAAABJRU5ErkJggg==']/following-sibling::*[1]/*[1]"));
                for(int i=0;i>incrmn;i--){
                    click(prevtm);
                }
            }
            List<WebElement> days = phonedriver.findElements(By.xpath("//android.widget.Image[@text='HeuzCfvVnroPgkJD3PIX17cO1fwBERq70fdaTSwAAAABJRU5ErkJggg==']/following-sibling::*[1]/*[5]/*/*/*[1]"));
            assertt(x->days.size()==42);
            //找到第一个1号在日历控件中的位置
            int oneindex;
            for(oneindex=0;oneindex<days.size();oneindex++){
                if(days.get(oneindex).getText().equals("1")){
                    break;
                }
            }
            //点击正确日期
            click(days.get(oneindex+playday-1));
        }

        //保证游玩日期选择正确
        assertt(x->playdate.equals(phonedriver.findElement(By.xpath("//android.widget.Image[@text='HeuzCfvVnroPgkJD3PIX17cO1fwBERq70fdaTSwAAAABJRU5ErkJggg==']/preceding-sibling::*[1]")).getText()));

        //下滑到增加购买数量的控件
        WebElement _e = scrollToVisible(phonedriver.findElement(By.xpath("//android.view.View[@text='"+tkc+"']/following-sibling::*[1]/android.widget.Image[last()]")),970,158,1338);
        for(int i=0;i<tc;i++){
            //买几张就点几下
            click(_e);
        }

        //点击阅读条款
        click(scrollToVisible(phonedriver.findElement(By.xpath("//android.widget.Image[@text='pnHYeRdC3IXHkeWXR6D8ksLnpq7Q6qiv8DFomdYfBCQeMAAAAASUVORK5CYII=']")),970,158,1338));

        //立即购买
        click(phonedriver.findElement(By.xpath("//android.view.View[@text='立即购买']")));

        //填出行人信息
        phonedriver.findElement(By.xpath("//android.view.View[@text='*姓  名']/following-sibling::*[1]/android.widget.EditText")).sendKeys(wp.getOrderItemEntity().getPassengerInfos().get(0).getName());
        phonedriver.findElement(By.xpath("//android.view.View[@text='*身份证']/following-sibling::*[1]/android.widget.EditText")).sendKeys(wp.getOrderItemEntity().getPassengerInfos().get(0).getIdCardNo());
        phonedriver.findElement(By.xpath("//android.view.View[@text='*手机号']/following-sibling::*[1]/android.widget.EditText")).sendKeys(wp.getOrderItemEntity().getPassengerInfos().get(0).getContactInfo());

        scrollToVisible(phonedriver.findElement(By.xpath("//android.view.View[@text='特别提示：']")),970,158,1338);

        //上午还是下午
        click(phonedriver.findElement(By.xpath("//android.view.View[@text='"+period+"']")));

        if(cwait(By.xpath("//android.view.View[@text='是否自驾 ']"),0)!=null){
            List<WebElement> _ess = phonedriver.findElements(By.xpath("//android.view.View[@text='是否自驾 '][1]/../android.widget.Image"));
            click(_ess.get(1));
        }

        click(phonedriver.findElement(By.xpath("//android.view.View[@text='立即购买']")));

        //供应商订单号
        String orderid = phonedriver.findElement(By.xpath("//android.view.View[@text='订单号：']/following-sibling::*[1]")).getText();

        //再次确认游玩日期设置正确
        assertt(x->playdate.equals(phonedriver.findElement(By.xpath("//android.view.View[@text='出游时间：']/following-sibling::*[1]")).getText()));

        click(phonedriver.findElement(By.xpath("//android.view.View[@text='立即支付']")));
        tapPayPassword("111111");
        updaterow(wp,PayConst.State.PaySuccess,null,System.currentTimeMillis(),"输入支付密码成功");

        click(phonedriver.findElement(By.xpath("//android.widget.TextView[@text='支付成功']")));
        click(phonedriver.findElement(By.xpath("//android.widget.Button[@text='完成']")));
        click(phonedriver.findElement(By.xpath("//android.widget.Button[@text='确定']")));

        //凭证码
        String voucherid = phonedriver.findElement(By.xpath("//android.view.View[contains(@text, '凭证码：')]")).getText();
        voucherid = voucherid.split("：")[1].trim();
        //截屏base64
        String shbs = phonedriver.getScreenshotAs(OutputType.BASE64);

        wp.getOrderItemEntity().setThirdOrderId(orderid);
        List<CrawlerOrderVoucherEntity> vouchers = new ArrayList<>();
        CrawlerOrderVoucherEntity vv = new CrawlerOrderVoucherEntity();
        vouchers.add(vv);
        vv.setVoucherNo(shbs);
        vv.setVoucherSource(VoucherContants.Source.STRING_CREATE_IMAGE);
        CrawlerOrderVoucherEntity vv1 = new CrawlerOrderVoucherEntity();
        vouchers.add(vv1);
        vv1.setVoucherNo(voucherid);
        vv1.setVoucherSource(VoucherContants.Source.STRING_CREATE_IMAGE);
        wp.getOrderItemEntity().setVouchers(vouchers);

        updaterow(wp,PayConst.State.PaySuccess,null,System.currentTimeMillis(),"支付成功");

        //click(scrollToVisible(phonedriver.findElement(By.xpath("//android.view.View[@text='我知道了']")),970,158,phonedriver.manage().window().getSize().getHeight()));

        //回到与供应商公众号的聊天窗口
        click(phonedriver.findElement(By.xpath("//android.widget.ImageView[@content-desc='返回']")));
    }

    public void refund() throws Exception{
        String url = "http://gucun.leyoujisan.com/#/payrefund?orderNo="+wp.getOrderItemEntity().getThirdOrderId()+"&flag=false";
        if(cwait(By.xpath("//android.widget.TextView[@text='通讯录']"),0)!=null){
            click(phonedriver.findElement(By.xpath("//android.view.View[@text='顾村公园服务号']")));
        }
        click(phonedriver.findElement(By.xpath("//android.widget.ImageView[@content-desc='消息']")));
        phonedriver.findElement(By.xpath("//android.widget.ImageButton[@content-desc='切换到按住说话']/following-sibling::*[1]/android.widget.EditText")).sendKeys(url);
        click(phonedriver.findElement(By.xpath("//android.widget.Button[@text='发送']")));
        Point pp = phonedriver.findElement(By.xpath("//android.widget.ImageButton[@content-desc='切换到按住说话'][1]/ancestor::android.widget.FrameLayout[1]/preceding-sibling::*[1]//android.widget.ListView/*[last()]//android.view.View[1]")).getLocation();
        new TouchAction(phonedriver).tap(PointOption.point(pp.getX()+200, pp.getY()+50)).perform();
        click(cwait(By.xpath("//android.view.View[@text='申请退款']"),10));
        phonedriver.findElement(By.xpath("//android.widget.TextView[@text='退款成功！']"));
        phonedriver.findElement(By.xpath("//android.widget.Button[@text='确定']")).click();
        updaterow(wp,PayConst.State.PaySuccess,null,null,"取消成功");
    }

    public WebElement scrollToVisible(WebElement el,int ystart,int upperbound,int lowerbound){
        String ss;
        Matcher m1;
        Matcher m2;
        int ue;
        int le;
        int count=0;
        while(count++<3 && (ss = el.getAttribute("bounds"))!=null && (m1=Pattern.compile("(?<=,)\\d+(?=]\\[)").matcher(ss)).find() && (m2=Pattern.compile("(?<=,)\\d+(?=]$)").matcher(ss)).find() && !((ue=Integer.valueOf(m1.group(0)))>upperbound & (le=Integer.valueOf(m2.group(0)))<lowerbound)){
            if(ue<=upperbound){
                new TouchAction(phonedriver).press(PointOption.point(500,ystart)).waitAction(WaitOptions.waitOptions(Duration.ofSeconds(0))).moveTo(PointOption.point(500,Math.min(phonedriver.manage().window().getSize().getHeight()-1,ystart+lowerbound-le))).release().perform();
            }
            if(le>=lowerbound){
                new TouchAction(phonedriver).press(PointOption.point(500,ystart)).waitAction(WaitOptions.waitOptions(Duration.ofSeconds(0))).moveTo(PointOption.point(500,Math.max(0,ystart-(ue-upperbound)))).release().perform();
            }
        }
        return el;
    }

   /* public void toVisible(String s){
        //((AndroidDriver)phonedriver).findElementByAndroidUIAutomator("new UiScrollable(new UiSelector().scrollable(true).instance(0)).scrollIntoView(new UiSelector().textMatches(\""+s+"\").instance(0))");

    }*/

    /**
     * 等timeout时间，直到满足条件
     */
    public boolean cwait(Predicate<AndroidDriver> pred, int timeout) throws InterruptedException {
        phonedriver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        int count = 0;
        boolean flag = false;
        long thresh = new Date().getTime();
        thresh += timeout * timeout * 1000;
        while ((count++ == 0 || new Date().getTime() < thresh) && !(flag = pred.test((AndroidDriver) phonedriver))) {
            if (timeout != 0) Thread.currentThread().sleep(500);
        }
        phonedriver.manage().timeouts().implicitlyWait(phonetimeout, TimeUnit.SECONDS);
        return flag;
    }

    public boolean cwait(int timeout, By... by) throws InterruptedException {
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
            if (timeout != 0) Thread.currentThread().sleep(500);
        }
        phonedriver.manage().timeouts().implicitlyWait(phonetimeout, TimeUnit.SECONDS);
        return valid;
    }

    public WebElement cwait(By by, int timeout) {
        phonedriver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        List<WebElement> els = new ArrayList<>();
        int count = 0;
        long thresh = new Date().getTime();
        thresh += timeout * timeout * 1000;
        while ((count++ == 0 || new Date().getTime() < thresh) && (els = phonedriver.findElements(by)).isEmpty()) {
            if (timeout != 0) {
                try {
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        phonedriver.manage().timeouts().implicitlyWait(phonetimeout, TimeUnit.SECONDS);
        return els.isEmpty() ? null : els.get(0);
    }

    private String getDroidVer() {
        String ver = CommandUtil.executeCommand("adb shell getprop ro.build.version.release", deviceid);
        return ver.trim();
    }

    public void check() throws MalformedURLException, InterruptedException {
        try {
            phonedriver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);

            WebElement el;
            if ((el = cwait(By.xpath("//android.widget.Button[@text='确定']"), 0)) != null) {
                click(el);
            }

            /**
             * 如果当前不在微信首页，不断点返回
             */
            int count = 0;
            while (cwait(By.xpath("//android.widget.TextView[@text='通讯录']"),0)==null && count++ < 7) {
                System.out.println("当前不在微信首页，点返回");
                ((AndroidDriver) phonedriver).pressKey(new KeyEvent(AndroidKey.BACK));
            }

            /**
             * 如果微信在后台运行，把它调到前台
             */
            CommandUtil.executeCommand("adb shell am start --activity-single-top com.tencent.mm/.ui.LauncherUI", deviceid);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("检查异常");
            init();
        } finally {
            System.out.println("检查结束");
            phonedriver.manage().timeouts().implicitlyWait(phonetimeout, TimeUnit.SECONDS);
        }
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

    private void tapPayPassword(String s) throws InterruptedException {
        WebElement _k = phonedriver.findElement(By.xpath("//android.widget.ImageView[@content-desc='收起键盘']/../../android.widget.LinearLayout"));//密码键盘
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

    public void assertt(Predicate f) throws Exception {
        if(!f.test(null)){
            throw new Exception(f.toString());
        }
    }



    @Override
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

                /**
                 * 从pay表取一条任务
                 */
                System.out.println("获取任务中");
                while (wp == null) {
                    List<CrawlerPayEntity> ls = service.getPayOrderList(deviceid, PayConst.Source.Wechath5,connectno,null, 1);
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
                System.out.println("取到一个任务");
                wp.setPayCount(Optional.ofNullable(wp.getPayCount()).orElse(0)+1);

                if(wp.getOrderItemEntity().getOperation().equalsIgnoreCase("createorder")){
                    placeorder();
                }else if(wp.getOrderItemEntity().getOperation().equalsIgnoreCase("cancelorder")){
                    refund();
                }

            } catch (Throwable e) {
                e.printStackTrace();
                System.out.println("支付失败");
                if (wp != null) {
                    String msg = e.getMessage();
                    if (msg != null && e.getMessage().length() > 100) {
                        msg = msg.substring(0, 100);
                    }
                    updaterow(wp, wp.getPayStatus()==PayConst.State.PaySuccess?PayConst.State.PaySuccess:PayConst.State.PayFail, null, System.currentTimeMillis(), msg);
                }
            } finally {
                wp = null;
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


    public static void main(String[] args) throws Exception {
        GuCun dd = new GuCun();
        dd.init();
        dd.refund();
    }
}
