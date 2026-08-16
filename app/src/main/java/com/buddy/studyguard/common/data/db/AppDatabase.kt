package com.buddy.studyguard.common.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.buddy.studyguard.common.data.db.dao.AiMessageDao
import com.buddy.studyguard.common.data.db.dao.AppCategoryDao
import com.buddy.studyguard.common.data.db.dao.AppLimitRuleDao
import com.buddy.studyguard.common.data.db.dao.AppLockStateDao
import com.buddy.studyguard.common.data.db.dao.AppUsageRecordDao
import com.buddy.studyguard.common.data.db.dao.BlockedTimeWindowDao
import com.buddy.studyguard.common.data.db.dao.ChatMessageDao
import com.buddy.studyguard.common.data.db.dao.CourseDao
import com.buddy.studyguard.common.data.db.dao.ParentMessageDao
import com.buddy.studyguard.common.data.db.dao.ParentPinDao
import com.buddy.studyguard.common.data.db.dao.StudySessionDao
import com.buddy.studyguard.common.data.db.dao.TaskDao
import com.buddy.studyguard.common.data.db.entity.AiMessageEntity
import com.buddy.studyguard.common.data.db.entity.AppCategoryEntity
import com.buddy.studyguard.common.data.db.entity.AppLimitRuleEntity
import com.buddy.studyguard.common.data.db.entity.AppLockStateEntity
import com.buddy.studyguard.common.data.db.entity.AppUsageRecordEntity
import com.buddy.studyguard.common.data.db.entity.BlockedTimeWindowAppEntity
import com.buddy.studyguard.common.data.db.entity.BlockedTimeWindowEntity
import com.buddy.studyguard.common.data.db.entity.CourseEntity
import com.buddy.studyguard.common.data.db.entity.ParentMessageEntity
import com.buddy.studyguard.common.data.db.entity.ParentPinEntity
import com.buddy.studyguard.common.data.db.entity.StudySessionEntity
import com.buddy.studyguard.common.data.db.entity.ChatMessageEntity
import com.buddy.studyguard.common.data.db.entity.TaskEntity
import com.buddy.studyguard.common.util.PinHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用唯一本地数据库。
 *
 * 所有字段均使用基础类型（Long/String/Int/Boolean），无需 TypeConverter。
 * 版本号升级时请在 [MIGRATIONS] 中补充迁移。
 */
@Database(
    entities = [
        TaskEntity::class,
        AppUsageRecordEntity::class,
        AppLimitRuleEntity::class,
        BlockedTimeWindowEntity::class,
        BlockedTimeWindowAppEntity::class,
        AppLockStateEntity::class,
        ParentMessageEntity::class,
        StudySessionEntity::class,
        CourseEntity::class,
        AiMessageEntity::class,
        ParentPinEntity::class,
        AppCategoryEntity::class,
        ChatMessageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun appUsageRecordDao(): AppUsageRecordDao
    abstract fun appLimitRuleDao(): AppLimitRuleDao
    abstract fun blockedTimeWindowDao(): BlockedTimeWindowDao
    abstract fun appLockStateDao(): AppLockStateDao
    abstract fun parentMessageDao(): ParentMessageDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun courseDao(): CourseDao
    abstract fun aiMessageDao(): AiMessageDao
    abstract fun parentPinDao(): ParentPinDao
    abstract fun appCategoryDao(): AppCategoryDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        const val DB_NAME = "buddy_study_guard.db"

        /**
         * 首次创建数据库时写入默认家长口令 "123456" 的哈希。
         * 必须同步执行：onCreate 在数据库创建事务里调用，
         * 异步执行会导致事务状态不一致从而崩溃。
         */
        val SEED_CALLBACK = object : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val salt = PinHasher.newSalt()
                val hash = PinHasher.hash(PinHasher.DEFAULT_PIN, salt)
                db.execSQL(
                    "INSERT OR REPLACE INTO parent_pin (id, pinHash, salt, updatedAt) VALUES (?, ?, ?, ?)",
                    arrayOf(
                        ParentPinEntity.PIN_ROW_ID,
                        hash,
                        salt,
                        System.currentTimeMillis()
                    )
                )
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v1→v2: 初始迁移，无 schema 变更
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE chat_messages ADD COLUMN cloudId TEXT")
                    db.execSQL("ALTER TABLE tasks ADD COLUMN cloudId TEXT")
                }
            },
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE chat_messages ADD COLUMN senderUid TEXT")
                    db.execSQL("ALTER TABLE chat_messages ADD COLUMN senderName TEXT")
                }
            }
        )
    }
}
