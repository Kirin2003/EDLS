package ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JPython {
    public static void Graphic(double time1, double time2, double time3, double time4, String str) throws IOException, InterruptedException {
        //通过原生方式调用，解决python文件引入第三方库的问题
        //第一个参数默认是python3,第二个参数python脚本路径，第三和第四个参数是python要接收的参数
        String[] argg = new String[] { "python3", "src/main/java/ui/test2.py", String.valueOf(time1), String.valueOf(time2), String.valueOf(time3), String.valueOf(time4),str};

        Process pr = Runtime.getRuntime().exec(argg);
        BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));

        //错误流
        BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

        System.out.println("start...");

        String line = null;
        while ((line = in.readLine()) != null) {
            System.out.println("=====python返回结果：" + line);
            if (line.contains("[{")) {
                System.out.println("======最想要的结果：" + line);

            }
        }

        String err = null;
        while ((err = error.readLine()) != null) {
            System.out.println("=====error：" + err);
        }

        System.out.println("end...");
        in.close();
        pr.waitFor();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Graphic(1,1,1,1,"hello");
    }
}
