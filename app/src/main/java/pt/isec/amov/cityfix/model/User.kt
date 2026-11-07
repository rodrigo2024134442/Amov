package pt.isec.amov.cityfix.model

/**
 * Perfil de um utilizador autenticado (enunciado, secção I). O anónimo não tem perfil:
 * é simplesmente quem ainda não iniciou sessão.
 */
enum class UserRole { CITIZEN, OPERATOR }

/**
 * O munícipe fica PENDING até um operador o validar pelo NIF; o operador fica logo ACTIVE.
 */
enum class UserStatus { PENDING, ACTIVE }

/**
 * Dados de um utilizador registado, guardados no Firestore em users/{uid}
 * (o uid é o identificador do Firebase Authentication).
 */
data class User(
    // Identificador do Firebase Authentication (também é o ID do documento)
    val uid: String,

    // Nome completo (não pode ser alterado depois do registo)
    val name: String,

    // E-mail de autenticação
    val email: String,

    // Número de telefone (9 dígitos)
    val phone: String,

    // NIF, único por tipo de perfil (não pode ser alterado depois do registo)
    val nif: String,

    // Munícipe ou operador
    val role: UserRole,

    // Pendente ou ativo
    val status: UserStatus,

    // Só munícipes: se quer a funcionalidade "Deteção de Impactos"
    val impactDetection: Boolean = false,

    // Endereço da fotografia de perfil no Storage (opcional)
    val photoUrl: String? = null,

    // Georreferenciação do munícipe (preenchida na aula de GPS)
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Data do registo (Unix Epoch em milissegundos)
    val createdAt: Long = System.currentTimeMillis()
)