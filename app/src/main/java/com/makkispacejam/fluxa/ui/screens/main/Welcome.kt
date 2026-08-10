package com.makkispacejam.fluxa.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R


// Pantalla de Bienvenida
@Composable
fun FluxaWelcome(modifier: Modifier = Modifier, onNavigate: () -> Unit) {

    Surface(modifier = modifier.fillMaxSize().clipToBounds(),
        color = MaterialTheme.colorScheme.background) {

        Column(modifier = modifier.fillMaxSize().clipToBounds(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.fluxa_icon),
                contentDescription = null,
                modifier = Modifier.size(100.dp).padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(text = stringResource(R.string.welcome_tagline), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
            Button(onClick = onNavigate, shape = RoundedCornerShape(100.dp)) { Text(stringResource(R.string.welcome_next)) }
        }
    }

}

// Pantalla -- No Ads
@Composable
fun FluxaIntro(modifier: Modifier = Modifier, onNext: () -> Unit) {

    Surface(modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background) {

        Column(modifier = modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.fluxa_icon),
                contentDescription = null,
                modifier = Modifier.size(80.dp).padding(bottom = 24.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(text = stringResource(R.string.intro_no_ads_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.intro_no_ads_desc), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(40.dp))
            Button(onClick = onNext, shape = RoundedCornerShape(100.dp)) { Text(stringResource(R.string.welcome_next)) }
        }
    }
}

// Pantalla -- Sin subscripciones o pagos
@Composable
fun FluxaSubs(modifier: Modifier = Modifier, onNext: () -> Unit) {

    Surface(modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background) {

        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fluxa_icon),
                contentDescription = null,
                modifier = Modifier.size(80.dp).padding(bottom = 24.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = stringResource(R.string.intro_no_subs_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.intro_no_subs_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(onClick = onNext, shape = RoundedCornerShape(100.dp)) { Text(stringResource(R.string.intro_get_started)) }
        }
    }
}



