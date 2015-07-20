package com.dotnar.wx.service;

import com.dotnar.api.PayMchAPI;
import com.dotnar.bean.paymch.*;
import com.dotnar.contant.WXPayConfigure;
import com.dotnar.dao.MchPayNotifyRepository;
import com.dotnar.dao.UnifiedorderKeyRepository;
import com.dotnar.dao.UnifiedorderRepository;
import com.dotnar.dao.UnifiedorderResultRepository;
import com.dotnar.exception.WXPayExceptioin;
import com.dotnar.util.*;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Created by chovans on 15/7/17.
 */
@Service
public class UnifiedorderService {

    //重复通知过滤  时效60秒
    private static ExpireSet<String> expireSet = new ExpireSet<String>(60);
    private static UnifiedorderRepository unifiedorderRepository;
    private static UnifiedorderKeyRepository unifiedorderKeyRepository;
    private static UnifiedorderResultRepository unifiedorderResultRepository;
    private static MchPayNotifyRepository mchPayNotifyRepository;

    @Autowired(required = true)
    public UnifiedorderService(@Qualifier("UnifiedorderRepository") UnifiedorderRepository unifiedorderRepository,
                               @Qualifier("UnifiedorderKeyRepository") UnifiedorderKeyRepository unifiedorderKeyRepository,
                               @Qualifier("UnifiedorderResultRepository") UnifiedorderResultRepository unifiedorderResultRepository,
                               @Qualifier("MchPayNotifyRepository") MchPayNotifyRepository mchPayNotifyRepository) {
        UnifiedorderService.unifiedorderRepository = unifiedorderRepository;
        UnifiedorderService.unifiedorderKeyRepository = unifiedorderKeyRepository;
        UnifiedorderService.unifiedorderResultRepository = unifiedorderResultRepository;
        UnifiedorderService.mchPayNotifyRepository = mchPayNotifyRepository;
    }


    /**
     * 微信支付申请（兼容js和微信扫码模式2）
     * 对应内容文档中所有信息
     * 省略：nonce_str,sign,time_start,time_expire,notify_url
     *
     * @param objectInfo
     * @param key
     * @param security_key 识别标志，支付成功回调函数中使用
     * @return 支付请求的所有参数{appId,timeStamp,nonceStr,package,signType,paySin}
     */
    public static String payRequestJson(String objectInfo, String key, String security_key) {


        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        Unifiedorder unifiedorder = JsonUtil.parseObject(objectInfo, Unifiedorder.class);
        unifiedorder.setNonce_str(UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20));
        unifiedorder.setTime_start(sdf.format(new Date()));
        unifiedorder.setTime_expire(sdf.format(new Date().getTime() + (1000 * 60 * WXPayConfigure.EFFECTIVE_TIME)));
        unifiedorder.setNotify_url(WXPayConfigure.NOTIFY_URL);

        System.out.println("====接受预支付订单请求：" + unifiedorder + new Date());


        //记录一个UnifiedorderKey，key信息
        UnifiedorderKey unifiedorderKey = new UnifiedorderKey();
        unifiedorderKey.setAppId(unifiedorder.getAppid());
        unifiedorderKey.setMchId(unifiedorder.getMch_id());
        unifiedorderKey.setOpenId(unifiedorder.getOpenid());
        unifiedorderKey.setOutTradeNo(unifiedorder.getOut_trade_no());
        unifiedorderKey.set_key(key);
        unifiedorderKey.setSecurityKey(security_key);
        unifiedorderKeyRepository.save(unifiedorderKey);


        String json = null;

        try {
            UnifiedorderResult unifiedorderResult = PayMchAPI.payUnifiedorder(unifiedorder, key);

            //记录一个请求
            unifiedorderRepository.save(unifiedorder);
            //记录一个返回值
            unifiedorderResultRepository.save(unifiedorderResult);

            json = PayUtil.generateMchPayJsRequestJson(unifiedorderResult.getPrepay_id(),
                    unifiedorderResult.getAppid(), key, unifiedorderResult.getCode_url());

        } catch (Exception e) {
            return JsonUtil.toJSONString(new WXPayExceptioin(e.getMessage()));
        }

