package com.berry.patchguide.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.berry.patchguide.data.model.PatchItem
import com.berry.patchguide.data.model.PatchSource

@Composable
fun PatchCard(
    patch: PatchItem,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onApplyClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                // 카드 클릭 시 다운로드 URL로 이동
                patch.downloadUrl?.let { url ->
                    if (url.isNotBlank()) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                }
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = patch.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patch.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = patch.sourceEnum.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (patch.sourceEnum) {
                        PatchSource.MODDB -> MaterialTheme.colorScheme.primary
                        PatchSource.GAMEBANANA -> MaterialTheme.colorScheme.secondary
                        PatchSource.RHDN -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
                patch.author?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "by $it",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                patch.description?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 패치 적용 버튼
                IconButton(onClick = onApplyClick) {
                    Icon(
                        imageVector = Icons.Default.BuildCircle,
                        contentDescription = "패치 적용",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // 다운로드 버튼
                IconButton(
                    onClick = {
                        patch.downloadUrl?.let { url ->
                            if (url.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "다운로드",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 즐겨찾기 버튼
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
