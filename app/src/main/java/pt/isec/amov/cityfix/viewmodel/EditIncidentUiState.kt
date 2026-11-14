package pt.isec.amov.cityfix.viewmodel

import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation

/**
 * Tudo o que o ecrã de edição precisa de mostrar, num só objeto imutável:
 * os valores dos campos, os erros de validação e se está a gravar.
 * Os erros são IDs de strings.xml (R.string...), para a mensagem sair na língua do dispositivo.
 */
data class EditIncidentUiState(
    val incidentId: String? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val situation: Situation? = null,
    val scope: Scope = Scope.PUBLIC,
    val mediaPaths: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    val titleError: Int? = null,
    val descriptionError: Int? = null,
    val isSaving: Boolean = false
) {
    // Calculado: o FAB de gravar só fica ativo quando tudo está preenchido e sem erros
    // (o enunciado obriga a pelo menos uma imagem/vídeo)
    val isValid: Boolean
        get() = titleError == null && descriptionError == null &&
                title.isNotBlank() && description.isNotBlank() &&
                category.isNotBlank() && situation != null &&
                mediaPaths.isNotEmpty()

    // Quantas fotografias ainda se podem anexar
    val remainingMedia: Int
        get() = MAX_MEDIA - mediaPaths.size

    companion object {
        // Limite de fotografias por ocorrência (o mesmo do PickMultipleVisualMedia da aula P09)
        const val MAX_MEDIA = 3
    }
}