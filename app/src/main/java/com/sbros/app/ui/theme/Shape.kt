package com.sbros.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),      // Чипы
    medium = RoundedCornerShape(20.dp),     // Текстовые поля / карточки полок
    large = RoundedCornerShape(24.dp),      // Кнопки, FAB, док
    extraLarge = RoundedCornerShape(28.dp)  // Центральная карточка фокуса
)

val BottomSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
