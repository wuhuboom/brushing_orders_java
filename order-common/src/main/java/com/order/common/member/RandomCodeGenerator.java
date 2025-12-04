package com.order.common.member;

import java.util.Random;

public class RandomCodeGenerator {

    public static String generateRandomCode() {
        Random random = new Random();
        // 第一个字符是字母
        char firstLetter = (char) ('A' + random.nextInt(26));
        // 第二个字符是字母
        char secondLetter = (char) ('A' + random.nextInt(26));
        // 第三个字符是数字
        char number = (char) ('0' + random.nextInt(10));
        // 第四个字符是字母
        char thirdLetter = (char) ('A' + random.nextInt(26));
        // 第五个字符是字母
        char fourthLetter = (char) ('A' + random.nextInt(26));

        // 拼接成最终的随机码
        return "" + firstLetter + secondLetter + number + thirdLetter + fourthLetter;
    }
}
