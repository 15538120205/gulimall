package com.atguigu.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "9021000162629413";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCbJnd18tbqMcZDT8H3R2K0qI7leVy+15+hNKYnsSY52I6aM90/yusNX5D3aWwxoZdT88br+gRNdSVVZsuQa2JwGleb5q51j2LoqnHd2v4HioHgdRii5Dl/Dxlbfh3dDDJiUgO0vE83nUMuVBB4ColBNzVJ31ciXn/hXodoH8nAtCyEm3oYma+W3N+iqi8a1yZ3b1IezX7ttNMP+NoH6lNf6N3s23YOtvdGWXFs8Ep+KYc3i7PPMJtB06rZTwqThyDo07e63vXaaFXXBABFPKL8KE5rxLzGnmRo7rBn9YyUUfiGFxKcZQdCEJ8C0NaMSoLPc127a2Aqg9+TqnUacYBNAgMBAAECggEAPU2SsCkWoAywwRH3myQ6rRVY+PTeH44FReYOrOpDJ2IY3ncVucoM3Ajht9CQ7n9h0ssk00LDhnN+H+NxFYxIpfDSowgf8NWKCFKokTehXgttS+oYpOw2h6zIoS0Q0bGrCsxLKJOrXVcnH1wZ8cR//SO6S4kS3cKnFr6KFCtTRW+NWq8Yv1Fj14GYDUJ3PMZCBZJG8LUtyCLxt9RF5O5OFKE2SrQolBkJRcNPcLUJ5euijRzpLu4l0lZ6sJF6QzESTu1z3vYquXJGqMcJXmPuR237gX60/3ItPStR2ceP75WtgGkhykQN4ytguoKfp4cad6Cdhmb2TG+hSnFJCKHbIQKBgQDbaWXSbSMdZHNxcdbyWdww8yK0Y4kv3D3nUxEHcZotfQmxkAf2FA73QNAuTWZpocdX6EYcTfcukq4CrAetMDSBXNN5M6fft7Q6kjp1dYaSboSWB/G5Cf8EBeV3yZH4l3MNg3tGms9geW8ZbbTkUw7m/gMKXscikLSs4TNJcXQCGQKBgQC1BcWpRGjJzqCZX+M862WF6h94o5u3e1tmQiFkWHGr7BzDq3w5C0I/uDimK/DdlD/bDZgUDRli31QwwKilPka8N22L9jSv7+YjkAX5cTeTV7BJPvT8+SS2kf1HUX2wpWSHCpIMF/zrgm/qGu77kH1he6gNTI/eThKkwImfhpj+VQKBgQCxi9BBPt8zYZ2pJx1wbxam5ZgnsIPKnsXY8nbNNzgzpwK5FHvcvM5/dYsAbNF6mNbeY66YKN7kaP02XE657lAOfjcB1LL7bYQCAWIcukVzKMtRPIx114jskHjnPc1JBASt4r9Wmt9Eif9E7Gl8eVmNwe5j7d3mMoKBgpolzBC7wQKBgG1xwvK2yIASrChuv82KKIRRa5udlzzUmQr3OocXrgoao71thZpbgz9RGvbkpCjjQ8QlsOeWj92mVplvMcdOIqgS/ula/rnMWiDwJ5yLZeVwY8W1CZuU6ixzkWU4ELznEiNPMcOcES82WoF4rTptPlDc2VrJCkaGf2Eag1punztNAoGBAKym+0Monkbmx4IMnzWp8NlNKMef3z5MoY3RBEh7tocTBxnUkHhe25uUOm84E14toxmze0iJypLXZtJXXwcAnN57b62KflX4P/06p424NDsD476n7EZHThQ2FXmlFhD6nymIe1pvUG6yrSVzrE2judQlQOxRZ0i/V4bUtASvrz8e";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkwoDnWS/qTQe95nYAXnq1YaX5Yb7X5Yh31Cl0IKSBX11/HlrvhmtOTyYHBshGEvZUBRZtZQtjt/fRnWELJ/gGoCa9IjjMqJ+AIhqCtuZfJMuoSpvIHJicUww8hlHURqbXGUHKDHXya3CaYMhH8yoUVXmCfcnEzLS3s98hE+F6ka9MmBEqAkLXTuPJq0Zs3KEQ52A6vVRL6vQGSJ2fj0jRjgseyTAw6JstvGTF2QKrD5wq5reHTSMXklHm2Iovxzpz2kOtxDX9dh7wlyc+gbHDhoeD/cnQXRGfu9KYfH62bnSVCk8ssPVwy0d0m4Is+oPwdWY66oEJdwnN3jknFaM4QIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url;

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url;

    //TODO 修改自动收单时间
    private  String timeout_express = "1m";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+timeout_express+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
