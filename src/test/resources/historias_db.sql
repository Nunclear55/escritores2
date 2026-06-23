-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Apr 02, 2026 at 09:22 PM
-- Server version: 8.4.3
-- PHP Version: 8.3.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


 /*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
 /*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
 /*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
 /*!40101 SET NAMES utf8mb4 */;

--
-- Database: `historias_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `app_user`
--

CREATE TABLE `app_user` (
  `id` int NOT NULL,
  `login_name` varchar(100) NOT NULL,
  `email_address` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `access_level` varchar(30) NOT NULL DEFAULT 'user',
  `account_state` varchar(30) NOT NULL DEFAULT 'active',
  `display_name` varchar(150) DEFAULT NULL,
  `bio_text` text,
  `avatar_url` varchar(500) DEFAULT NULL,
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `global_notice`
--

CREATE TABLE `global_notice` (
  `id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `message_text` text NOT NULL,
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `starts_at` datetime DEFAULT NULL,
  `ends_at` datetime DEFAULT NULL,
  `created_by_user_id` int NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `story`
--

CREATE TABLE `story` (
  `id` int NOT NULL,
  `owner_user_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `slug_text` varchar(255) DEFAULT NULL,
  `description` text,
  `cover_image_url` varchar(500) DEFAULT NULL,
  `visibility_state` varchar(30) NOT NULL DEFAULT 'public',
  `publication_state` varchar(30) NOT NULL DEFAULT 'draft',
  `allow_feedback` tinyint(1) NOT NULL DEFAULT '1',
  `allow_scores` tinyint(1) NOT NULL DEFAULT '1',
  `started_on` date DEFAULT NULL,
  `published_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `archived_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `arc`
--

CREATE TABLE `arc` (
  `id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `subtitle` varchar(255) DEFAULT NULL,
  `story_id` int NOT NULL,
  `position_index` int DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `volume`
--

CREATE TABLE `volume` (
  `id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `story_id` int NOT NULL,
  `arc_id` int DEFAULT NULL,
  `position_index` int DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `chapter`
--

CREATE TABLE `chapter` (
  `id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `subtitle` varchar(255) DEFAULT NULL,
  `content` longtext,
  `published_on` date DEFAULT NULL,
  `story_id` int NOT NULL,
  `volume_id` int DEFAULT NULL,
  `position_index` int DEFAULT NULL,
  `reading_minutes` int DEFAULT NULL,
  `word_count` int DEFAULT NULL,
  `publication_state` varchar(30) NOT NULL DEFAULT 'draft',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `archived_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `story_character`
--

CREATE TABLE `story_character` (
  `id` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text,
  `character_role_name` varchar(255) DEFAULT NULL,
  `profession` varchar(255) DEFAULT NULL,
  `ability` varchar(255) DEFAULT NULL,
  `age` int DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `is_alive` tinyint(1) DEFAULT NULL,
  `story_id` int NOT NULL,
  `roles_json` json DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `skill`
--

CREATE TABLE `skill` (
  `id` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text,
  `level_value` int DEFAULT NULL,
  `category_name` varchar(100) DEFAULT NULL,
  `story_id` int NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `character_skill`
--

CREATE TABLE `character_skill` (
  `id` int NOT NULL,
  `story_character_id` int NOT NULL,
  `skill_id` int NOT NULL,
  `proficiency` int DEFAULT NULL,
  `notes` text,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `story_event`
--

CREATE TABLE `story_event` (
  `id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `event_on` date DEFAULT NULL,
  `importance` int DEFAULT NULL,
  `event_kind` varchar(100) DEFAULT NULL,
  `tags_json` json DEFAULT NULL,
  `linked_characters_json` json DEFAULT NULL,
  `story_id` int NOT NULL,
  `chapter_id` int DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `idea`
--

CREATE TABLE `idea` (
  `id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text,
  `story_id` int NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `item`
--

CREATE TABLE `item` (
  `id` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text,
  `quantity` int DEFAULT NULL,
  `unit_name` varchar(50) DEFAULT NULL,
  `story_id` int NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `media`
--

CREATE TABLE `media` (
  `id` int NOT NULL,
  `filename` varchar(255) NOT NULL,
  `original_filename` varchar(255) DEFAULT NULL,
  `media_kind` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `chapter_id` int NOT NULL,
  `storage_path` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `story_comment`
--

CREATE TABLE `story_comment` (
  `id` int NOT NULL,
  `story_id` int NOT NULL,
  `chapter_id` int DEFAULT NULL,
  `author_user_id` int NOT NULL,
  `parent_comment_id` int DEFAULT NULL,
  `content` text NOT NULL,
  `visibility_state` varchar(30) NOT NULL DEFAULT 'visible',
  `edited_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `story_rating`
--

CREATE TABLE `story_rating` (
  `id` int NOT NULL,
  `story_id` int NOT NULL,
  `author_user_id` int NOT NULL,
  `score_value` int NOT NULL,
  `review_text` text,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `content_report`
--

CREATE TABLE `content_report` (
  `id` int NOT NULL,
  `reporter_user_id` int NOT NULL,
  `target_user_id` int DEFAULT NULL,
  `story_id` int DEFAULT NULL,
  `chapter_id` int DEFAULT NULL,
  `comment_id` int DEFAULT NULL,
  `reason_text` text NOT NULL,
  `status_name` varchar(30) NOT NULL DEFAULT 'pending',
  `reviewed_by_user_id` int DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `resolution_text` text,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_sanction`
--

CREATE TABLE `user_sanction` (
  `id` int NOT NULL,
  `target_user_id` int NOT NULL,
  `applied_by_user_id` int NOT NULL,
  `sanction_kind` varchar(30) NOT NULL,
  `reason_text` text NOT NULL,
  `starts_at` datetime DEFAULT NULL,
  `ends_at` datetime DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_follow`
--

CREATE TABLE `user_follow` (
  `id` int NOT NULL,
  `follower_user_id` int NOT NULL,
  `followed_user_id` int NOT NULL,
  `created_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `story_favorite`
--

CREATE TABLE `story_favorite` (
  `id` int NOT NULL,
  `user_id` int NOT NULL,
  `story_id` int NOT NULL,
  `created_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `story_view_log`
--

CREATE TABLE `story_view_log` (
  `id` int NOT NULL,
  `story_id` int NOT NULL,
  `chapter_id` int DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `visitor_token` varchar(255) DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent_text` varchar(500) DEFAULT NULL,
  `viewed_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Indexes for dumped tables
--

--
-- Indexes for table `app_user`
--
ALTER TABLE `app_user`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `login_name` (`login_name`),
  ADD UNIQUE KEY `email_address` (`email_address`);

--
-- Indexes for table `global_notice`
--
ALTER TABLE `global_notice`
  ADD PRIMARY KEY (`id`),
  ADD KEY `created_by_user_id` (`created_by_user_id`),
  ADD KEY `is_enabled` (`is_enabled`);

--
-- Indexes for table `story`
--
ALTER TABLE `story`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `slug_text` (`slug_text`),
  ADD KEY `owner_user_id` (`owner_user_id`),
  ADD KEY `visibility_state` (`visibility_state`),
  ADD KEY `publication_state` (`publication_state`);

--
-- Indexes for table `arc`
--
ALTER TABLE `arc`
  ADD PRIMARY KEY (`id`),
  ADD KEY `story_id` (`story_id`);

--
-- Indexes for table `volume`
--
ALTER TABLE `volume`
  ADD PRIMARY KEY (`id`),
  ADD KEY `story_id` (`story_id`),
  ADD KEY `arc_id` (`arc_id`);

--
-- Indexes for table `chapter`
--
ALTER TABLE `chapter`
  ADD PRIMARY KEY (`id`),
  ADD KEY `story_id` (`story_id`),
  ADD KEY `volume_id` (`volume_id`),
  ADD KEY `publication_state` (`publication_state`);

--
-- Indexes for table `story_character`
--
ALTER TABLE `story_character`
  ADD PRIMARY KEY (`id`),
  ADD KEY `story_id` (`story_id`);

--
-- Indexes for table `skill`
--
ALTER TABLE `skill`
  ADD PRIMARY KEY (`id`),
  ADD KEY `story_id` (`story_id`);

--
-- Indexes for table `character_skill`
--
ALTER TABLE `character_skill`
  ADD PRIMARY KEY (`id`),
  ADD KEY `story_character_id` (`story_character_id`),
  ADD KEY `skill_id` (`skill_id`);

--
-- Indexes for table `story_event`
--
ALTER TABLE `story_event`
  ADD PRIMARY KEY (`id`),
  ADD KEY `story_id` (`story_id`),
  ADD KEY `chapter_id` (`chapter_id`);

--
-- Indexes for table `idea`
--
ALTER TABLE `idea`
  ADD PRIMARY KEY (`id`),
  ADD KEY `story_id` (`story_id`);

--
-- Indexes for table `item`
--
ALTER TABLE `item`
  ADD PRIMARY KEY (`id`),
  ADD KEY `story_id` (`story_id`);

--
-- Indexes for table `media`
--
ALTER TABLE `media`
  ADD PRIMARY KEY (`id`),
  ADD KEY `chapter_id` (`chapter_id`);

--
-- Indexes for table `story_comment`
--
ALTER TABLE `story_comment`
  ADD PRIMARY KEY (`id`),
  ADD KEY `story_id` (`story_id`),
  ADD KEY `chapter_id` (`chapter_id`),
  ADD KEY `author_user_id` (`author_user_id`),
  ADD KEY `parent_comment_id` (`parent_comment_id`),
  ADD KEY `visibility_state` (`visibility_state`);

--
-- Indexes for table `story_rating`
--
ALTER TABLE `story_rating`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uniq_story_rating_user` (`story_id`,`author_user_id`),
  ADD KEY `author_user_id` (`author_user_id`);

--
-- Indexes for table `content_report`
--
ALTER TABLE `content_report`
  ADD PRIMARY KEY (`id`),
  ADD KEY `reporter_user_id` (`reporter_user_id`),
  ADD KEY `target_user_id` (`target_user_id`),
  ADD KEY `story_id` (`story_id`),
  ADD KEY `chapter_id` (`chapter_id`),
  ADD KEY `comment_id` (`comment_id`),
  ADD KEY `reviewed_by_user_id` (`reviewed_by_user_id`),
  ADD KEY `status_name` (`status_name`);

--
-- Indexes for table `user_sanction`
--
ALTER TABLE `user_sanction`
  ADD PRIMARY KEY (`id`),
  ADD KEY `target_user_id` (`target_user_id`),
  ADD KEY `applied_by_user_id` (`applied_by_user_id`),
  ADD KEY `is_active` (`is_active`);

--
-- Indexes for table `user_follow`
--
ALTER TABLE `user_follow`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uniq_user_follow` (`follower_user_id`,`followed_user_id`),
  ADD KEY `followed_user_id` (`followed_user_id`);

--
-- Indexes for table `story_favorite`
--
ALTER TABLE `story_favorite`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uniq_story_favorite` (`user_id`,`story_id`),
  ADD KEY `story_id` (`story_id`);

--
-- Indexes for table `story_view_log`
--
ALTER TABLE `story_view_log`
  ADD PRIMARY KEY (`id`),
  ADD KEY `story_id` (`story_id`),
  ADD KEY `chapter_id` (`chapter_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `viewed_at` (`viewed_at`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `app_user`
--
ALTER TABLE `app_user`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `global_notice`
--
ALTER TABLE `global_notice`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `story`
--
ALTER TABLE `story`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `arc`
--
ALTER TABLE `arc`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `volume`
--
ALTER TABLE `volume`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `chapter`
--
ALTER TABLE `chapter`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `story_character`
--
ALTER TABLE `story_character`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `skill`
--
ALTER TABLE `skill`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `character_skill`
--
ALTER TABLE `character_skill`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `story_event`
--
ALTER TABLE `story_event`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `idea`
--
ALTER TABLE `idea`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `item`
--
ALTER TABLE `item`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `media`
--
ALTER TABLE `media`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `story_comment`
--
ALTER TABLE `story_comment`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `story_rating`
--
ALTER TABLE `story_rating`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `content_report`
--
ALTER TABLE `content_report`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `user_sanction`
--
ALTER TABLE `user_sanction`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `user_follow`
--
ALTER TABLE `user_follow`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `story_favorite`
--
ALTER TABLE `story_favorite`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `story_view_log`
--
ALTER TABLE `story_view_log`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `global_notice`
--
ALTER TABLE `global_notice`
  ADD CONSTRAINT `global_notice_ibfk_1` FOREIGN KEY (`created_by_user_id`) REFERENCES `app_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Constraints for table `story`
--
ALTER TABLE `story`
  ADD CONSTRAINT `story_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `arc`
--
ALTER TABLE `arc`
  ADD CONSTRAINT `arc_ibfk_1` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `volume`
--
ALTER TABLE `volume`
  ADD CONSTRAINT `volume_ibfk_1` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `volume_ibfk_2` FOREIGN KEY (`arc_id`) REFERENCES `arc` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `chapter`
--
ALTER TABLE `chapter`
  ADD CONSTRAINT `chapter_ibfk_1` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `chapter_ibfk_2` FOREIGN KEY (`volume_id`) REFERENCES `volume` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `story_character`
--
ALTER TABLE `story_character`
  ADD CONSTRAINT `story_character_ibfk_1` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `skill`
--
ALTER TABLE `skill`
  ADD CONSTRAINT `skill_ibfk_1` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `character_skill`
--
ALTER TABLE `character_skill`
  ADD CONSTRAINT `character_skill_ibfk_1` FOREIGN KEY (`story_character_id`) REFERENCES `story_character` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `character_skill_ibfk_2` FOREIGN KEY (`skill_id`) REFERENCES `skill` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `story_event`
--
ALTER TABLE `story_event`
  ADD CONSTRAINT `story_event_ibfk_1` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `story_event_ibfk_2` FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `idea`
--
ALTER TABLE `idea`
  ADD CONSTRAINT `idea_ibfk_1` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `item`
--
ALTER TABLE `item`
  ADD CONSTRAINT `item_ibfk_1` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `media`
--
ALTER TABLE `media`
  ADD CONSTRAINT `media_ibfk_1` FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `story_comment`
--
ALTER TABLE `story_comment`
  ADD CONSTRAINT `story_comment_ibfk_1` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `story_comment_ibfk_2` FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `story_comment_ibfk_3` FOREIGN KEY (`author_user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `story_comment_ibfk_4` FOREIGN KEY (`parent_comment_id`) REFERENCES `story_comment` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `story_rating`
--
ALTER TABLE `story_rating`
  ADD CONSTRAINT `story_rating_ibfk_1` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `story_rating_ibfk_2` FOREIGN KEY (`author_user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `content_report`
--
ALTER TABLE `content_report`
  ADD CONSTRAINT `content_report_ibfk_1` FOREIGN KEY (`reporter_user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `content_report_ibfk_2` FOREIGN KEY (`target_user_id`) REFERENCES `app_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `content_report_ibfk_3` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `content_report_ibfk_4` FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `content_report_ibfk_5` FOREIGN KEY (`comment_id`) REFERENCES `story_comment` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `content_report_ibfk_6` FOREIGN KEY (`reviewed_by_user_id`) REFERENCES `app_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `user_sanction`
--
ALTER TABLE `user_sanction`
  ADD CONSTRAINT `user_sanction_ibfk_1` FOREIGN KEY (`target_user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `user_sanction_ibfk_2` FOREIGN KEY (`applied_by_user_id`) REFERENCES `app_user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Constraints for table `user_follow`
--
ALTER TABLE `user_follow`
  ADD CONSTRAINT `user_follow_ibfk_1` FOREIGN KEY (`follower_user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `user_follow_ibfk_2` FOREIGN KEY (`followed_user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `story_favorite`
--
ALTER TABLE `story_favorite`
  ADD CONSTRAINT `story_favorite_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `story_favorite_ibfk_2` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `story_view_log`
--
ALTER TABLE `story_view_log`
  ADD CONSTRAINT `story_view_log_ibfk_1` FOREIGN KEY (`story_id`) REFERENCES `story` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `story_view_log_ibfk_2` FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `story_view_log_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
COMMIT;