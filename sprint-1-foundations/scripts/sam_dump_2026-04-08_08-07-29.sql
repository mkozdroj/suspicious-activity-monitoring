-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: localhost    Database: sam
-- ------------------------------------------------------
-- Server version	8.0.41

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
-- Table structure for table `account`
--

DROP TABLE IF EXISTS `account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account` (
  `account_id` int NOT NULL AUTO_INCREMENT,
  `account_number` varchar(40) NOT NULL,
  `customer_id` int NOT NULL,
  `account_type` varchar(20) NOT NULL,
  `currency` char(3) NOT NULL,
  `balance` decimal(18,2) NOT NULL DEFAULT '0.00',
  `opened_date` date NOT NULL,
  `status` varchar(10) NOT NULL DEFAULT 'ACTIVE',
  `branch_code` varchar(10) NOT NULL,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `account_number` (`account_number`),
  KEY `idx_account_customer` (`customer_id`),
  CONSTRAINT `account_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`customer_id`),
  CONSTRAINT `account_chk_1` CHECK ((`account_type` in (_utf8mb4'CURRENT',_utf8mb4'SAVINGS',_utf8mb4'TRADING',_utf8mb4'CUSTODY',_utf8mb4'CORRESPONDENT'))),
  CONSTRAINT `account_chk_2` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'FROZEN',_utf8mb4'CLOSED',_utf8mb4'RESTRICTED')))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account`
--

LOCK TABLES `account` WRITE;
/*!40000 ALTER TABLE `account` DISABLE KEYS */;
INSERT INTO `account` VALUES (1,'GB29HSBC10204010000001',1,'CURRENT','GBP',52400.00,'2018-03-01','ACTIVE','LON001'),(2,'GB29HSBC10204010000002',2,'SAVINGS','GBP',18750.00,'2019-07-15','ACTIVE','LON001'),(3,'GB29HSBC10204010000003',3,'CURRENT','GBP',284000.00,'2020-01-10','ACTIVE','LON002'),(4,'AE070331234567890123456',4,'CURRENT','USD',1250000.00,'2021-02-28','ACTIVE','DXB001'),(5,'FR7630004000031234567890',5,'SAVINGS','EUR',9800.00,'2020-09-05','ACTIVE','PAR001'),(6,'HK12345678901234567890',6,'CURRENT','HKD',4200000.00,'2023-11-20','RESTRICTED','HKG001'),(7,'MX123456789012345678',7,'CURRENT','MXN',3800000.00,'2017-05-14','ACTIVE','MEX001'),(8,'SG12345678901234567',8,'TRADING','SGD',175000.00,'2022-04-03','ACTIVE','SIN001'),(9,'GB29HSBC10204010000009',9,'CURRENT','GBP',31200.00,'2016-08-22','ACTIVE','LON003'),(10,'KY12345678901234567',10,'SAVINGS','USD',8750000.00,'2019-03-07','FROZEN','CYM001'),(11,'GB29HSBC10204010000011',11,'CURRENT','GBP',4200.00,'2021-10-18','ACTIVE','LON001'),(12,'IT60X0542811101000000123456',12,'SAVINGS','EUR',22300.00,'2015-12-01','ACTIVE','MIL001'),(13,'US12345678901234567890',13,'CURRENT','USD',920000.00,'2020-06-30','ACTIVE','NYC001'),(14,'DE89370400440532013000',14,'CURRENT','EUR',67400.00,'2022-08-11','ACTIVE','FRA001'),(15,'JP12345678901234567890',15,'SAVINGS','JPY',1820000.00,'2023-01-25','ACTIVE','TKY001'),(16,'CH5604835012345678009',16,'CURRENT','CHF',385000.00,'2018-07-19','ACTIVE','ZRH001'),(17,'BE68539007547034',17,'CURRENT','EUR',115000.00,'2020-03-12','ACTIVE','BRU001'),(18,'IE29AIBK93115212345678',18,'CURRENT','EUR',7800.00,'2021-05-07','ACTIVE','DUB001'),(19,'GB29HSBC10204010000019',19,'CURRENT','GBP',41600.00,'2019-11-30','ACTIVE','LON002'),(20,'PA12345678901234567890',20,'CURRENT','USD',0.00,'2024-01-05','FROZEN','PAN001');
/*!40000 ALTER TABLE `account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alert`
--

DROP TABLE IF EXISTS `alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert` (
  `alert_id` int NOT NULL AUTO_INCREMENT,
  `alert_ref` varchar(15) NOT NULL,
  `rule_id` int NOT NULL,
  `account_id` int NOT NULL,
  `transaction_id` int DEFAULT NULL,
  `triggered_at` timestamp NOT NULL,
  `alert_score` smallint NOT NULL,
  `status` varchar(15) NOT NULL DEFAULT 'OPEN',
  `assigned_to` varchar(60) DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`alert_id`),
  UNIQUE KEY `alert_ref` (`alert_ref`),
  KEY `rule_id` (`rule_id`),
  KEY `transaction_id` (`transaction_id`),
  KEY `idx_alert_account` (`account_id`),
  KEY `idx_alert_status` (`status`),
  CONSTRAINT `alert_ibfk_1` FOREIGN KEY (`rule_id`) REFERENCES `alert_rule` (`rule_id`),
  CONSTRAINT `alert_ibfk_2` FOREIGN KEY (`account_id`) REFERENCES `account` (`account_id`),
  CONSTRAINT `alert_ibfk_3` FOREIGN KEY (`transaction_id`) REFERENCES `transaction` (`transaction_id`),
  CONSTRAINT `alert_chk_1` CHECK ((`alert_score` between 0 and 100)),
  CONSTRAINT `alert_chk_2` CHECK ((`status` in (_utf8mb4'OPEN',_utf8mb4'UNDER_REVIEW',_utf8mb4'ESCALATED',_utf8mb4'CLOSED',_utf8mb4'SAR_FILED')))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alert`
--

LOCK TABLES `alert` WRITE;
/*!40000 ALTER TABLE `alert` DISABLE KEYS */;
INSERT INTO `alert` VALUES (1,'ALT-2026-0001',1,1,3,'2026-03-03 09:15:00',78,'UNDER_REVIEW','aml.analyst1@hsbc.com','Three consecutive cash deposits just below ┬ú10K over 3 days'),(2,'ALT-2026-0002',5,4,5,'2026-03-11 14:22:00',92,'ESCALATED','aml.senior@hsbc.com','Wire to Iranian bank ÔÇö sanctions review required'),(3,'ALT-2026-0003',5,14,12,'2026-03-16 11:05:00',88,'UNDER_REVIEW','aml.analyst2@hsbc.com','Wire to Iranian bank Tejarat ÔÇö OFAC check pending'),(4,'ALT-2026-0004',9,6,7,'2026-03-12 16:40:00',85,'UNDER_REVIEW','aml.analyst1@hsbc.com','Funds in from China, out to Cayman same day'),(5,'ALT-2026-0005',14,10,8,'2026-03-13 10:30:00',71,'OPEN',NULL,'Trust account transfer via Cayman Islands bank'),(6,'ALT-2026-0006',3,4,20,'2026-03-25 09:00:00',65,'OPEN',NULL,'Third large UAE wire in 15 days ÔÇö velocity check'),(7,'ALT-2026-0007',8,7,11,'2026-03-15 13:10:00',74,'UNDER_REVIEW','aml.analyst2@hsbc.com','PEP (former minister) sending >$50K offshore'),(8,'ALT-2026-0008',6,20,15,'2026-01-08 08:00:00',99,'SAR_FILED','aml.senior@hsbc.com','Sanctioned entity ÔÇö SAR filed, account frozen'),(9,'ALT-2026-0009',5,16,18,'2026-03-21 15:55:00',95,'ESCALATED','aml.senior@hsbc.com','Transfer to Syrian bank ÔÇö possible sanctions breach'),(10,'ALT-2026-0010',10,6,6,'2026-03-12 17:00:00',80,'UNDER_REVIEW','aml.analyst1@hsbc.com','Layering suspected: HK acct receives and immediately retransfers'),(11,'ALT-2026-0011',1,19,19,'2026-03-22 12:00:00',58,'OPEN',NULL,'Cash deposit ┬ú9,200 ÔÇö single event below threshold, flagged for pattern'),(12,'ALT-2026-0012',14,7,11,'2026-03-15 14:00:00',62,'CLOSED','aml.analyst1@hsbc.com','MXN transfer to US ÔÇö reviewed, confirmed legitimate property purchase'),(13,'ALT-2026-0013',11,3,NULL,'2026-03-20 09:00:00',55,'OPEN',NULL,'Corporate account 6-month dormancy broken with large credit'),(14,'ALT-2026-0014',7,20,15,'2026-01-08 08:01:00',100,'SAR_FILED','aml.senior@hsbc.com','OFAC watchlist match confirmed ÔÇö Narco Shell Corp'),(15,'ALT-2026-0015',4,10,9,'2026-03-13 11:00:00',82,'UNDER_REVIEW','aml.analyst2@hsbc.com','Frozen trust account: $750K in / $680K out same day to Russia'),(16,'ALT-2026-0016',8,17,14,'2026-03-18 16:30:00',70,'UNDER_REVIEW','aml.analyst1@hsbc.com','PEP wire to DRC ÔÇö enhanced due diligence required'),(17,'ALT-2026-0017',2,1,1,'2026-03-01 18:00:00',60,'CLOSED','aml.analyst1@hsbc.com','Reviewed ÔÇö salary deposits confirmed by employer letter'),(18,'ALT-2026-0018',20,16,18,'2026-03-21 16:00:00',68,'OPEN',NULL,'Charity sending $28K to Syria ÔÇö humanitarian claim unverified'),(19,'ALT-2026-0019',19,10,9,'2026-03-13 12:00:00',77,'UNDER_REVIEW','aml.analyst2@hsbc.com','Cayman trustÔåÆRussia wire corridor alert'),(20,'ALT-2026-0020',5,9,9,'2026-03-13 10:45:00',91,'ESCALATED','aml.senior@hsbc.com','Wire to Russia from trust account under freeze review');
/*!40000 ALTER TABLE `alert` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alert_rule`
--

DROP TABLE IF EXISTS `alert_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_rule` (
  `rule_id` int NOT NULL AUTO_INCREMENT,
  `rule_code` varchar(20) NOT NULL,
  `rule_name` varchar(100) NOT NULL,
  `rule_category` varchar(30) NOT NULL,
  `description` varchar(255) NOT NULL,
  `threshold_amount` decimal(18,2) DEFAULT NULL,
  `threshold_count` int DEFAULT NULL,
  `lookback_days` int DEFAULT '30',
  `severity` varchar(10) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `rule_code` (`rule_code`),
  CONSTRAINT `alert_rule_chk_1` CHECK ((`rule_category` in (_cp850'STRUCTURING',_cp850'SMURFING',_cp850'VELOCITY',_cp850'WATCHLIST',_cp850'GEOGRAPHY',_cp850'PATTERN'))),
  CONSTRAINT `alert_rule_chk_2` CHECK ((`severity` in (_cp850'LOW',_cp850'MEDIUM',_cp850'HIGH',_cp850'CRITICAL')))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alert_rule`
--

LOCK TABLES `alert_rule` WRITE;
/*!40000 ALTER TABLE `alert_rule` DISABLE KEYS */;
INSERT INTO `alert_rule` VALUES (1,'STR-001','Cash Structuring Below Reporting Threshold','STRUCTURING','Multiple cash transactions just below ┬ú10,000 CTR threshold within 5 days',10000.00,3,5,'HIGH',1),(2,'STR-002','Smurfing Pattern Detection','STRUCTURING','Multiple small cash deposits across linked accounts in 24 hours',5000.00,5,1,'HIGH',1),(3,'VEL-001','High-Value Wire Velocity','VELOCITY','More than 3 international wires exceeding $100K in 7 days',100000.00,3,7,'CRITICAL',1),(4,'VEL-002','Rapid Account Turnover','VELOCITY','Account balance drained by 90%+ within 48 hours of large credit',NULL,NULL,2,'HIGH',1),(5,'GEO-001','High-Risk Country Wire','GEOGRAPHY','Wire transfer to/from FATF high-risk jurisdiction (IR, KP, SY, MM, etc.)',NULL,NULL,90,'CRITICAL',1),(6,'GEO-002','Sanctions Country Transaction','GEOGRAPHY','Any transaction involving OFAC/HMT sanctioned country',NULL,NULL,90,'CRITICAL',1),(7,'WL-001','Watchlist Name Match','WATCHLIST','Customer or counterparty name matches OFAC / UN / HMT watchlist',NULL,NULL,NULL,'CRITICAL',1),(8,'WL-002','PEP High-Value Transfer','WATCHLIST','Politically Exposed Person transfers >$50K in single transaction',50000.00,NULL,NULL,'HIGH',1),(9,'PAT-001','Round Tripping','PATTERN','Funds leave account and return within 3 days via different route',NULL,NULL,3,'HIGH',1),(10,'PAT-002','Layering via Multiple Accounts','PATTERN','Funds pass through 3+ linked accounts within 24 hours',NULL,NULL,1,'HIGH',1),(11,'PAT-003','Dormant Account Suddenly Active','PATTERN','Account inactive 180+ days receives large credit then outward transfer',25000.00,NULL,180,'MEDIUM',1),(12,'STR-003','Crypto Off-Ramp Pattern','STRUCTURING','Multiple crypto-to-fiat conversions below reporting threshold',9500.00,3,7,'HIGH',1),(13,'VEL-003','Unusual Foreign Exchange Activity','VELOCITY','Customer converts >5 different currencies in rolling 30-day period',NULL,5,30,'MEDIUM',1),(14,'GEO-003','Offshore Shell Jurisdiction','GEOGRAPHY','Counterparty bank in known shell company jurisdiction (KY, BVI, PA)',NULL,NULL,90,'MEDIUM',1),(15,'WL-003','Adverse Media Match','WATCHLIST','Customer name matches adverse media screening result',NULL,NULL,NULL,'MEDIUM',1),(16,'PAT-004','Trade-Based Money Laundering','PATTERN','Significant mismatch between declared trade volumes and transaction values',NULL,NULL,90,'HIGH',1),(17,'STR-004','Cheque Kiting','STRUCTURING','Cheque deposits followed by rapid withdrawal before clearance',NULL,NULL,5,'MEDIUM',1),(18,'VEL-004','Account Balance Concentration','VELOCITY','Account holds balance more than 10x prior 12-month average',NULL,NULL,365,'MEDIUM',1),(19,'GEO-004','High-Risk Corridor Wire','GEOGRAPHY','Wire in predefined high-risk money-laundering corridor (e.g. MXÔåÆUSÔåÆKY)',NULL,NULL,30,'HIGH',1),(20,'PAT-005','Charity / NGO Fund Diversion','PATTERN','Charitable org receiving large donations quickly transferred offshore',100000.00,NULL,30,'HIGH',1);
/*!40000 ALTER TABLE `alert_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer`
--

DROP TABLE IF EXISTS `customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer` (
  `customer_id` int NOT NULL AUTO_INCREMENT,
  `customer_ref` varchar(15) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `date_of_birth` date DEFAULT NULL,
  `nationality` char(2) NOT NULL,
  `country_of_residence` char(2) NOT NULL,
  `customer_type` varchar(20) NOT NULL,
  `risk_rating` varchar(10) NOT NULL,
  `kyc_status` varchar(15) NOT NULL,
  `onboarded_date` date NOT NULL,
  `is_pep` tinyint(1) NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`customer_id`),
  UNIQUE KEY `customer_ref` (`customer_ref`),
  CONSTRAINT `customer_chk_1` CHECK ((`customer_type` in (_cp850'INDIVIDUAL',_cp850'CORPORATE',_cp850'TRUST',_cp850'CHARITY'))),
  CONSTRAINT `customer_chk_2` CHECK ((`risk_rating` in (_cp850'LOW',_cp850'MEDIUM',_cp850'HIGH'))),
  CONSTRAINT `customer_chk_3` CHECK ((`kyc_status` in (_cp850'VERIFIED',_cp850'PENDING',_cp850'EXPIRED',_cp850'BLOCKED')))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer`
--

LOCK TABLES `customer` WRITE;
/*!40000 ALTER TABLE `customer` DISABLE KEYS */;
INSERT INTO `customer` VALUES (1,'CUST-00001','James Thornton','1978-04-12','GB','GB','INDIVIDUAL','LOW','VERIFIED','2018-03-01',0,1),(2,'CUST-00002','Sophia Andersson','1985-09-22','SE','GB','INDIVIDUAL','LOW','VERIFIED','2019-07-15',0,1),(3,'CUST-00003','Meridian Trading Ltd',NULL,'GB','GB','CORPORATE','MEDIUM','VERIFIED','2020-01-10',0,1),(4,'CUST-00004','Viktor Sokolov','1963-11-03','RU','AE','INDIVIDUAL','HIGH','VERIFIED','2021-02-28',0,1),(5,'CUST-00005','Amara Diallo','1990-06-17','SN','FR','INDIVIDUAL','MEDIUM','VERIFIED','2020-09-05',0,1),(6,'CUST-00006','Pacific Bridge Holdings',NULL,'HK','HK','CORPORATE','HIGH','PENDING','2023-11-20',0,1),(7,'CUST-00007','Carlos Mendez Ruiz','1955-03-28','MX','MX','INDIVIDUAL','LOW','VERIFIED','2017-05-14',1,1),(8,'CUST-00008','Liu Wei','1982-08-09','CN','SG','INDIVIDUAL','MEDIUM','VERIFIED','2022-04-03',0,1),(9,'CUST-00009','Fatima Al-Rashidi','1974-12-30','KW','GB','INDIVIDUAL','LOW','VERIFIED','2016-08-22',0,1),(10,'CUST-00010','Greenleaf Trust Company',NULL,'KY','KY','TRUST','HIGH','EXPIRED','2019-03-07',0,1),(11,'CUST-00011','Benjamin Osei','1988-07-14','GH','GB','INDIVIDUAL','LOW','VERIFIED','2021-10-18',0,1),(12,'CUST-00012','Elena Marchetti','1971-05-25','IT','IT','INDIVIDUAL','LOW','VERIFIED','2015-12-01',0,1),(13,'CUST-00013','Dusk Capital Partners LLC',NULL,'US','US','CORPORATE','MEDIUM','VERIFIED','2020-06-30',0,1),(14,'CUST-00014','Hamid Rahimi','1960-02-14','IR','DE','INDIVIDUAL','HIGH','VERIFIED','2022-08-11',0,1),(15,'CUST-00015','Yuki Tanaka','1995-10-08','JP','JP','INDIVIDUAL','LOW','VERIFIED','2023-01-25',0,1),(16,'CUST-00016','Atlas Charitable Foundation',NULL,'CH','CH','CHARITY','MEDIUM','VERIFIED','2018-07-19',0,1),(17,'CUST-00017','Andre Mobutu','1968-09-01','CD','BE','INDIVIDUAL','HIGH','VERIFIED','2020-03-12',1,1),(18,'CUST-00018','Niamh O\'Brien','1992-04-16','IE','IE','INDIVIDUAL','LOW','VERIFIED','2021-05-07',0,1),(19,'CUST-00019','Rashid Al-Farouq','1979-01-22','SA','GB','INDIVIDUAL','MEDIUM','VERIFIED','2019-11-30',0,1),(20,'CUST-00020','Narco Shell Corp',NULL,'PA','PA','CORPORATE','HIGH','BLOCKED','2024-01-05',0,0);
/*!40000 ALTER TABLE `customer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `high_risk_accounts_vw`
--

DROP TABLE IF EXISTS `high_risk_accounts_vw`;
/*!50001 DROP VIEW IF EXISTS `high_risk_accounts_vw`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `high_risk_accounts_vw` AS SELECT 
 1 AS `customer_id`,
 1 AS `customer_ref`,
 1 AS `full_name`,
 1 AS `risk_rating`,
 1 AS `account_id`,
 1 AS `alert_id`,
 1 AS `alert_status`,
 1 AS `alert_date`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `investigation`
--

DROP TABLE IF EXISTS `investigation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `investigation` (
  `investigation_id` int NOT NULL AUTO_INCREMENT,
  `investigation_ref` varchar(15) NOT NULL,
  `alert_id` int NOT NULL,
  `customer_id` int NOT NULL,
  `opened_by` varchar(60) NOT NULL,
  `opened_at` timestamp NOT NULL,
  `closed_at` timestamp NULL DEFAULT NULL,
  `outcome` varchar(20) DEFAULT NULL,
  `priority` varchar(10) NOT NULL DEFAULT 'MEDIUM',
  `findings` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`investigation_id`),
  UNIQUE KEY `investigation_ref` (`investigation_ref`),
  UNIQUE KEY `alert_id` (`alert_id`),
  KEY `customer_id` (`customer_id`),
  CONSTRAINT `investigation_ibfk_1` FOREIGN KEY (`alert_id`) REFERENCES `alert` (`alert_id`),
  CONSTRAINT `investigation_ibfk_2` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`customer_id`),
  CONSTRAINT `investigation_chk_1` CHECK ((`outcome` in (_cp850'SAR_FILED',_cp850'NO_ACTION',_cp850'ACCOUNT_CLOSED',_cp850'ESCALATED',_cp850'MONITORING'))),
  CONSTRAINT `investigation_chk_2` CHECK ((`priority` in (_cp850'LOW',_cp850'MEDIUM',_cp850'HIGH',_cp850'URGENT')))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investigation`
--

LOCK TABLES `investigation` WRITE;
/*!40000 ALTER TABLE `investigation` DISABLE KEYS */;
INSERT INTO `investigation` VALUES (1,'INV-2026-001',8,20,'aml.senior@hsbc.com','2026-01-08 09:00:00','2026-01-15 17:00:00','SAR_FILED','URGENT','Narco Shell Corp matched OFAC SDN list. SAR filed with NCA. Account frozen per policy.'),(2,'INV-2026-002',14,20,'aml.senior@hsbc.com','2026-01-08 09:01:00','2026-01-15 17:01:00','ACCOUNT_CLOSED','URGENT','Watchlist match confirmed. Account closed and reported to regulators.'),(3,'INV-2026-003',2,4,'aml.senior@hsbc.com','2026-03-11 15:00:00',NULL,NULL,'URGENT','Customer wired $125K to Bank Mellat Iran ÔÇö sanctioned entity. OFAC check initiated. Customer account restricted pending review.'),(4,'INV-2026-004',4,6,'aml.analyst1@hsbc.com','2026-03-12 17:30:00',NULL,NULL,'HIGH','HKD 2.1M received from China, HKD 1.95M immediately sent to Cayman Islands. Round-trip layering pattern under investigation.'),(5,'INV-2026-005',15,10,'aml.analyst2@hsbc.com','2026-03-13 12:00:00',NULL,NULL,'HIGH','Cayman-registered trust received $750K then transferred $680K to Russian bank same day. Trust KYC expired.'),(6,'INV-2026-006',9,16,'aml.senior@hsbc.com','2026-03-21 16:30:00',NULL,NULL,'URGENT','Wire transfer to Commercial Bank of Syria by Swiss charity. Syria sanctioned. Humanitarian exemption process initiated.'),(7,'INV-2026-007',7,7,'aml.analyst2@hsbc.com','2026-03-15 14:00:00',NULL,NULL,'HIGH','Former Mexican Transport Minister (PEP) transferred $59K USD equivalent to US. EDD requested. SOF documentation outstanding.'),(8,'INV-2026-008',3,14,'aml.analyst2@hsbc.com','2026-03-16 12:00:00',NULL,NULL,'HIGH','German-based Iranian national wired Ôé¼45K to Tejarat Bank ÔÇö Iranian state bank subject to EU/OFAC sanctions.'),(9,'INV-2026-009',20,16,'aml.analyst1@hsbc.com','2026-03-21 17:00:00',NULL,NULL,'MEDIUM','Atlas Foundation CHF 28K to Syria. Reviewing legitimacy of humanitarian exemption claim.'),(10,'INV-2026-010',1,1,'aml.analyst1@hsbc.com','2026-03-04 09:00:00','2026-03-10 11:00:00','NO_ACTION','MEDIUM','Three cash deposits of ┬ú9,500 / ┬ú9,400 / ┬ú8,900 reviewed. Customer provided salary slip and bonus documentation. Resolved ÔÇö no action.'),(11,'INV-2026-011',16,17,'aml.analyst1@hsbc.com','2026-03-18 17:00:00',NULL,NULL,'HIGH','PEP (Andre Mobutu, DRC) transferred Ôé¼88K to Rawbank DRC. SOF documents requested. Enhanced monitoring applied.'),(12,'INV-2026-012',10,6,'aml.analyst1@hsbc.com','2026-03-12 18:00:00',NULL,NULL,'HIGH','Layering investigation. Pacific Bridge Holdings received HKD 2.1M from China and transferred HKD 1.95M to Cayman same day.'),(13,'INV-2026-013',12,7,'aml.analyst1@hsbc.com','2026-03-15 15:00:00','2026-03-19 09:00:00','NO_ACTION','LOW','MXN 1.2M transfer by PEP to US. Confirmed property purchase with notarised title deeds. Closed ÔÇö no action.'),(14,'INV-2026-014',19,10,'aml.analyst2@hsbc.com','2026-03-13 13:00:00',NULL,NULL,'HIGH','Cayman Islands trust ÔåÆ Russia wire corridor alert. Reviewing beneficial ownership structure of trust.'),(15,'INV-2026-015',6,4,'aml.analyst2@hsbc.com','2026-03-25 10:00:00',NULL,NULL,'MEDIUM','Third large inbound wire from UAE for Viktor Sokolov. Reviewing source of funds ÔÇö customer claims real estate sales.'),(16,'INV-2026-016',5,10,'aml.analyst2@hsbc.com','2026-03-13 11:30:00',NULL,NULL,'MEDIUM','Greenleaf Trust Company Cayman Islands ÔÇö KYC expired. Requesting UBO documentation and re-verification.'),(17,'INV-2026-017',18,16,'aml.analyst1@hsbc.com','2026-03-21 16:15:00',NULL,NULL,'MEDIUM','Atlas Charitable Foundation flagged for rapid offshore transfer. Reviewing grant disbursement approvals.'),(18,'INV-2026-018',17,9,'aml.senior@hsbc.com','2026-03-13 11:00:00',NULL,NULL,'HIGH','Trust wire to Russia escalated. Account under enhanced monitoring pending beneficial ownership disclosure.'),(19,'INV-2026-019',11,19,'aml.analyst1@hsbc.com','2026-03-22 13:00:00',NULL,NULL,'LOW','Single cash deposit ┬ú9,200. Reviewing for structuring pattern ÔÇö no prior alerts. Customer is regular salaried employee.'),(20,'INV-2026-020',13,3,'aml.analyst1@hsbc.com','2026-03-20 10:00:00',NULL,NULL,'MEDIUM','Meridian Trading Ltd dormant 6 months then ┬ú95K payroll run. Reviewing business accounts and board resolutions.');
/*!40000 ALTER TABLE `investigation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `open_alerts_vw`
--

DROP TABLE IF EXISTS `open_alerts_vw`;
/*!50001 DROP VIEW IF EXISTS `open_alerts_vw`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `open_alerts_vw` AS SELECT 
 1 AS `alert_id`,
 1 AS `alert_ref`,
 1 AS `alert_status`,
 1 AS `transaction_id`,
 1 AS `transaction_ref`,
 1 AS `amount`,
 1 AS `currency`,
 1 AS `amount_usd`,
 1 AS `transaction_date`,
 1 AS `counterparty_country`,
 1 AS `transaction_type`,
 1 AS `direction`,
 1 AS `customer_id`,
 1 AS `customer_ref`,
 1 AS `full_name`,
 1 AS `date_of_birth`,
 1 AS `nationality`,
 1 AS `country_of_residence`,
 1 AS `customer_type`,
 1 AS `risk_rating`,
 1 AS `kyc_status`,
 1 AS `is_pep`,
 1 AS `is_active`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `transaction`
--

DROP TABLE IF EXISTS `transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transaction` (
  `transaction_id` int NOT NULL AUTO_INCREMENT,
  `transaction_ref` varchar(20) NOT NULL,
  `account_id` int NOT NULL,
  `counterparty_account` varchar(30) DEFAULT NULL,
  `counterparty_bank` varchar(60) DEFAULT NULL,
  `counterparty_country` char(2) DEFAULT NULL,
  `transaction_type` varchar(20) NOT NULL,
  `direction` char(2) NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` char(3) NOT NULL,
  `amount_usd` decimal(18,2) NOT NULL,
  `transaction_date` date NOT NULL,
  `value_date` date NOT NULL,
  `status` varchar(12) NOT NULL DEFAULT 'COMPLETED',
  `description` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`transaction_id`),
  UNIQUE KEY `transaction_ref` (`transaction_ref`),
  KEY `idx_txn_account` (`account_id`),
  KEY `idx_txn_date` (`transaction_date`),
  KEY `idx_txn_amount` (`amount_usd`),
  KEY `idx_txn_cc` (`counterparty_country`),
  CONSTRAINT `transaction_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `account` (`account_id`),
  CONSTRAINT `chk_value_date` CHECK ((`value_date` >= `transaction_date`)),
  CONSTRAINT `transaction_chk_1` CHECK ((`transaction_type` in (_utf8mb4'WIRE',_utf8mb4'CASH',_utf8mb4'CARD',_utf8mb4'INTERNAL',_utf8mb4'CRYPTO',_utf8mb4'CHEQUE'))),
  CONSTRAINT `transaction_chk_2` CHECK ((`direction` in (_utf8mb4'CR',_utf8mb4'DR'))),
  CONSTRAINT `transaction_chk_3` CHECK ((`amount` > 0)),
  CONSTRAINT `transaction_chk_4` CHECK ((`amount_usd` > 0)),
  CONSTRAINT `transaction_chk_5` CHECK ((`status` in (_utf8mb4'COMPLETED',_utf8mb4'PENDING',_utf8mb4'REVERSED',_utf8mb4'FAILED')))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transaction`
--

LOCK TABLES `transaction` WRITE;
/*!40000 ALTER TABLE `transaction` DISABLE KEYS */;
INSERT INTO `transaction` VALUES (1,'TXN-2026-000001',1,NULL,NULL,NULL,'CASH','CR',9500.00,'GBP',12017.50,'2026-03-01','2026-03-01','COMPLETED','Cash deposit'),(2,'TXN-2026-000002',1,NULL,NULL,NULL,'CASH','CR',9400.00,'GBP',11891.80,'2026-03-02','2026-03-02','COMPLETED','Cash deposit'),(3,'TXN-2026-000003',1,NULL,NULL,NULL,'CASH','CR',8900.00,'GBP',11258.30,'2026-03-03','2026-03-03','COMPLETED','Cash deposit'),(4,'TXN-2026-000004',4,'AE070331234567000999','Emirates NBD','AE','WIRE','DR',480000.00,'USD',480000.00,'2026-03-10','2026-03-12','COMPLETED','Business transfer'),(5,'TXN-2026-000005',4,'IR12345678901234','Bank Mellat Iran','IR','WIRE','DR',125000.00,'USD',125000.00,'2026-03-11','2026-03-13','PENDING','Vendor payment'),(6,'TXN-2026-000006',6,'CN12345678901','Bank of China','CN','WIRE','CR',2100000.00,'HKD',269230.77,'2026-03-12','2026-03-14','COMPLETED','Capital injection'),(7,'TXN-2026-000007',6,'KY98765432109','Cayman Islands Bank','KY','WIRE','DR',1950000.00,'HKD',250000.00,'2026-03-12','2026-03-14','COMPLETED','Investment transfer'),(8,'TXN-2026-000008',10,'CH5604835099988','UBS Geneva','CH','WIRE','CR',750000.00,'USD',750000.00,'2026-03-13','2026-03-15','COMPLETED','Trust income'),(9,'TXN-2026-000009',10,'RU12345678901234','Sberbank Russia','RU','WIRE','DR',680000.00,'USD',680000.00,'2026-03-13','2026-03-15','COMPLETED','Investment'),(10,'TXN-2026-000010',3,'2034567891234','Monzo','GB','INTERNAL','DR',95000.00,'GBP',120190.00,'2026-03-14','2026-03-14','COMPLETED','Payroll run'),(11,'TXN-2026-000011',7,'US98765432109876','Wells Fargo','US','WIRE','DR',1200000.00,'MXN',59406.00,'2026-03-15','2026-03-17','COMPLETED','Property acquisition'),(12,'TXN-2026-000012',14,'IR99887766554433','Tejarat Bank','IR','WIRE','DR',45000.00,'EUR',48825.00,'2026-03-16','2026-03-18','PENDING','Consultancy fee'),(13,'TXN-2026-000013',11,NULL,NULL,NULL,'CASH','DR',4800.00,'GBP',6073.44,'2026-03-17','2026-03-17','COMPLETED','Cash withdrawal'),(14,'TXN-2026-000014',17,'CD12345678901234','Rawbank DRC','CD','WIRE','DR',88000.00,'EUR',95546.40,'2026-03-18','2026-03-20','COMPLETED','Family remittance'),(15,'TXN-2026-000015',20,'CO12345678901','Bancolombia','CO','WIRE','CR',320000.00,'USD',320000.00,'2026-01-08','2026-01-10','COMPLETED','Export proceeds'),(16,'TXN-2026-000016',20,'MX12345678901234','BBVA Mexico','MX','WIRE','DR',315000.00,'USD',315000.00,'2026-01-09','2026-01-11','COMPLETED','Import payment'),(17,'TXN-2026-000017',8,'SG98765432109','DBS Singapore','SG','WIRE','CR',82000.00,'SGD',60740.00,'2026-03-20','2026-03-22','COMPLETED','Brokerage proceeds'),(18,'TXN-2026-000018',16,'SY12345678901','Commercial Bank Syria','SY','WIRE','DR',28000.00,'CHF',31191.20,'2026-03-21','2026-03-23','PENDING','Humanitarian aid'),(19,'TXN-2026-000019',19,NULL,NULL,NULL,'CASH','CR',9200.00,'GBP',11637.96,'2026-03-22','2026-03-22','COMPLETED','Cash deposit'),(20,'TXN-2026-000020',4,'AE070331234599999','Abu Dhabi Islamic Bk','AE','WIRE','CR',390000.00,'USD',390000.00,'2026-03-25','2026-03-27','COMPLETED','Business revenue');
/*!40000 ALTER TABLE `transaction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `watchlist`
--

DROP TABLE IF EXISTS `watchlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `watchlist` (
  `watchlist_id` int NOT NULL AUTO_INCREMENT,
  `list_type` varchar(20) NOT NULL,
  `entity_name` varchar(120) NOT NULL,
  `entity_type` varchar(20) NOT NULL,
  `country` char(2) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `reason` varchar(200) NOT NULL,
  `listed_date` date NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`watchlist_id`),
  CONSTRAINT `watchlist_chk_1` CHECK ((`list_type` in (_cp850'OFAC',_cp850'UN',_cp850'EU',_cp850'HMT',_cp850'INTERPOL',_cp850'INTERNAL',_cp850'PEP'))),
  CONSTRAINT `watchlist_chk_2` CHECK ((`entity_type` in (_cp850'INDIVIDUAL',_cp850'ENTITY',_cp850'VESSEL',_cp850'AIRCRAFT')))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `watchlist`
--

LOCK TABLES `watchlist` WRITE;
/*!40000 ALTER TABLE `watchlist` DISABLE KEYS */;
INSERT INTO `watchlist` VALUES (1,'OFAC','Narco Shell Corp','ENTITY','PA',NULL,'SDN ÔÇö narcotics trafficking financing','2021-06-15',1),(2,'OFAC','Bank Mellat','ENTITY','IR',NULL,'SDN ÔÇö Iranian state bank, sanctions evasion','2012-01-23',1),(3,'UN','Tejarat Bank','ENTITY','IR',NULL,'UN Security Council sanctions ÔÇö proliferation financing','2010-09-09',1),(4,'HMT','Commercial Bank of Syria','ENTITY','SY',NULL,'HMT financial sanctions ÔÇö Syria conflict financing','2012-03-01',1),(5,'EU','Rossiya Bank','ENTITY','RU',NULL,'EU sanctions ÔÇö linked to destabilisation of Ukraine','2022-03-01',1),(6,'OFAC','Viktor Alexandr Sokolov','INDIVIDUAL','RU','1963-11-03','SDN ÔÇö oligarch linked to sanctioned Russian entities','2022-04-06',1),(7,'INTERPOL','Juan Carlos Esparza Rios','INDIVIDUAL','CO','1971-04-14','Red notice ÔÇö money laundering and narcotics','2019-11-20',1),(8,'INTERNAL','Greenleaf Trust Company','ENTITY','KY',NULL,'Internal watch ÔÇö beneficial ownership unresolved','2024-02-01',1),(9,'OFAC','Mahan Air','ENTITY','IR',NULL,'SDN ÔÇö IRGC-linked airline','2011-12-09',1),(10,'UN','Kim Chol','INDIVIDUAL','KP','1968-09-18','UN DPRK sanctions ÔÇö WMD financing','2016-11-30',1),(11,'EU','Belarus Potash Company','ENTITY','BY',NULL,'EU sanctions ÔÇö Lukashenko regime financing','2021-08-09',1),(12,'HMT','Abdullahi Al-Shabaab Network','ENTITY','SO',NULL,'HMT ÔÇö terrorist financing (Al-Shabaab)','2010-03-12',1),(13,'OFAC','Tethys Petroleum','ENTITY','RU',NULL,'SDN ÔÇö Russian energy sector restricted entity','2022-06-28',1),(14,'INTERNAL','Pacific Bridge Holdings','ENTITY','HK',NULL,'Internal watch ÔÇö unresolved UBO, layering indicators','2024-03-01',1),(15,'PEP','Carlos Mendez Ruiz','INDIVIDUAL','MX','1955-03-28','Former Mexican Transport Minister ÔÇö Class B PEP','2017-05-14',1),(16,'PEP','Andre Mobutu','INDIVIDUAL','CD','1968-09-01','Current DRC State Mining Authority Director ÔÇö Class A PEP','2020-03-12',1),(17,'OFAC','Crypto Exchange Bitzlato','ENTITY','HK',NULL,'SDN ÔÇö primary money laundering for ransomware actors','2023-01-18',1),(18,'EU','Promsvyazbank','ENTITY','RU',NULL,'EU/UK sanctions ÔÇö Russian defence bank','2022-02-28',1),(19,'INTERPOL','Huang Wei (alias: Michael Wan)','INDIVIDUAL','CN','1975-06-06','Red notice ÔÇö trade-based money laundering','2021-09-14',1),(20,'HMT','Khurram Shah','INDIVIDUAL','PK','1980-12-11','HMT ÔÇö financing terrorism (Lashkar-e-Taiba links)','2020-07-07',1);
/*!40000 ALTER TABLE `watchlist` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `watchlist_match`
--

DROP TABLE IF EXISTS `watchlist_match`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `watchlist_match` (
  `match_id` int NOT NULL AUTO_INCREMENT,
  `transaction_id` int NOT NULL,
  `watchlist_id` int NOT NULL,
  `match_type` varchar(20) NOT NULL,
  `match_score` decimal(5,2) NOT NULL,
  `matched_field` varchar(50) NOT NULL,
  `matched_value` varchar(120) NOT NULL,
  `status` varchar(15) NOT NULL DEFAULT 'PENDING',
  `reviewed_by` varchar(60) DEFAULT NULL,
  `reviewed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`match_id`),
  KEY `transaction_id` (`transaction_id`),
  KEY `watchlist_id` (`watchlist_id`),
  CONSTRAINT `watchlist_match_ibfk_1` FOREIGN KEY (`transaction_id`) REFERENCES `transaction` (`transaction_id`),
  CONSTRAINT `watchlist_match_ibfk_2` FOREIGN KEY (`watchlist_id`) REFERENCES `watchlist` (`watchlist_id`),
  CONSTRAINT `watchlist_match_chk_1` CHECK ((`match_type` in (_cp850'NAME',_cp850'ACCOUNT',_cp850'COUNTRY',_cp850'FUZZY_NAME'))),
  CONSTRAINT `watchlist_match_chk_2` CHECK ((`match_score` between 0.00 and 100.00)),
  CONSTRAINT `watchlist_match_chk_3` CHECK ((`status` in (_cp850'PENDING',_cp850'FALSE_POSITIVE',_cp850'CONFIRMED',_cp850'ESCALATED')))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `watchlist_match`
--

LOCK TABLES `watchlist_match` WRITE;
/*!40000 ALTER TABLE `watchlist_match` DISABLE KEYS */;
INSERT INTO `watchlist_match` VALUES (1,15,1,'NAME',100.00,'customer_name','Narco Shell Corp','CONFIRMED','aml.senior@hsbc.com','2026-01-08 08:05:00'),(2,16,1,'NAME',100.00,'customer_name','Narco Shell Corp','CONFIRMED','aml.senior@hsbc.com','2026-01-08 08:05:00'),(3,5,2,'NAME',98.50,'counterparty_bank','Bank Mellat Iran','CONFIRMED','aml.senior@hsbc.com','2026-03-11 15:30:00'),(4,12,3,'NAME',97.00,'counterparty_bank','Tejarat Bank','CONFIRMED','aml.analyst2@hsbc.com','2026-03-16 12:30:00'),(5,18,4,'NAME',100.00,'counterparty_bank','Commercial Bank Syria','CONFIRMED','aml.senior@hsbc.com','2026-03-21 16:05:00'),(6,9,5,'COUNTRY',85.00,'counterparty_country','RU','CONFIRMED','aml.analyst2@hsbc.com','2026-03-13 14:00:00'),(7,4,6,'FUZZY_NAME',91.00,'customer_name','Viktor Sokolov','CONFIRMED','aml.senior@hsbc.com','2026-03-12 09:00:00'),(8,11,15,'NAME',100.00,'customer_name','Carlos Mendez Ruiz','CONFIRMED','aml.analyst2@hsbc.com','2026-03-15 13:30:00'),(9,14,16,'NAME',100.00,'customer_name','Andre Mobutu','CONFIRMED','aml.analyst1@hsbc.com','2026-03-18 17:15:00'),(10,6,14,'NAME',95.00,'customer_name','Pacific Bridge Holdings','CONFIRMED','aml.analyst1@hsbc.com','2026-03-12 17:30:00'),(11,8,8,'NAME',95.00,'customer_name','Greenleaf Trust Company','CONFIRMED','aml.analyst2@hsbc.com','2026-03-13 10:45:00'),(12,9,18,'NAME',88.00,'counterparty_bank','Sberbank Russia','CONFIRMED','aml.analyst2@hsbc.com','2026-03-13 15:00:00'),(13,20,6,'FUZZY_NAME',89.00,'customer_name','Viktor Sokolov','CONFIRMED','aml.analyst2@hsbc.com','2026-03-25 11:00:00'),(14,1,11,'FUZZY_NAME',42.00,'customer_name','James Thorton','FALSE_POSITIVE','aml.analyst1@hsbc.com','2026-03-05 10:00:00'),(15,10,7,'NAME',35.00,'customer_name','Meridian Trad Ltd','FALSE_POSITIVE','aml.analyst1@hsbc.com','2026-03-15 11:00:00'),(16,17,19,'FUZZY_NAME',72.00,'counterparty_name','Liu Wei','PENDING',NULL,NULL),(17,3,9,'FUZZY_NAME',38.00,'customer_name','James Thornton','FALSE_POSITIVE','aml.analyst1@hsbc.com','2026-03-10 09:00:00'),(18,7,8,'ACCOUNT',78.00,'counterparty_account','KY98765432109','CONFIRMED','aml.analyst1@hsbc.com','2026-03-13 09:00:00'),(19,12,20,'FUZZY_NAME',65.00,'counterparty_country','IR','PENDING',NULL,NULL),(20,18,4,'COUNTRY',90.00,'counterparty_country','SY','CONFIRMED','aml.senior@hsbc.com','2026-03-21 16:10:00');
/*!40000 ALTER TABLE `watchlist_match` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'sam'
--

--
-- Final view structure for view `high_risk_accounts_vw`
--

/*!50001 DROP VIEW IF EXISTS `high_risk_accounts_vw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `high_risk_accounts_vw` AS select distinct `c`.`customer_id` AS `customer_id`,`c`.`customer_ref` AS `customer_ref`,`c`.`full_name` AS `full_name`,`c`.`risk_rating` AS `risk_rating`,`t`.`account_id` AS `account_id`,`a`.`alert_id` AS `alert_id`,`a`.`status` AS `alert_status`,`a`.`triggered_at` AS `alert_date` from ((`customer` `c` join `transaction` `t` on((`c`.`customer_id` = `t`.`account_id`))) join `alert` `a` on((`t`.`transaction_id` = `a`.`transaction_id`))) where ((`c`.`risk_rating` = 'High') and (`a`.`triggered_at` >= (curdate() - interval 30 day))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `open_alerts_vw`
--

/*!50001 DROP VIEW IF EXISTS `open_alerts_vw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `open_alerts_vw` AS select `a`.`alert_id` AS `alert_id`,`a`.`alert_ref` AS `alert_ref`,`a`.`status` AS `alert_status`,`t`.`transaction_id` AS `transaction_id`,`t`.`transaction_ref` AS `transaction_ref`,`t`.`amount` AS `amount`,`t`.`currency` AS `currency`,`t`.`amount_usd` AS `amount_usd`,`t`.`transaction_date` AS `transaction_date`,`t`.`counterparty_country` AS `counterparty_country`,`t`.`transaction_type` AS `transaction_type`,`t`.`direction` AS `direction`,`c`.`customer_id` AS `customer_id`,`c`.`customer_ref` AS `customer_ref`,`c`.`full_name` AS `full_name`,`c`.`date_of_birth` AS `date_of_birth`,`c`.`nationality` AS `nationality`,`c`.`country_of_residence` AS `country_of_residence`,`c`.`customer_type` AS `customer_type`,`c`.`risk_rating` AS `risk_rating`,`c`.`kyc_status` AS `kyc_status`,`c`.`is_pep` AS `is_pep`,`c`.`is_active` AS `is_active` from ((`alert` `a` join `transaction` `t` on((`a`.`transaction_id` = `t`.`transaction_id`))) join `customer` `c` on((`t`.`account_id` = `c`.`customer_id`))) where (`a`.`status` in ('open','under_review')) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-08  8:07:29
