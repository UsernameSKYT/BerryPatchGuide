package com.berry.patchguide.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val productDetails by viewModel.productDetails.collectAsStateWithLifecycle()
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    val isAdFree by viewModel.isAdFree.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.startConnection()
        onDispose {
            viewModel.endConnection()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("개발자 지원") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isConnected) {
                Text(
                    text = "결제 서비스 연결 중...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(32.dp)
                )
            } else {
                // 광고 제거 상태
                if (isAdFree) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "광고가 제거되었습니다!",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "개발자를 지원해 주셔서 감사합니다.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // 개발자 지원 설명
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "개발자에게 광고 수익이 가는 결제 시스템",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "이 앱은 물론 묶여 있지만, 개발자를 지원하고 싶으시다면 아래 옵션을 선택해 주세요. 모든 수익은 앱 개발 및 유지보수에 사용됩니다.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // 결제 옵션들
                productDetails.forEach { product ->
                    val isPurchased = purchases.any { it.products.contains(product.productId) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                product.description?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                product.oneTimePurchaseOfferDetails?.let { offer ->
                                    Text(
                                        text = "${offer.formattedPrice}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (isPurchased) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "구매 완료",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Button(
                                    onClick = {
                                        val activity = context as? android.app.Activity
                                        activity?.let {
                                            viewModel.launchBillingFlow(it, product)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("구매")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "개발자: Berry\n문의: berry@example.com",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
