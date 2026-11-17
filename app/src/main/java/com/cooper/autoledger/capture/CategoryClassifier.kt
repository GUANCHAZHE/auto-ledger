package com.cooper.autoledger.capture

import com.cooper.autoledger.model.Category
import com.cooper.autoledger.model.TransactionType
import java.util.Locale

object CategoryClassifier {

    private val SALARY_KEYWORDS = listOf(
        "代发工资", "工资", "薪资", "薪酬", "发薪", "月薪", "年终奖", "绩效",
        "奖金", "津贴", "补贴", "劳务费", "稿费",
        "salary", "payroll", "wage", "bonus", "stipend"
    )

    private val REFUND_KEYWORDS = listOf(
        "退款", "退回", "退货", "退费", "退票", "返现", "赔付", "理赔", "报销", "红包",
        "refund", "cashback", "reimburse", "red packet"
    )

    private val EXPENSE_RULES: List<Pair<Category, List<String>>> = listOf(
        Category.MEDICAL to listOf(
            "京东大药房", "阿里健康", "美团买药", "饿了么买药", "叮当快药",
            "大药房", "药房", "药店", "医院", "诊所", "门诊", "卫生院", "社康",
            "体检", "医保", "挂号", "口腔", "牙科", "眼科", "爱康国宾", "美年大健康",
            "丁香", "买药",
            "hospital", "pharmacy", "clinic", "medical", "dental", "health"
        ),
        Category.TRANSPORT to listOf(
            "美团打车", "美团单车", "高德打车", "滴滴", "花小猪", "曹操出行", "t3出行",
            "享道出行", "神州专车", "首汽约车", "哈啰", "青桔", "单车", "打车",
            "出租车", "网约车", "地铁", "轨道交通", "公交", "乘车码", "一卡通",
            "深圳通", "羊城通", "岭南通", "交通卡", "出行", "12306", "铁路",
            "高铁", "动车", "火车票", "机票", "航空", "国航", "东航", "南航", "海航",
            "携程", "去哪儿", "飞猪", "加油", "中石化", "中国石化", "中石油", "中国石油",
            "壳牌", "充电桩", "特来电", "星星充电", "停车", "泊车", "车牌付",
            "高速", "粤通卡", "etc",
            "uber", "lyft", "taxi", "metro", "subway", "bus", "train", "railway",
            "airline", "flight", "parking", "fuel", "gas station", "toll"
        ),
        Category.GROCERY to listOf(
            "美团买菜", "小象超市", "盒马", "叮咚", "朴朴", "天猫超市", "京东七鲜", "七鲜",
            "山姆", "开市客", "costco", "沃尔玛", "永辉", "大润发", "华润万家",
            "物美", "联华", "家乐福", "奥乐齐", "aldi", "超市", "便利店",
            "全家", "罗森", "7-11", "7eleven", "711", "美宜佳", "便利蜂", "十足", "喜士多",
            "生鲜", "菜场", "菜市场", "农贸", "果蔬", "水果", "百果园", "鲜丰水果", "钱大妈", "粮油",
            "grocery", "supermarket", "convenience", "familymart", "lawson", "fresh"
        ),
        Category.DINING to listOf(
            "美团外卖", "饿了么", "瑞幸", "星巴克", "肯德基", "麦当劳", "必胜客",
            "汉堡王", "赛百味", "达美乐", "华莱士", "塔斯汀", "海底捞", "呷哺",
            "喜茶", "奈雪", "霸王茶姬", "茶百道", "古茗", "蜜雪冰城", "一点点",
            "沪上阿姨", "库迪", "manner", "arabica", "tims", "老乡鸡", "真功夫",
            "萨莉亚", "西贝", "外婆家", "太二", "八合里", "好利来", "鲍师傅",
            "外卖", "餐饮", "餐厅", "饭店", "酒楼", "茶餐厅", "火锅", "烧烤",
            "烤肉", "烤鱼", "串串", "麻辣烫", "冒菜", "米线", "螺蛳粉", "拉面",
            "面馆", "粉店", "水饺", "饺子", "包子", "早餐", "快餐", "便当",
            "小吃", "食堂", "奶茶", "咖啡", "茶饮", "甜品", "糖水", "烘焙",
            "蛋糕", "面包", "美食", "美团",
            "luckin", "starbucks", "kfc", "mcdonald", "pizza", "burger",
            "cafe", "coffee", "bakery", "restaurant", "dining", "bistro", "grill"
        ),
        Category.ENTERTAINMENT to listOf(
            "猫眼", "淘票票", "电影", "影城", "影院", "万达影城", "百丽宫",
            "steam", "playstation", "nintendo", "xbox", "epic",
            "网易云", "qq音乐", "酷狗", "酷我", "腾讯视频", "爱奇艺", "优酷", "芒果tv",
            "b站", "哔哩哔哩", "bilibili", "netflix", "spotify", "youtube", "apple music",
            "会员", "超级会员", "vip", "游戏", "米哈游", "原神", "崩坏",
            "腾讯游戏", "网易游戏", "王者", "ktv", "剧本杀", "密室", "桌游",
            "网吧", "网咖", "电玩", "泡泡玛特", "popmart", "游乐", "迪士尼",
            "环球影城", "欢乐谷", "方特", "门票", "演唱会", "音乐节", "大麦",
            "cinema", "movie", "game", "gaming", "entertainment", "concert"
        ),
        Category.HOUSING to listOf(
            "电费", "供电", "国家电网", "南方电网", "水费", "水务", "自来水",
            "燃气", "煤气", "天然气", "热力", "供暖", "暖气", "物业", "业委会",
            "房租", "租房", "自如", "相寓", "泊寓", "贝壳", "链家",
            "宽带", "话费", "手机充值", "通讯费", "中国移动", "中国联通", "中国电信", "中国广电",
            "移动", "联通", "电信", "生活缴费", "家政", "保洁", "干洗", "洗衣",
            "开锁", "维修", "快递", "顺丰", "京东快递", "菜鸟", "中通", "圆通",
            "申通", "韵达", "极兔", "丰巢", "宜家", "ikea",
            "electric", "water bill", "gas bill", "property", "rent",
            "broadband", "telecom", "utility", "utilities"
        ),
        Category.SHOPPING to listOf(
            "淘宝", "天猫", "京东", "拼多多", "唯品会", "得物", "小红书",
            "抖音商城", "快手小店", "苏宁", "国美", "闲鱼", "亚马逊", "amazon",
            "apple store", "小米之家", "小米商城", "华为商城", "荣耀商城", "大疆", "dji",
            "优衣库", "uniqlo", "zara", "hm", "迪卡侬", "耐克", "nike", "阿迪达斯", "adidas",
            "名创优品", "miniso", "无印良品", "muji", "屈臣氏", "丝芙兰", "万宁",
            "万达广场", "银泰", "大悦城", "万象城", "奥特莱斯", "百货", "购物中心",
            "商场", "购物", "服饰", "服装", "女装", "男装", "童装", "鞋帽", "箱包",
            "美妆", "护肤", "化妆品", "数码", "电脑", "家电", "电器", "家居",
            "旗舰店", "专卖店", "专营店", "商城",
            "mall", "shop", "store", "boutique", "fashion", "electronics",
            "taobao", "tmall", "jd.com"
        ),
        Category.TRANSFER to listOf(
            "转账", "微信红包", "发红包", "红包", "面对面收钱", "个人收款",
            "二维码收款", "扫二维码付款", "群收款", "aa收款", "亲属卡",
            "信用卡还款", "花呗还款", "白条还款", "还款",
            "transfer", "red packet", "remittance"
        )
    )

