package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentsList

/**
 * ViewModel responsável pela gestão do estado e operações relacionadas com os incidentes
 * na aplicação CityFix. Faz a ponte entre o modelo de dados (IncidentsList) e a UI.
 */
class IncidentsViewModel : ViewModel() {

    // Instância do modelo de dados que gere a coleção de incidentes (inclui dados iniciais de exemplo)
    private val incidentsListModel = IncidentsList(
        listOf(
            Incident(
                title = "Buraco na via",
                description = "Buraco profundo na faixa direita",
                category = "Vias Públicas",
                situation = "Dano",
                scope = "Público",
                latitude = 40.205,
                longitude = -8.419,
                mediaPath = ""
            ),
            Incident(
                title = "Lâmpada fundida",
                description = "Poste de iluminação sem luz",
                category = "Iluminação",
                situation = "Avaria",
                scope = "Público",
                latitude = 40.206,
                longitude = -8.420,
                mediaPath = ""
            )
        )
    )

    // StateFlow mutável interno que armazena a lista atual de incidentes ordenada
    private val _incidentsList = MutableStateFlow(incidentsListModel.getAllSorted())

    /**
     * StateFlow público e imutável que expõe a lista de incidentes para observação reativa por parte da UI.
     */
    val incidentsList: StateFlow<List<Incident>> = _incidentsList.asStateFlow()

    /**
     * Procura e retorna um incidente específico através do seu ID único,
     * consultando o modelo subjacente.
     */
    fun getIncidentById(id: String): Incident? = incidentsListModel.findById(id)
}