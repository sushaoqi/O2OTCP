package com.dotnar.test;

import hprose.client.HproseHttpClient;
import hprose.client.HproseTcpClient;
import org.bson.types.ObjectId;

/**
 * 测试咯
 * Created by chovans on 15/7/23.
 */
public class ServletTest {
//    public static void main(String[] args) throws Exception{
//        HproseHttpClient client = new HproseHttpClient();
//        client.useService("http://localhost:8080/checkCode");
//        String result = (String) client.invoke("getCheckCode", new Object[] { "Hprose" });
//        System.out.println(result);
//    }

    public static void main(String[] args) {
        try {
//            HproseTcpClient client = new HproseTcpClient("tcp://121.40.72.93:7074");
//            String string = (String) client.invoke("insert",new Object[]{"Gaubee_test_1","user","{'_id':'8888','name':'ddd'}"});
//            System.out.println(string);

            ObjectId id = new ObjectId();
            System.out.println(id);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