    private val DISCOUNT_NOISE_REGEX = Regex("(红包抵扣|红包优惠|立减红包|优惠红包|满减红包)")
    private val SHORT_ASCII_REGEX = Regex("^[a-z0-9]{2,4}$")

    fun classify(merchant: String, rawText: String, type: TransactionType): Category {
        val merchantNorm = merchant.trim().lowercase(Locale.ROOT)
        val rawNorm = rawText.trim().lowercase(Locale.ROOT)

        if (type == TransactionType.INCOME) {
            if (matchesAny(merchantNorm, SALARY_KEYWORDS) || matchesAny(rawNorm, SALARY_KEYWORDS)) {
                return Category.SALARY
            }
            if (matchesAny(merchantNorm, REFUND_KEYWORDS) || matchesAny(rawNorm, REFUND_KEYWORDS)) {
                return Category.REFUND
            }
            return Category.OTHER_INCOME
        }

        // Pass 1: Prioritize merchant name for maximum precision.
        if (merchantNorm.isNotEmpty()) {
            for ((category, keywords) in EXPENSE_RULES) {
                if (matchesAny(merchantNorm, keywords)) {
                    return category
                }
            }
        }

        // Pass 2: Fall back to sanitized raw screen/notification text.
        if (rawNorm.isNotEmpty()) {
            val sanitizedRaw = rawNorm.replace(DISCOUNT_NOISE_REGEX, "")
            for ((category, keywords) in EXPENSE_RULES) {
                if (matchesAny(sanitizedRaw, keywords)) {
                    return category
                }
            }
        }

        return Category.OTHER_EXPENSE
    }

    private fun matchesAny(text: String, keywords: List<String>): Boolean {
        if (text.isEmpty()) return false
        return keywords.any { keyword -> containsKeyword(text, keyword) }
    }

    private fun containsKeyword(text: String, keyword: String): Boolean {
        if (SHORT_ASCII_REGEX.matches(keyword)) {
            val boundaryRegex = Regex("(?<![a-z0-9])${Regex.escape(keyword)}(?![a-z0-9])")
            return boundaryRegex.containsMatchIn(text)
        }
        return text.contains(keyword)
    }
}
