package com.chuzhen.healthreminder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.google.common.collect.ImmutableSet;
import com.taobao.api.ApiException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author chuzhen
 * @date 2021/6/5
 */
public class Main {

    private static final List<HealthRemind> HEALTH_REMIND_LIST;

    public static void main(String[] args){
        String dingUrl = System.getProperty("ding_url");
        String onlyWorkDays = System.getProperty("only_work_day");
        String workUrl = System.getProperty("work_url");
        String test = System.getProperty("test");
        boolean isTest = "true".equals(test);
        DingTalkClient client = new DefaultDingTalkClient(dingUrl);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        int chineseHour = now.getHour();
        List<HealthRemind> hitReminders = HEALTH_REMIND_LIST;
        if(!isTest){
            hitReminders = HEALTH_REMIND_LIST.stream().filter(healthRemind -> healthRemind.hit(chineseHour)).collect(Collectors.toList());
        }

        if(hitReminders.isEmpty()){
            return;
        }
        if(Objects.equals(onlyWorkDays, "true")){
            String workDays = doGet(workUrl + now.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            JSONObject jsonObject =JSON.parseObject(workDays);
            System.out.println("test whether work days response:" + workDays);
            if("2".equals(jsonObject.getJSONObject("result").getString("workmk")) && !isTest){
                return;
            }
        }

        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("???????????????????????????\n");

            hitReminders.forEach(healthRemind -> stringBuilder.append(healthRemind.getContent()).append("\n"));

            OapiRobotSendRequest request = new OapiRobotSendRequest();
            request.setMsgtype("text");
            OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
            text.setContent(stringBuilder.toString());
            request.setText(text);
            OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
            at.setIsAtAll(true);
            request.setAt(at);
            client.execute(request);
        } catch (ApiException e) {
            e.printStackTrace();
        }

    }

    public static String doGet(String url) {
        try {
            HttpClient client = new DefaultHttpClient();
            //??????get??????
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);

            /**????????????????????????????????????**/
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                /**??????????????????????????????json???????????????**/
                String strResult = EntityUtils.toString(response.getEntity());

                return strResult;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    static {
        HEALTH_REMIND_LIST = new ArrayList<>();
        HEALTH_REMIND_LIST.add(new HealthRemind(ImmutableSet.of(10, 11, 13, 14, 15, 16, 17, 18), "emmm...???????????????????????????????????????????????????~"));
        HEALTH_REMIND_LIST.add(new HealthRemind(ImmutableSet.of(11, 14, 17), "??????...?????????????????????1???2???3???4???2???2???3???4..."));
        HEALTH_REMIND_LIST.add(new HealthRemind(ImmutableSet.of(15), "??????????????????????????????body???????????????~?????????~??????????????????~??????..."));
        HEALTH_REMIND_LIST.add(new HealthRemind(ImmutableSet.of(11, 15), "?????????????????????????????????????????????(??? ???) (??? ???) (??? ???) (??? ???)"));
    }

    @AllArgsConstructor
    @Data
    public static class HealthRemind{
        private Set<Integer> triggerHours;
        private String content;

        public boolean hit(int hour){
            return triggerHours.contains(hour);
        }
    }
}
