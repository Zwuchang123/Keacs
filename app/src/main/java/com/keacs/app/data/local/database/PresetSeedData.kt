package com.keacs.app.data.local.database

import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity

object PresetSeedData {
    const val CATEGORY_INCOME = "INCOME"
    const val CATEGORY_EXPENSE = "EXPENSE"
    const val CATEGORY_ACCOUNT = "ACCOUNT"
    const val ACCOUNT_ASSET = "ASSET"
    const val ACCOUNT_LIABILITY = "LIABILITY"

    fun categories(now: Long): List<CategoryEntity> {
        val incomes = listOf("工资", "奖金", "报销", "理财收益", "礼金", "兼职", "其他")
        val expenses = listOf("餐饮", "交通", "日用", "住房", "水电煤", "通讯", "医疗", "娱乐", "教育", "投资", "人情", "其他")
        val accountCategories = listOf(
            Triple("现金", "wallet", "green"),
            Triple("支付宝", "alipay", "blue"),
            Triple("微信", "wechat", "cyan"),
            Triple("银行卡", "bank", "indigo"),
            Triple("信用卡", "card", "orange"),
            Triple("花呗白条", "credit_line", "yellow"),
            Triple("公积金", "home", "purple"),
            Triple("投资账户", "chart", "pink"),
            Triple("借款", "loan", "red"),
            Triple("储蓄账户", "savings", "green"),
            Triple("储值卡", "cash_card", "cyan"),
            Triple("基金", "fund", "purple"),
            Triple("股票", "stock", "green"),
            Triple("现金卡", "atm", "indigo"),
            Triple("房产", "house_asset", "blue"),
            Triple("车辆", "car_asset", "orange"),
            Triple("经营账户", "business_account", "yellow"),
            Triple("公司账户", "office_account", "indigo"),
            Triple("应收款", "request_account", "green"),
            Triple("付款账户", "paid_account", "cyan"),
            Triple("礼金账户", "gift_account", "pink"),
            Triple("其他", "more", "gray"),
        )
        return incomes.mapIndexed { index, name ->
            presetCategory(name, CATEGORY_INCOME, index, now, categoryIconKey(name), categoryColorKey(name))
        } + expenses.mapIndexed { index, name ->
            presetCategory(name, CATEGORY_EXPENSE, index, now, categoryIconKey(name), categoryColorKey(name))
        } + accountCategories.mapIndexed { index, (name, iconKey, colorKey) ->
            presetCategory(name, CATEGORY_ACCOUNT, index, now, iconKey, colorKey)
        }
    }

    fun accounts(now: Long): List<AccountEntity> {
        val assets = listOf("支付宝", "微信", "现金", "银行卡", "公积金", "投资账户", "其他资产")
        val liabilities = listOf("信用卡", "花呗/白条", "消费贷", "房贷/车贷", "亲友借款", "其他负债")
        return assets.map { name ->
            presetAccount(name, ACCOUNT_ASSET, now, accountIconKey(name), accountColorKey(name))
        } + liabilities.map { name ->
            presetAccount(name, ACCOUNT_LIABILITY, now, accountIconKey(name), accountColorKey(name))
        }
    }

    private fun presetCategory(
        name: String,
        direction: String,
        sortOrder: Int,
        now: Long,
        iconKey: String,
        colorKey: String,
    ) = CategoryEntity(
        name = name,
        direction = direction,
        iconKey = iconKey,
        colorKey = colorKey,
        isPreset = true,
        isEnabled = true,
        sortOrder = sortOrder,
        createdAt = now,
        updatedAt = now,
    )

    private fun presetAccount(
        name: String,
        nature: String,
        now: Long,
        iconKey: String,
        colorKey: String,
    ) = AccountEntity(
        name = name,
        nature = nature,
        type = name,
        iconKey = iconKey,
        colorKey = colorKey,
        initialBalanceCent = 0,
        isEnabled = true,
        createdAt = now,
        updatedAt = now,
    )

    fun categoryIconKey(name: String): String = when (name) {
        "餐饮" -> "food"
        "交通" -> "bus"
        "日用", "购物" -> "bag"
        "住房" -> "home"
        "水电煤" -> "bolt"
        "通讯" -> "phone"
        "医疗" -> "medical"
        "娱乐" -> "game"
        "教育" -> "school"
        "投资", "理财收益" -> "chart"
        "人情" -> "heart"
        "工资" -> "work"
        "奖金", "礼金" -> "gift"
        "报销" -> "receipt"
        "兼职" -> "coins"
        else -> "more"
    }

    fun categoryColorKey(name: String): String = when (name) {
        "餐饮", "礼金" -> "orange"
        "交通", "工资" -> "blue"
        "日用", "购物", "报销", "投资" -> "green"
        "住房", "兼职" -> "cyan"
        "水电煤", "奖金", "教育" -> "yellow"
        "通讯" -> "indigo"
        "医疗" -> "red"
        "娱乐", "理财收益" -> "purple"
        "人情" -> "pink"
        else -> "gray"
    }

    fun accountIconKey(name: String): String = when (name) {
        "现金" -> "wallet"
        "支付宝" -> "alipay"
        "微信" -> "wechat"
        "银行卡" -> "bank"
        "信用卡" -> "card"
        "花呗/白条" -> "credit_line"
        "公积金" -> "home"
        "投资账户" -> "chart"
        "消费贷", "房贷/车贷", "亲友借款" -> "loan"
        else -> "more"
    }

    fun accountColorKey(name: String): String = when (name) {
        "现金" -> "green"
        "支付宝" -> "blue"
        "微信" -> "cyan"
        "银行卡" -> "indigo"
        "信用卡" -> "orange"
        "花呗/白条" -> "yellow"
        "公积金" -> "purple"
        "投资账户" -> "pink"
        "消费贷", "房贷/车贷", "亲友借款" -> "red"
        else -> "gray"
    }
}
