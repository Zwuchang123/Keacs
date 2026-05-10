package com.keacs.app.ui.management

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AssuredWorkload
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Atm
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CardMembership
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.ChildFriendly
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.CorporateFare
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CreditScore
import androidx.compose.material.icons.rounded.CrueltyFree
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Commute
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsTransit
import androidx.compose.material.icons.rounded.Diversity1
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.DryCleaning
import androidx.compose.material.icons.rounded.Elderly
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.LocalActivity
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Money
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.LocalMall
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.Loyalty
import androidx.compose.material.icons.rounded.MapsHomeWork
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PedalBike
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.PriceCheck
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.RealEstateAgent
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.material.icons.rounded.SportsMartialArts
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Subway
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Villa
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Yard
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.ui.theme.KeacsColors

data class IconOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val colorKey: String,
)

val expenseIconOptions = listOf(
    IconOption("food", "餐饮", Icons.Rounded.Restaurant, "orange"),
    IconOption("fastfood", "快餐", Icons.Rounded.Fastfood, "orange"),
    IconOption("cafe", "咖啡", Icons.Rounded.LocalCafe, "orange"),
    IconOption("pizza", "小吃", Icons.Rounded.LocalPizza, "yellow"),
    IconOption("bus", "交通", Icons.Rounded.DirectionsBus, "blue"),
    IconOption("car", "打车", Icons.Rounded.DirectionsCar, "blue"),
    IconOption("train", "铁路", Icons.Rounded.Train, "indigo"),
    IconOption("flight", "出行", Icons.Rounded.Flight, "cyan"),
    IconOption("bag", "购物", Icons.Rounded.ShoppingBag, "green"),
    IconOption("cart", "超市", Icons.Rounded.ShoppingCart, "green"),
    IconOption("grocery", "买菜", Icons.Rounded.LocalGroceryStore, "green"),
    IconOption("mall", "日用", Icons.Rounded.LocalMall, "cyan"),
    IconOption("home", "住房", Icons.Rounded.Home, "cyan"),
    IconOption("bolt", "水电煤", Icons.Rounded.Bolt, "yellow"),
    IconOption("water", "水费", Icons.Rounded.WaterDrop, "blue"),
    IconOption("game", "娱乐", Icons.Rounded.SportsEsports, "purple"),
    IconOption("movie", "电影", Icons.Rounded.Movie, "purple"),
    IconOption("phone", "通讯", Icons.Rounded.PhoneAndroid, "indigo"),
    IconOption("medical", "医疗", Icons.Rounded.LocalHospital, "red"),
    IconOption("school", "教育", Icons.Rounded.School, "yellow"),
    IconOption("chart", "投资", Icons.Rounded.BarChart, "green"),
    IconOption("heart", "人情", Icons.Rounded.Favorite, "pink"),
    IconOption("love", "恋爱", Icons.Rounded.Celebration, "pink"),
    IconOption("luggage", "旅行", Icons.Rounded.Luggage, "cyan"),
    IconOption("elder", "长辈", Icons.Rounded.Elderly, "red"),
    IconOption("mortgage", "房贷", Icons.Rounded.RealEstateAgent, "blue"),
    IconOption("pet", "宠物", Icons.Rounded.Pets, "pink"),
    IconOption("ramen", "面食", Icons.Rounded.RamenDining, "orange"),
    IconOption("bakery", "烘焙", Icons.Rounded.BakeryDining, "yellow"),
    IconOption("icecream", "甜品", Icons.Rounded.Icecream, "pink"),
    IconOption("bike", "骑行", Icons.Rounded.TwoWheeler, "blue"),
    IconOption("subway", "地铁", Icons.Rounded.Subway, "indigo"),
    IconOption("hotel", "住宿", Icons.Rounded.Hotel, "cyan"),
    IconOption("clothes", "衣物", Icons.Rounded.Checkroom, "purple"),
    IconOption("fitness", "运动", Icons.Rounded.FitnessCenter, "green"),
    IconOption("basketball", "球类", Icons.Rounded.SportsBasketball, "orange"),
    IconOption("music", "音乐", Icons.Rounded.MusicNote, "purple"),
    IconOption("theater", "演出", Icons.Rounded.TheaterComedy, "pink"),
    IconOption("book", "读书", Icons.Rounded.MenuBook, "yellow"),
    IconOption("medicine", "药品", Icons.Rounded.Medication, "red"),
    IconOption("spa", "护理", Icons.Rounded.Spa, "green"),
    IconOption("child", "孩子", Icons.Rounded.ChildCare, "cyan"),
    IconOption("flower", "鲜花", Icons.Rounded.LocalFlorist, "pink"),
    IconOption("park", "户外", Icons.Rounded.Park, "green"),
    IconOption("more", "其他", Icons.Rounded.MoreHoriz, "gray"),
)

