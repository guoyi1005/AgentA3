-- Campus activity demo seed exported from the local demo database.
-- Safe for an empty deployment; rerunning skips existing rows.

-- MySQL dump 10.13  Distrib 8.0.40, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: smart-campus
-- ------------------------------------------------------
-- Server version	8.4.10

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `activity_category`
--

LOCK TABLES `activity_category` WRITE;
/*!40000 ALTER TABLE `activity_category` DISABLE KEYS */;
INSERT IGNORE INTO `activity_category` (`id`, `category_name`, `create_time`, `sort`, `status`) VALUES (9,'学术讲座','2026-08-11 10:29:39',0,1);
INSERT IGNORE INTO `activity_category` (`id`, `category_name`, `create_time`, `sort`, `status`) VALUES (10,'学科竞赛','2026-08-11 10:30:00',0,1);
INSERT IGNORE INTO `activity_category` (`id`, `category_name`, `create_time`, `sort`, `status`) VALUES (11,'求职宣讲','2026-08-11 10:30:08',0,1);
INSERT IGNORE INTO `activity_category` (`id`, `category_name`, `create_time`, `sort`, `status`) VALUES (12,'文体活动','2026-08-11 10:30:12',0,1);
INSERT IGNORE INTO `activity_category` (`id`, `category_name`, `create_time`, `sort`, `status`) VALUES (13,'志愿实践','2026-08-11 10:30:17',0,1);
INSERT IGNORE INTO `activity_category` (`id`, `category_name`, `create_time`, `sort`, `status`) VALUES (14,'技能培训','2026-08-11 10:30:29',0,1);
/*!40000 ALTER TABLE `activity_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `activity`
--

LOCK TABLES `activity` WRITE;
/*!40000 ALTER TABLE `activity` DISABLE KEYS */;
INSERT IGNORE INTO `activity` (`id`, `cancel_requires_audit`, `category_id`, `contact_name`, `contact_phone`, `content`, `cover_image`, `create_time`, `credit_config`, `current_people`, `end_time`, `images`, `location`, `max_people`, `organizer_id`, `organizer_name`, `requires_audit`, `score`, `sign_in_end_time`, `sign_in_open`, `sign_in_start_time`, `sign_in_type`, `signup_end_time`, `signup_start_time`, `start_time`, `status`, `title`) VALUES (16,0,14,NULL,NULL,'本次活动面向需要阅读专业文献、整理课程资料和准备论文写作的学生，介绍 AI 在文献检索、内容摘要、知识分类、思维导图整理和学习资料生成中的使用方法。活动包含工具演示和现场练习，建议参与者准备一台电脑或移动设备，并携带一个希望整理的学习主题。','/uploads/smart-campus/media/2026-09-24/0694f96e-f96b-432c-8b99-0a278283b141.png','2026-09-24 21:20:53',NULL,120,'2026-10-07 20:30:00',NULL,'腾讯会议',120,NULL,'图书馆',0,0.0,NULL,0,NULL,1,'2026-10-06 18:00:00',NULL,'2026-10-07 19:00:00','PUBLISHED','AI 辅助文献阅读与知识整理');
INSERT IGNORE INTO `activity` (`id`, `cancel_requires_audit`, `category_id`, `contact_name`, `contact_phone`, `content`, `cover_image`, `create_time`, `credit_config`, `current_people`, `end_time`, `images`, `location`, `max_people`, `organizer_id`, `organizer_name`, `requires_audit`, `score`, `sign_in_end_time`, `sign_in_open`, `sign_in_start_time`, `sign_in_type`, `signup_end_time`, `signup_start_time`, `start_time`, `status`, `title`) VALUES (17,0,9,NULL,NULL,'本次讲座围绕计算机专业学习方向、技术发展趋势和行业能力要求展开，邀请专业教师进行主题分享，并通过案例帮助学生理解课程知识与实际应用的衔接。活动适合计算机相关专业学生以及对计算机技术感兴趣的全校学生。','/uploads/smart-campus/media/2026-09-24/4fae865c-7f98-4c64-9d17-59df2c821b59.png','2026-09-24 21:23:06',NULL,200,'2026-10-12 16:00:00',NULL,'报告厅',200,NULL,'计算机学院',0,0.0,NULL,0,NULL,1,'2026-10-16 18:00:00',NULL,'2026-10-12 14:00:00','PUBLISHED','珠峰大讲堂—计算机专业专题');
INSERT IGNORE INTO `activity` (`id`, `cancel_requires_audit`, `category_id`, `contact_name`, `contact_phone`, `content`, `cover_image`, `create_time`, `credit_config`, `current_people`, `end_time`, `images`, `location`, `max_people`, `organizer_id`, `organizer_name`, `requires_audit`, `score`, `sign_in_end_time`, `sign_in_open`, `sign_in_start_time`, `sign_in_type`, `signup_end_time`, `signup_start_time`, `start_time`, `status`, `title`) VALUES (18,0,10,NULL,NULL,'物理实验竞赛以实验设计、规范操作、数据记录和结果分析为主要内容，重点考查参与者的实践能力、分析能力和团队协作能力。参赛者需提前到达实验室完成签到，按照现场要求使用实验设备，并独立完成规定任务。','/uploads/smart-campus/media/2026-09-24/20517f95-70f6-4160-9e9d-544d22ebe7a8.png',NULL,NULL,67,'2026-09-26 18:00:00',NULL,'实验楼',90,NULL,'物理学院',0,0.0,NULL,0,NULL,1,'2026-09-16 19:00:00',NULL,'2026-09-24 09:00:00','COMPLETED','物理实验竞赛');
INSERT IGNORE INTO `activity` (`id`, `cancel_requires_audit`, `category_id`, `contact_name`, `contact_phone`, `content`, `cover_image`, `create_time`, `credit_config`, `current_people`, `end_time`, `images`, `location`, `max_people`, `organizer_id`, `organizer_name`, `requires_audit`, `score`, `sign_in_end_time`, `sign_in_open`, `sign_in_start_time`, `sign_in_type`, `signup_end_time`, `signup_start_time`, `start_time`, `status`, `title`) VALUES (19,0,13,NULL,NULL,'本次活动组织学生前往养老院开展陪伴交流、环境整理和文化互动等志愿服务。参与者需遵守服务规范，尊重老人隐私，服从现场负责人安排。完成规定服务时长的同学可获得活动参与证明，相关记录可用于个人实践材料整理。','/uploads/smart-campus/media/2026-09-24/94430fbe-2b76-439c-b9a2-898d2f0305ff.png',NULL,NULL,10,'2026-09-23 12:00:00',NULL,'养老院',10,NULL,'青年志愿者协会',0,0.0,NULL,0,NULL,1,'2026-09-19 18:00:00',NULL,'2026-09-23 09:00:00','COMPLETED','养老院志愿服务');
INSERT IGNORE INTO `activity` (`id`, `cancel_requires_audit`, `category_id`, `contact_name`, `contact_phone`, `content`, `cover_image`, `create_time`, `credit_config`, `current_people`, `end_time`, `images`, `location`, `max_people`, `organizer_id`, `organizer_name`, `requires_audit`, `score`, `sign_in_end_time`, `sign_in_open`, `sign_in_start_time`, `sign_in_type`, `signup_end_time`, `signup_start_time`, `start_time`, `status`, `title`) VALUES (20,0,11,NULL,NULL,'本次工作坊面向正在准备实习和就业的学生，提供简历结构检查、内容诊断、岗位匹配分析和模拟面试训练。参与者可携带个人简历参加，通过现场反馈发现表达、经历呈现和岗位匹配方面的问题，并获得后续修改建议。','/uploads/smart-campus/media/2026-09-24/3d8ff2f6-9b0e-4c11-bf46-5347f77eaa5c.png','2026-09-24 21:31:08',NULL,49,'2026-10-23 17:30:00',NULL,'招生就业处',60,NULL,'就业指导中心',0,0.0,NULL,0,NULL,1,'2026-10-22 18:30:00',NULL,'2026-10-23 14:30:00','PUBLISHED','AI 简历诊断与模拟面试工作坊');
INSERT IGNORE INTO `activity` (`id`, `cancel_requires_audit`, `category_id`, `contact_name`, `contact_phone`, `content`, `cover_image`, `create_time`, `credit_config`, `current_people`, `end_time`, `images`, `location`, `max_people`, `organizer_id`, `organizer_name`, `requires_audit`, `score`, `sign_in_end_time`, `sign_in_open`, `sign_in_start_time`, `sign_in_type`, `signup_end_time`, `signup_start_time`, `start_time`, `status`, `title`) VALUES (21,0,11,NULL,NULL,'本次活动邀请行业从业者或就业经验丰富的学生，围绕岗位选择、求职准备、实习经历、能力要求和职业发展路径进行分享。活动设置互动答疑环节，参与者可以提前准备求职方向、简历准备和行业发展等问题。','/uploads/smart-campus/media/2026-09-24/0b6374de-2523-4405-8e38-f6a9ef1b183f.png','2026-09-24 21:33:01',NULL,300,'2026-10-28 20:30:00',NULL,'腾讯会议',300,NULL,NULL,0,0.0,NULL,0,NULL,1,'2026-10-27 18:30:00',NULL,'2026-10-28 19:00:00','PUBLISHED','行业就业经验分享');
INSERT IGNORE INTO `activity` (`id`, `cancel_requires_audit`, `category_id`, `contact_name`, `contact_phone`, `content`, `cover_image`, `create_time`, `credit_config`, `current_people`, `end_time`, `images`, `location`, `max_people`, `organizer_id`, `organizer_name`, `requires_audit`, `score`, `sign_in_end_time`, `sign_in_open`, `sign_in_start_time`, `sign_in_type`, `signup_end_time`, `signup_start_time`, `start_time`, `status`, `title`) VALUES (22,0,12,NULL,NULL,'心理摄影比赛以校园生活、情绪表达、人际关系、成长体验和心理健康为主题，鼓励学生通过摄影作品表达观察与感受。参与者需在规定时间内提交原创作品，并附上简短的作品说明。作品内容应积极健康，不得侵犯他人肖像权和版权。','/uploads/smart-campus/media/2026-09-24/b34f6c27-742f-4f1d-b87c-9265d1e0992e.png','2026-09-24 21:35:22',NULL,165,'2026-10-31 22:30:00',NULL,'线上',200,NULL,'心理中心',0,0.0,NULL,0,NULL,1,'2026-09-29 12:30:00',NULL,'2026-09-30 09:00:00','PUBLISHED','心理摄影比赛');
INSERT IGNORE INTO `activity` (`id`, `cancel_requires_audit`, `category_id`, `contact_name`, `contact_phone`, `content`, `cover_image`, `create_time`, `credit_config`, `current_people`, `end_time`, `images`, `location`, `max_people`, `organizer_id`, `organizer_name`, `requires_audit`, `score`, `sign_in_end_time`, `sign_in_open`, `sign_in_start_time`, `sign_in_type`, `signup_end_time`, `signup_start_time`, `start_time`, `status`, `title`) VALUES (23,0,12,NULL,NULL,'本次活动通过传统文化展示、互动体验和交流分享，让学生了解中华优秀传统文化的内容与魅力。活动包含现场参观、互动体验和主题交流等环节，参与者应遵守现场秩序，爱护活动物资，并按照工作人员指引完成体验项目。','/uploads/smart-campus/media/2026-09-24/4d0a5e5e-1612-47d1-b0df-8217d390efa2.png',NULL,NULL,407,'2026-10-04 18:00:00',NULL,'体育馆',500,NULL,'社团中心',0,0.0,NULL,0,NULL,1,'2026-09-30 00:00:00',NULL,'2026-10-03 08:00:00','PUBLISHED','墨韵东方·中华传统文化体验');
/*!40000 ALTER TABLE `activity` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-27 15:11:42
