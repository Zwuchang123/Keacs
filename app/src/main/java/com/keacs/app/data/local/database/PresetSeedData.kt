package com.keacs.app.data.local.database

import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity

object PresetSeedData {
    const val CATEGORY_INCOME = "INCOME"
    const val CATEGORY_EXPENSE = "EXPENSE"
    const val ACCOUNT_ASSET = "ASSET"
    const val ACCOUNT_LIABILITY = "LIABILITY"

    fun categories(now: Long): List<CategoryEntity> {
        val incomes = listOf("工资", "奖金", "报销", "理财收益", "礼金", "兼职", "其他")
        val expenses = listOf("餐饮", "交通", "日用", "住房", "水电煤", "通讯", "医疗", "娱乐", "教育", "投资", "人情", "其他")
        return incomes.mapIndexed { index, name ->
            presetCategory(name, CATEGORY_INCOME, index, now)
        } + expenses.mapIndexed { index, name ->
            presetCategory(name, CATEGORY_EXPENSE, index, now)
        }
    }

    fun accounts(now: Long): List<AccountEntity> {
        val assets = listOf("支付宝", "微信", "现金", "银行卡", "公积金", "投资账户", "其他资产")
        val liabilities = listOf("信用卡", "花呗/白条", "消费贷", "房贷/车贷", "亲友借款", "其他负债")
        return assets.map { name ->
            presetAccount(name, ACCOUNT_ASSET, now)
        } + liabilities.map { name ->
            presetAccount(name, ACCOUNT_LIABILITY, now)
        }
    }

    private fun presetCategory(
        name: String,
        direction: String,
        sortOrder: Int,
        now: Long,
    ) = CategoryEntity(
        name = name,
        direction = direction,
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
    ) = AccountEntity(
        name = name,
        nature = nature,
        type = name,
        initialBalanceCent = 0,
        isEnabled = true,
        createdAt = now,
        updatedAt = now,
    )
}
