package ru.job4j.quartz;

import java.io.IOException;
import java.sql.*;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Properties;

public class AlertRabbit {
    public static Connection initConnection(Properties config) throws SQLException, ClassNotFoundException {
            Class.forName(config.getProperty("connection.driver_class"));
            return DriverManager.getConnection(
                    config.getProperty("connection.url"),
                    config.getProperty("connection.username"),
                    config.getProperty("connection.password")
            );
    }

    public static Properties readProperties() {
        Properties config = new Properties();
        try (InputStream in = AlertRabbit.class.getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            config.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    public static void main(String[] args) throws InterruptedException, SQLException,
            ClassNotFoundException {
        Properties config = readProperties();
        try (Connection connection = initConnection(config)) {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("connection", connection);
            JobDetail job = JobBuilder.newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            SimpleScheduleBuilder times = SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(Integer.parseInt(config.getProperty("rabbit.interval")))
                    .repeatForever();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
            System.out.println(connection);
        } catch (SchedulerException schedulerException) {
            schedulerException.printStackTrace();
        }
    }

    public static class Rabbit implements Job {
        public Rabbit() {
            System.out.println(hashCode());
        }
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            System.out.println("Rabbit runs here ...");
            Connection connection = (Connection) jobExecutionContext.getJobDetail()
                    .getJobDataMap().get("connection");
            try (PreparedStatement statement = connection.prepareStatement(
                    "insert into rabbit (created_date) values (?)")) {
                statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
