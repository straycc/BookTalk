
CREATE TABLE `xxl_job_qrtz_job_groups` (
                                           `job_group` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
                                           `desc` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                           `add_time` bigint(13) DEFAULT NULL,
                                           `update_time` bigint(13) DEFAULT NULL,
                                           `author` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                           PRIMARY KEY (`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `xxl_job_qrtz_jobs` (
                                     `job_name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
                                     `job_group` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
                                     `desc` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                     `add_time` bigint(13) DEFAULT NULL,
                                     `update_time` bigint(13) DEFAULT NULL,
                                     `author` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                     `alarm_email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                     `executor_route_strategy` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                     `executor_handler` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                     `executor_param` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                     `executor_block_strategy` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                     `executor_fail_retry_count` int(11) DEFAULT NULL,
                                     `child_jobid` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                     `trigger_status` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                     PRIMARY KEY (`job_name`,`job_group`),
                                     KEY `idx_qrtz_jobs_group` (`job_group`),
                                     FOREIGN KEY (`job_group`) REFERENCES `xxl_job_qrtz_job_groups` (`job_group`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `xxl_job_qrtz_triggers` (
                                         `trigger_name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `trigger_group` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `job_name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `job_group` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `next_fire_time` bigint(13) DEFAULT NULL,
                                         `prev_fire_time` bigint(13) DEFAULT NULL,
                                         `priority` integer(11) DEFAULT NULL,
                                         `trigger_state` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `trigger_type` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `start_time` bigint(13) NOT NULL,
                                         `end_time` bigint(13) DEFAULT NULL,
                                         `calendar_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                         `misfire_instr` integer(11) DEFAULT NULL,
                                         `job_data` blob,
                                         PRIMARY KEY (`trigger_name`,`trigger_group`),
                                         FOREIGN KEY (`job_name`,`job_group`) REFERENCES `xxl_job_qrtz_jobs` (`job_name`,`job_group`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `xxl_job_qrtz_lock` (
                                     `lock_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                                     `lock_value` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                     `lock_grant` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                     `lock_thread` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                     PRIMARY KEY (`lock_name`),
                                     UNIQUE KEY `idx_lock_name` (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
