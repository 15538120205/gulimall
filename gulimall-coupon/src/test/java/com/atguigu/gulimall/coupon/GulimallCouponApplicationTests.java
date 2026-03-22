package com.atguigu.gulimall.coupon;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

//@SpringBootTest
class GulimallCouponApplicationTests {

    @Test
    void contextLoads() {
        System.out.println(startTime() + ", " + endTime());
    }
    private String startTime(){
        LocalDate now = LocalDate.now();
        LocalTime min = LocalTime.MIN;
        LocalDateTime start = LocalDateTime.of(now, min);
        String formattedStart = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return formattedStart;
    }
    private String endTime(){
        LocalDate now = LocalDate.now();
        LocalDate plus = now.plusDays(2);
        LocalTime max = LocalTime.MAX;
        LocalDateTime end = LocalDateTime.of(plus, max);
        String formattedEnd = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return formattedEnd;
    }

}