        return json;
    }

    /**
     * 支付回调请求Notify
     *
     * @param xmlString
     * @param response
     * @throws IOException
     * @warn 没有和js后台进行交互，待添加
     */
    public static UnifiedorderKey payRequestResultJson(String xmlString,
                                                       HttpServletResponse response) throws Exception {

        System.out.println("==== 收到微信回调：" + xmlString);
        //获取请求数据
        MchPayNotify payNotify = XMLConverUtil.convertToObject(MchPayNotify.class, xmlString);
        payNotify.setCreateOn(new Date());
        //已处理 去重
        if (expireSet.contains(payNotify.getTransaction_id())) {
            return null;
        }
        //获取key
        UnifiedorderKey unifiedorderKey = unifiedorderKeyRepository.findByAppIdAndMchIdAndOutTradeNoAndOpenId(payNotify.getAppid(), payNotify.getMch_id(),
                payNotify.getOut_trade_no(), payNotify.getOpenid());
        System.out.println("==== 验证签名 ====");
        //签名验证
        if (SignatureUtil.validateAppSignature(payNotify, unifiedorderKey.get_key())) {
            System.out.println("==== 验证签名成功 ====");
            expireSet.add(payNotify.getTransaction_id());
            MchNotifyXml baseResult = new MchNotifyXml();
            baseResult.setReturn_code("SUCCESS");
            baseResult.setReturn_msg("OK");
            response.getOutputStream().write(XMLConverUtil.convertToXML(baseResult).getBytes());
            mchPayNotifyRepository.save(payNotify);
            payNotify.setSecurityKey(unifiedorderKey.getSecurityKey());

            //发送到js端
            try {
                NotifyUtil.sendToJS(payNotify);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return unifiedorderKey;
        } else {
            System.out.println("==== 验证签名失败 ====");
            MchNotifyXml baseResult = new MchNotifyXml();
            baseResult.setReturn_code("FAIL");
            baseResult.setReturn_msg("ERROR");
            response.getOutputStream().write(XMLConverUtil.convertToXML(baseResult).getBytes());
        }

        return null;
    }

    public static void main(String[] args){
        String xml = "<xml><appid><![CDATA[wxcf74f930098faee1]]></appid>\n" +
                "<bank_type><![CDATA[ICBC_DEBIT]]></bank_type>\n" +
                "<cash_fee><![CDATA[1]]></cash_fee>\n" +
                "<fee_type><![CDATA[CNY]]></fee_type>\n" +
                "<is_subscribe><![CDATA[Y]]></is_subscribe>\n" +
                "<mch_id><![CDATA[1233472902]]></mch_id>\n" +
                "<nonce_str><![CDATA[8410b0d26cc54d56a8f8]]></nonce_str>\n" +
                "<openid><![CDATA[o_QrEjiA5Yvgp3N4HnKhy9O5J1mU]]></openid>\n" +
                "<out_trade_no><![CDATA[PA_A_aauvmig0]]></out_trade_no>\n" +
                "<result_code><![CDATA[SUCCESS]]></result_code>\n" +
                "<return_code><![CDATA[SUCCESS]]></return_code>\n" +
                "<sign><![CDATA[11C5BA8F52370B4094D6E0385B5AA1FE]]></sign>\n" +
                "<time_end><![CDATA[20150720175545]]></time_end>\n" +
                "<total_fee>1</total_fee>\n" +
                "<trade_type><![CDATA[JSAPI]]></trade_type>\n" +
                "<transaction_id><![CDATA[1009600275201507200443479051]]></transaction_id>\n" +
                "</xml>";

        try{
            NotifyUtil.sendToJS(XMLConverUtil.convertToObject(MchPayNotify.class, xml));
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }



}