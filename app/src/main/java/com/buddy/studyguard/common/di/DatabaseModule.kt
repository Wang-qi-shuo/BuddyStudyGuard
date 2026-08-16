package com.buddy.studyguard.common.di

import android.content.Context
import androidx.room.Room
import com.buddy.studyguard.common.data.db.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库与 DAO 的 Hilt 提供者。
 * AppDatabase 为单例，注入 [AppDatabase.SEED_CALLBACK] 以在首次创建时写入默认家长口令。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME)
            .addCallback(AppDatabase.SEED_CALLBACK)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideAppUsageRecordDao(db: AppDatabase): AppUsageRecordDao = db.appUsageRecordDao()
    @Provides fun provideAppLimitRuleDao(db: AppDatabase): AppLimitRuleDao = db.appLimitRuleDao()
    @Provides fun provideBlockedTimeWindowDao(db: AppDatabase): BlockedTimeWindowDao = db.blockedTimeWindowDao()
    @Provides fun provideAppLockStateDao(db: AppDatabase): AppLockStateDao = db.appLockStateDao()
    @Provides fun provideParentMessageDao(db: AppDatabase): ParentMessageDao = db.parentMessageDao()
    @Provides fun provideStudySessionDao(db: AppDatabase): StudySessionDao = db.studySessionDao()
    @Provides fun provideCourseDao(db: AppDatabase): CourseDao = db.courseDao()
    @Provides fun provideAiMessageDao(db: AppDatabase): AiMessageDao = db.aiMessageDao()
    @Provides fun provideParentPinDao(db: AppDatabase): ParentPinDao = db.parentPinDao()
    @Provides fun provideAppCategoryDao(db: AppDatabase): AppCategoryDao = db.appCategoryDao()
    @Provides fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()
}
