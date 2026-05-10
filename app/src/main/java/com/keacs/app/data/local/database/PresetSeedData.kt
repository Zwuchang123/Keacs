package com.keacs.app.data.local.database

import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity

object PresetSeedData {
    const val CATEGORY_INCOME = "INCOME"
    const val CATEGORY_EXPENSE = "EXPENSE"
    const val CATEGORY_ACCOUNT = "ACCOUNT"
    const val CATEGORY_ACCOUNT_ASSET = "ACCOUNT_ASSET"
    const val CATEGORY_ACCOUNT_LIABILITY = "ACCOUNT_LIABILITY"
    const val ACCOUNT_ASSET = "ASSET"
    const val ACCOUNT_LIABILITY = "LIABILITY"

    fun categories(now: Long): List<CategoryEntity> {
        val incomes = listOf("工资", "奖金", "报销", "理财收益", "礼金", "兼职", "其他")
        val expenses = listOf(
            "餐饮", "交通", "日用", "住房", "水电煤", "通讯", "医疗", "娱乐", "教育", "投资", "人情",
            "恋爱", "旅行", "长辈", "房贷", "宠物", "其他",
        )
        val assetAccountCategories = listOf(
            Triple("支付宝", "alipay", "blue"),
            Triple("微信", "wechat_asset", "cyan"),
            Triple("现金", "cash_asset", "green"),
            Triple("银行卡", "bank_asset", "indigo"),
            Triple("公积金", "housing_fund", "purple"),
            Triple("投资账户", "investment_asset", "pink"),
            Triple("其他资产", "asset_more", "gray"),
        )
        val liabilityAccountCategories = listOf(
            Triple("信用卡", "credit_card_liability", "orange"),
            Triple("花呗白条", "credit_line", "yellow"),
            Triple("消费贷", "consumer_loan", "red"),
            Triple("房贷车贷", "mortgage_liability", "blue"),
            Triple("亲友借款", "friend_loan", "green"),
            Triple("其他负债", "liability_more", "gray"),
        )
        return incomes.mapIndexed { index, name ->
            presetCategory(name, CATEGORY_INCOME, index, now, categoryIconKey(name, CATEGORY_INCOME), categoryColorKey(name))
        } + expenses.mapIndexed { index, name ->
            presetCategory(name, CATEGORY_EXPENSE, index, now, categoryIconKey(name, CATEGORY_EXPENSE), categoryColorKey(name))
        } + assetAccountCategories.mapIndexed { index, (name, iconKey, colorKey) ->
            presetCategory(name, CATEGORY_ACCOUNT_ASSET, index, now, iconKey, colorKey)
        } + liabilityAccountCategories.mapIndexed { index, (name, iconKey, colorKey) ->
            presetCategory(name, CATEGORY_ACCOUNT_LIABILITY, index, now, iconKey, colorKey)
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

    fun categoryIconKey(name: String, direction: String = CATEGORY_EXPENSE): String = when (name) {
        "餐饮" -> "food"
        "交通" -> "bus"
        "日用", "购物" -> "bag"
        "住房" -> "home"
        "水电煤" -> "bolt"
        "通讯" -> "phone"
        "医疗" -> "medical"
        "娱乐" -> "game"
        "教育" -> "school"
        "投资" -> "chart"
        "理财收益" -> "profit_chart"
        "人情" -> "heart"
        "恋爱" -> "love"
        "旅行" -> "luggage"
        "长辈" -> "elder"
        "房贷" -> "mortgage"
        "宠物" -> "pet"
        "工资" -> "work"
        "奖金" -> "bonus"
        "礼金" -> "gift"
        "报销" -> "receipt"
        "兼职" -> "coins"
        "其他" -> if (direction == CATEGORY_INCOME) "income_more" else "more"
        else -> "more"
    }

    fun categoryColorKey(name: String): String = when (name) {
        "餐饮", "礼金" -> "orange"
        "交通", "工资" -> "blue"
        "日用", "购物", "报销", "投资" -> "green"
        "住房", "兼职" -> "cyan"
        "水电煤", "奖金", "教育" -> "yellow"
        "通讯" -> "indigo"
        "医疗", "长辈" -> "red"
        "娱乐", "理财收益" -> "purple"
        "人情", "恋爱", "宠物" -> "pink"
        "旅行" -> "cyan"
        "房贷" -> "blue"
        else -> "gray"
    }

    fun accountIconKey(name: String): String = when (name) {
        "现金" -> "cash_asset"
        "支付宝" -> "alipay"
        "微信" -> "wechat_asset"
        "银行卡" -> "bank_asset"
        "信用卡" -> "credit_card_liability"
        "花呗/白条" -> "credit_line"
        "公积金" -> "housing_fund"
        "投资账户" -> "investment_asset"
        "消费贷" -> "consumer_loan"
        "房贷/车贷" -> "mortgage_liability"
        "亲友借款" -> "friend_loan"
        "其他资产" -> "asset_more"
        "其他负债" -> "liability_more"
        else -> "asset_more"
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
        "消费贷" -> "red"
        "房贷/车贷" -> "blue"
        "亲友借款" -> "green"
        else -> "gray"
    }

    fun accountCategoryDirectionFor(nature: String): String =
        if (nature == ACCOUNT_LIABILITY) CATEGORY_ACCOUNT_LIABILITY else CATEGORY_ACCOUNT_ASSET

    fun accountCategoryNatureFor(direction: String): String =
        if (direction == CATEGORY_ACCOUNT_LIABILITY) ACCOUNT_LIABILITY else ACCOUNT_ASSET

    fun isAccountCategoryDirection(direction: String): Boolean =
        direction == CATEGORY_ACCOUNT ||
            direction == CATEGORY_ACCOUNT_ASSET ||
            direction == CATEGORY_ACCOUNT_LIABILITY

    fun isAccountCategoryForNature(direction: String, nature: String): Boolean =
        when (direction) {
            CATEGORY_ACCOUNT_ASSET -> nature == ACCOUNT_ASSET
            CATEGORY_ACCOUNT_LIABILITY -> nature == ACCOUNT_LIABILITY
            // 兼容旧数据，旧账户分类会在初始化时迁移到新的资产/负债方向。
            CATEGORY_ACCOUNT -> true
            else -> false
        }
}
