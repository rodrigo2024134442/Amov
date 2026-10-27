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

    // Área técnica do problema (Ex: "Vias Públicas", "Água", "Iluminação", "Saneamento")
    val category: String,

    // Natureza ou gravidade do sinistro (Ex: "Dano", "Avaria", "Destruição", "Roubo")
    val situation: String,

    // Âmbito territorial ou de propriedade da ocorrência ("Público" ou "Privado")
    val scope: String,

    // Coordenada de latitude para o mapeamento e cálculos de proximidade/raios
    val latitude: Double,

    // Coordenada de longitude para o mapeamento e cálculos de proximidade/raios
    val longitude: Double,

    // Caminho local ou URI de rede correspondente à imagem ou vídeo obrigatório do reporte
    val mediaPath: String,

    // Estado atual no ciclo de vida: "Aberto", "Pedido de Informações", "Em Resolução", "Resolvido", "Encerrado"
    val status: String = "Aberto",

    // Carimbo de data/hora (Unix Epoch em milissegundos) registado no momento da criação
    val timestamp: Long = System.currentTimeMillis(),

    // Data/hora em que a ocorrência passou a "Resolvido" (utilizado para validar o limite de 5 dias para reabertura)
    val resolvedAt: Long? = null
)