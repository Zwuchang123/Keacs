package com.keacs.app.ui.management

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalMall
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.ui.theme.KeacsColors

data class IconOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val colorKey: String,
)

val expenseIconOptions = listOf(
    IconOption("food", "餐饮", Icons.Rounded.Restaurant, "orange"),
    IconOption("bus", "交通", Icons.Rounded.DirectionsBus, "blue"),
    IconOption("bag", "购物", Icons.Rounded.ShoppingBag, "green"),
    IconOption("home", "住房", Icons.Rounded.Home, "cyan"),
    IconOption("bolt", "水电煤", Icons.Rounded.Bolt, "yellow"),
    IconOption("game", "娱乐", Icons.Rounded.SportsEsports, "purple"),
    IconOption("phone", "通讯", Icons.Rounded.PhoneAndroid, "indigo"),
    IconOption("medical", "医疗", Icons.Rounded.LocalHospital, "red"),
    IconOption("school", "教育", Icons.Rounded.School, "yellow"),
    IconOption("chart", "投资", Icons.Rounded.BarChart, "green"),
    IconOption("heart", "人情", Icons.Rounded.Favorite, "pink"),
    IconOption("pet", "宠物", Icons.Rounded.Favorite, "pink"),
    IconOption("baby", "母婴", Icons.Rounded.Favorite, "orange"),
    IconOption("clothes", "服饰", Icons.Rounded.ShoppingBag, "purple"),
    IconOption("beauty", "美容", Icons.Rounded.Favorite, "pink"),
    IconOption("sport", "运动", Icons.Rounded.SportsEsports, "blue"),
    IconOption("book", "书籍", Icons.Rounded.School, "green"),
    IconOption("coffee", "咖啡", Icons.Rounded.Restaurant, "orange"),
    IconOption("travel", "旅行", Icons.Rounded.DirectionsBus, "cyan"),
    IconOption("rent", "房租", Icons.Rounded.Home, "indigo"),
    IconOption("gift", "礼物", Icons.Rounded.CardGiftcard, "pink"),
    IconOption("repair", "维修", Icons.Rounded.Build, "gray"),
    IconOption("tax", "税费", Icons.Rounded.AccountBalance, "red"),
    IconOption("insurance", "保险", Icons.Rounded.Favorite, "indigo"),
    IconOption("more", "其他", Icons.Rounded.MoreHoriz, "gray"),
)

val incomeIconOptions = listOf(
    IconOption("work", "工资", Icons.Rounded.Work, "blue"),
    IconOption("bonus", "奖金", Icons.Rounded.CardGiftcard, "yellow"),
    IconOption("overtime", "加班费", Icons.Rounded.Work, "orange"),
    IconOption("receipt", "报销", Icons.AutoMirrored.Rounded.ReceiptLong, "green"),
    IconOption("chart", "理财收益", Icons.Rounded.BarChart, "purple"),
    IconOption("fund", "基金收益", Icons.Rounded.BarChart, "cyan"),
    IconOption("stock", "股票收益", Icons.Rounded.BarChart, "green"),
    IconOption("gift", "礼金", Icons.Rounded.CardGiftcard, "orange"),
    IconOption("redpacket", "红包", Icons.Rounded.CardGiftcard, "red"),
    IconOption("coins", "兼职", Icons.Rounded.AccountBalanceWallet, "cyan"),
    IconOption("refund", "退款", Icons.Rounded.AccountBalanceWallet, "green"),
    IconOption("transfer", "转账收入", Icons.Rounded.AccountBalanceWallet, "indigo"),
    IconOption("rental", "租金", Icons.Rounded.Home, "purple"),
    IconOption("dividend", "分红", Icons.Rounded.BarChart, "pink"),
    IconOption("interest", "利息", Icons.Rounded.AccountBalance, "yellow"),
    IconOption("more", "其他", Icons.Rounded.MoreHoriz, "gray"),
)

val accountIconOptions = listOf(
    IconOption("wallet", "现金", Icons.Rounded.AccountBalanceWallet, "green"),
    IconOption("alipay", "支付宝", Icons.Rounded.AccountBalanceWallet, "blue"),
    IconOption("wechat", "微信", Icons.Rounded.AccountBalanceWallet, "cyan"),
    IconOption("bank", "银行卡", Icons.Rounded.AccountBalance, "indigo"),
    IconOption("debit", "储蓄卡", Icons.Rounded.AccountBalance, "purple"),
    IconOption("credit", "信用卡", Icons.Rounded.CreditCard, "orange"),
    IconOption("huabei", "花呗/白条", Icons.Rounded.CreditCard, "yellow"),
    IconOption("housing", "公积金", Icons.Rounded.Home, "purple"),
    IconOption("investment", "投资账户", Icons.Rounded.BarChart, "pink"),
    IconOption("fund", "基金", Icons.Rounded.BarChart, "cyan"),
    IconOption("stock", "股票", Icons.Rounded.BarChart, "green"),
    IconOption("insurance", "保险", Icons.Rounded.Favorite, "indigo"),
    IconOption("loan", "贷款", Icons.Rounded.CreditCard, "red"),
    IconOption("mortgage", "房贷", Icons.Rounded.Home, "orange"),
    IconOption("car", "车贷", Icons.Rounded.DirectionsBus, "blue"),
    IconOption("personal", "私人借款", Icons.Rounded.Person, "gray"),
    IconOption("business", "对公账户", Icons.Rounded.Business, "indigo"),
    IconOption("more", "其他", Icons.Rounded.MoreHoriz, "gray"),
)

data class AccountTypeOption(
    val type: String,
    val iconKey: String,
    val colorKey: String,
)

val accountTypeOptions = listOf(
    AccountTypeOption("现金", "wallet", "green"),
    AccountTypeOption("支付宝", "alipay", "blue"),
    AccountTypeOption("微信", "wechat", "cyan"),
    AccountTypeOption("银行卡", "bank", "indigo"),
    AccountTypeOption("储蓄卡", "debit", "purple"),
    AccountTypeOption("信用卡", "credit", "orange"),
    AccountTypeOption("花呗/白条", "huabei", "yellow"),
    AccountTypeOption("公积金", "housing", "purple"),
    AccountTypeOption("投资账户", "investment", "pink"),
    AccountTypeOption("基金", "fund", "cyan"),
    AccountTypeOption("股票", "stock", "green"),
    AccountTypeOption("保险", "insurance", "indigo"),
    AccountTypeOption("贷款", "loan", "red"),
    AccountTypeOption("房贷", "mortgage", "orange"),
    AccountTypeOption("车贷", "car", "blue"),
    AccountTypeOption("私人借款", "personal", "gray"),
    AccountTypeOption("对公账户", "business", "indigo"),
    AccountTypeOption("其他", "more", "gray"),
)

fun categoryOptions(direction: String): List<IconOption> =
    if (direction == PresetSeedData.CATEGORY_INCOME) incomeIconOptions else expenseIconOptions

fun iconFor(key: String): ImageVector =
    (expenseIconOptions + incomeIconOptions + accountIconOptions)
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
