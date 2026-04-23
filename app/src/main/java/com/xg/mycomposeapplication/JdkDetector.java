package com.xg.mycomposeapplication;

public class JdkDetector {
    // 检查使用的jdk 是否是OpenJDK
    public static void main(String[] args) {
        String vendor = System.getProperty("java.vendor");
        String vmName = System.getProperty("java.vm.name");

        System.out.println("JDK厂商：" + vendor);
        System.out.println("虚拟机名称：" + vmName);

        if (vendor.contains("Oracle Corporation")) {
            System.out.println(">>>当前使用的是：Oracle JDK");
        } else if (vmName.contains("OpenJDK")) {
            System.out.println(">>>当前使用的是：OpenJDK");
        } else {
            System.out.println(">>>当前使用：其他JDK发行版");
        }
    }
}