package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AlertRabbit {
    public static void main(String[] args) throws Exception {
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDetail job = JobBuilder.newJob(Rabbit.class).build();
            SimpleScheduleBuilder times = SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(readInterval())
                    .repeatForever();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException schedulerException) {
            schedulerException.printStackTrace();
        }
    }

    public static int readInterval() {
        Properties cfg = new Properties();
        try (InputStream in = AlertRabbit.class.getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            cfg.load(in);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return Integer.parseInt(cfg.getProperty("rabbit.interval"));
    }

    public static class Rabbit implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            System.out.println("Rabbit runs here ...");
        }
    }
}
