package com.keacs.app.ui.management

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalMall
import androidx.compose.material.icons.rounded.MoreHoriz
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
    IconOption("more", "其他", Icons.Rounded.MoreHoriz, "gray"),
)

val incomeIconOptions = listOf(
    IconOption("work", "工资", Icons.Rounded.Work, "blue"),
    IconOption("gift", "奖金", Icons.Rounded.CardGiftcard, "yellow"),
    IconOption("receipt", "报销", Icons.AutoMirrored.Rounded.ReceiptLong, "green"),
    IconOption("chart", "理财收益", Icons.Rounded.BarChart, "purple"),
    IconOption("gift", "礼金", Icons.Rounded.CardGiftcard, "orange"),
    IconOption("coins", "兼职", Icons.Rounded.AccountBalanceWallet, "cyan"),
    IconOption("more", "其他", Icons.Rounded.MoreHoriz, "gray"),
)

val accountIconOptions = listOf(
    IconOption("wallet", "现金", Icons.Rounded.AccountBalanceWallet, "green"),
    IconOption("wallet", "支付宝", Icons.Rounded.AccountBalanceWallet, "blue"),
    IconOption("wallet", "微信", Icons.Rounded.AccountBalanceWallet, "cyan"),
    IconOption("bank", "银行卡", Icons.Rounded.AccountBalance, "indigo"),
    IconOption("card", "信用卡", Icons.Rounded.CreditCard, "orange"),
    IconOption("card", "花呗白条", Icons.Rounded.CreditCard, "yellow"),
    IconOption("home", "公积金", Icons.Rounded.Home, "purple"),
    IconOption("chart", "投资账户", Icons.Rounded.BarChart, "pink"),
    IconOption("loan", "借款", Icons.Rounded.CreditCard, "red"),
    IconOption("more", "其他", Icons.Rounded.MoreHoriz, "gray"),
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