val incomeIconOptions = listOf(
    IconOption("work", "工资", Icons.Rounded.Work, "blue"),
    IconOption("bonus", "奖金", Icons.Rounded.CardGiftcard, "yellow"),
    IconOption("receipt", "报销", Icons.AutoMirrored.Rounded.ReceiptLong, "green"),
    IconOption("profit_chart", "理财收益", Icons.Rounded.PieChart, "purple"),
    IconOption("gift", "礼金", Icons.Rounded.Redeem, "orange"),
    IconOption("coins", "兼职", Icons.Rounded.AccountBalanceWallet, "cyan"),
    IconOption("cash", "现金收入", Icons.Rounded.AttachMoney, "green"),
    IconOption("saving_income", "存款收益", Icons.Rounded.CurrencyExchange, "blue"),
    IconOption("payback", "退款", Icons.Rounded.Paid, "green"),
    IconOption("business", "经营", Icons.Rounded.PointOfSale, "orange"),
    IconOption("office", "办公", Icons.Rounded.BusinessCenter, "indigo"),
    IconOption("trend", "收益", Icons.Rounded.TrendingUp, "purple"),
    IconOption("support", "补贴", Icons.Rounded.VolunteerActivism, "pink"),
    IconOption("award", "奖励", Icons.Rounded.CardMembership, "yellow"),
    IconOption("rent_income", "租金", Icons.Rounded.Cottage, "cyan"),
    IconOption("bank_income", "银行", Icons.Rounded.CreditScore, "blue"),
    IconOption("request", "收款", Icons.Rounded.LocalActivity, "green"),
    IconOption("atm_income", "取现", Icons.Rounded.Atm, "indigo"),
    IconOption("mall_income", "销售", Icons.Rounded.ShoppingBasket, "orange"),
    IconOption("study_income", "助学", Icons.Rounded.LocalLibrary, "yellow"),
    IconOption("care_income", "人情", Icons.Rounded.Diversity1, "pink"),
    IconOption("celebration_income", "红包", Icons.Rounded.EmojiEvents, "pink"),
    IconOption("money_income", "现金", Icons.Rounded.Money, "green"),
    IconOption("real_income", "房产", Icons.Rounded.Villa, "blue"),
    IconOption("pet_income", "宠物", Icons.Rounded.CrueltyFree, "pink"),
    IconOption("elder_income", "长辈", Icons.Rounded.AccessibilityNew, "red"),
    IconOption("flower_income", "鲜花", Icons.Rounded.Yard, "pink"),
    IconOption("hotel_income", "住宿", Icons.Rounded.Bed, "cyan"),
    IconOption("luggage_income", "旅行", Icons.Rounded.TravelExplore, "cyan"),
    IconOption("bike_income", "骑行", Icons.Rounded.PedalBike, "blue"),
    IconOption("medicine_income", "医疗", Icons.Rounded.LocalPharmacy, "red"),
    IconOption("book_income", "图书", Icons.Rounded.AutoStories, "yellow"),
    IconOption("music_income", "演出", Icons.Rounded.Mic, "purple"),
    IconOption("fitness_income", "运动", Icons.Rounded.SportsGymnastics, "green"),
    IconOption("clothes_income", "衣物", Icons.Rounded.DryCleaning, "purple"),
    IconOption("income_more", "其他", Icons.Rounded.Apps, "gray"),
)

