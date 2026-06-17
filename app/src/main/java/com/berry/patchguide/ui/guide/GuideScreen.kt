package com.berry.patchguide.ui.guide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    outputPath: String? = null,
    appliedFormat: String? = null,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("패치 적용 가이드") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 완료 배너
            if (outputPath != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "패치 적용 완료",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = outputPath,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Text(
                text = "다음 단계",
                style = MaterialTheme.typography.titleMedium
            )

            // 에뮬레이터 사용 가이드
            ExpandableGuideCard(
                icon = Icons.Default.SportsEsports,
                title = "에뮬레이터에서 실행하기",
                subtitle = "RetroArch, Delta, PPSSPP 등"
            ) {
                GuideStep(step = "1", text = "Downloads/BerryPatchGuide/ 폴더에서 패치된 ROM 파일을 찾습니다.")
                GuideStep(step = "2", text = "에뮬레이터 앱을 실행합니다.")
                GuideStep(step = "3", text = "에뮬레이터의 '파일 열기' 또는 '게임 추가' 기능으로 패치된 ROM을 선택합니다.")
                GuideStep(step = "4", text = "게임이 정상적으로 실행되는지 확인합니다.")
                Spacer(modifier = Modifier.height(4.dp))
                InfoNote(text = "원본 ROM 파일은 변경되지 않았습니다. 문제가 생기면 원본으로 다시 시도하세요.")
            }

            // 기기 전송 가이드
            ExpandableGuideCard(
                icon = Icons.Default.PhoneAndroid,
                title = "다른 기기로 전송하기",
                subtitle = "PC, 닌텐도 DS, PSP 등"
            ) {
                GuideStep(step = "1", text = "파일 관리자 앱에서 Downloads/BerryPatchGuide/ 폴더를 엽니다.")
                GuideStep(step = "2", text = "패치된 ROM 파일을 선택하고 '공유' 또는 '전송'을 탭합니다.")
                GuideStep(step = "3", text = "Bluetooth, USB, 클라우드 등 원하는 방법으로 전송합니다.")
                GuideStep(step = "4", text = "대상 기기의 ROM 폴더에 파일을 복사합니다.")
                Spacer(modifier = Modifier.height(4.dp))
                InfoNote(text = "닌텐도 DS: /roms/nds/, PSP: /PSP/GAME/, GBA: /roms/gba/ 등 기기별 경로를 확인하세요.")
            }

            // 포맷별 안내
            if (appliedFormat != null) {
                ExpandableGuideCard(
                    icon = Icons.Default.Info,
                    title = "적용된 패치 형식: $appliedFormat",
                    subtitle = "형식별 특이사항"
                ) {
                    when (appliedFormat.uppercase()) {
                        "IPS" -> {
                            GuideNote(text = "IPS 패치는 오프셋 기반으로 ROM을 수정합니다. 원본 ROM 버전이 정확히 일치해야 합니다.")
                            GuideNote(text = "일부 IPS 패치는 헤더 제거(512바이트)가 필요할 수 있습니다.")
                        }
                        "UPS" -> {
                            GuideNote(text = "UPS 패치는 CRC32 체크섬으로 원본 ROM을 검증합니다. 원본 파일이 정확해야 합니다.")
                        }
                        "BPS" -> {
                            GuideNote(text = "BPS 패치는 CRC32 검증을 포함합니다. 원본 ROM과 출력 파일 모두 검증됩니다.")
                        }
                        "XDELTA" -> {
                            GuideNote(text = "xdelta/VCDIFF 패치는 바이너리 차분 방식입니다. 원본 ROM 버전이 정확히 일치해야 합니다.")
                        }
                        else -> {
                            GuideNote(text = "패치가 성공적으로 적용되었습니다. 에뮬레이터에서 정상 동작을 확인하세요.")
                        }
                    }
                }
            }

            // 주의사항
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "주의사항",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• 패치된 ROM의 배포는 저작권법에 위반될 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "• 본인이 소유한 게임의 ROM에만 패치를 적용하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "• 패치 파일 자체는 저작권 문제가 없으나, 원본 ROM은 직접 덤프하거나 합법적으로 취득해야 합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ExpandableGuideCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "접기" else "펼치기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun GuideStep(step: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.size(20.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GuideNote(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun InfoNote(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
