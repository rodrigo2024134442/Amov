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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.isec.amov.cityfix.AppConfig
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

/**
 * O NIF já está registado para este tipo de perfil.
 */
class NifAlreadyUsedException : Exception("NIF already used")

/**
 * Autenticação (Firebase Authentication) e perfis (coleção "users" do Firestore).
 *
 * Login em dois fatores, ambos gratuitos:
 *  1. e-mail e password (signInWithEmailAndPassword);
 *  2. código de 6 dígitos enviado para o e-mail da conta (EmailCodeSender: coleção "mail" + extensão Trigger Email).
 *     O código é gerado no telemóvel e só fica guardado o resumo (SHA-256), com a validade
 *     (AppConfig.MFA_CODE_VALIDITY_MINUTES) e o nº de tentativas falhadas.
 * O uid que já passou o 2.º fator fica guardado no telemóvel (SharedPreferences): a sessão mantém-se
 * em cache depois da autenticação inicial (enunciado IV) e o código só volta a ser pedido depois de sair.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepository(context: Context) {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")
    private val nifsCollection = db.collection("nifs")
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val emailCodeSender = EmailCodeSender()

    // Evita enviar dois códigos ao mesmo tempo (por exemplo, se o ecrã rodar durante o envio)
    private val sendMutex = Mutex()

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
     * Junta três fontes: o utilizador do Firebase Auth, o 2.º fator neste telemóvel
     * e o perfil (users/{uid}).
     */
    val session: StateFlow<Session> = firebaseUser
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                flowOf(Session.Guest)
            } else {
                val uid = firebaseUser.uid
                val email = firebaseUser.email ?: ""
                combine(mfaUid, observeUser(uid)) { mfa, user ->
                    when {
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

    /**
     * Perfil de outro utilizador em tempo real (o operador vê os contactos do autor de uma ocorrência).
     * As regras só deixam o operador ler perfis alheios.
     */
    fun observeUserProfile(uid: String): Flow<User?> = observeUser(uid)

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

    /**
     * Regista o utilizador: cria a conta, reserva o NIF e grava o perfil.
     * A reserva do NIF (nifs/{PERFIL}_{NIF}) e o perfil são gravados numa transação: se o NIF
     * já existir, nada é gravado e a conta acabada de criar é apagada.
     * Se foi escolhida uma fotografia, é enviada a seguir (sem ela o registo fica feito na mesma).
     * A seguir a sessão passa a AwaitingMfa (código enviado por e-mail).
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
     * 1.º fator: e-mail e password. A seguir a sessão passa a AwaitingMfa.
     */
    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    /**
     * 2.º fator: gera um código de 6 dígitos e envia-o para o e-mail da conta.
     * Sem force, não faz nada se já houver um código válido para este utilizador
     * (o ecrã pode ser recriado, por exemplo ao rodar, sem enviar outro e-mail).
     * Lança uma exceção se o e-mail não for enviado.
     */
    suspend fun sendMfaCode(force: Boolean = false) = sendMutex.withLock {
        val user = auth.currentUser ?: return@withLock
        val email = user.email ?: return@withLock
        val now = System.currentTimeMillis()
        if (!force && prefs.getString(KEY_CODE_UID, null) == user.uid && prefs.getLong(KEY_CODE_EXPIRES, 0) > now) {
            return@withLock
        }
        val code = String.format(Locale.ROOT, "%06d", SecureRandom().nextInt(1_000_000))
        emailCodeSender.send(email, code, AppConfig.MFA_CODE_VALIDITY_MINUTES)
        prefs.edit {
            putString(KEY_CODE_UID, user.uid)
            putString(KEY_CODE_HASH, hash(user.uid, code))
            putLong(KEY_CODE_EXPIRES, now + AppConfig.MFA_CODE_VALIDITY_MINUTES * 60_000L)
            putInt(KEY_CODE_ATTEMPTS, 0)
        }
    }

    /**
     * 2.º fator: confirma o código recebido por e-mail. Devolve false se estiver errado,
     * se já tiver expirado ou se já houve demasiadas tentativas (é preciso pedir outro).
     */
    fun verifyMfa(code: String): Boolean {
        val uid = currentUid ?: return false
        val attempts = prefs.getInt(KEY_CODE_ATTEMPTS, 0)
        val valid = prefs.getString(KEY_CODE_UID, null) == uid &&
            prefs.getLong(KEY_CODE_EXPIRES, 0) > System.currentTimeMillis() &&
            attempts < AppConfig.MFA_MAX_ATTEMPTS
        if (!valid || prefs.getString(KEY_CODE_HASH, null) != hash(uid, code.trim())) {
            prefs.edit { putInt(KEY_CODE_ATTEMPTS, attempts + 1) }
            return false
        }
        clearCode()
        markMfaDone(uid)
        return true
    }

    // Resumo do código (com o uid), para não guardar o código em claro no telemóvel
    private fun hash(uid: String, code: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$uid:$code".toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun clearCode() {
        prefs.edit {
            remove(KEY_CODE_UID)
            remove(KEY_CODE_HASH)
            remove(KEY_CODE_EXPIRES)
            remove(KEY_CODE_ATTEMPTS)
        }
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
        // Este telemóvel deixa de receber as notificações push desta conta
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken()
        } catch (_: Exception) {
        }
        auth.signOut()
        prefs.edit { remove(KEY_MFA_UID) }
        clearCode()
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
        private const val KEY_CODE_UID = "mfa_code_uid"
        private const val KEY_CODE_HASH = "mfa_code_hash"
        private const val KEY_CODE_EXPIRES = "mfa_code_expires"
        private const val KEY_CODE_ATTEMPTS = "mfa_code_attempts"
    }
}