private val assetAccountIconOptions = listOf(
    IconOption("cash_asset", "现金", Icons.Rounded.Wallet, "green"),
    IconOption("alipay", "支付宝", Icons.Rounded.QrCode2, "blue"),
    IconOption("wechat_asset", "微信", Icons.Rounded.Chat, "cyan"),
    IconOption("bank_asset", "银行卡", Icons.Rounded.AccountBalance, "indigo"),
    IconOption("housing_fund", "公积金", Icons.Rounded.AssuredWorkload, "purple"),
    IconOption("investment_asset", "投资账户", Icons.Rounded.QueryStats, "pink"),
    IconOption("savings_asset", "储蓄账户", Icons.Rounded.Savings, "green"),
    IconOption("stored_value_asset", "储值卡", Icons.Rounded.ConfirmationNumber, "cyan"),
    IconOption("fund_asset", "基金", Icons.Rounded.DonutLarge, "purple"),
    IconOption("stock_asset", "股票", Icons.Rounded.ShowChart, "green"),
    IconOption("atm_asset", "现金卡", Icons.Rounded.LocalAtm, "indigo"),
    IconOption("house_asset", "房产", Icons.Rounded.Apartment, "blue"),
    IconOption("car_asset", "车辆", Icons.Rounded.Garage, "orange"),
    IconOption("business_account", "经营账户", Icons.Rounded.Storefront, "yellow"),
    IconOption("office_account", "公司账户", Icons.Rounded.CorporateFare, "indigo"),
    IconOption("receivable_account", "应收款", Icons.Rounded.RequestQuote, "green"),
    IconOption("paid_account", "付款账户", Icons.Rounded.PriceCheck, "cyan"),
    IconOption("gift_account", "礼金账户", Icons.Rounded.Loyalty, "pink"),
    IconOption("money_account", "零钱", Icons.Rounded.MonetizationOn, "green"),
    IconOption("real_account", "按揭", Icons.Rounded.HomeWork, "blue"),
    IconOption("travel_account", "旅行金", Icons.Rounded.DirectionsTransit, "cyan"),
    IconOption("elder_account", "长辈账户", Icons.Rounded.Diversity3, "red"),
    IconOption("pet_account", "宠物账户", Icons.Rounded.Forest, "pink"),
    IconOption("child_account", "孩子账户", Icons.Rounded.ChildFriendly, "cyan"),
    IconOption("health_account", "医疗账户", Icons.Rounded.HealthAndSafety, "red"),
    IconOption("study_account", "学习账户", Icons.Rounded.LaptopMac, "yellow"),
    IconOption("fitness_account", "运动账户", Icons.Rounded.SportsMartialArts, "green"),
    IconOption("flower_account", "礼物账户", Icons.Rounded.Palette, "pink"),
    IconOption("hotel_account", "住宿账户", Icons.Rounded.MeetingRoom, "cyan"),
    IconOption("bike_account", "交通账户", Icons.Rounded.Commute, "blue"),
    IconOption("subway_account", "通勤账户", Icons.Rounded.Route, "indigo"),
    IconOption("celebration_account", "红包账户", Icons.Rounded.Cake, "pink"),
    IconOption("asset_more", "其他", Icons.Rounded.Inventory2, "gray"),
)

private val liabilityAccountIconOptions = listOf(
    IconOption("credit_card_liability", "信用卡", Icons.Rounded.CreditCard, "orange"),
    IconOption("credit_line", "花呗白条", Icons.Rounded.Payments, "yellow"),
    IconOption("consumer_loan", "借款", Icons.Rounded.Gavel, "red"),
    IconOption("liability_payment", "付款账户", Icons.Rounded.Receipt, "cyan"),
    IconOption("mortgage_liability", "按揭", Icons.Rounded.MapsHomeWork, "blue"),
    IconOption("friend_loan", "亲友借款", Icons.Rounded.Handshake, "green"),
    IconOption("liability_more", "其他", Icons.Rounded.ReportProblem, "gray"),
)

