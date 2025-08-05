package com.brushing.common.utils;

public class SnowflakeIdGenerator {
    private static final long START_STAMP = 1609459200000L; // 2021-01-01
    private static final long SEQUENCE_BIT = 12;
    private static final long MACHINE_BIT = 5;
    private static final long DATACENTER_BIT = 5;

    private static final long MAX_DATACENTER_NUM = ~(-1L << DATACENTER_BIT);
    private static final long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    private static final long MACHINE_LEFT = SEQUENCE_BIT;
    private static final long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private static final long TIMESTAMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    private long datacenterId;
    private long machineId;
    private long sequence = 0L;
    private long lastStamp = -1L;

    public SnowflakeIdGenerator(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_NUM || machineId > MAX_MACHINE_NUM) {
            throw new IllegalArgumentException("datacenterId or machineId too big");
        }
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    public synchronized long nextId() {
        long currentStamp = System.currentTimeMillis();
        if (currentStamp < lastStamp) {
            throw new RuntimeException("Clock moved backwards!");
        }
        if (currentStamp == lastStamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentStamp = waitNextMillis(currentStamp);
            }
        } else {
            sequence = 0L;
        }
        lastStamp = currentStamp;

        return (currentStamp - START_STAMP) << TIMESTAMP_LEFT
                | datacenterId << DATACENTER_LEFT
                | machineId << MACHINE_LEFT
                | sequence;
    }

    private long waitNextMillis(long currentStamp) {
        while (currentStamp <= lastStamp) {
            currentStamp = System.currentTimeMillis();
        }
        return currentStamp;
    }

    public static void main(String[] args) {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        System.out.println(generator.nextId());
    }
}

