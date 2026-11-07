package pt.isec.amov.cityfix.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import pt.isec.amov.cityfix.model.Category

/**
 * Categorias (tipologias de problemas) na coleção "categories" do Firestore.
 * Todos podem ler; só os operadores escrevem (regras do passo 4.k).
 */
class CategoriesRepository {

    private val categoriesCollection = Firebase.firestore.collection("categories")

    // Categorias criadas pelo botão "Criar categorias de exemplo" do ecrã de categorias
    private val defaultCategories = listOf(
        "Vias Públicas", "Água", "Iluminação", "Saneamento", "Recolha de Resíduos", "Sinalização"
    )

    /**
     * Todas as categorias, ordenadas pelo nome, em tempo real (para o operador).
     */
    fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val registration = categoriesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Erro a ler as categorias", error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.mapNotNull { it.toCategory() }?.sortedBy { it.name } ?: emptyList())
        }
        awaitClose { registration.remove() }
    }

    /**
     * Só as ativas: as únicas que o munícipe pode escolher (enunciado I.3.E).
     */
    fun observeActiveCategories(): Flow<List<Category>> =
        observeCategories().map { list -> list.filter { it.isActive } }

    /**
     * Cria ou atualiza uma categoria (o ID do documento é o ID da categoria).
     */
    fun saveCategory(category: Category) {
        categoriesCollection.document(category.id).set(category.toFirestoreMap())
    }

    /**
     * Ativa ou desativa. Uma categoria nunca se apaga: as ocorrências antigas continuam a usá-la.
     */
    fun setActive(categoryId: String, active: Boolean) {
        categoriesCollection.document(categoryId).update("isActive", active)
    }

    /**
     * Cria as categorias de exemplo (usado uma vez, com a coleção vazia).
     */
    fun createDefaults() {
        defaultCategories.forEach { saveCategory(Category(name = it)) }
    }

    companion object {
        private const val TAG = "CategoriesRepository"
    }
}