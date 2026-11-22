package pt.isec.amov.cityfix.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Representa um incidente reportado na plataforma CityFix.
 * Esta classe armazena todos os detalhes necessários para a georreferenciação,
 * categorização, estado e ficheiros multimédia associados à ocorrência.
 */
@Serializable
data class Incident(
    // Identificador único do incidente (gerado automaticamente como UUID em formato String)
    val id: String = UUID.randomUUID().toString(),

    // Identificador do munícipe registado que submeteu o reporte
    val userId: String,

    // Título breve e descritivo do problema reportado
    val title: String,

    // Explicação detalhada com informações adicionais sobre a ocorrência
    val description: String,

    // Nome da categoria (Ex: "Vias Públicas", "Água", "Iluminação", "Saneamento"), escolhida entre as ativas
    val category: String,

    // Natureza do problema: Dano, Avaria, Destruição ou Roubo
    val situation: Situation,

    // Alcance da ocorrência: pública ou privada
    val scope: Scope,

    // Coordenadas GPS
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Endereço textual obtido por geocodificação inversa
    val address: String? = null,

    // Caminhos locais das imagens/vídeos anexados (o enunciado exige pelo menos um)
    val mediaPaths: List<String> = emptyList(),

    // Endereços das imagens no Firebase Storage (preenchidos pelo MediaUploadWorker depois do envio)
    val mediaUrls: List<String> = emptyList(),

    // Endereço da miniatura no Firebase Storage
    val thumbnailUrl: String? = null,

    // Estado atual no ciclo de vida do ticket
    val status: IncidentStatus = IncidentStatus.OPEN,

    // Carimbo de data/hora (Unix Epoch em milissegundos) registado no momento da criação
    val timestamp: Long = System.currentTimeMillis(),

    // Data/hora em que a ocorrência passou a "Resolvido" (utilizado para validar o limite de 5 dias para reabertura)
    val resolvedAt: Long? = null,

    // Histórico de mudanças de estado, por ordem (enunciado I.3.E: transição de estados com timestamp)
    val history: List<StatusChange> = emptyList(),

    // Fotografia comprovativa da resolução (enunciado II.4): caminho local no telemóvel do operador
    // e, depois do envio pelo ProofUploadWorker, o endereço no Firebase Storage
    val proofPath: String? = null,
    val proofUrl: String? = null,

    // Estado de sincronização com a nuvem (PENDING_SYNC enquanto só existe no dispositivo)
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,

    // Ocorrência de grupo (enunciado I.2, Obs.): outros munícipes que reportaram o mesmo problema
    // (mesma categoria e situação, a menos de 10 m) e passaram a acompanhar esta ocorrência
    val followers: List<String> = emptyList(),

    // Última mensagem trocada (quando e quem): serve para notificar a outra parte (enunciado III)
    val lastMessageAt: Long? = null,
    val lastMessageBy: String? = null,

    // Última vez que um operador entrou na geofence de 20 m desta ocorrência (enunciado I.3, Obs.)
    val operatorNearbyAt: Long? = null
) {
    // Número de munícipes que acompanham o problema (o autor mais os seguidores)
    val groupSize: Int
        get() = 1 + followers.size

    // true se a ocorrência agrega vários munícipes
    val isGroup: Boolean
        get() = followers.isNotEmpty()

    // Autor ou seguidor: quem deve ser notificado das mudanças desta ocorrência
    fun isFollowedBy(uid: String): Boolean = uid == userId || uid in followers

    // Imagens a mostrar: as da nuvem (visíveis em qualquer dispositivo) ou, antes do envio, as locais
    val displayMedia: List<String>
        get() = mediaUrls.ifEmpty { mediaPaths }

    // Miniatura a mostrar na lista: a da nuvem ou, antes do envio, a primeira imagem local
    val displayThumbnail: String?
        get() = thumbnailUrl ?: mediaPaths.firstOrNull()

    // Prova de resolução a mostrar: a da nuvem ou, antes do envio, a local
    val displayProof: String?
        get() = proofUrl ?: proofPath

    // Histórico completo para mostrar. As ocorrências criadas antes do histórico existir
    // não têm a entrada "Aberto": é acrescentada com a data de criação.
    val timeline: List<StatusChange>
        get() = if (history.firstOrNull()?.status == IncidentStatus.OPEN) history
        else listOf(StatusChange(IncidentStatus.OPEN, timestamp, userId)) + history
}