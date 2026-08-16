package com.buddy.studyguard.ai.data.local

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 离线 FAQ 兜底知识库。
 *
 * 在无网络或豆包 API 调用异常时，按关键词匹配返回预置答案，
 * 保证 AI 助手在离线状态下也能对常见学习问题给出基本回应。
 * 匹配策略：统计 query 命中的关键词数量，取得分最高者；无命中返回默认引导语。
 */
@Singleton
class FaqRepository @Inject constructor() {

    /** 一条预置 FAQ。 */
    data class FaqItem(
        val keywords: List<String>,
        val question: String,
        val answer: String
    )

    /** 全部预置 FAQ 列表。 */
    private val items: List<FaqItem> = listOf(
        FaqItem(
            keywords = listOf("分数", "约分", "通分"),
            question = "怎么把分数约分和通分？",
            answer = "约分：找分子分母的最大公因数，同时除掉它，例如 4/8 = 1/2。" +
                "通分：找两个分母的最小公倍数作公分母，再把每个分数扩成同分母分数，" +
                "例如 1/2 与 1/3 通分为 3/6 与 2/6。"
        ),
        FaqItem(
            keywords = listOf("一元一次方程", "解方程", "移项"),
            question = "怎么解一元一次方程？",
            answer = "步骤：去分母→去括号→移项（变号）→合并同类项→系数化为 1。" +
                "例如 2x+3=7，移项得 2x=4，两边同除 2 得 x=2。"
        ),
        FaqItem(
            keywords = listOf("二元一次方程组", "消元", "代入"),
            question = "二元一次方程组怎么解？",
            answer = "常用代入消元法和加减消元法。把一个未知数用另一个表示后代入另一方程，" +
                "或把两式相加/相减消去一个未知数，先解出一个未知数再回代求另一个。"
        ),
        FaqItem(
            keywords = listOf("勾股定理", "直角三角形", "勾股"),
            question = "勾股定理是什么？",
            answer = "直角三角形两直角边 a、b 的平方和等于斜边 c 的平方：a² + b² = c²。" +
                "例如直角边 3、4，斜边 = √(9+16) = 5。"
        ),
        FaqItem(
            keywords = listOf("三角形面积", "底乘高"),
            question = "三角形面积怎么算？",
            answer = "三角形面积 = 底 × 高 ÷ 2。先找到一条边作底，再量出这条边上的高，" +
                "代入公式即可。例如底 6、高 4，面积 = 6×4÷2 = 12。"
        ),
        FaqItem(
            keywords = listOf("圆", "面积", "周长", "半径", "直径"),
            question = "圆的周长和面积公式？",
            answer = "设半径 r、直径 d=2r。周长 C = 2πr = πd；面积 S = πr²。" +
                "例如半径 3，周长 ≈ 6π，面积 ≈ 9π。"
        ),
        FaqItem(
            keywords = listOf("作文", "开头", "写作文"),
            question = "作文开头怎么写更好？",
            answer = "好的开头要简短抓人。可以：①开门见山点题；②用一个细节或画面引入；" +
                "③用疑问或对话开篇。避免空话套话，紧扣中心，2-4 句即可。"
        ),
        FaqItem(
            keywords = listOf("阅读理解", "主旨", "概括"),
            question = "阅读理解怎么抓主旨？",
            answer = "先通读知大意，再细读理结构。抓主旨看：标题、开头段、结尾段、反复出现的词。" +
                "概括时用『谁+做什么+结果如何』的句式，避免只复述情节。"
        ),
        FaqItem(
            keywords = listOf("古诗", "背诵", "默写"),
            question = "古诗背不下来怎么办？",
            answer = "三步法：①先读懂诗意和作者情感；②按画面/情节分层记；③边背边默写。" +
                "易错字单独抄 3 遍。每天早读背 2 遍比一次性硬背更牢。"
        ),
        FaqItem(
            keywords = listOf("比喻", "拟人", "修辞", "修辞手法"),
            question = "比喻和拟人怎么区分？",
            answer = "比喻是把一样东西说成另一样东西，要有喻体（像……），如『月亮像盘子』。" +
                "拟人是把物当人写，赋予人的动作情感，如『风在唱歌』。"
        ),
        FaqItem(
            keywords = listOf("背单词", "记单词", "英语单词"),
            question = "英语单词老记不住怎么办？",
            answer = "用『音节拆分 + 词根词缀 + 语境例句』。每天少量多次（如 10 个×3 遍）" +
                "比一次背 50 个更有效。配合读音记忆，睡前和早起各复习一次。"
        ),
        FaqItem(
            keywords = listOf("一般现在时", "do does", "时态"),
            question = "一般现在时怎么用？",
            answer = "表示经常性、习惯性或客观事实。主语三单时动词加 s/es，疑问否定用 do/does。" +
                "常搭配 always、usually、every day 等时间状语。"
        ),
        FaqItem(
            keywords = listOf("一般过去时", "ed", "过去时"),
            question = "一般过去时怎么构成？",
            answer = "表示过去发生的事。动词用过去式（规则加 ed，不规则需单独记）。" +
                "疑问否定用 did，其后动词还原。常搭配 yesterday、last week 等时间状语。"
        ),
        FaqItem(
            keywords = listOf("名词复数", "复数", "s es"),
            question = "英语名词复数怎么加？",
            answer = "一般加 s；s/x/sh/ch 结尾加 es；辅音+y 结尾变 y 为 i 加 es；" +
                "f/fe 结尾多变 v 加 es；o 结尾有的加 s 有的加 es。不规则如 child→children、man→men 需记。"
        ),
        FaqItem(
            keywords = listOf("浮力", "阿基米德", "浮"),
            question = "浮力怎么算？",
            answer = "阿基米德原理：浮力 F浮 = ρ液 × g × V排，方向竖直向上。" +
                "V排 是物体排开液体的体积。当 F浮 ≥ G物 时物体漂浮或上浮，F浮 < G物 时下沉。"
        ),
        FaqItem(
            keywords = listOf("速度", "路程", "时间"),
            question = "速度、路程、时间的关系？",
            answer = "路程 = 速度 × 时间；速度 = 路程 ÷ 时间；时间 = 路程 ÷ 速度。" +
                "注意单位统一：m/s 与 km/h 换算时，1 m/s = 3.6 km/h。"
        ),
        FaqItem(
            keywords = listOf("元素周期表", "元素", "原子序数"),
            question = "元素周期表怎么记？",
            answer = "按周期和族记。前 20 号元素口诀：氢氦锂铍硼，碳氮氧氟氖，钠镁铝硅磷，" +
                "硫氯氩钾钙。原子序数 = 核电荷数 = 质子数 = 核外电子数。"
        ),
        FaqItem(
            keywords = listOf("原子", "分子", "化学变化"),
            question = "原子和分子有什么区别？",
            answer = "原子是化学变化中的最小粒子，在化学反应中不可再分；" +
                "分子是保持物质化学性质的最小粒子，由原子构成，在化学反应中可再分。"
        ),
        FaqItem(
            keywords = listOf("番茄钟", "专注", "番茄工作法"),
            question = "什么是番茄工作法？",
            answer = "专注 25 分钟、休息 5 分钟为一个『番茄』。每 4 个番茄大休 15-30 分钟。" +
                "期间不看手机、不分心，做完一个再开始下一个，能显著提升专注效率。"
        ),
        FaqItem(
            keywords = listOf("错题本", "错题"),
            question = "错题本怎么用才有效？",
            answer = "不只是抄题。每道写三栏：①错因（概念/计算/审题）；②正确思路；③举一反三的变式。" +
                "周末和考前重做错题，会的划掉，不会的继续留。"
        ),
        FaqItem(
            keywords = listOf("预习", "复习", "学习方法"),
            question = "预习和复习哪个更重要？",
            answer = "两者都重要。预习让听课有重点，复习让知识留存。建议每天课后 10 分钟" +
                "回顾+当晚作业应用=复习；第二天新课先通读标疑问=预习。循环才牢靠。"
        ),
        FaqItem(
            keywords = listOf("考试紧张", "紧张", "焦虑", "心慌"),
            question = "考试紧张怎么办？",
            answer = "三个小技巧：①深呼吸 4-4-4（吸 4 秒、屏 4 秒、呼 4 秒）；" +
                "②先做会的题建立信心；③把注意力放在题目本身而不是结果。适度紧张其实有助发挥。"
        ),
        FaqItem(
            keywords = listOf("时间分配", "考试时间", "做题顺序"),
            question = "考试时间怎么分配？",
            answer = "先易后难。拿到卷子先通览，按『易-中-难』顺序做。" +
                "给每大题预估时间，卡住先跳过，最后留 10 分钟检查。别在一道难题上耗太久。"
        ),
        FaqItem(
            keywords = listOf("检查", "检查试卷", "验算"),
            question = "考试怎么检查才有效？",
            answer = "别只看一遍。理科用不同方法验算（如逆向代入）；" +
                "文科检查漏题、错别字、答题位置。检查姓名考号是否填写，交卷前再看一眼。"
        ),
        FaqItem(
            keywords = listOf("怎么问", "提问", "怎么问问题"),
            question = "怎么向 AI 提问能得到好回答？",
            answer = "把问题说具体：说明年级、科目、题目、卡在哪一步、你的思路是什么。" +
                "比如『初二数学，解 2x-3=5 时我算成 x=4，哪里错了？』比『这题怎么做』更易得到启发。"
        )
    )

    /** 返回全部 FAQ。 */
    fun all(): List<FaqItem> = items

    /** 默认离线引导语。 */
    private val default: String = "这个问题我离线没法回答，连上网可以问 AI 助手哦～"

    /**
     * 按关键词匹配得分返回最佳答案。
     *
     * @param query 用户输入
     * @return 命中关键词最多的 FAQ 答案；无任何命中时返回 [default]。
     */
    fun answer(query: String): String {
        if (query.isBlank()) return default
        val q = query.lowercase()
        var best: FaqItem? = null
        var bestScore = 0
        for (item in items) {
            val score = item.keywords.count { kw -> q.contains(kw.lowercase()) }
            if (score > bestScore) {
                bestScore = score
                best = item
            }
        }
        return if (best != null && bestScore > 0) best!!.answer else default
    }
}
