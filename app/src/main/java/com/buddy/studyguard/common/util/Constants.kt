package com.buddy.studyguard.common.util

/**
 * 全局常量。
 */
object Constants {

    /** 豆包大模型 API 基础地址（OpenAI 兼容形态）。 */
    const val DOUBAO_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3/"

    /** 默认对话模型 ID，实际使用时替换为你在火山引擎控制台接入的模型 ID。 */
    const val DOUBAO_DEFAULT_MODEL = "doubao-pro-32k"

    /** 视觉（多模态）模型 ID，用于图片识别；请替换为你在火山引擎控制台接入的视觉模型接入点 ID。 */
    const val DOUBAO_VISION_MODEL = "doubao-1.5-vision-pro-32k-250115"

    /** AI 会话系统提示：限定为学习答疑助手角色。 */
    const val AI_SYSTEM_PROMPT = """你是一名耐心、友好的学习助手，服务于一名中小学生。
请只回答与学习相关的问题（如课本知识讲解、解题思路、学习方法、作业疑问）。
如果用户提出与学习无关的话题，请礼貌地引导回学习。
回答要清晰、易懂，必要时分步骤讲解。不要给出最终考试答案作弊式直接答案，应启发思考。"""

    /** 专注番茄钟默认工作时长（分钟）。 */
    const val POMODORO_WORK_MINUTES = 25

    /** 专注番茄钟默认休息时长（分钟）。 */
    const val POMODORO_BREAK_MINUTES = 5

    /** 前台服务通知渠道。 */
    const val CHANNEL_FOREGROUND = "foreground_guard"

    /** 限制提醒通知渠道。 */
    const val CHANNEL_LIMIT_ALERT = "limit_alert"

    /** 学习提醒通知渠道。 */
    const val CHANNEL_STUDY_REMINDER = "study_reminder"
}
