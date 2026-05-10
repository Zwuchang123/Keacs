package com.keacs.app.ui.components

import java.text.DecimalFormat

private val plainCentFormat = DecimalFormat("#0.##")
private val groupedCentFormat = DecimalFormat("#,##0.##")

fun formatCent(value: Long): String =
    plainCentFormat.format(value / 100.0)

fun formatGroupedCent(value: Long): String =
    groupedCentFormat.format(value / 100.0)