val accountIconOptions = assetAccountIconOptions + liabilityAccountIconOptions

private val legacyIconOptions = listOf(
    IconOption("wallet", "现金", Icons.Rounded.Wallet, "green"),
    IconOption("wechat", "微信", Icons.Rounded.Chat, "cyan"),
    IconOption("bank", "银行卡", Icons.Rounded.AccountBalance, "indigo"),
    IconOption("card", "信用卡", Icons.Rounded.CreditCard, "orange"),
    IconOption("loan", "借款", Icons.Rounded.Gavel, "red"),
    IconOption("cash_card", "储值卡", Icons.Rounded.ConfirmationNumber, "cyan"),
    IconOption("fund", "基金", Icons.Rounded.DonutLarge, "purple"),
    IconOption("atm", "现金卡", Icons.Rounded.LocalAtm, "indigo"),
    IconOption("request_account", "应收款", Icons.Rounded.RequestQuote, "green"),
)

fun categoryOptions(direction: String): List<IconOption> =
    when (direction) {
        PresetSeedData.CATEGORY_INCOME -> incomeIconOptions
        PresetSeedData.CATEGORY_ACCOUNT_ASSET -> assetAccountIconOptions
        PresetSeedData.CATEGORY_ACCOUNT_LIABILITY -> liabilityAccountIconOptions
        PresetSeedData.CATEGORY_ACCOUNT -> accountIconOptions
        else -> expenseIconOptions
    }

fun accountTypeOptions(categories: List<CategoryEntity>, nature: String): List<IconOption> =
    categories
        .filter {
            it.isEnabled &&
                PresetSeedData.isAccountCategoryForNature(it.direction, nature)
        }
        .map { IconOption(it.iconKey, it.name, iconFor(it.iconKey), it.colorKey) }
        .distinctBy { it.label }

fun accountIconOptionFor(account: AccountEntity?, categories: List<CategoryEntity>): IconOption {
    val fallback = IconOption(
        key = account?.iconKey ?: "more",
        label = account?.type ?: "其他",
        icon = iconFor(account?.iconKey ?: "more"),
        colorKey = account?.colorKey ?: "gray",
    )
    if (account == null) return fallback
    val typeName = normalizedAccountCategoryName(account.type)
    val name = normalizedAccountCategoryName(account.name)
    val category = categories.firstOrNull {
        PresetSeedData.isAccountCategoryForNature(it.direction, account.nature) &&
            normalizedAccountCategoryName(it.name) in listOf(typeName, name)
    }
    return category?.let {
        IconOption(it.iconKey, it.name, iconFor(it.iconKey), it.colorKey)
    } ?: fallback
}

internal fun normalizedAccountCategoryName(name: String): String = when (name) {
    "花呗/白条" -> "花呗白条"
    "房贷/车贷" -> "房贷车贷"
    else -> name
}

internal fun matchingAccountTypeOption(
    options: List<IconOption>,
    vararg names: String?,
): IconOption? {
    val normalizedNames = names
        .mapNotNull { it?.takeIf(String::isNotBlank) }
        .map { normalizedAccountCategoryName(it) }
    return options.firstOrNull { option ->
        normalizedAccountCategoryName(option.label) in normalizedNames
    }
}

fun iconFor(key: String): ImageVector =
    (expenseIconOptions + incomeIconOptions + accountIconOptions + legacyIconOptions)
        .firstOrNull { it.key == key }
        ?.icon ?: Icons.Rounded.MoreHoriz

fun colorFor(key: String): Color = when (key) {
    "orange" -> KeacsColors.CategoryOrange
    "blue" -> KeacsColors.CategoryBlue
    "green" -> KeacsColors.CategoryGreen
    "cyan" -> Color(0xFF4FC3D7)
    "yellow" -> Color(0xFFFFBF3F)
    "indigo" -> Color(0xFF7D91FF)
    "red" -> KeacsColors.Expense
    "purple" -> KeacsColors.CategoryPurple
    "pink" -> Color(0xFFEF7DC8)
    else -> KeacsColors.TextTertiary
}
