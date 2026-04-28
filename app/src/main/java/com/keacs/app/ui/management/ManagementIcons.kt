package com.keacs.app.ui.management

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CardMembership
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalMall
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PriceCheck
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.keacs.app.data.local.database.PresetSeedData
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
    IconOption("more", "其他", Icons.Rounded.MoreHoriz, "gray"),
)

val incomeIconOptions = listOf(
    IconOption("work", "工资", Icons.Rounded.Work, "blue"),
    IconOption("bonus", "奖金", Icons.Rounded.CardGiftcard, "yellow"),
    IconOption("receipt", "报销", Icons.AutoMirrored.Rounded.ReceiptLong, "green"),
    IconOption("chart", "理财收益", Icons.Rounded.BarChart, "purple"),
    IconOption("gift", "礼金", Icons.Rounded.Redeem, "orange"),
    IconOption("coins", "兼职", Icons.Rounded.AccountBalanceWallet, "cyan"),
    IconOption("cash", "现金收入", Icons.Rounded.AttachMoney, "green"),
    IconOption("saving_income", "存款收益", Icons.Rounded.Savings, "blue"),
    IconOption("payback", "退款", Icons.Rounded.Paid, "green"),
    IconOption("business", "经营", Icons.Rounded.Storefront, "orange"),
    IconOption("office", "办公", Icons.Rounded.BusinessCenter, "indigo"),
    IconOption("trend", "收益", Icons.Rounded.TrendingUp, "purple"),
    IconOption("support", "补贴", Icons.Rounded.VolunteerActivism, "pink"),
    IconOption("award", "奖励", Icons.Rounded.CardMembership, "yellow"),
    IconOption("rent_income", "租金", Icons.Rounded.Home, "cyan"),
    IconOption("bank_income", "银行", Icons.Rounded.AccountBalance, "blue"),
    IconOption("request", "收款", Icons.Rounded.RequestQuote, "green"),
    IconOption("atm_income", "取现", Icons.Rounded.LocalAtm, "indigo"),
    IconOption("mall_income", "销售", Icons.Rounded.LocalMall, "orange"),
    IconOption("study_income", "助学", Icons.Rounded.School, "yellow"),
    IconOption("care_income", "人情", Icons.Rounded.Favorite, "pink"),
    IconOption("more", "其他", Icons.Rounded.MoreHoriz, "gray"),
)

val accountIconOptions = listOf(
    IconOption("wallet", "现金", Icons.Rounded.AccountBalanceWallet, "green"),
    IconOption("alipay", "支付宝", Icons.Rounded.QrCode2, "blue"),
    IconOption("wechat", "微信", Icons.Rounded.PhoneAndroid, "cyan"),
    IconOption("bank", "银行卡", Icons.Rounded.AccountBalance, "indigo"),
    IconOption("card", "信用卡", Icons.Rounded.CreditCard, "orange"),
    IconOption("credit_line", "花呗白条", Icons.Rounded.Payments, "yellow"),
    IconOption("home", "公积金", Icons.Rounded.Home, "purple"),
    IconOption("chart", "投资账户", Icons.Rounded.BarChart, "pink"),
    IconOption("loan", "借款", Icons.AutoMirrored.Rounded.ReceiptLong, "red"),
    IconOption("savings", "储蓄账户", Icons.Rounded.Savings, "green"),
    IconOption("cash_card", "储值卡", Icons.Rounded.CardMembership, "cyan"),
    IconOption("fund", "基金", Icons.Rounded.TrendingUp, "purple"),
    IconOption("stock", "股票", Icons.Rounded.ShowChart, "green"),
    IconOption("atm", "现金卡", Icons.Rounded.LocalAtm, "indigo"),
    IconOption("house_asset", "房产", Icons.Rounded.Apartment, "blue"),
    IconOption("car_asset", "车辆", Icons.Rounded.DirectionsCar, "orange"),
    IconOption("business_account", "经营账户", Icons.Rounded.Storefront, "yellow"),
    IconOption("office_account", "公司账户", Icons.Rounded.BusinessCenter, "indigo"),
    IconOption("request_account", "应收款", Icons.Rounded.RequestQuote, "green"),
    IconOption("paid_account", "付款账户", Icons.Rounded.PriceCheck, "cyan"),
    IconOption("gift_account", "礼金账户", Icons.Rounded.Redeem, "pink"),
    IconOption("more", "其他", Icons.Rounded.MoreHoriz, "gray"),
)

fun categoryOptions(direction: String): List<IconOption> =
    when (direction) {
        PresetSeedData.CATEGORY_INCOME -> incomeIconOptions
        PresetSeedData.CATEGORY_ACCOUNT -> accountIconOptions
        else -> expenseIconOptions
    }

fun accountTypeOptions(categories: List<CategoryEntity>): List<IconOption> {
    val customOptions = categories
        .filter { it.direction == PresetSeedData.CATEGORY_ACCOUNT && it.isEnabled }
        .map { IconOption(it.iconKey, it.name, iconFor(it.iconKey), it.colorKey) }
    return (customOptions + accountIconOptions).distinctBy { it.label }
}

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
