package pt.isec.amov.cityfix.model

import java.text.Collator
import java.util.Locale

/**
 * Gestor da lista de incidentes da aplicação CityFix.
 * Permite armazenar, adicionar, pesquisar e ordenar incidentes.
 */
class IncidentsList(initialIncidents: List<Incident> = emptyList()) {

    // Lista mutável interna que armazena os incidentes de forma privada
    private val _incidents = initialIncidents.toMutableList()

    /**
     * Retorna todos os incidentes ordenados alfabeticamente pelo título.
     * Utiliza um Collator com força PRIMARY para ignorar acentos e maiúsculas/minúsculas
     * (ex: 'á', 'A' e 'a' são tratados da mesma forma na ordenação).
     */
    fun getAllSorted(): List<Incident> {
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
        return _incidents.sortedWith { i1, i2 -> collator.compare(i1.title, i2.title) }
    }

    /**
     * Procura e retorna um incidente específico através do seu ID único.
     * Retorna null caso o incidente não seja encontrado.
     */
    fun findById(id: String): Incident? = _incidents.find { it.id == id }

    /**
     * Adiciona um novo incidente à lista interna.
     */
    fun add(incident: Incident) {
        _incidents.add(incident)
    }

    /**
     * Retorna uma cópia imutável (List) da lista interna atual.
     * Útil para expor os dados sem permitir modificações diretas na lista original.
     */
    fun rawList(): List<Incident> = _incidents.toList()
}
