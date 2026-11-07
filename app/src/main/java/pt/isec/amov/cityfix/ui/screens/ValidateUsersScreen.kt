package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.ui.components.UserAvatar

/**
 * Validação de registos de munícipes pelo operador (enunciado I.3.D):
 * mostra o nome, o NIF e a georreferenciação, e ativa a conta.
 */
@Composable
fun ValidateUsersScreen(
    pending: List<User>,
    onActivate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pending.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_pending_users))
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pending, key = { it.uid }) { user ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserAvatar(name = user.name, photo = user.photoUrl, size = 48.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.name, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.label_nif, user.nif))
                        Text(
                            if (user.latitude != null && user.longitude != null)
                                stringResource(R.string.label_coordinates, user.latitude.toString(), user.longitude.toString())
                            else stringResource(R.string.no_coordinates),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(onClick = { onActivate(user.uid) }) { Text(stringResource(R.string.action_activate)) }
                }
            }
        }
    }
}