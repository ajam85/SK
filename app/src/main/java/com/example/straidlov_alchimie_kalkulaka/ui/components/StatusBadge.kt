package com.example.straidlov_alchimie_kalkulaka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.straidlov_alchimie_kalkulaka.model.CombinationStatus

private data class BadgeStyle(
    val bgColor: Color,
    val textColor: Color,
    val icon: ImageVector,
    val label: String
)

@Composable
fun StatusBadge(
    status: CombinationStatus,
    modifier: Modifier = Modifier
) {
    val style = when (status) {
        CombinationStatus.UNTESTED -> BadgeStyle(
            bgColor = Color(0xFFE2E8F0),
            textColor = Color(0xFF475569),
            icon = Icons.Default.HelpOutline,
            label = "Netestováno"
        )
        CombinationStatus.NONFUNCTIONAL -> BadgeStyle(
            bgColor = Color(0xFFFEE2E2),
            textColor = Color(0xFFDC2626),
            icon = Icons.Default.Close,
            label = "Nefunkční recept"
        )
        CombinationStatus.FUNCTIONAL -> BadgeStyle(
            bgColor = Color(0xFFDCFCE7),
            textColor = Color(0xFF16A34A),
            icon = Icons.Default.CheckCircle,
            label = "Funkční recept"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(color = style.bgColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = style.label,
            tint = style.textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = style.label,
            color = style.textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
