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

    // Coordenadas GPS (null até à aula de GPS, em que passam a ser preenchidas no registo)
    val latitude: Double? = null,
    val longitude: Double? = null,

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

    // Estado de sincronização com a nuvem (PENDING_SYNC enquanto só existe no dispositivo)
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC
) {
    // Imagens a mostrar: as da nuvem (visíveis em qualquer dispositivo) ou, antes do envio, as locais
    val displayMedia: List<String>
        get() = mediaUrls.ifEmpty { mediaPaths }

    // Miniatura a mostrar na lista: a da nuvem ou, antes do envio, a primeira imagem local
    val displayThumbnail: String?
        get() = thumbnailUrl ?: mediaPaths.firstOrNull()
}