package pt.isec.amov.cityfix.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.model.Session
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.model.UserStatus
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.ImageUtils
import pt.isec.amov.cityfix.utils.Totp

/**
 * O NIF já está registado para este tipo de perfil.
 */
class NifAlreadyUsedException : Exception("NIF already used")

/**
 * Autenticação (Firebase Authentication) e perfis (coleção "users" do Firestore).
 *
 * Login em dois fatores, ambos gratuitos e sem e-mails nem SMS:
 *  1. e-mail e password (signInWithEmailAndPassword);
 *  2. código de 6 dígitos de uma app autenticadora (TOTP). O segredo partilhado com a app
 *     autenticadora é criado no registo e guardado em mfa/{uid}, que só o próprio pode ler.
 * O uid que já passou o 2.º fator fica guardado no telemóvel (SharedPreferences): a sessão mantém-se
 * em cache depois da autenticação inicial (enunciado IV) e o código só volta a ser pedido depois de sair.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepository(context: Context) {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")
    private val nifsCollection = db.collection("nifs")
    private val mfaCollection = db.collection("mfa")
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    // uid do utilizador que já passou o 2.º fator neste telemóvel (null = nenhum)
    private val mfaUid = MutableStateFlow(prefs.getString(KEY_MFA_UID, null))

    // A sessão vive enquanto a app vive (não depende de nenhum ecrã)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Utilizador do Firebase Authentication (null = sem sessão), sempre atualizado
    private val firebaseUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Estado da sessão, partilhado por todos os ViewModels.
     * Junta quatro fontes: o utilizador do Firebase Auth, o 2.º fator neste telemóvel,
     * se já existe app autenticadora configurada (mfa/{uid}) e o perfil (users/{uid}).
     */
    val session: StateFlow<Session> = firebaseUser
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                flowOf(Session.Guest)
            } else {
                val uid = firebaseUser.uid
                val email = firebaseUser.email ?: ""
                combine(mfaUid, observeMfaConfigured(uid), observeUser(uid)) { mfa, configured, user ->
                    when {
                        mfa != uid && configured == null -> Session.Loading
                        mfa != uid && !configured!! -> Session.MfaSetup(email)
                        mfa != uid -> Session.AwaitingMfa(email)
                        user == null -> Session.Loading
                        user.status == UserStatus.PENDING -> Session.PendingValidation(user)
                        else -> Session.Active(user)
                    }
                }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, Session.Loading)

    val currentUid: String?
        get() = auth.currentUser?.uid

    private fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val registration = usersCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Erro a ler o perfil", error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toUser())
        }
        awaitClose { registration.remove() }
    }

    // true/false = já tem (ou não) app autenticadora configurada; null = ainda a ler
    private fun observeMfaConfigured(uid: String): Flow<Boolean?> = callbackFlow {
        trySend(null)
        val registration = mfaCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Erro a ler o 2.º fator", error)
                return@addSnapshotListener
            }
            trySend(snapshot?.exists() == true)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Regista o utilizador: cria a conta, reserva o NIF e grava o perfil.
     * A reserva do NIF (nifs/{PERFIL}_{NIF}) e o perfil são gravados numa transação: se o NIF
     * já existir, nada é gravado e a conta acabada de criar é apagada.
     * Se foi escolhida uma fotografia, é enviada a seguir (sem ela o registo fica feito na mesma).
     * A seguir a sessão passa a MfaSetup (configurar a app autenticadora).
     */
    suspend fun register(user: User, password: String, photoPath: String?) {
        val result = auth.createUserWithEmailAndPassword(user.email, password).await()
        val uid = result.user!!.uid
        val nifRef = nifsCollection.document("${user.role.name}_${user.nif}")
        try {
            db.runTransaction { transaction ->
                if (transaction.get(nifRef).exists()) throw NifAlreadyUsedException()
                transaction.set(nifRef, mapOf("uid" to uid))
                transaction.set(usersCollection.document(uid), user.copy(uid = uid).toFirestoreMap())
            }.await()
        } catch (e: Exception) {
            result.user?.delete()?.await()
            throw e
        }
        if (photoPath != null) {
            try {
                uploadProfilePhoto(photoPath)
            } catch (e: Exception) {
                Log.w(TAG, "Registo feito, mas a fotografia não foi enviada", e)
            }
        }
    }

    /**
     * Fotografia de perfil: comprime (512 px), envia para users/{uid}/avatar.jpg no Storage,
     * grava o endereço no perfil e apaga a cópia local. Ao contrário das ocorrências (WorkManager),
     * é enviada logo: é uma imagem pequena e o utilizador está à espera no ecrã.
     */
    suspend fun uploadProfilePhoto(localPath: String) {
        val uid = currentUid ?: return
        val bytes = ImageUtils.compress(localPath, maxSide = 512, quality = 80) ?: return
        val ref = Firebase.storage.reference.child("users/$uid/avatar.jpg")
        ref.putBytes(bytes).await()
        val url = ref.downloadUrl.await().toString()
        usersCollection.document(uid).update("photoUrl", url).await()
        FileUtils.deleteFileFromPath(localPath)
    }

    /**
     * 1.º fator: e-mail e password. A seguir a sessão passa a AwaitingMfa (ou MfaSetup).
     */
    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    /**
     * Configuração do 2.º fator: só grava o segredo se o código da app autenticadora estiver certo
     * (prova que o utilizador o guardou bem). Devolve false se o código estiver errado.
     */
    suspend fun confirmMfaSetup(secret: String, code: String): Boolean {
        val uid = currentUid ?: return false
        if (!Totp.verify(secret, code)) return false
        mfaCollection.document(uid).set(mapOf("secret" to secret)).await()
        markMfaDone(uid)
        return true
    }

    /**
     * 2.º fator no login: compara o código com o segredo guardado em mfa/{uid}.
     */
    suspend fun verifyMfa(code: String): Boolean {
        val uid = currentUid ?: return false
        val secret = mfaCollection.document(uid).get().await().getString("secret") ?: return false
        if (!Totp.verify(secret, code)) return false
        markMfaDone(uid)
        return true
    }

    private fun markMfaDone(uid: String) {
        prefs.edit { putString(KEY_MFA_UID, uid) }
        mfaUid.value = uid
    }

    /**
     * Recuperação de credenciais por e-mail (o Firebase envia o link para definir uma password nova).
     */
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() {
        auth.signOut()
        prefs.edit { remove(KEY_MFA_UID) }
        mfaUid.value = null
    }

    /**
     * Edição do perfil: só os campos que o enunciado deixa alterar (não o nome, o NIF nem a localização).
     */
    fun updateProfile(phone: String, impactDetection: Boolean) {
        val uid = currentUid ?: return
        usersCollection.document(uid).update(mapOf("phone" to phone, "impactDetection" to impactDetection))
    }

    /**
     * Munícipes à espera de validação (para o operador).
     */
    fun observePendingCitizens(): Flow<List<User>> = callbackFlow {
        val registration = usersCollection
            .whereEqualTo("role", UserRole.CITIZEN.name)
            .whereEqualTo("status", UserStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Erro a ler os munícipes pendentes", error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toUser() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /**
     * O operador ativa o registo de um munícipe (enunciado I.3.D).
     */
    fun activateCitizen(uid: String) {
        usersCollection.document(uid).update("status", UserStatus.ACTIVE.name)
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_MFA_UID = "mfa_uid"
    }
}