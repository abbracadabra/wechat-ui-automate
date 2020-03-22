package com.ctrip.ttd.connect.pay.controller;

import com.ctrip.ttd.connect.pay.bll.HttpService;
import com.ctrip.ttd.connect.pay.service.Device;
import com.ctrip.ttd.connect.pay.service.WeChat;
import com.ctrip.ttd.connect.pay.util.ConfigUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Component
@Scope("singleton")
public class OrderPayManager implements InitializingBean, ApplicationContextAware {

    @Autowired
    HttpService service;

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ConfigUtil.init("config.properties");

                //export devices=Wechat_62001_zhanghao&GuCun_62002_zhanghao
                String ss = System.getenv("devices");
                String[] sss = ss.split("&");
                for(String s : sss){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String[] y = s.split("_");
                            Device dd = (Device) applicationContext.getBean(y[0]);
                            dd.deviceid = "127.0.0.1:"+y[1];
                            dd.zhanghao = "xxx";
                            dd.start();
                        }
                    }).start();
                }
            }
        }).start();

    }

    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }
}
